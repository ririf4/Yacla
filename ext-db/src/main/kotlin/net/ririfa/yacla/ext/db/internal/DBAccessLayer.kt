package net.ririfa.yacla.ext.db.internal

interface DBAccessLayer {

    /**
     * Loads config for the given class and key.
     */
    fun <T : Any> load(clazz: Class<T>, key: String): T?

    /**
     * Saves the config instance to DB.
     */
    fun <T : Any> save(clazz: Class<T>, key: String, config: T): Boolean
}