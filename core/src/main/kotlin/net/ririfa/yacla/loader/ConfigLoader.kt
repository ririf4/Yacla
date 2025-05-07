package net.ririfa.yacla.loader

interface ConfigLoader<T : Any> {

    val config: T

    fun reload(): ConfigLoader<T>
}
