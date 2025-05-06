package net.ririfa.yacla.loader

import net.ririfa.yacla.logger.YaclaLogger

interface ConfigLoaderBuilder<T : Any> {
    /**
     * Builds and loads the config, returning a [ConfigLoader] instance.
     *
     * @return Loaded config handler.
     */
    fun load(): ConfigLoader<T>

    /**
     * Sets the logger used for informational and error messages.
     *
     * @param logger Implementation of [YaclaLogger].
     */
    fun withLogger(logger: YaclaLogger): ConfigLoaderBuilder<T>
}