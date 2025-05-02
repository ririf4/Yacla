package net.ririfa.yacla.loader

import net.ririfa.yacla.parser.ConfigParser

/**
 * A registry that maps [ConfigParser] implementations to their corresponding [UpdateStrategy].
 *
 * This registry allows associating a parser class with an update strategy so that
 * [ConfigLoader] can automatically perform configuration updates when needed.
 *
 * Typically, parser implementations register their strategy automatically (e.g., inside
 * their constructor or init block), but additional strategies can be registered manually
 * if necessary.
 *
 * Example usage:
 * ```
 * UpdateStrategyRegistry.register(MyParser::class.java, MyUpdateStrategy())
 * ```
 */
object UpdateStrategyRegistry {
    private val map = mutableMapOf<Class<out ConfigParser>, UpdateStrategy>()

    /**
     * Registers an [UpdateStrategy] for the given parser class.
     * If a strategy is already registered for this parser class, this method does nothing.
     *
     * @param parserClass the parser class to associate with the strategy
     * @param strategy the update strategy implementation
     */
    fun register(parserClass: Class<out ConfigParser>, strategy: UpdateStrategy) {
        if (!map.containsKey(parserClass)) {
            map[parserClass] = strategy
        }
    }

    /**
     * Retrieves the registered [UpdateStrategy] for the given parser instance, or null if none registered.
     *
     * @param parser the parser instance
     * @return the associated [UpdateStrategy], or null if not registered
     */
    fun strategyFor(parser: ConfigParser): UpdateStrategy? =
        map[parser::class.java]
}