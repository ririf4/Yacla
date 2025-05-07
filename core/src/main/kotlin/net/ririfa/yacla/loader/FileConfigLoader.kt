package net.ririfa.yacla.loader

interface FileConfigLoader<T : Any> : ConfigLoader<T> {

    /**
     * Validates the configuration object based on field annotations such as [net.ririfa.yacla.annotation.Required]
     * and [net.ririfa.yacla.annotation.Range].
     *
     * Throws an exception if validation fails.
     */
    fun validate(): ConfigLoader<T>

    /**
     * Fills in null or blank fields with default values if applicable.
     *
     * Default values are resolved from the [net.ririfa.yacla.annotation.Default] annotation
     * or type-based defaults registered in [net.ririfa.yacla.defaults.DefaultHandlers].
     */
    fun nullCheck(): ConfigLoader<T>

    /**
     * Attempts to update the configuration file by merging the default resource
     * with the current file, preserving user-defined values.
     *
     * Uses an [UpdateStrategy] registered for the parser type to perform the update.
     *
     * This method is automatically called during [ConfigLoaderBuilder.load] if
     * [FileConfigLoaderBuilder.autoUpdateIfOutdated] was enabled.
     *
     * It can also be called manually to trigger an update check at any time.
     */
    fun updateConfig(): ConfigLoader<T>
}