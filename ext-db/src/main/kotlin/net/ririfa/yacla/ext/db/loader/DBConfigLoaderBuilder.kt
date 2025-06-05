package net.ririfa.yacla.ext.db.loader

import javax.sql.DataSource

/**
 * Builder interface for creating DB-backed config loaders.
 *
 * This builder allows you to configure the target table, logical key, data source,
 * and other options for DB persistence before loading the configuration.
 *
 * Usage:
 * ```
 * val loader = Yacla.dbLoader<MyConfig> {
 *     dataSource(myDataSource)
 *     table("my_config")
 *     key("profile-1")
 * }.load()
 * ```
 *
 * @param T the type of config object to be loaded
 */
interface DBConfigLoaderBuilder<T : Any> {
    /**
     * Specifies the [DataSource] (JDBC) used for all DB operations.
     */
    fun dataSource(ds: DataSource): DBConfigLoaderBuilder<T>

    /**
     * Specifies the table name where config objects are stored.
     */
    fun table(name: String): DBConfigLoaderBuilder<T>

    /**
     * Sets the logical config key to use (acts as a primary key for lookup).
     */
    fun key(key: String): DBConfigLoaderBuilder<T>

    /**
     * Builds and returns a new [DBConfigLoader] instance with the provided settings.
     */
    fun load(): DBConfigLoader<T>
}
