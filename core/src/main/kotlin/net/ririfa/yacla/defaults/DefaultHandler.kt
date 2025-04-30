package net.ririfa.yacla.defaults

fun interface DefaultHandler {
    fun parse(raw: String, type: Class<*>): Any?
}
