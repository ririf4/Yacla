package net.ririfa.yacla.logger.impl

import net.ririfa.yacla.logger.YaclaLogger
import org.slf4j.LoggerFactory

object SLF4JYaclaLogger : YaclaLogger {
    private val logger = LoggerFactory.getLogger("Yacla")

    override fun info(message: String) {
        logger.info(message)
    }

    override fun warn(message: String) {
        logger.warn(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }
}