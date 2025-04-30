package net.ririfa.yacla.annotation

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Range(val min: Long = Long.MIN_VALUE, val max: Long = Long.MAX_VALUE)
