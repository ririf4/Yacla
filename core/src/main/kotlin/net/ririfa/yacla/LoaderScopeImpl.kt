package net.ririfa.yacla

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.JLoaderScope
import net.ririfa.yacla.loader.impl.DefaultConfigLoaderBuilder
import kotlin.reflect.KClass

/**
 * Internal implementation of the [LoaderScope] and [JLoaderScope] interfaces.
 *
 * This class provides methods to create and configure configuration loaders for a specific
 * class type. It applies the shared [LoaderSettings] provided during instantiation to each
 * loader, ensuring a consistent set of defaults such as the parser, logger, and auto-update behavior.
 *
 * The implementation supports both Kotlin DSL style with lambda blocks and Java-compatible
 * functional-style builder configuration.
 *
 * @constructor Creates a new instance of [LoaderScopeImpl] using the provided [LoaderSettings].
 * The settings define shared defaults for all loaders created within this scope.
 *
 * @param settings Shared configuration settings for all loaders created using this scope.
 *
 * @see LoaderSettings
 * @see ConfigLoader
 * @see ConfigLoaderBuilder
 */
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
