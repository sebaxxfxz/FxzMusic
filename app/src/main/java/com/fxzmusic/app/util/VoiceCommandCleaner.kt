package com.fxzmusic.app.util

object VoiceCommandCleaner {

    private val PREFIX_REGEX = Regex(
        """^(?i)\s*(?:reproduce(?:\s+la\s+canción|\s+el\s+tema|\s+el\s+disco|\s+el\s+álbum|\s+música\s+de|\s+a)?|reproducir|reprodúceme|pon(?:\s+la\s+canción|\s+el\s+tema|\s+el\s+disco|\s+el\s+álbum|\s+música\s+de|\s+a)?|pone|poner|ponme|toca|tocar|tócame|escucha|escuchar|escúchame|play(?:\s+song|\s+music\s+by|\s+album)?|listen\s+to|listen)\s+"""
    )

    private val SUFFIX_REGEX = Regex(
        """(?i)\s+(?:en\s+(?:la\s+app\s+|la\s+aplicación\s+)?(?:fxzmusic|fxz\s+music|fxz)|on\s+(?:the\s+app\s+)?(?:fxzmusic|fxz\s+music|fxz)|in\s+(?:the\s+app\s+)?(?:fxzmusic|fxz\s+music|fxz))\s*$"""
    )

    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    fun clean(rawQuery: String?): String {
        if (rawQuery.isNullOrBlank()) return ""
        return runCatching {
            var text = rawQuery.trim()
            text = text.replace(PREFIX_REGEX, "")
            text = text.replace(SUFFIX_REGEX, "")
            text = text.replace(MULTI_SPACE_REGEX, " ").trim()
            if (text.isBlank()) rawQuery.trim() else text
        }.getOrDefault(rawQuery.trim())
    }
}
