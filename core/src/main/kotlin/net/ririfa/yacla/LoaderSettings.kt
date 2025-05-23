package net.ririfa.yacla

import net.ririfa.yacla.logger.YaclaLogger
import net.ririfa.yacla.parser.ConfigParser

class LoaderSettings(
    val parser: ConfigParser,
    val logger: YaclaLogger? = null,
    val autoUpdate: Boolean = false
)
