@file:JvmName("YaclaDB")
package net.ririfa.yacla.ext.db

import net.ririfa.yacla.Yacla
import net.ririfa.yacla.ext.db.loader.DBConfigLoaderBuilder
import net.ririfa.yacla.ext.db.loader.impl.DefaultDBConfigLoaderBuilder

/**
 * Provides DB-based configuration loading for the given class.
 *
 * Only available when `yacla-ext-db` is present.
 */
@JvmName("dbLoaderForClass")
fun <T : Any> Yacla.dbLoader(clazz: Class<T>): DBConfigLoaderBuilder<T> =
    DefaultDBConfigLoaderBuilder(clazz)

/**
 * Provides DB-based configuration loading using Kotlin's reified generics.
 */
inline fun <reified T : Any> Yacla.dbLoader(): DBConfigLoaderBuilder<T> =
    dbLoader(T::class.java)

/**
 * DSL-style builder for DB config loader.
 */
fun <T : Any> Yacla.dbLoader(
    clazz: Class<T>,
    builder: DBConfigLoaderBuilder<T>.() -> Unit
): DBConfigLoaderBuilder<T> =
    dbLoader(clazz).apply(builder)

/**
 * DSL-style builder for DB config loader using Kotlin's reified generics.
 */
inline fun <reified T : Any> Yacla.dbLoader(
    noinline builder: DBConfigLoaderBuilder<T>.() -> Unit
): DBConfigLoaderBuilder<T> =
    dbLoader(T::class.java, builder)
