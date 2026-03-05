package net.ririfa.yacla.annotation

/**
 * Converts a YAML list to a Set. An empty collection is treated as null.
 *
 * YAML always parses sequences as lists. This annotation converts the resulting
 * collection to a Set and treats an empty collection as null (useful for nullable Set fields).
 *
 * Example:
 * ```kotlin
 * data class MyConfig(
 *     @SetOf val tags: Set<String>? = null
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class SetOf
