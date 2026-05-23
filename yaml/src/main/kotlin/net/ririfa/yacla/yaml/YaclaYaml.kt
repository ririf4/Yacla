package net.ririfa.yacla.yaml

import net.ririfa.yacla.Yacla
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.YaclaLogger
import java.nio.file.Path

inline fun <reified T : Any> Yacla.yaml(
    resource: String,
    file: Path,
    autoUpdate: Boolean = false,
    logger: YaclaLogger? = null
): T = yamlLoader<T>(
    resource = resource,
    file = file,
    autoUpdate = autoUpdate,
    logger = logger
).config

inline fun <reified T : Any> Yacla.yamlLoader(
    resource: String,
    file: Path,
    autoUpdate: Boolean = false,
    logger: YaclaLogger? = null
): ConfigLoader<T> {
    val builder = loader<T>()
        .fromResource(resource)
        .toFile(file)
        .parser(YamlParser())
        .autoUpdateIfOutdated(autoUpdate)

    if (logger != null) {
        builder.withLogger(logger)
    }

    return builder.load()
}

inline fun <reified T : Any> Yacla.yamlLoader(
    noinline block: ConfigLoaderBuilder<T>.() -> Unit
): ConfigLoader<T> {
    return loader<T>()
        .parser(YamlParser())
        .apply(block)
        .load()
}

inline fun <reified T : Any> Yacla.yaml(
    noinline block: ConfigLoaderBuilder<T>.() -> Unit
): T = yamlLoader<T>(block).config
