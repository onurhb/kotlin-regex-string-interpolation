package io.onurhb.regex.exception

data class ParameterNotFoundException(val parameters: List<String>): RuntimeException(
    "Could not find any of parameters: $parameters in groups"
)
