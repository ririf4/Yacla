package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.DBConfigLoader
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.nio.file.Path
import java.sql.Connection

class DefaultDBConfigLoader<T : Any> internal constructor(
    private val clazz: Class<T>,
    private val parser: ConfigParser,
    private val connection: Connection,
    private val schema: String?,
    private val resourcePath: String,
    private val ignoreExtensionCheck: Boolean = false
) : DBConfigLoader<T> {
    override val config: T
        get() = TODO("Not yet implemented")

    override fun reload(): ConfigLoader<T> {
        TODO("Not yet implemented")
    }
}