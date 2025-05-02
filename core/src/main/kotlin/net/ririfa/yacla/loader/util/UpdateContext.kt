package net.ririfa.yacla.loader.util

import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser
import java.nio.file.Path

/**
 * Represents the context used during a configuration update operation.
 *
 * This context provides all necessary information to perform an update:
 * - the target configuration file on disk
 * - the resource path to the default configuration inside the classpath
 * - the parser being used for this configuration format
 * - an optional logger for outputting update process logs
 *
 * It is passed to [net.ririfa.yacla.loader.UpdateStrategy.updateIfNeeded] to provide consistent access
 * to the environment and resources required for updating the config file.
 *
 * @property parser the [ConfigParser] used to parse the configuration file
 * @property targetFile the [Path] to the target configuration file on disk
 * @property resourcePath the path to the default configuration resource in the classpath
 * @property logger an optional [YaclaLogger] for logging update operations
 */
data class UpdateContext(
    val parser: ConfigParser,
    val targetFile: Path,
    val resourcePath: String,
    val logger: YaclaLogger?
)
