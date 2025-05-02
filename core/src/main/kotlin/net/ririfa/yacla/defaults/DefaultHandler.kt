package net.ririfa.yacla.defaults

/**
 * Defines a handler for parsing a string value into an object of a specific type.
 *
 * Implementations of this interface are responsible for converting a raw string value
 * (typically from a @Default annotation) into the desired target type.
 *
 * Example:
 * ```
 * DefaultHandlers.register(MyType::class.java) { raw, _ -> MyType.parse(raw) }
 * ```
 *
 * @see DefaultHandlers to register custom handlers
 */
@FunctionalInterface
fun interface DefaultHandler {
    /**
     * Parses the provided raw string into an object of the given type.
     *
     * @param raw the string value to parse
     * @param type the target type class
     * @return the parsed object, or null if parsing failed
     */
    fun parse(raw: String, type: Class<*>): Any?
}
