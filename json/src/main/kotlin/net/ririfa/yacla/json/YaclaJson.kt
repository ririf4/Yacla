package net.ririfa.yacla.json

import net.ririfa.yacla.Yacla
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.YaclaLogger
import java.nio.file.Path

inline fun <reified T : Any> Yacla.json(
    resource: String,
    file: Path,
    autoUpdate: Boolean = false,
    logger: YaclaLogger? = null
): T = jsonLoader<T>(
    resource = resource,
    file = file,
    autoUpdate = autoUpdate,
    logger = logger
).config

inline fun <reified T : Any> Yacla.jsonLoader(
    resource: String,
    file: Path,
    autoUpdate: Boolean = false,
    logger: YaclaLogger? = null
): ConfigLoader<T> {
    val builder = loader<T>()
        .fromResource(resource)
        .toFile(file)
        .parser(JsonParser())
        .autoUpdateIfOutdated(autoUpdate)

    if (logger != null) {
        builder.withLogger(logger)
    }

    return builder.load()
}

inline fun <reified T : Any> Yacla.jsonLoader(
    noinline block: ConfigLoaderBuilder<T>.() -> Unit
): ConfigLoader<T> {
    return loader<T>()
        .parser(JsonParser())
        .apply(block)
        .load()
}

inline fun <reified T : Any> Yacla.json(
    noinline block: ConfigLoaderBuilder<T>.() -> Unit
): T = jsonLoader<T>(block).config
