package net.ririfa.yacla.ext.db.loader

import net.ririfa.yacla.ext.db.sync.SyncDispatcher

interface DBConfigLoader<T : Any> {
    val config: T

    val syncDispatcher: SyncDispatcher

    fun reload(): DBConfigLoader<T>
    fun save(): Boolean
    fun update(block: T.() -> Unit)

    fun withKey(key: String): DBConfigLoader<T>
}
