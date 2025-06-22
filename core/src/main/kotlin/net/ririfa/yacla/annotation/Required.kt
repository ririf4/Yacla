package net.ririfa.yacla.annotation

/**
 * Marks a field as required in the configuration.
 *
 * If the field is missing or blank in the loaded configuration, an error will be reported.
 * If [soft] is true, the absence will be logged as a warning instead of throwing an error.
 *
 * This annotation also allows specifying a custom name to report in logs via [named].
 *
 * Example:
 * ```
 * @Required
 * String apiKey;
 *
 * @Required(soft = true, named = "logChannelId")
 * String logChannel;
 * ```
 *
 * @property soft whether this is a soft requirement (log warning instead of error)
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Required(val soft: Boolean = false)
