package net.ririfa.yacla.logger

/**
 * Interface for logging within the Yacla framework.
 *
 * This allows pluggable logging backends (e.g., SLF4J, simple console output).
 */
interface YaclaLogger {

    /**
     * Logs an informational message.
     *
     * @param message The message to log.
     */
    fun info(message: String)

    /**
     * Logs a warning message.
     *
     * @param message The message to log.
     */
    fun warn(message: String)

    /**
     * Logs an error message, optionally with a throwable.
     *
     * @param message The message to log.
     * @param throwable Optional exception to include in the log.
     */
    fun error(message: String, throwable: Throwable? = null)
}
