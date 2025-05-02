package net.ririfa.yacla.yaml

import net.ririfa.yacla.annotation.NamedRecord
import net.ririfa.yacla.exception.YaclaConfigException
import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.parser.ConfigParser
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.full.primaryConstructor

/**
 * Default [ConfigParser] implementation for YAML configuration files.
 *
 * This parser uses SnakeYAML Engine to parse and serialize YAML configuration data.
 * It supports deserialization into Kotlin data classes (using primary constructors)
 * or Java records (with optional [NamedRecord] annotation for component name mapping).
 *
 * It also automatically registers a [YamlUpdateStrategy] for handling configuration updates.
 *
 * Typically passed to [net.ririfa.yacla.loader.ConfigLoaderBuilder.parser] when using Yacla with YAML.
 *
 * Example:
 * ```
 * val loader = Yacla.loader<MyConfig>()
 *     .fromResource("/defaults/config.yml")
 *     .toFile(Paths.get("config.yml"))
 *     .parser(YamlParser())
 *     .load()
 * ```
 */
class YamlParser : ConfigParser {
    private val loadSettings = LoadSettings.builder().build()
    private val dumpSettings = DumpSettings.builder().build()
    private val loader = Load(loadSettings)
    private val dumper = Dump(dumpSettings)

    init {
        UpdateStrategyRegistry.register(YamlParser::class.java, YamlUpdateStrategy())
    }

    override val supportedExtensions: Set<String> = setOf("yml", "yaml")

    override fun <T : Any> parse(input: InputStream, clazz: Class<T>): T {
        val bytes = input.readBytes()

        val loadedObj = loader.loadFromInputStream(ByteArrayInputStream(bytes))
            ?: throw IllegalStateException("Parsed config is null")

        @Suppress("UNCHECKED_CAST")
        val map = loadedObj as? Map<String, Any>
            ?: throw YaclaConfigException("Expected Map<String, Any> from YAML, but got ${loadedObj::class.simpleName}")

        return when {
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
                    ?: throw YaclaConfigException("Cannot resolve name for record component $comp")
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

    override fun <T : Any> write(output: OutputStream, config: T) {
        val yamlString = dumper.dumpToString(config)
        output.writer().use { it.write(yamlString) }
    }
}
