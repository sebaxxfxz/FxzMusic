package com.fxzmusic.app.service.lyrics

import org.junit.Assert.*
import org.junit.Test

class LrcParserTest {
    @Test
    fun `parseSyncedLyrics parses valid LRC`() {
        val lrc = """
            [00:12.00]First line
            [00:15.50]Second line
            [00:20.100]Third line
        """.trimIndent()

        val result = LrcParser.parseSyncedLyrics(lrc)

        assertEquals(3, result.size)
        assertEquals(12000L, result[0].timeMs)
        assertEquals("First line", result[0].text)
        assertEquals(15500L, result[1].timeMs)
        assertEquals("Second line", result[1].text)
        assertEquals(20100L, result[2].timeMs)
        assertEquals("Third line", result[2].text)
    }

    @Test
    fun `parseSyncedLyrics skips empty lines`() {
        val lrc = """
            [00:12.00]First line

            [00:15.50]Second line
        """.trimIndent()

        val result = LrcParser.parseSyncedLyrics(lrc)

        assertEquals(2, result.size)
    }

    @Test
    fun `parseSyncedLyrics returns sorted by time`() {
        val lrc = """
            [00:20.00]Third line
            [00:12.00]First line
            [00:15.50]Second line
        """.trimIndent()

        val result = LrcParser.parseSyncedLyrics(lrc)

        assertEquals(12000L, result[0].timeMs)
        assertEquals(15500L, result[1].timeMs)
        assertEquals(20000L, result[2].timeMs)
    }

    @Test
    fun `parseLrcTimestamp parses mm-ss-ms`() {
        assertEquals(12000L, LrcParser.parseLrcTimestamp("00:12.00"))
        assertEquals(15500L, LrcParser.parseLrcTimestamp("00:15.50"))
        assertEquals(20100L, LrcParser.parseLrcTimestamp("00:20.100"))
    }

    @Test
    fun `parseLrcTimestamp returns null for invalid`() {
        assertNull(LrcParser.parseLrcTimestamp("invalid"))
        assertNull(LrcParser.parseLrcTimestamp("1:2.3"))
    }
}
