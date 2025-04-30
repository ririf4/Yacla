package net.ririfa.yacla.loader

/**
 * Interface representing a loaded configuration instance.
 *
 * Provides access to the loaded configuration object and actions to reload,
 * validate, or update it if outdated.
 */
interface ConfigLoader<T : Any> {

    /**
     * The loaded configuration object.
     */
    val config: T

    /**
     * Reloads the configuration from the backing file.
     */
    fun reload()

    /**
     * Validates the configuration object using annotations (e.g., @Required).
     */
    fun validate()


    /**
     * Fills in null or blank values with appropriate defaults (if applicable).
     */
    fun nullCheck()

    /**
     * Updates the configuration file from the resource if it is outdated.
     */
    fun updateIfNeeded()
}