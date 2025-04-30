package net.ririfa.yacla

import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.impl.DefaultConfigLoaderBuilder

/**
 * Yacla - Yet Another Config Loading API
 *
 * Provides entry points to load configuration files into Kotlin or Java objects
 * with optional validation, logging, and auto-update support.
 */
object Yacla {

    /**
     * Creates a loader for the specified class using Kotlin's reified type.
     *
     * @return ConfigLoaderBuilder<T> for the reified type.
     */
    @JvmStatic
    inline fun <reified T : Any> loader(): ConfigLoaderBuilder<T> {
        return loader(T::class.java)
    }

    /**
     * Creates a loader for the specified class.
     *
     * @param clazz Class of the target config data class.
     * @return ConfigLoaderBuilder<T>
     */
    @JvmStatic
    fun <T : Any> loader(clazz: Class<T>): ConfigLoaderBuilder<T> {
        return DefaultConfigLoaderBuilder(clazz)
    }

    /**
     * Creates a loader and applies configuration via DSL-style builder.
     *
     * @param clazz Class of the target config data class.
     * @param builder Lambda to configure the loader.
     * @return ConfigLoaderBuilder<T>
     */
    @JvmStatic
    fun <T : Any> loader(clazz: Class<T>, builder: ConfigLoaderBuilder<T>.() -> Unit): ConfigLoaderBuilder<T> {
        return DefaultConfigLoaderBuilder(clazz).apply(builder)
    }
}