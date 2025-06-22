package net.ririfa.yacla.defaults

import kotlin.reflect.KClass

/**
 * Functional interface for converting a *raw* value (usually a `String` but any `Any?` is allowed)
 * into an instance of the requested Kotlin type.
 *
 * Implementations should **not** throw unless the value is inherently un‐parsable; callers treat
 * an exception as a hard failure during configuration loading.
 */
fun interface DefaultHandler {
    /**
     * @param raw  the raw value provided in the `@Default` annotation or parsed from file
     * @param type the *erased* Kotlin class representing the desired target type
     * @return a value compatible with [type]
     */
    fun parse(raw: Any?, type: KClass<*>): Any?
}
