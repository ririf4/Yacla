package net.ririfa.yacla.constructor

/**
 * Converts a key-value map into an instance of the given class.
 *
 * Implementations can use reflection, codegen, or any mapping strategy.
 */
interface ObjectConstructor {
    /**
     * Constructs an object of type [clazz] from the provided [map].
     *
     * @param map The key-value map representing field names to values.
     * @param clazz The target class to instantiate.
     * @return An instance of [clazz].
     */
    fun <T : Any> construct(map: Map<String, Any?>, clazz: Class<T>): T
}