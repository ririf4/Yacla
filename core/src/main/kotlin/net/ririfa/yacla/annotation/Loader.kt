package net.ririfa.yacla.annotation

import net.ririfa.yacla.loader.FieldLoader
import kotlin.reflect.KClass

/**
 * Specifies a custom [FieldLoader] for this field.
 *
 * The referenced class must implement [FieldLoader] and have a no-argument constructor.
 * It is instantiated via reflection and its [FieldLoader.load] method is called
 * with the raw parsed value before any built-in type coercion.
 *
 * Example:
 * ```kotlin
 * class MyCustomLoader : FieldLoader {
 *     override fun load(raw: Any?): Any? = raw?.toString()?.trim()
 * }
 *
 * data class MyConfig(
 *     @Loader(MyCustomLoader::class) val value: String? = null
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Loader(val value: KClass<out FieldLoader>)
