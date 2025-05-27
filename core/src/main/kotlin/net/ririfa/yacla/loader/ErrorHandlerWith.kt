package net.ririfa.yacla.loader

import net.ririfa.yacla.loader.util.ContextType

/**
 * An interface for handling errors with a specific context type.
 *
 * @param T The type of the context that will be passed to the handler.
 */
interface ErrorHandlerWith<T : Any?> {
    fun handle(
        fieldValue: Any?,
        configInstance: Any,
        context: T,
        contextType: ContextType
    )
}