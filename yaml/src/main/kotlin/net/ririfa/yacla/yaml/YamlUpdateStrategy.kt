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
import org.snakeyaml.engine.v2.nodes.NodeTuple
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.parser.ParserImpl
import org.snakeyaml.engine.v2.scanner.StreamReader
import java.io.InputStreamReader

/**
 * UpdateStrategy implementation for YAML that merges default config into existing config,
 * preserving user-defined values and as many comments as possible (including nested).
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

        val defaultVersion = getVersion(defaultMappingNode)
        val currentVersion = getVersion(currentMappingNode)

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

    private fun loadSingleNode(inputStream: java.io.InputStream): Node {
        return loadSingleNode(InputStreamReader(inputStream))
    }

    private fun loadSingleNode(inputReader: InputStreamReader): Node {
        val parser = ParserImpl(loadSettings, StreamReader(loadSettings, inputReader))
        val composer = Composer(loadSettings, parser)
        return composer.singleNode.orElseThrow { IllegalStateException("No YAML document found") }
    }

    private fun getVersion(node: MappingNode): String {
        return node.value
            .find { (it.keyNode as? ScalarNode)?.value?.equals("version", ignoreCase = true) == true }
            ?.valueNode
            ?.let { (it as? ScalarNode)?.value }
            ?: "1.0.0"
    }

    private fun mergeMappingNode(baseNode: MappingNode, overrideNode: MappingNode) {
        val baseTuples = baseNode.value.toMutableList()

        for (overrideTuple in overrideNode.value) {
            val overrideKeyNode = overrideTuple.keyNode
            val overrideValueNode = overrideTuple.valueNode

            val existingTupleIndex = baseTuples.indexOfFirst { sameKey(it.keyNode, overrideKeyNode) }

            if (existingTupleIndex != -1) {
                val existingTuple = baseTuples[existingTupleIndex]

                mergeComments(existingTuple.keyNode, overrideKeyNode)
                mergeComments(existingTuple.valueNode, overrideValueNode)

                if (existingTuple.valueNode is MappingNode && overrideValueNode is MappingNode) {
                    mergeMappingNode(existingTuple.valueNode as MappingNode, overrideValueNode)
                } else {
                    mergeComments(overrideValueNode, existingTuple.valueNode)
                    baseTuples[existingTupleIndex] = NodeTuple(existingTuple.keyNode, overrideValueNode)
                }
            } else {
                mergeComments(overrideKeyNode, overrideKeyNode)
                mergeComments(overrideValueNode, overrideValueNode)
                baseTuples.add(NodeTuple(overrideKeyNode, overrideValueNode))
            }
        }

        baseNode.value.clear()
        baseNode.value.addAll(baseTuples)
    }

    private fun mergeComments(target: Node, source: Node) {
        target.blockComments = mergeCommentLists(target.blockComments, source.blockComments)
        target.inLineComments = mergeCommentLists(target.inLineComments, source.inLineComments)
        target.endComments = mergeCommentLists(target.endComments, source.endComments)
    }

    private fun mergeCommentLists(
        existing: List<org.snakeyaml.engine.v2.comments.CommentLine>?,
        incoming: List<org.snakeyaml.engine.v2.comments.CommentLine>?
    ): List<org.snakeyaml.engine.v2.comments.CommentLine>? {
        return (existing ?: emptyList()) + (incoming ?: emptyList())
    }

    private fun sameKey(key1: Node, key2: Node): Boolean {
        if (key1 is ScalarNode && key2 is ScalarNode) {
            return key1.value == key2.value
        }
        return false
    }
}
