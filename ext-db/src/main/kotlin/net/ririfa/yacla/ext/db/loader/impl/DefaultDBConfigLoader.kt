package net.ririfa.yacla.ext.db.loader.impl

import net.ririfa.cask.Cask
import net.ririfa.yacla.ext.db.loader.DBConfigLoader
import net.ririfa.yacla.ext.db.internal.DBAccessLayer

class DefaultDBConfigLoader<T : Any>(
    private val type: Class<T>,
    private val key: String,
    private val cache: Cask<String, T>,
    private val db: DBAccessLayer
) : DBConfigLoader<T> {

    override val config: T
        get() = cache.get(key) ?: throw IllegalStateException("No config cached for key=$key")

    override fun reload(): DBConfigLoader<T> {
        val fresh = db.load(type, key)
        cache.put(key, fresh)
        return this
    }

    override fun save(): Boolean {
        val current = cache.get(key) ?: return false
        return db.save(type, key, current)
    }

    override fun update(block: T.() -> Unit) {
        val current = cache.get(key) ?: return
        current.block()
        cache.put(key, current)
    }

    override fun withKey(key: String): DBConfigLoader<T> {
        return DefaultDBConfigLoader(type, key, cache, db)
    }
}
