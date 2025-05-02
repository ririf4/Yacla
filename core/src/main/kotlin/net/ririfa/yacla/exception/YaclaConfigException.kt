package net.ririfa.yacla.exception

/**
 * Thrown when configuration loading or parsing fails.
 */
class YaclaConfigException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)