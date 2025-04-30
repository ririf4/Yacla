package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.annotation.Default
import net.ririfa.yacla.annotation.Range
import net.ririfa.yacla.annotation.Required
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path

class DefaultConfigLoader<T : Any>(
    private val clazz: Class<T>,
    private val parser: ConfigParser,
    private val file: Path,
    private val logger: YaclaLogger?
) : ConfigLoader<T> {

    override var config: T = loadFromFile()
        private set

    override fun reload() {
        logger?.info("Reloading config from $file")
        config = loadFromFile()
    }

    override fun validate() {
        logger?.info("Validating config class: ${clazz.simpleName}")

        for (field in clazz.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue

            field.isAccessible = true
            val value = field.get(config)
            val fieldName = field.name

            val required = field.getAnnotation(Required::class.java)
            if (required != null) {
                val isEmptyString = value is String && value.isBlank()
                if (value == null || isEmptyString) {
                    if (required.soft) {
                        logger?.warn("Soft required field '$fieldName' is not set.")
                    } else {
                        logger?.error("Required field '$fieldName' is missing or blank!")
                        throw IllegalStateException("Missing required config field: $fieldName")
                    }
                }
            }

            val range = field.getAnnotation(Range::class.java)
            if (range != null && value is Number) {
                val longValue = value.toLong()
                if (longValue < range.min || longValue > range.max) {
                    logger?.error("Field '$fieldName' is out of range (${range.min}..${range.max}): $longValue")
                    throw IllegalArgumentException("Config field '$fieldName' out of range: $longValue")
                }
            }
        }
    }

    override fun nullCheck() {
        logger?.info("Running nullCheck for ${clazz.simpleName}")

        for (field in clazz.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue

            field.isAccessible = true
            val value = field.get(config)
            val isEmpty = value == null || (value is String && value.isBlank())
            if (!isEmpty) continue

            val fieldType = field.type
            val defaultAnnotation = field.getAnnotation(Default::class.java)

            val defaultValue: Any? = if (defaultAnnotation != null) {
                val handler = DefaultHandlers.get(fieldType)
                if (handler != null) {
                    try {
                        handler.parse(defaultAnnotation.value, fieldType)
                    } catch (e: Exception) {
                        logger?.error("Failed to parse @Default value for field '${field.name}'", e)
                        null
                    }
                } else {
                    logger?.warn("No DefaultHandler registered for '${fieldType.simpleName}' to parse @Default on '${field.name}'")
                    null
                }
            } else {
                when {
                    fieldType == String::class.java -> ""
                    java.lang.Boolean::class.java == fieldType || Boolean::class.java == fieldType -> false
                    Integer::class.java == fieldType || Int::class.java == fieldType -> 0
                    java.lang.Long::class.java == fieldType || Long::class.java == fieldType -> 0L
                    java.lang.Double::class.java == fieldType || Double::class.java == fieldType -> 0.0
                    java.lang.Float::class.java == fieldType || Float::class.java == fieldType -> 0f
                    List::class.java.isAssignableFrom(fieldType) -> emptyList<Any>()
                    Set::class.java.isAssignableFrom(fieldType) -> emptySet<Any>()
                    else -> {
                        logger?.warn("Field '${field.name}' is null but no default is defined for type ${fieldType.simpleName}")
                        null
                    }
                }
            }

            if (defaultValue != null) {
                try {
                    field.set(config, defaultValue)
                    logger?.info("Field '${field.name}' was null or blank, set default: $defaultValue")
                } catch (e: Exception) {
                    logger?.error("Failed to set default for field '${field.name}'", e)
                }
            }
        }
    }

    override fun updateIfNeeded() {
        throw IllegalStateException("Config auto-update is not supported by DefaultConfigLoader. Use a format-specific loader if needed.")
    }

    private fun loadFromFile(): T {
        return Files.newInputStream(file).use {
            parser.parse(it, clazz)
        }
    }
}