package net.ririfa.yacla.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import net.ririfa.yacla.loader.UpdateStrategy
import net.ririfa.yacla.loader.util.UpdateContext
import net.ririfa.yacla.loader.util.isOlderVersion

/**
 * Default [UpdateStrategy] implementation for JSON configuration files.
 *
 * This strategy updates an existing JSON configuration file by merging it
 * with the default configuration resource, preserving user-defined values
 * while adding new keys or updating missing entries.
 *
 * Typically used internally by the JSON parser module.
 */
class JsonUpdateStrategy : UpdateStrategy {
    private val mapper = ObjectMapper()

    override fun updateIfNeeded(context: UpdateContext): Boolean {
        val logger = context.logger
        logger?.info("Checking if config update is needed: ${context.targetFile}")

        val defaultInputStream = javaClass.getResourceAsStream(context.resourcePath)
            ?: throw IllegalStateException("Default config not found at ${context.resourcePath}")

        val currentInputStream = context.targetFile.toFile().inputStream()

        val defaultNode = mapper.readTree(defaultInputStream) as? ObjectNode
            ?: throw IllegalStateException("The default config root must be an ObjectNode")

        val currentNode = mapper.readTree(currentInputStream) as? ObjectNode
            ?: throw IllegalStateException("The current config root must be an ObjectNode")

        val defaultVersion = defaultNode.get("version")?.asText() ?: "1.0.0"
        val currentVersion = currentNode.get("version")?.asText() ?: "1.0.0"

        if (!isOlderVersion(currentVersion, defaultVersion)) {
            logger?.info("Config is up-to-date (version $currentVersion >= $defaultVersion)")
            return false
        }

        logger?.info("Updating config from version $currentVersion to $defaultVersion")

        mergeObjectNode(defaultNode, currentNode)

        val jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(defaultNode)
        context.targetFile.toFile().writeText(jsonString)

        logger?.info("Config updated successfully!")

        return true
    }

    private fun mergeObjectNode(baseNode: ObjectNode, overrideNode: ObjectNode) {
        val fieldNames = overrideNode.fieldNames()
        while (fieldNames.hasNext()) {
            val fieldName = fieldNames.next()
            val overrideValue = overrideNode.get(fieldName)
            val baseValue = baseNode.get(fieldName)

            if (baseValue != null && baseValue.isObject && overrideValue.isObject) {
                mergeObjectNode(baseValue as ObjectNode, overrideValue as ObjectNode)
            } else {
                baseNode.set<JsonNode>(fieldName, overrideValue)
            }
        }
    }
}
