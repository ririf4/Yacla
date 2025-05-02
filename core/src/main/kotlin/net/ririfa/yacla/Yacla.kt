package net.ririfa.yacla

import net.ririfa.yacla.loader.ConfigLoaderBuilder
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
     *  //or, you can use the method-chain
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
}
