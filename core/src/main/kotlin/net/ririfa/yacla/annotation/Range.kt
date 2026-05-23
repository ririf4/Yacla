package net.ririfa.yacla.annotation

/**
 * Validates that a numeric field is within the given inclusive range.
 *
 * The annotation applies to Byte, Short, Int, Long, Float, and Double values.
 *
 * Example:
 * ```kotlin
 * data class MyConfig(
 *     @Range(min = 1, max = 65535) val port: Int = 8080
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Range(
    val min: Long = Long.MIN_VALUE,
    val max: Long = Long.MAX_VALUE
)
