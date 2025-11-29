package net.ririfa.yacla.schema

/**
 * Defines a schema for a configuration data class, describing how each field should be
 * loaded and processed by Yacla.
 *
 * A schema describes how each property of a configuration data class should be loaded,
 * transformed, validated, and defaulted. Implementations receive a [FieldDefBuilder]
 * that allows registering rules for each field.
 *
 * Typical responsibilities inside [configure] include:
 *  - Specify a custom loader to transform raw values (e.g., `String -> Enum`)
 *  - Validate loaded values (e.g., warn if a field is missing)
 *  - Provide default values when a field is absent or invalid
 *
 * Schemas are typically declared as `object` singletons and registered implicitly
 * when the configuration is loaded by Yacla. They do not perform loading themselves,
 * but supply metadata that the loader will use to process each property.
 *
 * @param T the target configuration data class type
 */
interface YaclaSchema<T : Any> {
    /**
     * Called by the configuration loader to describe field-level behaviors.
     * Implementations must register definitions for any fields that are to be
     * customized or validated. Fields not registered here will use default
     * loading behavior.
     *
     * @param def builder used to configure each property of the configuration class
     */
    fun configure(def: FieldDefBuilder<T>)
}
