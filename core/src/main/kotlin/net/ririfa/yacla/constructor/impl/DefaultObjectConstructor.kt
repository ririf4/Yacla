package net.ririfa.yacla.constructor.impl

import net.ririfa.yacla.constructor.ObjectConstructor
import net.ririfa.yacla.exception.YaclaConfigException
import kotlin.reflect.full.primaryConstructor

/**
 * Default implementation of [ObjectConstructor] using Kotlin reflection.
 *
 * Uses primary constructor for Kotlin data classes.
 */
class DefaultObjectConstructor : ObjectConstructor {
    override fun <T : Any> construct(map: Map<String, Any?>, clazz: Class<T>): T {
        val ctor = clazz.kotlin.primaryConstructor
            ?: throw YaclaConfigException("Class ${clazz.simpleName} lacks primary constructor")

        val args = ctor.parameters.associateWith { param ->
            map.entries.find { it.key.equals(param.name, ignoreCase = true) }?.value
        }

        return ctor.callBy(args)
    }
}