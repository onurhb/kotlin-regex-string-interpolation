package io.onurhb.regex

import io.onurhb.regex.exception.ParameterNotFoundException

fun main(args: Array<String>) {
    val pattern = "(?<year>\\d{4})-(?<month>\\d{1,2})(-(?<day>\\d{1,2}))?".toRegex()

    val match = pattern.matchEntire("1992-1-3")!!
    val result = RegexValueExtractor.extractValues(
        template = "year: {year}, month: {month}( and maybe day: {day})",
        groups = match.groups
    ) { parameter, value ->
        if (parameter.equals("month").or(parameter.equals("day"))) {
            if (value?.length == 1) "0$value"
            else value
        }
        else value
    }

    // prints: 'year: 1992, month: 01 and maybe day: 03'
    println(result)
}


object RegexValueExtractor {
    // Ignores escaped brackets
    private val bracketMatcher = "(?<!\\\\)([({])".toRegex()

    private const val PARENTHESES_OPEN = '('
    private const val BRACKET_CLOSE = '}'
    private const val PARENTHESES_CLOSE = ')'

    fun extractValues(
        template: String,
        groups: MatchGroupCollection,
        process: (parameter: String, value: String?) -> String? = { _, value -> value }
    ): String {
        var result = template
        do {
            result = extractNextParameter(result, groups, process = process).also {
                if (result.equals(it)) return result
            }
        } while (true)
    }

    private fun extractNextParameter(
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
        } catch (ex: IllegalArgumentException) {
            return if (optional) "" else throw ex
        }
    }

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
            template.replaceRange(
                parenthesesStart,
                offset + 1,
                extractNextParameter(substring, groups, optional = true, process = process)
            )
        } ?: template
    }

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
                    var parameter = ""
                    val default = if (s.contains(":")) {
                        s.split(":").let {
                            parameter = it.first()
                            it.last()
                        }
                    } else {
                        parameter = s
                        null
                    }

                    parameter to default
                }


            try {
                replaceFirstGroupElseDefault(parameters, groups) { parameter, value ->
                    template.replaceRange(
                        parameterStart,
                        offset + 1,
                        process(parameter, value)!!
                    )
                }
            } catch (ex: ParameterNotFoundException) {
                // Give user another chance
                val replc = process(ex.parameters.first(), null) ?: throw ex

                template.replaceRange(
                    parameterStart,
                    offset + 1,
                    replc
                )
            }

        } ?: template
    }

    internal fun replaceFirstGroupElseDefault(
        parameters: Map<String, String?>,
        groups: MatchGroupCollection,
        replace: (parameter: String, value: String?) -> String?
    ): String? {
        val undefinedParameters = mutableListOf<String>()

        for ((parameter, default) in parameters) {
            try {
                return replace(
                    parameter,
                    getGroupElseDefaultIfExists(parameter, groups, default)
                )
            } catch (ex: IllegalArgumentException) {
                undefinedParameters.add(parameter)
            }
        }

        throw ParameterNotFoundException(undefinedParameters)
    }

    private fun getGroupElseDefaultIfExists(
        parameter: String,
        groups: MatchGroupCollection,
        default: String?
    ): String {
        try {
            return groups[parameter]?.value!!
        } catch (ex: IllegalArgumentException) {
            default?.let { return default } ?: throw ex
        }
    }

    internal fun getClosingBracketIndex(string: String): Int? {
        string.withIndex().forEach { (index, c) ->
            if (c.equals(BRACKET_CLOSE)) {
                return index
            }
        }
        return null
    }

    internal fun getClosingParenthesesIndex(string: String): Int? {
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
