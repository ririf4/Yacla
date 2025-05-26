package net.ririfa.yacla.ext.db.loader

interface DBConfigLoader<T : Any> {
    val config: T
    fun reload(): DBConfigLoader<T>
    fun save(): Boolean
    fun update(block: T.() -> Unit)

    fun withKey(key: String): DBConfigLoader<T>
}
