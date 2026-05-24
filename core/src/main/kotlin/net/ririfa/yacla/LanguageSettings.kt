package net.ririfa.yacla

import java.util.Locale

/**
 * Shared language settings for localized configuration output.
 *
 * @property language selected language. Defaults to the current JVM locale.
 * @property defaultLanguage fallback language used when the selected resource does not exist.
 * @property outputMode whether localized resources are copied to a single file or multiple files.
 * @property outputLanguages languages copied when [outputMode] is [LanguageOutputMode.MULTIPLE_FILES].
 */
data class LanguageSettings @JvmOverloads constructor(
    val language: String = systemLanguage(),
    val defaultLanguage: String = "en",
    val outputMode: LanguageOutputMode = LanguageOutputMode.SINGLE_FILE,
    val outputLanguages: Set<String> = emptySet()
) {
    companion object {
        @JvmStatic
        fun systemLanguage(): String {
            return Locale.getDefault().toLanguageTag().replace('-', '_')
        }
    }
}
