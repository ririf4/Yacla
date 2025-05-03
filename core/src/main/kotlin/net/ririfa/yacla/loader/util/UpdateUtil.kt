package net.ririfa.yacla.loader.util

/**
 * Compares two semantic version strings and determines if the current version is older than the latest version.
 *
 * This function splits the version strings by dots, parses each segment as an integer (defaulting to 0 if parsing fails),
 * and compares corresponding segments from left to right. Missing segments are treated as zeros.
 *
 * Example comparisons:
 * - isOlderVersion("1.2.3", "1.2.4") -> true
 * - isOlderVersion("1.3.0", "1.2.9") -> false
 * - isOlderVersion("1.2", "1.2.0.1") -> true
 *
 * @param current The current version string (e.g., "1.2.3")
 * @param latest The latest version string (e.g., "1.2.4")
 * @return `true` if the current version is older than the latest version, `false` otherwise
 */
fun isOlderVersion(current: String, latest: String): Boolean {
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

    val maxLength = maxOf(currentParts.size, latestParts.size)
    val paddedCurrent = currentParts + List(maxLength - currentParts.size) { 0 }
    val paddedLatest = latestParts + List(maxLength - latestParts.size) { 0 }

    for (i in 0 until maxLength) {
        if (paddedCurrent[i] < paddedLatest[i]) return true
        if (paddedCurrent[i] > paddedLatest[i]) return false
    }
    return false
}