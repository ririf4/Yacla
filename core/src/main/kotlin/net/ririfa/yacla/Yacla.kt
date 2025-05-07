package net.ririfa.yacla

import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.DBConfigLoaderBuilder
import net.ririfa.yacla.loader.FileConfigLoader
import net.ririfa.yacla.loader.FileConfigLoaderBuilder
import net.ririfa.yacla.loader.impl.DefaultDBConfigLoaderBuilder
import net.ririfa.yacla.loader.impl.DefaultFileConfigLoaderBuilder

/**
 * Yacla - Yet Another Config Loading API
 *
 * Entry point to create and configure loaders that map configuration files
 * into Kotlin or Java objects, with support for validation, logging, and auto-update.
 */
object Yacla {

    @JvmStatic
    inline fun <reified T : Any> fileLoader(): FileConfigLoaderBuilder<T> = fileLoader(T::class.java)

    @JvmStatic
    fun <T : Any> fileLoader(clazz: Class<T>): FileConfigLoaderBuilder<T> = DefaultFileConfigLoaderBuilder(clazz)

    @JvmStatic
    fun <T : Any> fileLoader(
        clazz: Class<T>,
        builder: FileConfigLoaderBuilder<T>.() -> Unit
    ): FileConfigLoaderBuilder<T> = DefaultFileConfigLoaderBuilder(clazz).apply(builder)

    @JvmStatic
    inline fun <reified T : Any> dbLoader(): DBConfigLoaderBuilder<T> = dbLoader(T::class.java)

    @JvmStatic
    fun <T : Any> dbLoader(clazz: Class<T>): DBConfigLoaderBuilder<T> = DefaultDBConfigLoaderBuilder(clazz)

    @JvmStatic
    fun <T : Any> dbLoader(
        clazz: Class<T>,
        builder: DBConfigLoaderBuilder<T>.() -> Unit
    ): DBConfigLoaderBuilder<T> = DefaultDBConfigLoaderBuilder(clazz).apply(builder)
}
