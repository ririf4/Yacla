package net.ririfa.yacla

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.JLoaderScope
import net.ririfa.yacla.loader.impl.DefaultConfigLoaderBuilder

/**
 * Yacla - Yet Another Config Loading API
 *
 * Entry point to create and configure loaders that map configuration files
 * into Kotlin or Java objects, with support for validation, logging, and auto-update.
 */
object Yacla {

    /**
     * Creates a configuration loader for the specified type using Kotlin's reified generics.
     *
     * Example (Kotlin - Method Chain):
     * ```kotlin
     * val loader: ConfigLoader<AppConfig> = Yacla.loader<AppConfig>()
     *     .fromResource("/defaults/config.yml")
     *     .toFile(Paths.get("config.yml"))
     *     .parser(SnakeYamlParser())
     *     .autoUpdateIfOutdated(true)
     *     .withLogger(ConsoleLogger())
     *     .load()
     *
     * val config: AppConfig = loader.config
     * ```
     *
     * @return a [ConfigLoaderBuilder] for type [T].
     */
    @JvmStatic
    inline fun <reified T : Any> loader(): ConfigLoaderBuilder<T> = loader(T::class.java)

    /**
     * Creates a configuration loader for the specified class.
     *
     * Example (Java):
     * ```java
     * ConfigLoader<AppConfig> loader = Yacla.loader(AppConfig.class)
     *     .fromResource("/defaults/config.yml")
     *     .toFile(Paths.get("config.yml"))
     *     .parser(new SnakeYamlParser())
     *     .autoUpdateIfOutdated(true)
     *     .withLogger(new ConsoleLogger())
     *     .load();
     *
     * AppConfig config = loader.get();
     * ```
     *
     * @param clazz the target class of the configuration data.
     * @return a [ConfigLoaderBuilder] for type [T].
     */
    @JvmStatic
    fun <T : Any> loader(clazz: Class<T>): ConfigLoaderBuilder<T> = DefaultConfigLoaderBuilder(clazz)

    /**
     * Creates and configures a loader in a DSL-style block.
     *
     * Example (Kotlin DSL):
     * ```kotlin
     * val loader = Yacla.loader<AppConfig> {
     *     fromResource("/defaults/config.yml")
     *     toFile(Paths.get("config.yml"))
     *     parser(SnakeYamlParser())
     *     autoUpdateIfOutdated(true)
     *     withLogger(ConsoleLogger())
     * }.load()
     *  //or, you can use the method chain
     *
     * val config: AppConfig = loader.config
     * ```
     *
     * @param clazz the target class of the configuration data.
     * @param builder lambda to customize the [ConfigLoaderBuilder].
     * @return the configured [ConfigLoaderBuilder].
     */
    @JvmStatic
    fun <T : Any> loader(
        clazz: Class<T>,
        builder: ConfigLoaderBuilder<T>.() -> Unit
    ): ConfigLoaderBuilder<T> = DefaultConfigLoaderBuilder(clazz).apply(builder)

    /**
     * Creates a DSL scope that applies the provided default settings to all loaders defined inside.
     *
     * This version is Kotlin-idiomatic, allowing `loader<ConfigClass> { ... }` syntax with receiver scoping.
     *
     * Example (Kotlin DSL):
     * ```kotlin
     * val config = Yacla.withDefaults(sharedSettings) {
     *     loader<MainConfig> {
     *         fromResource("/defaults/main.yml")
     *         toFile(Paths.get("config/main.yml"))
     *     }.config
     * }
     * ```
     *
     * @param settings common loader defaults such as parser, logger, and auto-update flag
     * @param block a lambda with [LoaderScope] receiver to define one or more loaders
     * @return the result returned from the provided block
     */
    @JvmStatic
    fun <R> withDefaults(
        settings: LoaderSettings,
        block: LoaderScope.() -> R
    ): R {
        return LoaderScopeImpl(settings).block()
    }

    /**
     * Creates a functional scope for defining multiple loaders with common defaults, for use in Java.
     *
     * This method enables Java-friendly loader definition using lambdas or anonymous classes.
     *
     * Example (Java):
     * ```java
     * LoaderSettings settings = new LoaderSettings(parser, logger, true);
     *
     * MainConfig config = Yacla.withDefaults(settings, scope -> {
     *     ConfigLoader<MainConfig> loader = scope.loader(MainConfig.class, b -> {
     *         b.fromResource("/defaults/main.yml");
     *         b.toFile(Paths.get("config/main.yml"));
     *     });
     *     return loader.getConfig();
     * });
     * ```
     *
     * @param settings common loader defaults such as parser, logger, and auto-update flag
     * @param block function receiving a [JLoaderScope] for defining one or more loaders
     * @return the result returned from the provided function
     */
    @JvmStatic
    fun <R> withDefaults(
        settings: LoaderSettings,
        block: java.util.function.Function<JLoaderScope, R>
    ): R {
        return block.apply(LoaderScopeImpl(settings))
    }

    /**
     * Creates a config loader for the specified class within a [LoaderScope], applying shared defaults.
     *
     * This extension is meant for use within `Yacla.withDefaults` DSL blocks.
     *
     * Example:
     * ```kotlin
     * val loader = loader<MyConfig> {
     *     fromResource("/defaults/my.yml")
     *     toFile(Paths.get("config/my.yml"))
     * }
     * ```
     *
     * @param block configuration lambda for customizing the [ConfigLoaderBuilder]
     * @return the loaded [ConfigLoader] for the specified type [T]
     */
    inline fun <reified T : Any> LoaderScope.loader(
        noinline block: ConfigLoaderBuilder<T>.() -> Unit
    ): ConfigLoader<T> {
        return loader(T::class, block)
    }
}
