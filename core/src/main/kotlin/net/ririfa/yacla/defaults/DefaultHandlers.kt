package net.ririfa.yacla.defaults

/**
 * A registry of [DefaultHandler] instances for different types.
 *
 * This object holds the mapping of supported types to their corresponding parsers
 * used when applying default values via the [net.ririfa.yacla.annotation.Default] annotation.
 *
 * The registry includes built-in handlers for common primitive types (String, Boolean, Int, etc.)
 * and allows registering additional custom handlers for other types.
 *
 * Example:
 * ```
 * DefaultHandlers.register(MyType::class.java) { raw, _ -> MyType.parse(raw) }
 * ```
 */
object DefaultHandlers {
    private val handlers = mutableMapOf<Class<*>, DefaultHandler>()

    /**
     * Registers a [DefaultHandler] for the given type.
     *
     * If a handler is already registered for this type, it will be overwritten.
     *
     * @param type the target type to associate with the handler
     * @param handler the handler instance responsible for parsing the type
     */
    fun register(type: Class<*>, handler: DefaultHandler) {
        handlers[type] = handler
    }

    /**
     * Retrieves the registered [DefaultHandler] for the given type, or null if none registered.
     *
     * @param type the target type to look up
     * @return the handler for the type, or null if not registered
     */
    fun get(type: Class<*>): DefaultHandler? = handlers[type]

    /**
     * Registers the default handlers for common primitive types.
     *
     * This method is called automatically when [net.ririfa.yacla.loader.impl.DefaultConfigLoaderBuilder] is initialized.
     */
    fun registerDefaults() {
        register(String::class.java) { raw, _ -> raw }
        register(java.lang.Boolean::class.java) { raw, _ -> raw.toBoolean() }
        register(Boolean::class.java) { raw, _ -> raw.toBoolean() }
        register(Int::class.java) { raw, _ -> raw.toInt() }
        register(Integer::class.java) { raw, _ -> raw.toInt() }
        register(Long::class.java) { raw, _ -> raw.toLong() }
        register(java.lang.Long::class.java) { raw, _ -> raw.toLong() }
        register(Float::class.java) { raw, _ -> raw.toFloat() }
        register(java.lang.Float::class.java) { raw, _ -> raw.toFloat() }
        register(Double::class.java) { raw, _ -> raw.toDouble() }
        register(java.lang.Double::class.java) { raw, _ -> raw.toDouble() }
    }
}
