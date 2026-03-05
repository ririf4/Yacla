package net.ririfa.yacla.annotation

/**
 * Converts a list of Strings to a Set of Enum values (case-insensitive).
 *
 * The enum type is inferred from the type parameter of the field (e.g., `Set<MyEnum>`).
 * Strings that do not match any enum constant are silently dropped.
 *
 * Example:
 * ```kotlin
 * enum class Mode { READ, WRITE, ADMIN }
 *
 * data class MyConfig(
 *     @EnumSet val modes: Set<Mode>? = null
 * )
 * ```
 *
 * YAML:
 * ```yaml
 * modes:
 *   - read
 *   - write
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnumSet
