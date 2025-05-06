package net.ririfa.yacla.yaml

import net.ririfa.yacla.annotation.NamedRecord
import net.ririfa.yacla.constructor.ObjectConstructor
import net.ririfa.yacla.constructor.impl.DefaultObjectConstructor
import net.ririfa.yacla.exception.YaclaConfigException
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
class YamlParser(
    private val objectConstructor: ObjectConstructor = DefaultObjectConstructor()
) : ConfigParser {
    private val loadSettings = LoadSettings.builder().build()
    private val loader = Load(loadSettings)

    override val supportedExtensions: Set<String> = setOf("yml", "yaml")

    override fun <T : Any> parse(input: InputStream, clazz: Class<T>): T {
        val obj = loader.loadFromInputStream(input)
            ?: throw YaclaConfigException("YAML is empty")

        @Suppress("UNCHECKED_CAST")
        val map = obj as? Map<String, Any?>
            ?: throw YaclaConfigException("Expected Map<String, Any?> but got ${obj::class.simpleName}")

        return objectConstructor.construct(map, clazz)
    }

    override fun <T : Any> write(output: OutputStream, config: T) {
        val dumper = Dump(DumpSettings.builder().build())
        val yamlString = dumper.dumpToString(config)
        output.writer().use { it.write(yamlString) }
    }
}
