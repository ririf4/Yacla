package net.ririfa.yacla.annotation

/**
 * Specifies the field name to be used for mapping a Java Record component
 * when parameter names are not available at runtime.
 *
 * This annotation is useful for environments where the Java compiler was invoked
 * without the `-parameters` option, causing constructor parameter names
 * to be omitted from the class file.
 *
 * Example:
 * ```
 * record MyConfig(
 *     @NamedRecord("some_field") String someValue,
 *     @NamedRecord("another_field") int anotherValue
 * ) {}
 * ```
 *
 * When parsing YAML, the parser will look up `some_field` and `another_field`
 * from the YAML data instead of relying on the parameter names.
 *
 * @property value the field name to map this constructor parameter to
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class NamedRecord(val value: String)
