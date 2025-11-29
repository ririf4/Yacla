package net.ririfa.yacla.schema

import net.ririfa.yacla.loader.ErrorHandlerWith
import net.ririfa.yacla.loader.FieldLoader
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

/**
 * Holds definition metadata for a single config field.
 */
data class FieldDefinition(
    val name: String,
    val yamlName: String?,
    val type: KType,
    val defaultValue: Any? = NO_DEFAULT,
    val required: Boolean = false,
    val softRequired: Boolean = false,
    val rangeMin: Long? = null,
    val rangeMax: Long? = null,
    val loader: FieldLoader? = null,
    val nullHandler: ErrorHandlerWith? = null,
    val validators: List<(Any?) -> Unit> = emptyList()
) {
    companion object {
        val NO_DEFAULT = Any()
    }
}

/**
 * Provides a DSL-style builder to configure fields in a configuration schema.
 *
 * This class allows defining field-specific properties, validation logic, and custom behavior for
 * configuration loading and parsing. It supports defining default values, marking fields as
 * required, setting value ranges, and adding custom deserialization logic.
 *
 * @param V the type of the value associated with this field
 */
class FieldConfigScope<V> {

    private var defaultValue: Any? = FieldDefinition.NO_DEFAULT
    private var required: Boolean = false
    private var softRequired: Boolean = false
    private var loader: FieldLoader? = null
    private var nullHandler: ErrorHandlerWith? = null
    private var yamlName: String? = null
    private var rangeMin: Long? = null
    private var rangeMax: Long? = null

    private val validators = mutableListOf<(Any?) -> Unit>()

    /**
     * Sets the default value for the configuration field.
     *
     * @param value the default value to be assigned to the field
     */
    fun default(value: V) {
        defaultValue = value
    }

    /**
     * Marks the field as required or soft-required during configuration validation.
     *
     * A required field must be explicitly provided in the configuration.
     * A soft-required field may still allow null or default values, depending on the loader logic.
     *
     * @param soft if true, marks the field as soft-required; otherwise, it is marked as strictly required
     */
    fun required(soft: Boolean = false) {
        if (soft) softRequired = true
        else required = true
    }

    /**
     * Sets a custom loader for the configuration field.
     *
     * The custom loader is responsible for converting raw parsed values (e.g., from YAML/JSON)
     * into the final expected type of the field. This can be used for fields requiring
     * special deserialization logic.
     *
     * @param loader an implementation of the `FieldLoader` interface for custom deserialization logic
     */
    fun loader(loader: FieldLoader) {
        this.loader = loader
    }

    /**
     * Sets a handler to be used when the configuration field is null.
     *
     * This method allows specifying a custom implementation of the `ErrorHandlerWith` interface
     * that defines the behavior when a required configuration field is found to be null during validation
     * or loading.
     *
     * @param handler the class of the custom error handler implementing `ErrorHandlerWith`
     */
    fun ifNull(handler: KClass<out ErrorHandlerWith>) {
        this.nullHandler = handler.java.getDeclaredConstructor().newInstance()
    }

    fun name(yamlName: String) {
        this.yamlName = yamlName
    }

    /**
     * Sets a range validator for the configuration field.
     *
     * The range validator ensures that the field value is within the specified minimum
     * and maximum bounds. If the field value is out of the defined range, a validation
     * error is triggered. This validation applies to numeric types (e.g., Int, Long, Float, Double).
     *
     * @param min the minimum permissible value for the field (default is Long.MIN_VALUE)
     * @param max the maximum permissible value for the field (default is Long.MAX_VALUE)
     */
    fun range(min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE) {
        this.rangeMin = min
        this.rangeMax = max
        validators += fun(v: Any?) {
            if (v == null) return

            val num = when (v) {
                is Byte -> v.toLong()
                is Short -> v.toLong()
                is Int -> v.toLong()
                is Long -> v
                is Float -> v.toLong()
                is Double -> v.toLong()
                else -> return
            }

            if (num < min || num > max) {
                throw IllegalArgumentException("Value $num is out of range [$min, $max]")
            }
        }
    }

    /**
     * Adds a custom validator for the configuration field.
     *
     * Validators are functions that perform additional validation on the field's value.
     * If the validation fails, the validator should throw an exception or indicate an error.
     *
     * @param block a lambda function that takes the field value as input and validates it
     */
    fun validate(block: (V) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        validators += { v -> block(v as V) }
    }

    /**
     * Builds a definition for a configuration field based on the provided property and the
     * settings specified within the `FieldConfigScope`.
     *
     * @param prop the property representing the configuration field for which the definition is being built
     * @return a `FieldDefinition` object containing metadata and validation rules for the configuration field
     */
    fun buildDefinition(prop: KProperty1<*, *>): FieldDefinition {
        return FieldDefinition(
            name = prop.name,
            yamlName = yamlName,
            type = prop.returnType,
            defaultValue = defaultValue,
            required = required,
            softRequired = softRequired,
            rangeMin = rangeMin,
            rangeMax = rangeMax,
            loader = loader,
            nullHandler = nullHandler,
            validators = validators
        )
    }
}

/**
 *
 */
class FieldDefBuilder<T : Any> {

    private val fields = mutableMapOf<String, FieldDefinition>()

    fun <V> field(
        prop: KProperty1<T, V>,
        block: FieldConfigScope<V>.() -> Unit
    ) {
        val scope = FieldConfigScope<V>()
        scope.block()
        fields[prop.name] = scope.buildDefinition(prop)
    }

    fun build(): Map<String, FieldDefinition> = fields
}
