package net.ririfa.yacla.annotation

/**
 * Specifies a default value to be used for a field when the parsed configuration
 * does not provide a value or the value is null/empty.
 *
 * The value will be parsed as a string and converted to the field's type using
 * the registered default handlers in Yacla.
 *
 * Example:
 * ```
 * @Default("8080")
 * int port;
 * ```
 *
 * @property value the default value as a string representation
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Default(val value: String)
