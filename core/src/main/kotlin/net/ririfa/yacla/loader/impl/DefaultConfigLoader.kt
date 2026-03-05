package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.annotation.BlankToNull
import net.ririfa.yacla.annotation.EnumList
import net.ririfa.yacla.annotation.EnumSet
import net.ririfa.yacla.annotation.Loader
import net.ririfa.yacla.annotation.SetOf
import net.ririfa.yacla.annotation.Warn
import net.ririfa.yacla.exception.YaclaConfigException
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.FieldLoader
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

class DefaultConfigLoader<T : Any>(
    private val clazz: Class<T>,
    private val parser: ConfigParser,
    private val file: Path,
    private val logger: YaclaLogger?,
    private val resourcePath: String
) : ConfigLoader<T> {

    override var config: T = loadFromFile()
        private set

    override fun reload(): ConfigLoader<T> = apply {
        logger?.info("Reloading config from $file")
        config = loadFromFile()
    }

    override fun updateConfig(): Boolean {
        val strategy = UpdateStrategyRegistry.strategyFor(parser)
        return if (strategy != null) {
            val ctx = UpdateContext(parser, file, resourcePath, logger)
            val updated = strategy.updateIfNeeded(ctx)
            if (!updated) logger?.info("Config already up-to-date.")
            updated
        } else {
            logger?.warn("No UpdateStrategy registered for ${parser::class.java.simpleName}")
            false
        }
    }

    private fun loadFromFile(): T {
        val raw = Files.newInputStream(file).use { parser.parse(it) }
        val normalizedMap = raw.entries.associate { (k, v) -> k.lowercase() to v }
        return constructConfig(clazz, normalizedMap)
    }

    private fun constructConfig(clazz: Class<T>, rawMap: Map<String, Any?>): T {
        val kClazz = clazz.kotlin
        val ctor = kClazz.primaryConstructor
            ?: throw YaclaConfigException("Config class ${clazz.simpleName} must have a primary constructor")

        val args = mutableMapOf<KParameter, Any?>()

        for (param in ctor.parameters) {
            val paramName = param.name ?: continue
            val resolvedKey = resolveKey(paramName, rawMap)
            val rawValue = resolvedKey?.let { rawMap[it] }
            val processedValue = processParam(param, rawValue)

            when {
                processedValue != null -> args[param] = processedValue
                param.isOptional -> { /* omit — Kotlin uses the default value */
                }

                param.type.isMarkedNullable -> args[param] = null
                else -> throw YaclaConfigException(
                    "Required field '$paramName' is missing or null in config file: $file"
                )
            }
        }

        return ctor.callBy(args)
    }

    private fun processParam(param: KParameter, rawValue: Any?): Any? {
        val annotations = param.annotations
        var value: Any? = rawValue

        // 1. @Loader — apply custom loader before any built-in coercion
        val loaderAnn = annotations.filterIsInstance<Loader>().firstOrNull()
        if (loaderAnn != null && value != null) {
            @Suppress("UNCHECKED_CAST")
            value = (loaderAnn.value.createInstance() as FieldLoader).load(value)
        }

        // 2. Built-in type coercion
        value = coerce(value, param.type)

        // 3. @BlankToNull — blank String → null
        if (annotations.any { it is BlankToNull } && value is String && value.isBlank()) {
            value = null
        }

        // 4. @SetOf — Collection → Set (empty → null)
        if (annotations.any { it is SetOf } && value is Collection<*>) {
            value = value.toSet().takeIf { it.isNotEmpty() }
        }

        // 5. @EnumList — Collection<*> → List<Enum> (case-insensitive)
        if (annotations.any { it is EnumList } && value is Collection<*>) {
            val elemClass = param.type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
            if (elemClass?.java?.isEnum == true) {
                @Suppress("UNCHECKED_CAST")
                value = value.mapNotNull { elem ->
                    (elemClass.java.enumConstants as Array<Enum<*>>)
                        .firstOrNull { it.name.equals(elem.toString(), ignoreCase = true) }
                }
            }
        }

        // 6. @EnumSet — Collection<*> → Set<Enum> (case-insensitive)
        if (annotations.any { it is EnumSet } && value is Collection<*>) {
            val elemClass = param.type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
            if (elemClass?.java?.isEnum == true) {
                @Suppress("UNCHECKED_CAST")
                value = value.mapNotNull { elem ->
                    (elemClass.java.enumConstants as Array<Enum<*>>)
                        .firstOrNull { it.name.equals(elem.toString(), ignoreCase = true) }
                }.toSet()
            }
        }

        // 7. @Warn — log if still null
        if (value == null) {
            val warnAnn = annotations.filterIsInstance<Warn>().firstOrNull()
            warnAnn?.let { logger?.warn(it.message) }
        }

        return value
    }

    private fun coerce(value: Any?, targetType: KType): Any? {
        if (value == null) return null
        val classifier = targetType.classifier as? KClass<*> ?: return value
        if (classifier.isInstance(value)) return value

        return when {
            classifier == String::class ->
                value.toString()

            classifier == Int::class ->
                (value as? Number)?.toInt() ?: value.toString().toIntOrNull()

            classifier == Long::class ->
                (value as? Number)?.toLong() ?: value.toString().toLongOrNull()

            classifier == Double::class ->
                (value as? Number)?.toDouble() ?: value.toString().toDoubleOrNull()

            classifier == Float::class ->
                (value as? Number)?.toFloat() ?: value.toString().toFloatOrNull()

            classifier == Boolean::class -> when (value) {
                is Boolean -> value
                is String -> value.lowercase().toBooleanStrictOrNull()
                else -> null
            }

            classifier.java.isEnum -> {
                @Suppress("UNCHECKED_CAST")
                (classifier.java.enumConstants as Array<Enum<*>>)
                    .firstOrNull { it.name.equals(value.toString(), ignoreCase = true) }
            }

            classifier == Set::class || classifier == MutableSet::class ->
                (value as? Collection<*>)?.toSet() ?: setOf(value)

            classifier == List::class || classifier == MutableList::class ->
                (value as? Collection<*>)?.toList() ?: listOf(value)

            classifier == Map::class || classifier == MutableMap::class ->
                value as? Map<*, *>

            else -> value
        }
    }

    private fun resolveKey(paramName: String, map: Map<String, Any?>): String? {
        val candidates = linkedSetOf(
            paramName.lowercase(),
            camelToSnake(paramName),
            camelToKebab(paramName)
        )
        return candidates.firstOrNull { map.containsKey(it) }
    }

    private fun camelToSnake(name: String): String =
        name.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

    private fun camelToKebab(name: String): String =
        name.replace(Regex("([a-z])([A-Z])"), "$1-$2").lowercase()
}
