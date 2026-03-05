package net.ririfa.yacla.annotation

/**
 * Logs a warning via the configured [net.ririfa.yacla.logger.YaclaLogger] when this field is null.
 *
 * If no logger is configured, the warning is silently suppressed.
 *
 * Example:
 * ```kotlin
 * data class MyConfig(
 *     @Warn("API token is not set — features requiring authentication will be disabled.")
 *     val token: String? = null
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Warn(val message: String)
