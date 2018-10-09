package io.onurhb.regex

import io.onurhb.regex.RegexValueExtractor.interpolateString
import io.onurhb.regex.exception.ParameterNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RegexValueExtractorTest {

    @Test
    fun `extractValues extracts correctly`() {
        val template = "ignore(-{p0})"

        assertEquals(
            "ignore-a",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues ignores escaped parentheses`() {
        val template = "ignore\\(-{p9})"

        assertThrows<ParameterNotFoundException> {
            interpolateString(template, matchGroups)
        }
    }

    @Test
    fun `extractValues ignores escaped brackets`() {
        val template = "ignore\\(-\\{p9})"

        assertEquals(
            "ignore\\(-\\{p9})",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts and transforms correctly`() {
        val template = "ignore(-{p0})"
        val result = interpolateString(template, matchGroups) { parameter, value ->
            if (parameter.equals("p0")) null
            else value
        }
        assertEquals(
            "ignore-processed",
            result
        )
    }

    @Test
    fun `extractValues extracts and transforms undefined parameters correctly`() {
        val template = "ignore-{p0:*}"
        val result = interpolateString(template, matchGroups) { parameter, value ->
            if (parameter.equals("p0")) null
            else value
        }
        assertEquals(
            "ignore-*",
            result
        )
    }

    @Test
    fun `extractValues extracts and transforms undefined parameters correctly 2`() {
        val template = "ignore-{p0:*}"
        val result = interpolateString(template, matchGroups) { parameter, value ->
            if (parameter.equals("p0")) "processed"
            else value
        }
        assertEquals(
            "ignore-processed",
            result
        )
    }

    @Test
    fun `extractValues extracts and transforms undefined parameters correctly 3`() {
        val template = "ignore-{p9|p0:*}"
        val result = interpolateString(template, matchGroups) { parameter, value ->
            if (parameter.equals("p0")) "processed"
            else value
        }
        assertEquals(
            "ignore-processed",
            result
        )
    }

    @Test
    fun `extractValues extracts and transforms undefined parameters correctly 4`() {
        val template = "ignore-{p9|p10:*}"
        val result = interpolateString(template, matchGroups) { parameter, value ->
            if (parameter.equals("p10") || parameter.equals("p9")) null
            else value
        }
        assertEquals(
            "ignore-*",
            result
        )
    }

    @Test
    fun `extractValues extracts and transforms undefined parameters correctly 5`() {
        val template = "ignore-{p9|p10:*}"
        val result = interpolateString(template, matchGroups) { parameter, value ->
            when {
                parameter.equals("p9") -> "p09"
                parameter.equals("p10") -> "p010"
                else -> value
            }
        }
        assertEquals(
            "ignore-p09",
            result
        )
    }

    @Test
    fun `extractValues extracts correctly when optional parameter is not found`() {
        val template = "ignore(-{p9})"

        assertEquals(
            "ignore",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are multiple parameters`() {
        val template = "ignore-{p9|p0}"

        assertEquals(
            "ignore-a",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are multiple parameters when last has default`() {
        val template = "ignore-{p9|p10:default0}"

        assertEquals(
            "ignore-default0",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are optional of multiple parameters`() {
        val template = "ignore(-{p9|p10})"

        assertEquals(
            "ignore",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues extracts correctly when there are optional of multiple parameters with last default`() {
        val template = "ignore(-{p9|p10:default})"

        assertEquals(
            "ignore-default",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues handles nested parentheses correctly`() {
        val template = "(ignore(-{p9}))"

        assertEquals(
            "ignore",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues handles nested parentheses correctly when parameter is found`() {
        val template = "(ignore(-{p0}))"

        assertEquals(
            "ignore-a",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues handles nested parentheses correctly and returns null if nothing is found`() {
        val template = "((-{p9}))"

        assertEquals(
            "",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters`() {
        val template = "{p0}-{p1}-{p2}-{p3}"

        assertEquals(
            "a-b-c-d",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with last as optional that does not exist`() {
        val template = "{p0}-{p1}-{p2}(-{p9})"

        assertEquals(
            "a-b-c",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with last as optional`() {
        val template = "{p0}-{p1}-{p2}(-{p3})"

        assertEquals(
            "a-b-c-d",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with nested optional`() {
        val template = "{p0}-{p1}(-{p9}(-{p3}))"

        assertEquals(
            "a-b",
            interpolateString(template, matchGroups)
        )
    }

    @Test
    fun `extractValues return correct when there are multiple parameters with nested optional 2`() {
        val template = "{p0}-{p1}(-{p3}(-{p9}))"

        assertEquals(
            "a-b-d",
            interpolateString(template, matchGroups)
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
