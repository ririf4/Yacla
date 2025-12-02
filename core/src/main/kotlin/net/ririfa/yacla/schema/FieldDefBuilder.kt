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
 * This class supports both **method chaining** and **block-style** configuration,
 * allowing flexible and type-safe field definitions.
 *
 * ## Method Chaining Style
 * ```kotlin
 * def.field(AppConfig::port)
 *     .default(8080)
 *     .range(1..65535)
 *     .validate { require(it > 0) { "Port must be positive" } }
 * ```
 *
 * ## Block Style
 * ```kotlin
 * def.field(AppConfig::apiKey) {
 *     required()
 *     validate { require(it.isNotBlank()) { "API key cannot be blank" } }
 * }
 * ```
 *
 * ## Mixed Style
 * Both styles can be combined as needed:
 * ```kotlin
 * def.field(AppConfig::timeout)
 *     .default(30)
 *     .range(1L..300L)
 * ```
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
    private lateinit var property: KProperty1<*, *>

    /**
     * Internal method to associate this scope with a specific property.
     *
     * This is called automatically by [FieldDefBuilder] and should not be called manually.
     *
     * @param prop the property to associate with this configuration scope
     */
    internal fun setProperty(prop: KProperty1<*, *>) {
        this.property = prop
    }

    /**
     * Sets the default value for the configuration field.
     *
     * If the field is missing or null in the loaded configuration, this value will be used instead.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::port).default(8080)
     * ```
     *
     * @param value the default value to be assigned to the field
     * @return this [FieldConfigScope] for method chaining
     */
    fun default(value: V): FieldConfigScope<V> = apply {
        defaultValue = value
    }

    /**
     * Marks the field as required or soft-required during configuration validation.
     *
     * - **Required (strict)**: The field must be explicitly provided in the configuration file.
     *   Loading will fail if the field is missing or null.
     * - **Soft-required**: A warning is logged if the field is missing, but loading continues.
     *
     * Example:
     * ```kotlin
     * // Strict requirement
     * field(AppConfig::apiKey).required()
     *
     * // Soft requirement (logs warning but doesn't fail)
     * field(AppConfig::debugMode).required(soft = true)
     * ```
     *
     * @param soft if true, marks the field as soft-required; otherwise, it is marked as strictly required
     * @return this [FieldConfigScope] for method chaining
     */
    fun required(soft: Boolean = false): FieldConfigScope<V> = apply {
        if (soft) softRequired = true
        else required = true
    }

    /**
     * Sets a custom loader for the configuration field.
     *
     * The custom loader is responsible for converting raw parsed values (e.g., from YAML/JSON)
     * into the final expected type of the field. This is useful for fields requiring
     * special deserialization logic, such as parsing enums, date formats, or custom objects.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::mode).loader(EnumLoader(AppMode::class))
     * ```
     *
     * @param loader an implementation of the [FieldLoader] interface for custom deserialization logic
     * @return this [FieldConfigScope] for method chaining
     */
    fun loader(loader: FieldLoader): FieldConfigScope<V> = apply {
        this.loader = loader
    }

    /**
     * Sets a handler to be invoked when the configuration field is null or missing.
     *
     * This method allows specifying a custom implementation of the [ErrorHandlerWith] interface
     * that defines the behavior when a field is found to be null during validation or loading.
     *
     * The handler can log warnings, throw exceptions, or provide fallback values.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::apiKey).ifNull(LogWarningHandler::class)
     * ```
     *
     * @param handler the class of the custom error handler implementing [ErrorHandlerWith]
     * @return this [FieldConfigScope] for method chaining
     */
    fun ifNull(handler: KClass<out ErrorHandlerWith>): FieldConfigScope<V> = apply {
        this.nullHandler = handler.java.getDeclaredConstructor().newInstance()
    }

    /**
     * Specifies the YAML/JSON key name for this field if it differs from the property name.
     *
     * This is useful when the configuration file uses a different naming convention than
     * your Kotlin code (e.g., snake_case vs camelCase).
     *
     * Example:
     * ```kotlin
     * // Maps "server_port" in config file to "serverPort" property
     * field(AppConfig::serverPort).name("server_port")
     * ```
     *
     * @param yamlName the key name to use in the configuration file
     * @return this [FieldConfigScope] for method chaining
     */
    fun name(yamlName: String): FieldConfigScope<V> = apply {
        this.yamlName = yamlName
    }

    /**
     * Sets a range validator for the configuration field using explicit min/max values.
     *
     * The range validator ensures that the field value is within the specified minimum
     * and maximum bounds. If the field value is out of the defined range, a validation
     * error is triggered. This validation applies to numeric types (e.g., Int, Long, Float, Double).
     *
     * Example:
     * ```kotlin
     * field(AppConfig::port).range(min = 1, max = 65535)
     * ```
     *
     * @param min the minimum permissible value for the field (default is [Long.MIN_VALUE])
     * @param max the maximum permissible value for the field (default is [Long.MAX_VALUE])
     * @return this [FieldConfigScope] for method chaining
     */
    fun range(min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): FieldConfigScope<V> = apply {
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

            if (num !in min..max) {
                throw IllegalArgumentException("Value $num is out of range [$min, $max]")
            }
        }
    }

    /**
     * Sets a range validator for the configuration field using a [LongRange].
     *
     * This is a convenience overload that accepts a Kotlin range object.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::port).range(1L..65535L)
     * ```
     *
     * @param range the valid range for the field value
     * @return this [FieldConfigScope] for method chaining
     */
    fun range(range: LongRange): FieldConfigScope<V> = apply {
        range(range.first, range.last)
    }

    /**
     * Sets a range validator for the configuration field using an [IntRange].
     *
     * This is a convenience overload for integer ranges.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::port).range(1..65535)
     * ```
     *
     * @param range the valid range for the field value
     * @return this [FieldConfigScope] for method chaining
     */
    fun range(range: IntRange): FieldConfigScope<V> = apply {
        range(range.first.toLong(), range.last.toLong())
    }

    /**
     * Adds a custom validator for the configuration field.
     *
     * Validators are lambda functions that perform additional validation on the field's value.
     * If the validation fails, the validator should throw an exception (typically [IllegalArgumentException]).
     *
     * Multiple validators can be added by calling this method multiple times.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::port)
     *     .validate { require(it > 0) { "Port must be positive" } }
     *     .validate { require(it < 65536) { "Port must be less than 65536" } }
     * ```
     *
     * @param block a lambda function that takes the field value and validates it
     * @return this [FieldConfigScope] for method chaining
     */
    fun validate(block: (V) -> Unit): FieldConfigScope<V> = apply {
        @Suppress("UNCHECKED_CAST")
        validators += { v -> block(v as V) }
    }

    /**
     * Adds multiple validators at once.
     *
     * This is a convenience method for adding several validation rules in a single call.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::port).validators(
     *     { require(it > 0) { "Port must be positive" } },
     *     { require(it < 65536) { "Port must be less than 65536" } }
     * )
     * ```
     *
     * @param blocks vararg of validator lambda functions
     * @return this [FieldConfigScope] for method chaining
     */
    fun validators(vararg blocks: (V) -> Unit): FieldConfigScope<V> = apply {
        @Suppress("UNCHECKED_CAST")
        blocks.forEach { block ->
            validators += { v -> block(v as V) }
        }
    }

    /**
     * Shortcut method to mark a field as required and provide a default value.
     *
     * This is useful when you want to ensure a field is always present in the schema,
     * but provide a fallback value if it's missing from the actual config file.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::timeout).requiredWithDefault(30)
     * ```
     *
     * @param value the default value to use if the field is missing
     * @return this [FieldConfigScope] for method chaining
     */
    fun requiredWithDefault(value: V): FieldConfigScope<V> = apply {
        required()
        default(value)
    }

    /**
     * Validates that a String field is not blank (not empty and not just whitespace).
     *
     * This validator only applies to [String] fields. For other types, it is silently ignored.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::apiKey).notBlank()
     * ```
     *
     * @return this [FieldConfigScope] for method chaining
     * @throws IllegalArgumentException if the field value is blank
     */
    @JvmName("notBlank")
    fun notBlank(): FieldConfigScope<V> = apply {
        @Suppress("UNCHECKED_CAST")
        validators += { v ->
            if (v is String && v.isBlank()) {
                throw IllegalArgumentException("Field '${property.name}' cannot be blank")
            }
        }
    }

    /**
     * Builds a [FieldDefinition] from the current configuration state.
     *
     * This method is called internally by [FieldDefBuilder.build] and should not be called manually.
     *
     * @return a [FieldDefinition] object containing metadata and validation rules for the configuration field
     */
    internal fun buildDefinition(): FieldDefinition {
        return FieldDefinition(
            name = property.name,
            yamlName = yamlName,
            type = property.returnType,
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
 * Builder for constructing field definitions in a configuration schema.
 *
 * This class provides a DSL for defining field-level validation, default values,
 * custom loaders, and other configuration metadata. It supports both **method chaining**
 * and **block-style** configuration.
 *
 * ## Usage Examples
 *
 * ### Method Chaining
 * ```kotlin
 * override fun configure(def: FieldDefBuilder<AppConfig>) {
 *     def.field(AppConfig::port)
 *         .default(8080)
 *         .range(1..65535)
 *         .validate { require(it > 0) }
 * }
 * ```
 *
 * ### Block Style
 * ```kotlin
 * override fun configure(def: FieldDefBuilder<AppConfig>) {
 *     def.field(AppConfig::apiKey) {
 *         required()
 *         notBlank()
 *     }
 * }
 * ```
 *
 * ### Mixed Style
 * ```kotlin
 * override fun configure(def: FieldDefBuilder<AppConfig>) {
 *     def.field(AppConfig::timeout)
 *         .default(30)
 *         .range(1L..300L)
 *
 *     def.field(AppConfig::debugMode) {
 *         default(false)
 *     }
 * }
 * ```
 *
 * @param T the configuration data class type
 */
class FieldDefBuilder<T : Any> {

    private val scopes = mutableMapOf<String, FieldConfigScope<*>>()

    /**
     * Starts defining a field configuration using method chaining.
     *
     * This method returns a [FieldConfigScope] that can be configured using
     * method chaining. All configuration methods return the scope itself,
     * allowing fluent API usage.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::port)
     *     .default(8080)
     *     .range(1..65535)
     *     .validate { it > 0 }
     * ```
     *
     * @param prop the property reference for the field to configure
     * @return a [FieldConfigScope] for method chaining
     */
    fun <V> field(prop: KProperty1<T, V>): FieldConfigScope<V> {
        val scope = FieldConfigScope<V>()
        scope.setProperty(prop)
        scopes[prop.name] = scope
        return scope
    }

    /**
     * Defines a field configuration using a DSL block.
     *
     * This method provides a traditional block-style DSL for configuring fields.
     * The provided lambda receives a [FieldConfigScope] as its receiver, allowing
     * direct calls to configuration methods.
     *
     * Example:
     * ```kotlin
     * field(AppConfig::apiKey) {
     *     required()
     *     validate { it.isNotBlank() }
     * }
     * ```
     *
     * @param prop the property reference for the field to configure
     * @param block a lambda with [FieldConfigScope] receiver for configuration
     */
    fun <V> field(
        prop: KProperty1<T, V>,
        block: FieldConfigScope<V>.() -> Unit
    ) {
        field(prop).apply(block)
    }

    /**
     * Builds the final map of field definitions.
     *
     * This method is called internally by the configuration loader to extract
     * all field definitions after the schema has been configured.
     *
     * @return a map of field names to their [FieldDefinition]s
     */
    fun build(): Map<String, FieldDefinition> {
        return scopes.mapValues { (_, scope) ->
            scope.buildDefinition()
        }
    }
}