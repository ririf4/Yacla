package net.ririfa.yacla

data class Type<T>(
    val value: T,
    val isMissing: Boolean = false,
)