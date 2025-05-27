package net.ririfa.yacla.annotation

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import java.util.function.Consumer

@FunctionalInterface
interface JLoaderScope {
    fun <T : Any> loader(
        clazz: Class<T>,
        builder: Consumer<ConfigLoaderBuilder<T>>
    ): ConfigLoader<T>
}