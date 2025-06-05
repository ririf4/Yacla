package net.ririfa.yacla.ext.db.loader

import net.ririfa.yacla.ext.db.sync.SyncDispatcher

/**
 * Main interface for DB-backed configuration loader.
 *
 * This loader provides access to a cached config object, automatic TTL cache management,
 * and methods for manual reload, save, mutation, and key switching.
 *
 * Typically, you obtain an instance via [DBConfigLoaderBuilder.load].
 *
 * @param T the type of the config object being managed
 */
interface DBConfigLoader<T : Any> {
    /**
     * The current cached config object for the given key.
     * May throw if no config is loaded or cache expired.
     */
    val config: T

    /**
     * The [SyncDispatcher] handling background DB sync operations for this loader.
     * Used for shutdown coordination if needed.
     */
    val syncDispatcher: SyncDispatcher

    /**
     * Forces a reload from the database for the current key, replacing the cached object.
     * @return this loader (for chaining)
     */
    fun reload(): DBConfigLoader<T>

    /**
     * Persists the current cached config object for the current key to the database immediately.
     * @return true if successful, false otherwise
     */
    fun save(): Boolean

    /**
     * Applies a mutation to the config object in-place and updates the cache.
     * Does **not** persist to the database until save or eviction.
     *
     * @param block mutation logic to run on the config object
     */
    fun update(block: T.() -> Unit)

    /**
     * Returns a new loader instance pointing to the given key in the same table.
     *
     * @param key the new logical key for the config object
     * @return a new loader for the specified key
     */
    fun withKey(key: String): DBConfigLoader<T>
}
