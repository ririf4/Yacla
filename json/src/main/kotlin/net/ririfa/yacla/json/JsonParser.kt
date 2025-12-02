package net.ririfa.yacla.json

import net.ririfa.yacla.exception.YaclaConfigException
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.parser.ConfigParser
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.io.InputStream
import java.io.OutputStream

class JsonParser : ConfigParser {
    val mapper: ObjectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()

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
