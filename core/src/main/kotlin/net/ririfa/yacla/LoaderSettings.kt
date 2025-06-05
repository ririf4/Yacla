package net.ririfa.yacla

import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser

/**
 * Configuration object used to supply default settings to one or more config loaders.
 *
 * This class is typically passed to [Yacla.withDefaults] to define shared configuration
 * such as the parser, logger, and whether automatic updates should be applied.
 *
 * @property parser the [ConfigParser] used for loading and writing config files
 * @property logger optional [YaclaLogger] to receive info/warning/error output
 * @property autoUpdate if true, triggers automatic config update via [net.ririfa.yacla.loader.UpdateStrategy]
 */
class LoaderSettings(
    val parser: ConfigParser,
    val logger: YaclaLogger? = null,
    val autoUpdate: Boolean = false
)
