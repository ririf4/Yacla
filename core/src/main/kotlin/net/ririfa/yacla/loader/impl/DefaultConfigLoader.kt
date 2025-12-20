package net.ririfa.yacla.loader.impl

import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import net.ririfa.yacla.schema.FieldDefBuilder
import net.ririfa.yacla.schema.FieldDefinition
import net.ririfa.yacla.schema.YaclaSchema
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.primaryConstructor

class DefaultConfigLoader<T : Any>(
    private val clazz: Class<T>,
    private val parser: ConfigParser,
    private val schema: YaclaSchema<T>?,
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

    private fun applySchema(
        schema: YaclaSchema<T>,
        raw: Map<String, Any?>
    ): Map<String, Any?> {
        val builder = FieldDefBuilder<T>()
        schema.configure(builder)
        val defs = builder.build()

        val out = raw.toMutableMap()

        for ((name, def) in defs) {
            val yamlKey = (def.yamlName ?: name).lowercase()
            val current = out[yamlKey]

            if (current == null || (current is String && current.isBlank())) {
                if (def.defaultValue !== FieldDefinition.NO_DEFAULT) {
                    out[yamlKey] = def.defaultValue
                    continue
                }

                if (def.required) {
                    throw IllegalArgumentException("Field '$yamlKey' is required but missing.")
                }

                if (def.softRequired) {
                    logger?.warn("Soft-required field '$yamlKey' is missing.")
                }

                if (def.nullHandler != null) {
                    def.nullHandler.handle(null, out)
                }

                continue
            }

            if (def.loader != null) {
                out[yamlKey] = def.loader.load(current)
            }

            if (def.rangeMin != null && def.rangeMax != null && current is Number) {
                val v = current.toLong()
                if (v < def.rangeMin || v > def.rangeMax) {
                    throw IllegalArgumentException("Field '$yamlKey' out of range (${def.rangeMin}..${def.rangeMax}) : $v")
                }
            }

            for (validator in def.validators) {
                validator(current)
            }
        }

        return out
    }

    private fun loadFromFile(): T {
        val raw = Files.newInputStream(file).use { parser.parse(it) }

        val caseInsensitiveMap = raw.entries.associate { (k, v) ->
            k.lowercase() to v
        }

        val processed = if (schema != null) {
            applySchema(schema, caseInsensitiveMap)
        } else {
            caseInsensitiveMap
        }

        return constructConfig(clazz, processed)
    }

    private fun <T : Any> constructConfig(clazz: Class<T>, rawMap: Map<String, Any?>): T {
        val kClazz = clazz.kotlin
        val ctor = kClazz.primaryConstructor
            ?: throw IllegalArgumentException("Config class must have a primary constructor")

        val args = ctor.parameters.associateWith { param ->
            rawMap[param.name?.lowercase()]
        }

        return ctor.callBy(args)
    }
}
