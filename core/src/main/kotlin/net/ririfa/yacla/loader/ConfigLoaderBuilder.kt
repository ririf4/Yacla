package net.ririfa.yacla.loader

import net.ririfa.yacla.LanguageOutputMode
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
     * Specifies the path to the resource file (or directory root for locale resolution)
     * bundled with the application. Used as a fallback or for initial creation.
     *
     * For locale-based loading, pass a directory root (e.g. `/assets/mymod/config/`)
     * and chain [pull] to resolve the locale-specific file.
     *
     * @param path Classpath-relative resource path (e.g., "/config.yml" or "/config/").
     */
    fun fromResource(path: String): ConfigLoaderBuilder<T>

    /**
     * Sets the file where the configuration will be loaded from or saved to.
     *
     * @param file Path to the config file.
     */
    fun toFile(file: Path): ConfigLoaderBuilder<T>

    /**
     * Resolves a locale-specific resource and sets the output path.
     *
     * Looks for `{resourceRoot}{locale}.{extension}` in the classpath, falling back to
     * `{resourceRoot}en.{extension}` by default. Requires [fromResource] to be called first
     * with a directory root path.
     *
     * @param locale the locale string, e.g. "ja_JP" or "en_US"
     * @param file the output path where the config file will be written
     */
    fun pull(locale: String, file: Path): ConfigLoaderBuilder<T>

    /**
     * Resolves a localized resource using the configured language.
     *
     * By default this uses the current JVM locale and falls back to `en`.
     * Requires [fromResource] to be called first with a directory root path.
     *
     * @param file the base output path for the selected config file
     */
    fun pull(file: Path): ConfigLoaderBuilder<T> {
        throw UnsupportedOperationException("Localized pull is not supported by this ConfigLoaderBuilder")
    }

    /**
     * Sets the language used when resolving localized resources.
     *
     * Values such as `ja`, `ja_JP`, and `ja-JP` are accepted.
     */
    fun language(language: String): ConfigLoaderBuilder<T> {
        throw UnsupportedOperationException("Language selection is not supported by this ConfigLoaderBuilder")
    }

    /**
     * Uses the current JVM locale when resolving localized resources.
     */
    fun systemLanguage(): ConfigLoaderBuilder<T> {
        throw UnsupportedOperationException("System language selection is not supported by this ConfigLoaderBuilder")
    }

    /**
     * Sets the fallback language used when the selected localized resource does not exist.
     *
     * Defaults to `en`.
     */
    fun defaultLanguage(language: String): ConfigLoaderBuilder<T> {
        throw UnsupportedOperationException("Default language selection is not supported by this ConfigLoaderBuilder")
    }

    /**
     * Selects whether localized resources are copied to a single file or one file per language.
     */
    fun languageOutputMode(mode: LanguageOutputMode): ConfigLoaderBuilder<T> {
        throw UnsupportedOperationException("Language output mode is not supported by this ConfigLoaderBuilder")
    }

    /**
     * Sets the languages copied when [languageOutputMode] is [LanguageOutputMode.MULTIPLE_FILES].
     */
    fun outputLanguages(vararg languages: String): ConfigLoaderBuilder<T> {
        throw UnsupportedOperationException("Multiple language output is not supported by this ConfigLoaderBuilder")
    }

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
