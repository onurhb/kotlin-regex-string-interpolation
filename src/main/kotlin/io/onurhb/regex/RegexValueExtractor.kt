package io.onurhb.regex

import io.onurhb.regex.exception.ParameterNotFoundException

object RegexValueExtractor {
    // Ignores escaped brackets
    private val bracketMatcher = "(?<!\\\\)([({])".toRegex()

    private const val PARENTHESES_OPEN = '('
    private const val BRACKET_CLOSE = '}'
    private const val PARENTHESES_CLOSE = ')'

    /**
     * Interpolate string like '{p0}(-{p1:p2:*})' with values from Regex match
     */
    fun interpolateString(
        template: String,
        groups: MatchGroupCollection,
        process: (parameter: String, value: String?) -> String? = { _, value -> value }
    ): String {
        var result = template
        do {
            result = replaceNextParameter(result, groups, process = process).also {
                if (result.equals(it)) return result
            }
        } while (true)
    }

    /**
     * Will replace the next parameter in '{p0}-{p1}' (which is 'p0')
     */
    private fun replaceNextParameter(
        template: String,
        groups: MatchGroupCollection,
        optional: Boolean = false,
        process: (parameter: String, value: String?) -> String?
    ): String {
        val bracketMatched = bracketMatcher.find(template) ?: return template
        try {
            bracketMatched.value.let { match ->
                val matchStart = bracketMatched.range.start

                return if (match.equals(PARENTHESES_OPEN.toString())) {
                    handleParentheses(template, matchStart, groups, process)
                } else {
                    replaceNextParameter(template, matchStart, groups, process)
                }
            }
        } catch (ex: ParameterNotFoundException) {
            // Set everything to empty if substring is optional
            return if (optional) "" else throw ex
        }
    }

    /**
     * Finds corresponding closing parentheses and runs interpolating recursively inside it
     * This will also handle nested parentheses
     */
    private fun handleParentheses(
        template: String,
        parenthesesStart: Int,
        groups: MatchGroupCollection,
        process: (parameter: String, value: String?) -> String?
    ): String {
        return getClosingParenthesesIndex(
            template.substring(
                parenthesesStart,
                template.length
            )
        )?.let { index ->
            val offset = parenthesesStart + index
            val substring = template.substring(parenthesesStart + 1, offset)

            // Recursively interpolate this substring which is wrapped with parentheses
            template.replaceRange(
                parenthesesStart,
                offset + 1,
                replaceNextParameter(substring, groups, optional = true, process = process)
            )
        } ?: template
    }

    /**
     * Finds the next parameter and replaces it with match value
     * If it can't find the value, then it will try to get the processed value from callback,
     * or use the default if exists.
     *
     * Parameters will be iterated from left to right: '{p0|p1|p2}' until a value is found
     * @throws ParameterNotFoundException if parameter value is null
     */
    private fun replaceNextParameter(
        template: String,
        parameterStart: Int,
        groups: MatchGroupCollection,
        process: (parameter: String, value: String?) -> String?
    ): String {
        return getClosingBracketIndex(
            template.substring(
                parameterStart,
                template.length
            )
        )?.let { index ->
            val offset = parameterStart + index

            // 'param0|param1:default1' becomes '{ param0 to null, param1 to default1 }'
            val parameters = template
                .substring(parameterStart + 1, offset)
                .split("|")
                .associate { s ->
                    var parameter = s
                    val default = if (s.contains(":")) {
                        s.split(":").let {
                            parameter = it.first()
                            it.last()
                        }
                    } else null

                    parameter to default
                }

            getFirstParameterValue(parameters, groups, process) { parameter, value ->
                // Parameter should always contain non null value
                if (value == null) {
                    throw ParameterNotFoundException(parameter)
                }

                // Replace parameter
                template.replaceRange(parameterStart, offset + 1, value)
            }
        } ?: template
    }

    /**
     * Will try to lookup all parameters in order and callback the first match which is not null
     */
    private fun getFirstParameterValue(
        // List of parameters and defaults, normally only the last parameter has a default
        parameters: Map<String, String?>,
        groups: MatchGroupCollection,
        // Check if user has provided value for this parameter, will use that if not null
        process: (parameter: String, value: String?) -> String?,
        // Callback to replaceNextParameter() so it can actually replace the value
        callback: (parameter: String, value: String?) -> String?
    ): String? {
        val undefinedParameters = mutableListOf<String>()

        for ((parameter, default) in parameters) {
            val value = getGroupValue(parameter, groups)
            val maybeProcessed: String? = process(parameter, value)

            maybeProcessed?.let { return callback(parameter, it) }
            default?.let { return callback(parameter, it) }

            // Meaning that the value was not processed and no default was provided
            undefinedParameters.add(parameter)
        }

        throw ParameterNotFoundException(undefinedParameters)
    }

    /**
     * @return a group value from Regex match, return null if group is missing or value is null
     */
    private fun getGroupValue(parameter: String, groups: MatchGroupCollection): String? {
        return try {
            groups[parameter]?.value
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    /**
     * @return closing bracket index
     * @return closing bracket index
     */
    private fun getClosingBracketIndex(string: String): Int? {
        string.withIndex().forEach { (index, c) ->
            if (c.equals(BRACKET_CLOSE)) {
                return index
            }
        }
        return null
    }

    /**
     * @return closing parentheses index
     */
    private fun getClosingParenthesesIndex(string: String): Int? {
        var index = 1
        var count = 0

        for (c in string.substring(1)) {
            if (c.equals(PARENTHESES_CLOSE)) {
                if (count-- == 0) {
                    break
                }
            } else if (c.equals(PARENTHESES_OPEN)) {
                count++
            }
            index++
        }

        return if (count < 0) index else null
    }
}
