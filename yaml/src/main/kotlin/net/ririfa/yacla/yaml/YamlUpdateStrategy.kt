package net.ririfa.yacla.yaml

import net.ririfa.yacla.loader.UpdateStrategy
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.loader.util.isOlderVersion
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.composer.Composer
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.parser.ParserImpl
import org.snakeyaml.engine.v2.scanner.StreamReader
import java.io.InputStreamReader

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
    private val loadSettings = LoadSettings.builder().build()
    private val dumpSettings = DumpSettings.builder().build()
    private val dumper = Dump(dumpSettings)

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
        val yamlString = dumper.dumpToString(defaultMappingNode)
        context.targetFile.toFile().writeText(yamlString)

        logger?.info("Config updated successfully!")

        return true
    }

    private fun loadSingleNode(input: InputStreamReader): Node {
        val parser = ParserImpl(loadSettings, StreamReader(loadSettings, input))
        val composer = Composer(loadSettings, parser)
        return composer.singleNode.orElseThrow { IllegalStateException("No YAML document found") }
    }

    private fun loadSingleNode(inputStream: java.io.InputStream): Node {
        return loadSingleNode(InputStreamReader(inputStream))
    }

    private fun mergeMappingNode(baseNode: MappingNode, overrideNode: MappingNode) {
        val baseTuples = baseNode.value.toMutableList()

        for (overrideTuple in overrideNode.value) {
            val overrideKey = overrideTuple.keyNode
            val existingTuple = baseTuples.find { sameKey(it.keyNode, overrideKey) }

            if (existingTuple != null) {
                if (existingTuple.valueNode is MappingNode && overrideTuple.valueNode is MappingNode) {
                    mergeMappingNode(
                        existingTuple.valueNode as MappingNode,
                        overrideTuple.valueNode as MappingNode
                    )
                } else {
                    baseTuples.remove(existingTuple)
                    baseTuples.add(overrideTuple)
                }
            } else {
                baseTuples.add(overrideTuple)
            }
        }

        baseNode.value.clear()
        baseNode.value.addAll(baseTuples)
    }

    private fun sameKey(key1: Node, key2: Node): Boolean {
        if (key1 is ScalarNode && key2 is ScalarNode) {
            return key1.value == key2.value
        }
        return false
    }
}
