package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects
import kotlin.io.path.notExists

class DefaultConfigLoaderBuilder<T : Any>(
    private val clazz: Class<T>
) : ConfigLoaderBuilder<T> {
    private var resourcePath: String? = null
    private var targetFile: Path? = null
    private var parser: ConfigParser? = null
    private var logger: YaclaLogger? = null
    private var autoUpdate = false

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
            logger = logger
        )
    }
}
