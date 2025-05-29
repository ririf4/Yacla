package net.ririfa.yacla.json

import com.fasterxml.jackson.core.type.TypeReference
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

    override fun parse(input: InputStream): Map<String, Any> {
        return try {
            mapper.readValue(input, object : TypeReference<Map<String, Any>>() {})
        } catch (ex: Exception) {
            throw YaclaConfigException("Failed to parse JSON", ex)
        }
    }

    override fun <T : Any> write(output: OutputStream, config: T) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, config)
    }
}
