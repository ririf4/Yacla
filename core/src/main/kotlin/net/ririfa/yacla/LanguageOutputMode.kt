package net.ririfa.yacla

/**
 * Controls how localized default configuration resources are copied to disk.
 */
enum class LanguageOutputMode {
    /**
     * Copy the resource matching the selected language to the configured target file.
     */
    SINGLE_FILE,

    /**
     * Copy one file per configured language, suffixing the target file name with the language.
     */
    MULTIPLE_FILES
}
