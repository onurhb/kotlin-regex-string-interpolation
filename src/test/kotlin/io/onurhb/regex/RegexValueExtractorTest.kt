package io.onurhb.regex

import io.onurhb.regex.RegexValueExtractor.extractValues
import io.onurhb.regex.RegexValueExtractor.getClosingBracketIndex
import io.onurhb.regex.RegexValueExtractor.getClosingParenthesesIndex
import io.onurhb.regex.RegexValueExtractor.replaceFirstGroupElseDefault
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RegexValueExtractorTest {

    @Test
    fun `extractValues extracts correctly`() {
        val template = "ignore(-{p0})"

        assertEquals(
            "ignore-a",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues ignores escaped parentheses`() {
        val template = "ignore\\(-{p9})"

        assertThrows<IllegalArgumentException> {
            extractValues(template, matchGroups)
        }
    }

    @Test
    fun `extractValues ignores escaped brackets`() {
        val template = "ignore\\(-\\{p9})"

        assertEquals(
            "ignore\\(-\\{p9})",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts and transforms correctly`() {
        val template = "ignore(-{p0})"
        val result = extractValues(template, matchGroups) { parameter, value ->
            if (parameter.equals("p0")) "processed"
            else value
        }
        assertEquals(
            "ignore-processed",
            result
        )
    }

    @Test
    fun `extractValues extracts and transforms undefined parameters correctly`() {
        val template = "ignore(-{p0})-{p9}"
        val result = extractValues(template, matchGroups) { parameter, value ->
            when {
                parameter.equals("p0") -> "processed"
                parameter.equals("p9") -> "processed"
                else -> value
            }
        }
        assertEquals(
            "ignore-processed-processed",
            result
        )
    }

    @Test
    fun `extractValues extracts correctly when optional parameter is not found`() {
        val template = "ignore(-{p9})"

        assertEquals(
            "ignore",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are multiple parameters`() {
        val template = "ignore-{p9|p0}"

        assertEquals(
            "ignore-a",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are multiple parameters when last has default`() {
        val template = "ignore-{p9|p10:default0}"

        assertEquals(
            "ignore-default0",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are optional of multiple parameters`() {
        val template = "ignore(-{p9|p10})"

        assertEquals(
            "ignore",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are optional of multiple parameters with last default`() {
        val template = "ignore(-{p9|p10:default})"

        assertEquals(
            "ignore-default",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues handles nested parentheses correctly`() {
        val template = "(ignore(-{p9}))"

        assertEquals(
            "ignore",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues handles nested parentheses correctly when parameter is found`() {
        val template = "(ignore(-{p0}))"

        assertEquals(
            "ignore-a",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues handles nested parentheses correctly and returns null if nothing is found`() {
        val template = "((-{p9}))"

        assertEquals(
            "",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters`() {
        val template = "{p0}-{p1}-{p2}-{p3}"

        assertEquals(
            "a-b-c-d",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with last as optional that does not exist`() {
        val template = "{p0}-{p1}-{p2}(-{p9})"

        assertEquals(
            "a-b-c",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with last as optional`() {
        val template = "{p0}-{p1}-{p2}(-{p3})"

        assertEquals(
            "a-b-c-d",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with nested optional`() {
        val template = "{p0}-{p1}(-{p9}(-{p3}))"

        assertEquals(
            "a-b",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with nested optional 2`() {
        val template = "{p0}-{p1}(-{p3}(-{p9}))"

        assertEquals(
            "a-b-d",
            extractValues(template, matchGroups)
        )
    }

    @Test
    fun `getSubstringUntilNextClosingParentheses returns correct string 1`() {
        val input = "(text-(some) text))-some text()"
        assertEquals(
            17,
            getClosingParenthesesIndex(input)
        )
    }

    @Test
    fun `getSubstringUntilNextClosingParentheses returns correct string 2`() {
        val input = "(text-(some) t)ext))"
        assertEquals(
            14,
            getClosingParenthesesIndex(input)
        )
    }

    @Test
    fun `getSubstringUntilNextClosingParentheses returns null if closing parentheses is not found`() {
        val input = "(text-(some) text"
        assertNull(
            getClosingParenthesesIndex(input)
        )
    }

    @Test
    fun `getClosingBracketIndex should return correct index`() {
        val input = "{param0}"
        assertEquals(
            7,
            getClosingBracketIndex(input)
        )
    }

    @Test
    fun `getClosingBracketIndex should return null if not found`() {
        val input = "{param0"
        assertNull(
            getClosingBracketIndex(input)
        )
    }

    @Test
    fun `getFirstGroupElseDefault callbacks first parameter with default`() {
        val parameters = mapOf(
            "p9" to null,
            "p8" to "default0"
        )

        val value = replaceFirstGroupElseDefault(parameters, matchGroups) { parameter, value ->
            assertEquals("p8", parameter)
            assertEquals("default0", value)
            "value0"
        }

        assertEquals("value0", value)
    }

    @Test
    fun `getFirstGroupElseDefault throws if all parameters are undefined`() {
        val parameters = mapOf(
            "p9" to null,
            "p8" to null
        )

        assertThrows<Exception> {
            replaceFirstGroupElseDefault(parameters, matchGroups) { _, value -> value }
        }
    }

    companion object {
        private val matchGroups = createMatchGroup()

        private fun createMatchGroup(): MatchGroupCollection {
            val regex = "(?<p0>a)(?<p1>b)(?<p2>c)(?<p3>d)(?<p4>e)"
            val text = "abcde"
            return regex.toRegex().matchEntire(text)!!.groups
        }
    }
}
