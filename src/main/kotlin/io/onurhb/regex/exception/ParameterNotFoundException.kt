package io.onurhb.regex.exception

data class ParameterNotFoundException(val parameters: List<String>) : RuntimeException(
    "Could not find value for parameter(s): '${parameters.joinToString(", ")}'"
) {
    constructor(parameter: String) : this(listOf(parameter))
}
