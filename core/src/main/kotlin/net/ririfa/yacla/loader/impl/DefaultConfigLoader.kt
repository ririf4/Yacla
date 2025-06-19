package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.annotation.CustomLoader
import net.ririfa.yacla.annotation.CustomValidateHandler
import net.ririfa.yacla.annotation.Default
import net.ririfa.yacla.annotation.IfNullEvenRequired
import net.ririfa.yacla.annotation.NamedRecord
import net.ririfa.yacla.annotation.Range
import net.ririfa.yacla.annotation.Required
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ErrorHandlerWith
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType

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
class DefaultConfigLoader<T : Any>(
    private val clazz: Class<T>,
    private val parser: ConfigParser,
    private val file: Path,
    private val logger: YaclaLogger?,
    private val resourcePath: String
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
                val named = required.named
                val isEmptyString = value is String && value.isBlank()
                if (value == null || isEmptyString) {
                    if (required.soft) {
                        logger?.warn("Soft required field '$named' is not set.")
                    } else {
                        logger?.error("Required field '$named' is missing or blank!")
                        throw IllegalStateException("Missing required config field: $named")
                    }
                }
            }

            val ifNull = field.getAnnotation(IfNullEvenRequired::class.java)
            if (ifNull != null && (value == null || (value is String && value.isBlank()))) {
                val handlerClass = ifNull.handler.java

                val handler = try {
                    handlerClass.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    logger?.error("Failed to instantiate handler: ${handlerClass.simpleName}", e)
                    continue
                }

                try {
                    @Suppress("UNCHECKED_CAST")
                    (handler as ErrorHandlerWith).handle(value)
                    logger?.info("Executed handler ${handlerClass.simpleName} for field '${field.name}'")
                } catch (e: Exception) {
                    logger?.error("Exception in handler '${handlerClass.simpleName}' for '${field.name}'", e)
                }
            }

            val customValidator = field.getAnnotation(CustomValidateHandler::class.java)
            if (customValidator != null) {
                runCatching {
                    val validator = customValidator.handler.java.getDeclaredConstructor().newInstance()
                    validator.validate(value, config)
                    logger?.info("CustomValidator '${validator::class.java.simpleName}' executed for field '$fieldName'")
                }.onFailure { e ->
                    logger?.error("Failed to execute CustomValidator for field '$fieldName'", e)
                    throw IllegalStateException("Custom validation failed for field '$fieldName': ${e.message}", e)
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
                    logger?.warn("No DefaultHandler registered for '${fieldType.simpleName}' to parse @Default on '${field.name}'")
                    null
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
            } else {
                logger?.warn("DefaultHandler returned null for '${field.name}'")
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

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> constructConfig(clazz: Class<T>, rawMap: Map<String, Any?>): T {
        val kClazz = clazz.kotlin
        val rawMutableMap = rawMap.toMutableMap()

        kClazz.primaryConstructor?.let { ctor ->
            ctor.parameters.forEach { param ->
                val name = param.name ?: return@forEach
                val paramClass = (param.type.javaType as? Class<*>) ?: return@forEach

                if (!rawMutableMap.containsKey(name)) {
                    param.findAnnotation<Default>()?.let { defaultAnn ->
                        val handler = DefaultHandlers.get(paramClass)
                        if (handler != null) {
                            try {
                                val defaultVal = handler.parse(defaultAnn.value, paramClass)
                                rawMutableMap[name] = defaultVal
                                logger?.info("Injected @Default for '$name': $defaultVal")
                            } catch (e: Exception) {
                                logger?.error("Failed to parse @Default for parameter '$name'", e)
                            }
                        } else {
                            logger?.warn("No DefaultHandler for parameter '$name' of type ${paramClass.simpleName}")
                        }
                    }
                }

                if (rawMutableMap.containsKey(name)) {
                    param.findAnnotation<CustomLoader>()?.let { customLoader ->
                        try {
                            val loader = customLoader.loader.java.getDeclaredConstructor().newInstance()
                            val originalValue = rawMutableMap[name]
                            val loadedValue = loader.load(originalValue)
                            rawMutableMap[name] = loadedValue
                            logger?.info("Applied CustomLoader for '$name': $loadedValue")
                        } catch (e: Exception) {
                            logger?.error("Failed to load custom value for parameter '$name'", e)
                        }
                    }
                }
            }

            val args = ctor.parameters.associateWith { param ->
                val name = param.name ?: return@associateWith null
                rawMutableMap.entries.find { it.key.equals(name, ignoreCase = true) }?.value
            }

            return ctor.callBy(args)
        }

        if (clazz.isRecord) {
            val components = clazz.recordComponents
            val ctor = clazz.declaredConstructors.first()

            for (comp in components) {
                val name = comp.name

                if (!rawMutableMap.containsKey(name)) {
                    val defaultAnn = comp.getAnnotation(Default::class.java)
                    if (defaultAnn != null) {
                        val handler = DefaultHandlers.get(comp.type)
                        if (handler != null) {
                            try {
                                val defaultVal = handler.parse(defaultAnn.value, comp.type)
                                rawMutableMap[name] = defaultVal
                                logger?.info("Injected @Default for record component '$name': $defaultVal")
                            } catch (e: Exception) {
                                logger?.error("Failed to parse @Default for record component '$name'", e)
                            }
                        } else {
                            logger?.warn("No DefaultHandler for record component '${comp.type.simpleName}'")
                        }
                    }
                }

                if (rawMutableMap.containsKey(name)) {
                    val customLoaderAnn = comp.getAnnotation(CustomLoader::class.java)
                    if (customLoaderAnn != null) {
                        try {
                            val loader = customLoaderAnn.loader.java.getDeclaredConstructor().newInstance()
                            val originalValue = rawMutableMap[name]
                            val loadedValue = loader.load(originalValue)
                            rawMutableMap[name] = loadedValue
                            logger?.info("Applied CustomLoader for record component '$name': $loadedValue")
                        } catch (e: Exception) {
                            logger?.error("Failed to apply CustomLoader for record component '$name'", e)
                        }
                    }
                }
            }

            val args = components.map { comp ->
                val key = comp.getAnnotation(NamedRecord::class.java)?.value ?: comp.name
                rawMutableMap.entries.find { it.key.equals(key, ignoreCase = true) }?.value
            }.toTypedArray()

            return ctor.newInstance(*args) as T
        }

        throw IllegalArgumentException("Class ${clazz.simpleName} must have a primary constructor or be a record")
    }

    private fun loadFromFile(): T {
        val rawMap = Files.newInputStream(file).use { parser.parse(it) }
        return constructConfig(clazz, rawMap)
    }
}