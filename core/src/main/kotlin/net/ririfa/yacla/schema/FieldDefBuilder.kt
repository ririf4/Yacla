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
 * Scope for configuring a single field.
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

    fun default(value: V) {
        defaultValue = value
    }

    fun required(soft: Boolean = false) {
        if (soft) softRequired = true
        else required = true
    }

    fun loader(loader: FieldLoader) {
        this.loader = loader
    }

    fun ifNull(handler: KClass<out ErrorHandlerWith>) {
        this.nullHandler = handler.java.getDeclaredConstructor().newInstance()
    }

    fun name(yamlName: String) {
        this.yamlName = yamlName
    }

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

    fun validate(block: (V) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        validators += { v -> block(v as V) }
    }

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

class FieldDefBuilder<T : Any>(
    private val clazz: KClass<T>
) {

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
