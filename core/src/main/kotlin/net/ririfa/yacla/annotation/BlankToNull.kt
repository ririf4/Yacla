package net.ririfa.yacla.annotation

/**
 * Converts a blank or empty String field value to null.
 *
 * If the loaded value is a String that is blank (empty or whitespace-only),
 * it will be replaced with null. If the parameter has a Kotlin default value,
 * that default will be used instead.
 *
 * Example:
 * ```kotlin
 * data class MyConfig(
 *     @BlankToNull val token: String? = null
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlankToNull
