package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.annotation.Default
import net.ririfa.yacla.annotation.Range
import net.ririfa.yacla.annotation.Required
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.exception.NullCheckException
import net.ririfa.yacla.exception.ValidationException
import net.ririfa.yacla.exception.YaclaConfigException
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * Default implementation of [ConfigLoader] that loads, validates, and updates a configuration object
 * from a file using a [ConfigParser].
 *
 * This loader handles the following:
 * - Loading the configuration object from a file
 * - Performing null/default value injection for missing fields
 * - Validating fields based on annotations such as [Required], [Range], and [Default]
 * - Automatically applying update strategies registered for the parser type
 *
 * Typically created via a [net.ririfa.yacla.loader.ConfigLoaderBuilder], not instantiated directly.
 *
 * @param T the type of the configuration object
 * @param clazz the target configuration class
 * @param parser the parser to deserialize the configuration
 * @param file the path to the configuration file
 * @param logger optional logger for diagnostics
 * @param resourcePath the path to the resource used for updating configs
 */
class DefaultConfigLoader<T : Any> internal constructor(
    private val clazz: Class<T>,
    private val parser: ConfigParser,
    private val file: Path,
    private val logger: YaclaLogger?,
    private val resourcePath: String,
    private val ignoreExtensionCheck: Boolean = false
) : ConfigLoader<T> {

    override var config: T = loadFromFile()
        private set

    override fun reload(): ConfigLoader<T> {
        logger?.info("Reloading config from $file")
        config = loadFromFile()
        return this
    }

    override fun validate(): ConfigLoader<T> {
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
                        throw ValidationException("Missing required config field: $fieldName")
                    }
                }
            }

            val range = field.getAnnotation(Range::class.java)
            if (range != null && value is Number) {
                val longValue = value.toLong()
                if (longValue < range.min || longValue > range.max) {
                    logger?.error("Field '$fieldName' is out of range (${range.min}..${range.max}): $longValue")
                    throw ValidationException("Config field '$fieldName' out of range: $longValue")
                }
            }
        }
        return this
    }

    override fun nullCheck(): ConfigLoader<T> {
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
                    throw NullCheckException("No DefaultHandler registered for '${fieldType.simpleName}' to parse @Default on '${field.name}'")
                }
            } else {
                // when no default value is provided, we can set it to null
                // Because if Yacla sets the default value on its own,
                // the developer will not be able to tell which of the values were actually missing when debugging.
                // But if it sets it to null, a developer gets an NPE, and that is easier to understand.
                null
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
        return this
    }

    override fun updateConfig(): ConfigLoader<T> {
        val strategy = UpdateStrategyRegistry.strategyFor(parser)
        if (strategy != null) {
            val ctx = UpdateContext(parser, file, resourcePath, logger)
            if (!strategy.updateIfNeeded(ctx)) {
                logger?.info("Config already up-to-date.")
            }
        } else {
            logger?.warn("No UpdateStrategy registered for ${parser::class.java.simpleName}")
        }
        return this
    }

    private fun loadFromFile(): T {
        if (ignoreExtensionCheck) {
            val ext = file.extension.lowercase()
            if (ext !in parser.supportedExtensions) {
                throw YaclaConfigException("Parser ${parser::class.simpleName} does not support extension '.$ext'")
            }
        }

        return Files.newInputStream(file).use {
            parser.parse(it, clazz)
        }
    }

}