package net.ririfa.yacla

import net.ririfa.yacla.annotation.JLoaderScope
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.impl.DefaultConfigLoaderBuilder
import kotlin.reflect.KClass

internal class LoaderScopeImpl(
    private val settings: LoaderSettings
) : LoaderScope, JLoaderScope {

    override fun <T : Any> loader(
        clazz: KClass<T>,
        block: ConfigLoaderBuilder<T>.() -> Unit
    ): ConfigLoader<T> {
        return build(clazz.java, block)
    }

    override fun <T : Any> loader(
        clazz: Class<T>,
        builder: java.util.function.Consumer<ConfigLoaderBuilder<T>>
    ): ConfigLoader<T> {
        return build(clazz) { builder.accept(this) }
    }

    private fun <T : Any> build(
        clazz: Class<T>,
        block: ConfigLoaderBuilder<T>.() -> Unit
    ): ConfigLoader<T> {
        return DefaultConfigLoaderBuilder(clazz)
            .withDefaults(settings)
            .apply(block)
            .load()
    }
}
