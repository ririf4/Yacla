package net.ririfa.yacla

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import kotlin.reflect.KClass

/**
 * DSL scope interface used within [Yacla.withDefaults] blocks.
 *
 * This interface provides a way to define one or more configuration loaders
 * that share a common set of defaults (parser, logger, auto-update settings).
 *
 * Usage typically looks like:
 * ```kotlin
 * Yacla.withDefaults(settings) {
 *     loader<MyConfig> {
 *         fromResource("/defaults/my.yml")
 *         toFile(Paths.get("config/my.yml"))
 *     }
 * }
 * ```
 *
 * Implementations like [LoaderScopeImpl] internally apply the shared settings
 * and handle the loading lifecycle automatically.
 */
interface LoaderScope {
    /**
     * Creates and loads a configuration for the given class using the provided block.
     *
     * @param clazz the configuration class to load
     * @param block the builder DSL for setting file, parser, logger, etc.
     * @return a fully loaded [ConfigLoader] instance
     */
    fun <T : Any> loader(
        clazz: KClass<T>,
        block: ConfigLoaderBuilder<T>.() -> Unit
    ): ConfigLoader<T>
}
