package net.ririfa.yacla.loader

/**
 * Interface for field validators in Yacla configuration loading.
 *
 * Implementations of this interface can be used to custom validate field values
 * during the configuration loading process.
 */
interface FieldValidator {
    fun validate(fieldValue: Any?, configInstance: Any)
}
