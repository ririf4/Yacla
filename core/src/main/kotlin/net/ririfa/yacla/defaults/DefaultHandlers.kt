package net.ririfa.yacla.defaults

import java.lang.reflect.Type

/**
 * A registry of [DefaultHandler] instances for different types.
 *
 * Supports generic types by using [Type] as the key (instead of Class<?>).
 * This allows registering handlers for e.g. List<String> separately from List<Int>.
 */
object DefaultHandlers {
    private val handlers = mutableMapOf<Type, DefaultHandler>()

    /**
     * Registers a [DefaultHandler] for the given [Type].
     *
     * If a handler is already registered for this type, it will be overwritten.
     */
    fun register(type: Type, handler: DefaultHandler) {
        handlers[type] = handler
    }

    /**
     * Registers a [DefaultHandler] for the given [Class].
     *
     * Provided for compatibility with non-generic types.
     */
    fun register(clazz: Class<*>, handler: DefaultHandler) {
        register(clazz as Type, handler)
    }

    /**
     * Retrieves the registered [DefaultHandler] for the given [Type], or null if none registered.
     */
    fun get(type: Type): DefaultHandler? = handlers[type]

    /**
     * Retrieves the registered [DefaultHandler] for the given [Class], or null if none registered.
     */
    fun get(clazz: Class<*>): DefaultHandler? = get(clazz as Type)

    /**
     * Registers default handlers for common primitive and wrapper types.
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
        register(Byte::class.java) { raw, _ -> raw.toByte() }
        register(java.lang.Byte::class.java) { raw, _ -> raw.toByte() }
        register(Short::class.java) { raw, _ -> raw.toShort() }
        register(java.lang.Short::class.java) { raw, _ -> raw.toShort() }
        register(Char::class.java) { raw, _ -> raw.single() }
        register(Character::class.java) { raw, _ -> raw.single() }
    }
}