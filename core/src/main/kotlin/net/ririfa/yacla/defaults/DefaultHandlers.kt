package net.ririfa.yacla.defaults

object DefaultHandlers {
    private val handlers = mutableMapOf<Class<*>, DefaultHandler>()

    fun register(type: Class<*>, handler: DefaultHandler) {
        handlers[type] = handler
    }

    fun get(type: Class<*>): DefaultHandler? = handlers[type]

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
