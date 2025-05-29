package net.ririfa.yacla.ext.db.loader.impl

import net.ririfa.cask.allowNullValues
import net.ririfa.cask.cask
import net.ririfa.cask.maxSize
import net.ririfa.cask.ttl
import net.ririfa.yacla.ext.db.loader.DBConfigLoader
import net.ririfa.yacla.ext.db.loader.DBConfigLoaderBuilder
import net.ririfa.yacla.ext.db.sync.SyncDispatcher
import net.ririfa.yacla.ext.db.sync.SyncDispatcher.SyncTask
import net.ririfa.yacla.ext.db.internal.impl.JdbcBinaryAccessLayer
import java.time.Duration
import javax.sql.DataSource

class DefaultDBConfigLoaderBuilder<T : Any>(
    private val clazz: Class<T>
) : DBConfigLoaderBuilder<T> {

    private lateinit var iDataSource: DataSource
    private var table: String = "yacla_config"
    private var key: String = "default"
    private var iTtl: Duration = Duration.ofMinutes(10)
    private var iMaxSize: Int = 100

    override fun dataSource(ds: DataSource): DBConfigLoaderBuilder<T> = apply {
        this.iDataSource = ds
    }

    override fun table(name: String): DBConfigLoaderBuilder<T> = apply {
        this.table = name
    }

    override fun key(key: String): DBConfigLoaderBuilder<T> = apply {
        this.key = key
    }

    fun ttl(duration: Int): DBConfigLoaderBuilder<T> = apply {
        this.iTtl = Duration.ofSeconds(duration.toLong())
    }

    fun ttl(duration: Duration): DBConfigLoaderBuilder<T> = apply {
        this.iTtl = duration
    }

    fun maxSize(size: Int): DBConfigLoaderBuilder<T> = apply {
        this.iMaxSize = size
    }

    override fun load(): DBConfigLoader<T> {
        if (!::iDataSource.isInitialized) {
            throw IllegalStateException("DataSource must be set")
        }

        val access = JdbcBinaryAccessLayer(iDataSource, table)
        val dispatcher = SyncDispatcher(access)

        val cache = cask<String, T> {
            ttl = iTtl
            maxSize = iMaxSize
            allowNullValues()
            loader { access.load(clazz, it) ?: throw IllegalStateException("No config found for $it") }
            onEvict { k, v -> if (v != null) dispatcher.enqueue(SyncTask(k, v, clazz)) }
            shareGcExecutor(true)
        }

        return DefaultDBConfigLoader(clazz, key, cache, access, dispatcher)
    }
}
