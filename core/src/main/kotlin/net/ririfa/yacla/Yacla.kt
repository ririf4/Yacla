package net.ririfa.yacla

import net.ririfa.yacla.Yacla.fillByDefault
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.JLoaderScope
import net.ririfa.yacla.loader.impl.DefaultConfigLoaderBuilder
import net.ririfa.yacla.schema.FieldDefBuilder
import net.ririfa.yacla.schema.FieldDefinition
import net.ririfa.yacla.schema.YaclaSchema
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

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

    /**
     * Creates a new config instance using only defaults.
     *
     * - If a field has a default in the given [schema], that value is used.
     * - Otherwise, the data class primary constructor's default parameter is used (if any).
     *
     * No file I/O or parsing is performed.
     */
    @JvmStatic
    inline fun <reified T : Any> fillByDefault(schema: YaclaSchema<T>): T =
        fillByDefault(T::class.java, schema)

    /**
     * Java-friendly overload of [fillByDefault].
     */
    @JvmStatic
    fun <T : Any> fillByDefault(clazz: Class<T>, schema: YaclaSchema<T>): T {
        val kClazz = clazz.kotlin

        // 1) Build field definitions from schema
        val builder = FieldDefBuilder<T>()
        schema.configure(builder)
        val defs: Map<String, FieldDefinition> = builder.build()

        // 2) Data class primary constructor path
        val ctor = kClazz.primaryConstructor
            ?: error("Config class ${clazz.simpleName} must have a primary constructor for fillByDefault()")

        val args = mutableMapOf<KParameter, Any?>()

        ctor.parameters.forEach { p ->
            val name = p.name ?: return@forEach
            val def = defs[name]

            // If schema has a default, use it.
            if (def != null && def.defaultValue !== FieldDefinition.NO_DEFAULT) {
                args[p] = def.defaultValue
            }
            // else: do not put anything -> Kotlin will fall back to the
            // primary constructor's default argument, if present.
        }

        return ctor.callBy(args)
    }
}
