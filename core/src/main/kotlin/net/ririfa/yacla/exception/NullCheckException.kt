package net.ririfa.yacla.exception

/**
 * Exception thrown when a null check fails.
 */
class NullCheckException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)