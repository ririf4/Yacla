package net.ririfa.yacla.defaults

import kotlin.reflect.KClass

/**
 * Registry for [DefaultHandler]s keyed purely by [KClass].
 *
 * - Built‑in handlers are lazily registered at first access.
 * - Primitive vs wrapper classes are resolved transparently.
 * - Java‑based APIs can still register via `register(java.lang.Class)` using the provided overload.
 */
object DefaultHandlers {

    private val handlers: MutableMap<KClass<*>, DefaultHandler> = mutableMapOf()
    private var initialized = false

    /* ---------- Public API ---------- */

    fun register(type: KClass<*>, handler: DefaultHandler) {
        ensureInitialized()
        handlers[type] = handler
    }

    /** Convenience overload for Java callers */
    @JvmStatic
    fun register(type: Class<*>, handler: DefaultHandler) =
        register(type.kotlin, handler)

    fun get(type: KClass<*>): DefaultHandler? {
        ensureInitialized()
        // 1) Exact type
        handlers[type]?.let { return it }
        // 2) Primitive ↔ Wrapper resolution
        handlers[type.javaPrimitiveType?.kotlin]?.let { return it }
        handlers[type.javaObjectType.kotlin]?.let { return it }
        return null
    }

    /* ---------- Lazy initialisation ---------- */

    private fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            registerBuiltIns()
        }
    }

    private fun registerBuiltIns() {
        // String
        register(String::class) { raw: Any?, _: KClass<*> ->
            when (raw) {
                is String -> raw
                null -> ""
                else -> raw.toString()
            }
        }

        /* Boolean */
        val boolParser = DefaultHandler { raw: Any?, _: KClass<*> ->
            when (raw) {
                is Boolean -> raw
                is String -> raw.toBooleanStrictOrNull()
                    ?: error("Invalid boolean string: '$raw'")

                else -> error("Unsupported Boolean raw: $raw (${raw?.javaClass?.name})")
            }
        }
        register(Boolean::class, boolParser)
        register(Boolean::class.javaObjectType.kotlin, boolParser)

        /* Int */
        val intParser = DefaultHandler { raw, _ ->
            when (raw) {
                is Int -> raw
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull()
                    ?: error("Invalid int literal: '$raw'")

                else -> error("Unsupported Int raw: $raw (${raw?.javaClass?.name})")
            }
        }
        register(Int::class, intParser)
        register(Int::class.javaObjectType.kotlin, intParser)

        /* Long */
        val longParser = DefaultHandler { raw, _ ->
            when (raw) {
                is Long -> raw
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull()
                    ?: error("Invalid long literal: '$raw'")

                else -> error("Unsupported Long raw: $raw (${raw?.javaClass?.name})")
            }
        }
        register(Long::class, longParser)
        register(Long::class.javaObjectType.kotlin, longParser)

        /* Float */
        val floatParser = DefaultHandler { raw, _ ->
            when (raw) {
                is Float -> raw
                is Number -> raw.toFloat()
                is String -> raw.toFloatOrNull()
                    ?: error("Invalid float literal: '$raw'")

                else -> error("Unsupported Float raw: $raw (${raw?.javaClass?.name})")
            }
        }
        register(Float::class, floatParser)
        register(Float::class.javaObjectType.kotlin, floatParser)

        /* Double */
        val doubleParser = DefaultHandler { raw, _ ->
            when (raw) {
                is Double -> raw
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull()
                    ?: error("Invalid double literal: '$raw'")

                else -> error("Unsupported Double raw: $raw (${raw?.javaClass?.name})")
            }
        }
        register(Double::class, doubleParser)
        register(Double::class.javaObjectType.kotlin, doubleParser)
    }
}
