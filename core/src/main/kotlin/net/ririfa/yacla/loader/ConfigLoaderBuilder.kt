package net.ririfa.yacla.loader

import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.nio.file.Path

/**
 * Builder interface for configuring and creating a [ConfigLoader] instance.
 *
 * Provides a fluent API for specifying where to load the configuration from,
 * which parser to use, and logging and auto-update behaviors.
 */
interface ConfigLoaderBuilder<T : Any> {

    /**
     * Specifies the path to the resource file bundled with the application.
     * Used as a fallback or for initial creation.
     *
     * @param path Classpath-relative resource path (e.g., "/config.yml").
     */
    fun fromResource(path: String): ConfigLoaderBuilder<T>

    /**
     * Sets the file where the configuration will be loaded from or saved to.
     *
     * @param file Path to the config file.
     */
    fun toFile(file: Path): ConfigLoaderBuilder<T>

    /**
     * Sets the parser used to read and write the config file.
     *
     * @param parser Implementation of [ConfigParser].
     */
    fun parser(parser: ConfigParser): ConfigLoaderBuilder<T>

    /**
     * Sets the logger used for informational and error messages.
     *
     * @param logger Implementation of [YaclaLogger].
     */
    fun withLogger(logger: YaclaLogger): ConfigLoaderBuilder<T>

    /**
     * Enables or disables automatic config updates based on resource version.
     *
     * @param enabled True to enable, false to disable.
     */
    fun autoUpdateIfOutdated(enabled: Boolean): ConfigLoaderBuilder<T>

    /**
     * Builds and loads the config, returning a [ConfigLoader] instance.
     *
     * @return Loaded config handler.
     */
    fun load(): ConfigLoader<T>
}
