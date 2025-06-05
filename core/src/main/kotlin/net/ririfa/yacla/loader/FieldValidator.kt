package net.ririfa.yacla.loader

/**
 * Interface for custom field validators in Yacla configuration loading.
 *
 * Implementations of this interface allow developers to enforce complex or domain-specific
 * validation rules beyond simple annotations like `@Required` or `@Range`.
 *
 * This interface is typically used in conjunction with the
 * [net.ririfa.yacla.annotation.CustomValidateHandler] annotation, which attaches a validator
 * to a specific field.
 *
 * Validators may throw exceptions (e.g., [IllegalArgumentException], [IllegalStateException]) to reject invalid values.
 *
 * Example:
 * ```kotlin
 * class UrlValidator : FieldValidator {
 *     override fun validate(fieldValue: Any?, configInstance: Any) {
 *         val url = fieldValue as? String ?: throw IllegalArgumentException("URL must be a string")
 *         if (!url.startsWith("https://")) {
 *             throw IllegalArgumentException("URL must start with https://")
 *         }
 *     }
 * }
 * ```
 */
interface FieldValidator {
    /**
     * Validates a field's value during config loading or validation phase.
     *
     * @param fieldValue the value of the field (may be null or blank)
     * @param configInstance the full config object this field belongs to (can be used for cross-field validation)
     * @throws Exception if validation fails; the exception will halt the load or validation process
     */
    fun validate(fieldValue: Any?, configInstance: Any)
}
