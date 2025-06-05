package net.ririfa.yacla.annotation

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import java.util.function.Consumer

/**
 * Java-compatible DSL scope interface for defining configuration loaders within
 * a shared default context using [net.ririfa.yacla.Yacla.withDefaults].
 *
 * This interface allows Java developers to define one or more configuration loaders
 * that share a common [net.ririfa.yacla.LoaderSettings], such as a parser, logger, or auto-update flag.
 *
 * Kotlin users should prefer [net.ririfa.yacla.LoaderScope] which provides a more idiomatic DSL.
 *
 * Example usage (Java):
 * ```java
 * LoaderSettings settings = new LoaderSettings(new YamlParser(), new ConsoleLogger(), true);
 *
 * MainConfig config = Yacla.withDefaults(settings, scope -> {
 *     ConfigLoader<MainConfig> loader = scope.loader(MainConfig.class, b -> {
 *         b.fromResource("/defaults/config.yml");
 *         b.toFile(Paths.get("config.yml"));
 *     });
 *     return loader.getConfig();
 * });
 * ```
 */
@FunctionalInterface
interface JLoaderScope {
    /**
     * Creates and loads a configuration for the given class using a builder consumer.
     *
     * @param clazz the class representing the configuration type
     * @param builder a consumer to configure the [ConfigLoaderBuilder]
     * @return a fully loaded [ConfigLoader] instance
     */
    fun <T : Any> loader(
        clazz: Class<T>,
        builder: Consumer<ConfigLoaderBuilder<T>>
    ): ConfigLoader<T>
}
