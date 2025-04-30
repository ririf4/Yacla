package net.ririfa.yacla.parser

import java.io.InputStream
import java.io.OutputStream

/**
 * Interface for parsing and writing configuration data.
 *
 * Implementations of this interface are responsible for reading configuration
 * files (YAML, JSON, TOML, etc.) into Java objects and writing them back to files.
 */
interface ConfigParser {

    /**
     * Set of file extensions this parser supports (e.g., ["yml", "yaml"]).
     */
    val supportedExtensions: Set<String>

    /**
     * Parses the input stream into an instance of the given class.
     *
     * @param input The input stream to read from.
     * @param clazz The class of the target type.
     * @return A deserialized config object.
     * @throws Exception if deserialization fails.
     */
    fun <T : Any> parse(input: InputStream, clazz: Class<T>): T

    /**
     * Writes the given config object to the output stream.
     *
     * @param output The output stream to write to.
     * @param config The config object to serialize.
     * @throws Exception if serialization fails.
     */
    fun <T : Any> write(output: OutputStream, config: T)
}
