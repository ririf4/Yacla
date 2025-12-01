package net.ririfa.yacla.loader

/**
 * Interface for handling null or blank field values during configuration loading.
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
    fun handle(fieldValue: Any?, config: Any?)
}
