package net.ririfa.yacla.yaml

import net.ririfa.yacla.loader.UpdateStrategyRegistry
import net.ririfa.yacla.parser.ConfigParser
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.InputStream
import java.io.OutputStream

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

    /**
     * Parses YAML content from the given [InputStream] into a [Map] of key-value pairs.
     *
     * @param input the input stream containing YAML data
     * @return a [Map] representing the YAML document root
     * @throws IllegalStateException if the parsed content is null
     * @throws IllegalArgumentException if the parsed content is not a [Map]
     */
    override fun parse(input: InputStream): Map<String, Any> {
        val obj = loader.loadFromInputStream(input)
            ?: throw IllegalStateException("Parsed config is null")

        @Suppress("UNCHECKED_CAST")
        return obj as? Map<String, Any>
            ?: throw IllegalArgumentException("Expected YAML to produce Map<String, Any>, got ${obj::class.java}")
    }

    /**
     * Serializes the given configuration object into YAML and writes it to the output stream.
     *
     * @param output the output stream to write YAML content
     * @param config the configuration object to serialize
     */
    override fun <T : Any> write(output: OutputStream, config: T) {
        val yamlString = dumper.dumpToString(config)
        output.writer().use { it.write(yamlString) }
    }
}
