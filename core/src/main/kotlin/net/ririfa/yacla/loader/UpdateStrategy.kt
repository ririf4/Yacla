package net.ririfa.yacla.loader

import net.ririfa.yacla.loader.util.UpdateContext

/**
 * Defines a strategy for updating an existing configuration file to match a newer default version.
 *
 * An implementation of this interface provides the logic to determine whether an update is needed,
 * and performs any necessary modifications to the target configuration file while preserving
 * existing values and comments as needed.
 *
 * This interface is typically used by format-specific update handlers (e.g., YAML, JSON).
 *
 * Example usage:
 * ```
 * UpdateStrategy { context ->
 *     // perform update logic using context.targetFile and context.resourcePath
 *     true // return true if the config file was updated, false otherwise
 * }
 * ```
 *
 * @see UpdateContext for the data provided to the update logic
 */
@FunctionalInterface
fun interface UpdateStrategy {
    /**
     * Attempts to update the target configuration file.
     *
     * @param context the [UpdateContext] providing information about the file, parser, logger, and resource path
     * @return true if the configuration file was modified and needs to be reloaded, false if no changes were made
     */
    fun updateIfNeeded(context: UpdateContext): Boolean
}
