package net.ririfa.yacla.loader

interface DBConfigLoaderBuilder<T : Any> : ConfigLoaderBuilder<T, DBConfigLoader<T>> {
    fun fromDatabase(url: String, user: String, pass: String): DBConfigLoaderBuilder<T>
    fun withSchema(schema: String): DBConfigLoaderBuilder<T>
}
