package net.ririfa.yacla

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import kotlin.reflect.KClass

interface LoaderScope {
    fun <T : Any> loader(
        clazz: KClass<T>,
        block: ConfigLoaderBuilder<T>.() -> Unit
    ): ConfigLoader<T>
}