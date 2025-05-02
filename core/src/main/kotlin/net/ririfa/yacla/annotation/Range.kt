package net.ririfa.yacla.annotation

/**
 * Specifies a valid numeric range for a field.
 *
 * When validating the loaded configuration, the field value will be checked to ensure
 * it falls within the defined range (inclusive). If the value is out of range,
 * an error will be raised.
 *
 * Example:
 * ```
 * @Range(min = 1, max = 100)
 * int threadCount;
 * ```
 *
 * @property min the minimum allowed value (inclusive)
 * @property max the maximum allowed value (inclusive)
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Range(val min: Long = Long.MIN_VALUE, val max: Long = Long.MAX_VALUE)
