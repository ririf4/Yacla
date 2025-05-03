package net.ririfa.yacla.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import net.ririfa.yacla.annotation.NamedRecord
import net.ririfa.yacla.exception.YaclaConfigException
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.parser.ConfigParser
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.full.primaryConstructor

class JsonParser : ConfigParser {
    val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    init {
        UpdateStrategyRegistry.register(JsonParser::class.java, JsonUpdateStrategy())
    }

    override val supportedExtensions: Set<String> = setOf("json")

    override fun <T : Any> parse(input: InputStream, clazz: Class<T>): T {
        val bytes = input.readBytes()
        return try {
            mapper.readValue(bytes, clazz)
        } catch (_: Exception) {
            val map: Map<String, Any> = mapper.readValue(bytes)

            when {
                clazz.kotlin.primaryConstructor != null -> {
                    val ctor = clazz.kotlin.primaryConstructor!!
                    val args = ctor.parameters.associateWith { param ->
                        map.entries.find { it.key.equals(param.name, ignoreCase = true) }?.value
                    }
                    ctor.callBy(args)
                }

                clazz.isRecord -> {
                    val components = clazz.recordComponents
                    val ctor = clazz.declaredConstructors.first()
                    val args = components.map { comp ->
                        val key = comp.getAnnotation(NamedRecord::class.java)?.value ?: comp.name
                        map.entries.find { it.key.equals(key, ignoreCase = true) }?.value
                    }.toTypedArray()
                    @Suppress("UNCHECKED_CAST")
                    ctor.newInstance(*args) as T
                }

                else -> {
                    throw YaclaConfigException("Class ${clazz.simpleName} does not have a primary constructor or is not a record")
                }
            }
        }
    }

    override fun <T : Any> write(output: OutputStream, config: T) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, config)
    }
}
