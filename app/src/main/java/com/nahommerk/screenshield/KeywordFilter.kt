package com.nahommerk.screenshield

object KeywordFilter {
    /** Lowercase, strip emojis/symbols/punctuation, collapse whitespace. */
    fun normalize(raw: String): String = raw.lowercase()
        .replace(Regex("[\\p{C}\\p{So}\\p{Sk}]"), " ")
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    
    /** Returns the matched keyword, or null if the text is clean.
     * Single keywords match whole words only ("ohio" does NOT match "ohioan").
     * Multi-word keywords match as substrings ("fanum tax"). */
    fun match(text: String, keywords: List<String>): String? {
        val words = normalize(text).split(" ").filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val joined = words.joinToString(" ")
        for (keyword in keywords) {
            val nk = normalize(keyword)
            if (nk.isEmpty()) continue
            if (nk.contains(" ")) {
                if (joined.contains(nk)) return keyword
            } else if (words.contains(nk)) {
                return keyword
            }
        }
        return null
    }
}
