package io.onurhb.regex

object RegexValueExtractor {

    // Ignores escaped brackets
    private val bracketMatcher = "(?<!\\\\)([({])".toRegex()

    private const val PARENTHESES_OPEN = '('
    private const val BRACKET_CLOSE = '}'
    private const val PARENTHESES_CLOSE = ')'

    fun extractValues(
        template: String,
        groups: MatchGroupCollection,
        process: (parameter: String, value: String) -> String = { _, value -> value!! }
    ): String {
        var result = template
        do {
            result = extractRecursively(result, groups, process = process).also {
                if (result.equals(it)) return result
            }
        } while (true)
    }

    private fun extractRecursively(
        template: String,
        groups: MatchGroupCollection,
        optional: Boolean = false,
        process: (parameter: String, value: String) -> String
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
        process: (parameter: String, value: String) -> String
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
                extractRecursively(substring, groups, optional = true, process = process)
            )
        } ?: template
    }

    private fun replaceNextParameter(
        template: String,
        parameterStart: Int,
        groups: MatchGroupCollection,
        process: (parameter: String, value: String) -> String
    ): String {
        return getClosingBracketIndex(
            template.substring(
                parameterStart,
                template.length
            )
        )?.let { index ->
            val offset = parameterStart + index
            val parameter = template.substring(parameterStart + 1, offset)

            groups[parameter]?.value?.let { value ->
                template.replaceRange(
                    parameterStart,
                    offset + 1,
                    process(parameter, value)
                )
            }
        } ?: template
    }

    fun getClosingBracketIndex(string: String): Int? {
        string.withIndex().forEach { (index, c) ->
            if (c.equals(BRACKET_CLOSE)) {
                return index
            }
        }
        return null
    }

    fun getClosingParenthesesIndex(string: String): Int? {
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