package net.ririfa.yacla.annotation

/**
 * Validates that a String value is not blank when present.
 *
 * Missing nullable values remain null. Use a non-nullable parameter without a
 * default value when the field must be present.
 *
 * Example:
 * ```kotlin
 * data class MyConfig(
 *     @NotBlank val apiKey: String? = null
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotBlank
