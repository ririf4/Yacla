package net.ririfa.yacla.loader

/**
 * Interface for handling null or blank field values during configuration loading.
 *
 * This interface is intended to be used with the [net.ririfa.yacla.annotation.IfNullEvenRequired]
 * annotation to provide custom logic when a required field is found to be null or blank.
 *
 * The [handle] method is invoked during validation or loading, allowing the handler to
 * log, throw, mutate state, or even silently recover from the missing value.
 *
 * Example:
 * ```kotlin
 * class PanicIfNull : ErrorHandlerWith {
 *     override fun handle(fieldValue: Any?) {
 *         throw IllegalStateException("Field was unexpectedly null: $fieldValue")
 *     }
 * }
 * ```
 */
interface ErrorHandlerWith {
    /**
     * Called when a required field is null or blank and marked with [net.ririfa.yacla.annotation.IfNullEvenRequired].
     *
     * @param fieldValue the current value of the field (typically null or blank string)
     */
    fun handle(fieldValue: Any?, config: Any?)
}
