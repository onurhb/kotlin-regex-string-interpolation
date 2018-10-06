package io.onurhb.regex

import io.onurhb.regex.RegexValueExtractor.extractValues
import io.onurhb.regex.RegexValueExtractor.getClosingBracketIndex
import io.onurhb.regex.RegexValueExtractor.getClosingParenthesesIndex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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
    fun `extractValues extracts and transforms correctly`() {
        val template = "ignore(-{p0})"
        val result = extractValues(template, matchGroups) { parameter, value ->
            when {
                parameter.equals("p0") -> "processed"
                else -> value
            }
        }
        assertEquals(
            "ignore-processed",
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

    companion object {
        private val matchGroups = createMatchGroup()

        private fun createMatchGroup(): MatchGroupCollection {
            val regex = "(?<p0>a)(?<p1>b)(?<p2>c)(?<p3>d)(?<p4>e)"
            val text = "abcde"
            return regex.toRegex().matchEntire(text)!!.groups
        }
    }
}