package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.loader.DBConfigLoader
import net.ririfa.yacla.loader.DBConfigLoaderBuilder
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser

class DefaultDBConfigLoaderBuilder<T : Any>(private val clazz: Class<T>) : DBConfigLoaderBuilder<T> {
    private var url: String? = null
    private var user: String? = null
    private var pass: String? = null
    private var schema: String? = null
    private var parser: ConfigParser? = null
    private var logger: YaclaLogger? = null

    override fun fromDatabase(url: String, user: String, pass: String): DBConfigLoaderBuilder<T> = apply {
        this.url = url; this.user = user; this.pass = pass
    }

    override fun withSchema(schema: String): DBConfigLoaderBuilder<T> = apply {
        this.schema = schema
    }

    override fun withLogger(logger: YaclaLogger): DBConfigLoaderBuilder<T> = apply {
        this.logger = logger
    }

    override fun load(): DBConfigLoader<T> {

    }
}
