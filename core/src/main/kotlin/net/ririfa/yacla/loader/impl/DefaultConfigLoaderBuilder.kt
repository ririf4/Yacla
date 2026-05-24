package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.LanguageOutputMode
import net.ririfa.yacla.LanguageSettings
import net.ririfa.yacla.LoaderSettings
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.notExists

/**
 * Default implementation of [ConfigLoaderBuilder].
 *
 * This builder creates a [ConfigLoader] for the given configuration class,
 * allowing fluent configuration of the input resource, target file,
 * parser, logger, and update behavior.
 *
 * Typically used internally via [net.ririfa.yacla.Yacla.loader], rather than instantiated directly.
 *
 * @param T the type of the configuration object
 */
class DefaultConfigLoaderBuilder<T : Any>(
    private val clazz: Class<T>
) : ConfigLoaderBuilder<T> {
    private var resourcePath: String? = null
    private var targetFile: Path? = null
    private var parser: ConfigParser? = null
    private var logger: YaclaLogger? = null
    private var autoUpdate = false
    private var language = LanguageSettings.systemLanguage()
    private var defaultLanguage = "en"
    private var languageOutputMode = LanguageOutputMode.SINGLE_FILE
    private var outputLanguages = emptySet<String>()
    private var localizedResourceRoot: String? = null
    private var localizedOutputBaseFile: Path? = null
    private var activeLanguage: String? = null

    override fun fromResource(path: String): ConfigLoaderBuilder<T> = apply {
        this.resourcePath = path
    }

    override fun toFile(file: Path): ConfigLoaderBuilder<T> = apply {
        this.targetFile = file
    }

    /**
     * Resolves a locale-specific resource file and sets the output path.
     *
     * Given a resource root directory (set via [fromResource]), this method tries:
     * 1. `{root}{locale}.{extension}`
     * 2. `{root}{language}.{extension}`
     * 3. `{root}en.{extension}` (fallback by default)
     *
     * If neither is found, an [IllegalStateException] is thrown.
     * The resolved resource path and the given [file] are stored for use during [load].
     *
     * Example:
     * ```kotlin
     * Yacla.loader<MyConfig>()
     *     .fromResource("/assets/mymod/config/")
     *     .pull("ja_JP", configFile)
     *     .parser(YamlParser())
     *     .load()
     * ```
     *
     * @param locale the locale string (e.g. "ja_JP", "en")
     * @param file the output path where the resolved resource will be copied
     */
    override fun pull(locale: String, file: Path): ConfigLoaderBuilder<T> = apply {
        language(locale)
        pull(file)
    }

    override fun pull(file: Path): ConfigLoaderBuilder<T> = apply {
        localizedResourceRoot = resourcePath
            ?: throw IllegalStateException("fromResource() must be called before pull()")
        localizedOutputBaseFile = file
    }

    override fun parser(parser: ConfigParser): ConfigLoaderBuilder<T> = apply {
        this.parser = parser
    }

    override fun withLogger(logger: YaclaLogger): ConfigLoaderBuilder<T> = apply {
        this.logger = logger
    }

    override fun autoUpdateIfOutdated(enabled: Boolean): ConfigLoaderBuilder<T> = apply {
        this.autoUpdate = enabled
    }

    override fun language(language: String): ConfigLoaderBuilder<T> = apply {
        this.language = normalizeLanguage(language)
    }

    override fun systemLanguage(): ConfigLoaderBuilder<T> = apply {
        this.language = LanguageSettings.systemLanguage()
    }

    override fun defaultLanguage(language: String): ConfigLoaderBuilder<T> = apply {
        this.defaultLanguage = normalizeLanguage(language)
    }

    override fun languageOutputMode(mode: LanguageOutputMode): ConfigLoaderBuilder<T> = apply {
        this.languageOutputMode = mode
    }

    override fun outputLanguages(vararg languages: String): ConfigLoaderBuilder<T> = apply {
        this.outputLanguages = languages.mapTo(linkedSetOf()) { normalizeLanguage(it) }
    }

    fun withDefaults(defaults: LoaderSettings): ConfigLoaderBuilder<T> = apply {
        if (parser == null) {
            parser = defaults.parser
        }
        if (logger == null && defaults.logger != null) {
            logger = defaults.logger
        }
        autoUpdate = defaults.autoUpdate
        language = defaults.languageSettings.language
        defaultLanguage = defaults.languageSettings.defaultLanguage
        languageOutputMode = defaults.languageSettings.outputMode
        outputLanguages = defaults.languageSettings.outputLanguages
    }

    override fun load(): ConfigLoader<T> {
        if (localizedResourceRoot != null) {
            prepareLocalizedOutput()
        }

        Objects.requireNonNull(resourcePath, "Resource path is not set")
        Objects.requireNonNull(targetFile, "Target file is not set")
        Objects.requireNonNull(parser, "Parser is not set")

        if (targetFile!!.notExists()) {
            logger?.info("Config file not found. Copying from resource: $resourcePath")
            val resourceStream: InputStream =
                javaClass.getResourceAsStream(resourcePath!!)
                    ?: throw IllegalStateException("Resource $resourcePath not found in classpath")
            resourceStream.use { Files.copy(it, targetFile!!) }
        }

        return DefaultConfigLoader(
            clazz = clazz,
            parser = parser!!,
            file = targetFile!!,
            logger = logger,
            resourcePath = resourcePath!!
        ).also { loader ->
            if (autoUpdate) {
                if (loader.updateConfig()) {
                    loader.reload()
                }
            }
        }
    }

    private fun prepareLocalizedOutput() {
        val parser = parser ?: throw IllegalStateException("Parser must be set before loading localized resources")
        val root = normalizedResourceRoot(localizedResourceRoot!!)
        val baseFile = localizedOutputBaseFile!!

        val selected = resolveLocalizedResource(root, language, parser)
            ?: resolveLocalizedResource(root, defaultLanguage, parser)
            ?: throw missingLocalizedResource(root, listOf(language, defaultLanguage), parser)

        when (languageOutputMode) {
            LanguageOutputMode.SINGLE_FILE -> {
                resourcePath = selected.path
                targetFile = baseFile
                activeLanguage = selected.language
            }

            LanguageOutputMode.MULTIPLE_FILES -> {
                val languagesToOutput = buildSet {
                    addAll(outputLanguages)
                    add(language)
                    add(defaultLanguage)
                }
                var selectedTargetFile: Path? = null

                for (candidateLanguage in languagesToOutput) {
                    val resolved = resolveLocalizedResource(root, candidateLanguage, parser)
                        ?: fallbackLocalizedResource(root, candidateLanguage, parser)
                        ?: continue
                    val outputFile = localizedFile(baseFile, resolved.language, parser)
                    copyResourceIfMissing(resolved.path, outputFile)
                    if (resolved.language == selected.language) {
                        selectedTargetFile = outputFile
                    }
                }

                resourcePath = selected.path
                targetFile = selectedTargetFile ?: localizedFile(baseFile, selected.language, parser)
                activeLanguage = selected.language
            }
        }

        logger?.info("Resolved localized config language '$activeLanguage' from resource: $resourcePath")
    }

    private fun fallbackLocalizedResource(
        root: String,
        requestedLanguage: String,
        parser: ConfigParser
    ): LocalizedResource? {
        if (requestedLanguage == defaultLanguage) return null
        return resolveLocalizedResource(root, defaultLanguage, parser)
    }

    private fun copyResourceIfMissing(resourcePath: String, targetFile: Path) {
        if (targetFile.notExists()) {
            logger?.info("Config file not found. Copying from resource: $resourcePath")
            val resourceStream = javaClass.getResourceAsStream(resourcePath)
                ?: throw IllegalStateException("Resource $resourcePath not found in classpath")
            resourceStream.use { Files.copy(it, targetFile) }
        }
    }

    private fun resolveLocalizedResource(
        root: String,
        language: String,
        parser: ConfigParser
    ): LocalizedResource? {
        val normalizedLanguage = normalizeLanguage(language)
        for (candidateLanguage in languageCandidates(normalizedLanguage)) {
            for (extension in parser.supportedExtensions) {
                val path = "$root$candidateLanguage.$extension"
                if (javaClass.getResource(path) != null) {
                    return LocalizedResource(candidateLanguage, path)
                }
            }
        }
        return null
    }

    private fun languageCandidates(language: String): List<String> {
        val normalized = normalizeLanguage(language)
        val baseLanguage = normalized.substringBefore('_')
        return linkedSetOf(normalized, baseLanguage).toList()
    }

    private fun normalizedResourceRoot(root: String): String {
        return if (root.endsWith("/")) root else "$root/"
    }

    private fun localizedFile(baseFile: Path, language: String, parser: ConfigParser): Path {
        val fileName = baseFile.fileName.toString()
        val knownExtension = parser.supportedExtensions
            .firstOrNull { fileName.endsWith(".$it", ignoreCase = true) }
        val localizedFileName = if (knownExtension != null) {
            val baseName = fileName.removeSuffix(".$knownExtension")
            "${baseName}_${normalizeLanguage(language)}.$knownExtension"
        } else {
            "${fileName}_${normalizeLanguage(language)}"
        }
        return baseFile.resolveSibling(localizedFileName)
    }

    private fun missingLocalizedResource(
        root: String,
        languages: Collection<String>,
        parser: ConfigParser
    ): IllegalStateException {
        val tried = languages
            .flatMap { languageCandidates(it) }
            .flatMap { language -> parser.supportedExtensions.map { extension -> "$root$language.$extension" } }
            .distinct()
        return IllegalStateException("No localized config resource found. Tried: ${tried.joinToString()}")
    }

    private fun normalizeLanguage(language: String): String {
        return language.trim().replace('-', '_')
    }

    private data class LocalizedResource(
        val language: String,
        val path: String
    )
}
