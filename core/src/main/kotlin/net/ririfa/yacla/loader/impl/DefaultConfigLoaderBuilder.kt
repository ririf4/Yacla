package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.LoaderSettings
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects
import kotlin.collections.set
import kotlin.io.path.notExists
import kotlin.reflect.KClass

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
    private val contextProviders = mutableMapOf<KClass<*>, () -> Any>()

    init {
        DefaultHandlers.registerDefaults()
    }

    override fun fromResource(path: String): ConfigLoaderBuilder<T> = apply {
        this.resourcePath = path
    }

    override fun toFile(file: Path): ConfigLoaderBuilder<T> = apply {
        this.targetFile = file
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

    fun withDefaults(defaults: LoaderSettings): ConfigLoaderBuilder<T> = apply {
        if (parser == null) {
            parser = defaults.parser
        }
        if (logger == null && defaults.logger != null) {
            logger = defaults.logger
        }
        autoUpdate = defaults.autoUpdate
    }

    override fun <CTX : Any> registerContextProvider(type: KClass<CTX>, provider: () -> CTX): ConfigLoaderBuilder<T> {
        contextProviders[type] = provider
        return this
    }

    override fun <CTX : Any> registerContextProvider(type: Class<CTX>, provider: () -> CTX): ConfigLoaderBuilder<T> {
        contextProviders[type.kotlin] = provider
        return this
    }

    override fun load(): ConfigLoader<T> {
        Objects.requireNonNull(resourcePath, "Resource path is not set")
        Objects.requireNonNull(targetFile, "Target file is not set")
        Objects.requireNonNull(parser, "Parser is not set")

        if (targetFile!!.notExists()) {
            logger?.info("Config file not found. Copying from resource: $resourcePath")
            val resourceStream: InputStream =
                javaClass.getResourceAsStream(resourcePath!!)
                    ?: throw IllegalStateException("Resource $resourcePath not found in classpath")
            Files.copy(resourceStream, targetFile!!)
        }

        return DefaultConfigLoader(
            clazz = clazz,
            parser = parser!!,
            file = targetFile!!,
            logger = logger,
            resourcePath = resourcePath!!,
            contextProviders = contextProviders
        ).also { loader ->
            if (autoUpdate) {
                loader.updateConfig()
                    .reload()
            }
        }
    }
}
