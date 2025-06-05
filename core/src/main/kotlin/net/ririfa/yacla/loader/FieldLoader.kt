package net.ririfa.yacla.loader

/**
 * Interface for custom field deserialization during config loading.
 *
 * Implement this interface to define a custom conversion strategy for a specific field.
 * It is used in conjunction with the [net.ririfa.yacla.annotation.CustomLoader] annotation.
 *
 * This allows transforming raw parsed values (e.g., from YAML/JSON) into complex or validated types.
 *
 * Example:
 * ```kotlin
 * class EnumListLoader : FieldLoader {
 *     override fun load(raw: Any?): Any? {
 *         return (raw as? List<*>)?.mapNotNull { MyEnum.valueOf(it.toString()) }
 *     }
 * }
 * ```
 */
interface FieldLoader {
    /**
     * Transforms a raw parsed value into the final field value.
     *
     * @param raw the raw object parsed from the config file
     * @return the transformed value to assign to the field
     */
    fun load(raw: Any?): Any?
}
