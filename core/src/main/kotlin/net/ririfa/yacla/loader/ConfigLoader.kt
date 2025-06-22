package net.ririfa.yacla.loader

/**
 * Represents a loaded configuration file mapped to a Kotlin or Java object.
 *
 * Provides access to the deserialized configuration object and lifecycle operations
 * such as reloading, validation, filling default values, and updating the file
 * from the default resource if outdated.
 *
 * This interface is typically obtained by calling [ConfigLoaderBuilder.load].
 *
 * Example usage:
 * ```
 * val loader = Yacla.loader<MyConfig>()
 *     .fromResource("/defaults/config.yml")
 *     .toFile(Paths.get("config.yml"))
 *     .parser(SnakeYamlParser())
 *     .withLogger(ConsoleLogger())
 *     .autoUpdateIfOutdated(true)
 *     .load()
 *
 * loader.validate()
 * loader.nullCheck()
 * val config = loader.config
 * ```
 *
 * @param T the type of the configuration object
 */
interface ConfigLoader<T : Any> {
    /**
     * The loaded configuration object.
     */
    val config: T

    /**
     * Reloads the configuration object from the backing file.
     *
     * This discards the current in-memory config object and replaces it
     * with a fresh parse of the file.
     */
    fun reload(): ConfigLoader<T>

    /**
     * Validates the configuration object based on field annotations such as [net.ririfa.yacla.annotation.Required]
     * and [net.ririfa.yacla.annotation.Range].
     *
     * Throws an exception if validation fails.
     */
    fun validate(): ConfigLoader<T>

    /**
     * Attempts to update the configuration file by merging the default resource
     * with the current file, preserving user-defined values.
     *
     * Uses an [UpdateStrategy] registered for the parser type to perform the update.
     *
     * This method is automatically called during [ConfigLoaderBuilder.load] if
     * [ConfigLoaderBuilder.autoUpdateIfOutdated] was enabled.
     *
     * It can also be called manually to trigger an update check at any time.
     */
    fun updateConfig(): Boolean
}
