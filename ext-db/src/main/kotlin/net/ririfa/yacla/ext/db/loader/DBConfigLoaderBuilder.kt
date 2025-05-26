package net.ririfa.yacla.ext.db.loader

import javax.sql.DataSource

interface DBConfigLoaderBuilder<T : Any> {
    fun dataSource(ds: DataSource): DBConfigLoaderBuilder<T>
    fun table(name: String): DBConfigLoaderBuilder<T>
    fun key(key: String): DBConfigLoaderBuilder<T>
    fun load(): DBConfigLoader<T>
}
