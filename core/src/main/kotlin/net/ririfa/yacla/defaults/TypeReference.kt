package net.ririfa.yacla.defaults

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Captures a generic [Type] token for runtime use.
 *
 * Example:
 * ```
 * val type = object : TypeReference<List<String>>() {}.type
 * ```
 */
abstract class TypeReference<T> {
    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
}