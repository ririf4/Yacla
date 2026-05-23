package net.ririfa.yacla.annotation

/**
 * Maps a constructor parameter to a specific config key.
 *
 * This is useful when the file uses a key that cannot be inferred from the
 * Kotlin parameter name, snake_case, or kebab-case variants.
 *
 * Example:
 * ```kotlin
 * data class MyConfig(
 *     @Key("server-port") val port: Int = 8080
 * )
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Key(val value: String)
