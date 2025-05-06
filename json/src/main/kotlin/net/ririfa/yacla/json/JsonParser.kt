package net.ririfa.yacla.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import net.ririfa.yacla.constructor.ObjectConstructor
import net.ririfa.yacla.constructor.impl.DefaultObjectConstructor
import net.ririfa.yacla.exception.YaclaConfigException
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.parser.ConfigParser
import java.io.InputStream
import java.io.OutputStream

class JsonParser(
    private val objectConstructor: ObjectConstructor = DefaultObjectConstructor()
) : ConfigParser {
    private val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    init {
        UpdateStrategyRegistry.register(JsonParser::class.java, JsonUpdateStrategy())
    }

    override val supportedExtensions: Set<String> = setOf("json")

    override fun <T : Any> parse(input: InputStream, clazz: Class<T>): T {
        val bytes = input.readBytes()
        return try {
            mapper.readValue(bytes, clazz)
        } catch (_: Exception) {
            val map: Map<String, Any?> = mapper.readValue(bytes)
            try {
                objectConstructor.construct(map, clazz)
            } catch (ex: Exception) {
                throw YaclaConfigException("Failed to construct ${clazz.simpleName} from JSON map", ex)
            }
        }
    }

    override fun <T : Any> write(output: OutputStream, config: T) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, config)
    }
}
