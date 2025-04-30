package net.ririfa.yacla.annotation

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Required(val soft: Boolean = false, val named: String = "")
