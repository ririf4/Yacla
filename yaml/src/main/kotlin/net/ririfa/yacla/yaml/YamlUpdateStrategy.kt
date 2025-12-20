package net.ririfa.yacla.yaml

import net.ririfa.yacla.loader.UpdateStrategy
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.loader.util.isOlderVersion
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.StreamDataWriter
import org.snakeyaml.engine.v2.composer.Composer
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.NodeTuple
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.parser.ParserImpl
import org.snakeyaml.engine.v2.scanner.StreamReader
import java.io.InputStreamReader
import java.io.StringWriter

/**
 * Internal helper class to support dumping to String
 */
private class StreamToStringWriter : StringWriter(), StreamDataWriter {
    override fun write(str: String) {
        super.write(str)
    }

    override fun write(str: String, off: Int, len: Int) {
        super.write(str, off, len)
    }

    override fun flush() {
        super<StreamDataWriter>.flush()
    }
}
/**
 * Default [UpdateStrategy] implementation for YAML configuration files.
 *
 * This strategy updates an existing YAML configuration file by merging it
 * with the default configuration resource, preserving user-defined values
 * while adding new keys or updating missing entries.
 *
 * It uses [org.snakeyaml.engine.v2] to parse and manipulate the YAML AST,
 * allowing structural merging while keeping comments intact at the root level.
 *
 * Typically used internally by the YAML parser module.
 */
class YamlUpdateStrategy : UpdateStrategy {
    private val loadSettings = LoadSettings.builder()
        .setParseComments(true)
        .build()
    private val dumpSettings = DumpSettings.builder()
        .setDumpComments(true)
        .build()
    private val dumper = Dump(dumpSettings)

    /**
     * Checks if the config file needs updating by comparing versions,
     * merges missing or updated keys preserving user data,
     * and writes the updated YAML back to the file.
     *
     * @param context provides information about the file, resource, parser, and logger
     * @return true if the config was updated, false if already up-to-date
     */
    override fun updateIfNeeded(context: UpdateContext): Boolean {
        val logger = context.logger
        logger?.info("Checking if config update is needed: ${context.targetFile}")

        val defaultInputStream = javaClass.getResourceAsStream(context.resourcePath)
            ?: throw IllegalStateException("Default config not found at ${context.resourcePath}")

        val currentInputStream = context.targetFile.toFile().inputStream()

        val defaultNode = loadSingleNode(defaultInputStream)
        val currentNode = loadSingleNode(currentInputStream)

        val defaultMappingNode = defaultNode as? MappingNode
            ?: throw IllegalStateException("The default config root must be a mapping node")

        val currentMappingNode = currentNode as? MappingNode
            ?: throw IllegalStateException("The current config root must be a mapping node")

        val defaultVersion = defaultMappingNode.value
            .find { (it.keyNode as? ScalarNode)?.value?.uppercase() == "VERSION" }
            ?.valueNode
            ?.let { (it as? ScalarNode)?.value }
            ?: "1.0.0"

        val currentVersion = currentMappingNode.value
            .find { (it.keyNode as? ScalarNode)?.value?.uppercase() == "VERSION" }
            ?.valueNode
            ?.let { (it as? ScalarNode)?.value }
            ?: "1.0.0"

        if (!isOlderVersion(currentVersion, defaultVersion)) {
            logger?.info("Config is up-to-date (version $currentVersion >= $defaultVersion)")
            return false
        }

        logger?.info("Updating config from version $currentVersion to $defaultVersion")

        mergeMappingNode(defaultMappingNode, currentMappingNode)

        // Use dumpNode with StreamToStringWriter
        val writer = StreamToStringWriter()
        dumper.dumpNode(defaultMappingNode, writer)
        val yamlString = writer.toString()

        context.targetFile.toFile().writeText(yamlString)

        logger?.info("Config updated successfully!")

        return true
    }

    /**
     * Loads a single YAML document node from the given [InputStreamReader].
     *
     * @param input the stream reader of the YAML content
     * @return the root YAML AST node
     * @throws IllegalStateException if no YAML document is found
     */
    private fun loadSingleNode(input: InputStreamReader): Node {
        val parser = ParserImpl(loadSettings, StreamReader(loadSettings, input))
        val composer = Composer(loadSettings, parser)
        return composer.singleNode.orElseThrow { IllegalStateException("No YAML document found") }
    }

    /**
     * Loads a single YAML document node from the given [java.io.InputStream].
     *
     * @param inputStream the input stream of the YAML content
     * @return the root YAML AST node
     * @throws IllegalStateException if no YAML document is found
     */
    private fun loadSingleNode(inputStream: java.io.InputStream): Node {
        return loadSingleNode(InputStreamReader(inputStream))
    }

    /**
     * Merges keys and values from [overrideNode] into [baseNode], recursively merging mappings.
     * Existing user keys are preserved and updated, new keys are added.
     *
     * @param baseNode the YAML mapping node to merge into (usually defaults)
     * @param overrideNode the YAML mapping node to merge from (usually user config)
     */
    private fun mergeMappingNode(baseNode: MappingNode, overrideNode: MappingNode) {
        val newTuples = baseNode.value.map { baseTuple ->
            val baseKey = baseTuple.keyNode
            val baseValue = baseTuple.valueNode

            val keyValue = (baseKey as? ScalarNode)?.value
            if (keyValue?.uppercase() == "VERSION") {
                return@map baseTuple
            }

            val overrideTuple = overrideNode.value.find { sameKey(it.keyNode, baseKey) }

            if (overrideTuple != null) {
                val overrideValue = overrideTuple.valueNode

                if (baseValue is MappingNode && overrideValue is MappingNode) {
                    mergeMappingNode(baseValue, overrideValue)
                    baseTuple
                } else {
                    NodeTuple(baseKey, overrideValue)
                }
            } else {
                baseTuple
            }
        }

        baseNode.value.clear()
        baseNode.value.addAll(newTuples)
    }

    /**
     * Transfers all comment nodes from one YAML AST node to another.
     *
     * @param from the node from which to copy comments
     * @param to the node to which comments will be applied
     */
    private fun transferComments(from: Node, to: Node) {
        to.blockComments = from.blockComments
        to.inLineComments = from.inLineComments
        to.endComments = from.endComments
    }

    /**
     * Checks if two YAML AST nodes represent the same scalar key.
     *
     * @param key1 first YAML key node
     * @param key2 second YAML key node
     * @return true if both are scalar nodes with equal values, false otherwise
     */
    private fun sameKey(key1: Node, key2: Node): Boolean {
        if (key1 is ScalarNode && key2 is ScalarNode) {
            return key1.value == key2.value
        }
        return false
    }
}