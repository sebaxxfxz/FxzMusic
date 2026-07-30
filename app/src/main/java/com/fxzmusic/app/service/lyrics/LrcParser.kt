package com.fxzmusic.app.service.lyrics

import com.fxzmusic.app.data.LyricsLine
import com.fxzmusic.app.data.LyricsWord
import android.text.format.DateUtils

object LrcParser {

    private val LINE_REGEX = Regex("""\[(\d{1,2}):(\d{2})\.(\d{2,3})\](.*)""")
    private val TIME_TAG_REGEX = Regex("""\[(\d{1,2}):(\d{2})\.(\d{2,3})\]""")
    private val RICH_SYNC_LINE_REGEX = Regex("""\[(\d{1,2}):(\d{2})\.(\d{2,3})\](.+)""")
    private val RICH_SYNC_WORD_REGEX = Regex("""<(\d{1,2}):(\d{2})\.(\d{2,3})>\s*([^<]+)""")
    private val AGENT_REGEX = Regex("""\{agent:([^}]+)\}""")
    private val BACKGROUND_REGEX = Regex("""^\{bg\}""")
    private val WORD_DATA_LINE_REGEX = Regex("""^<[^>]*\|.*>$""")
    private val HEX_ENTITY_REGEX = Regex("&#x([0-9a-fA-F]+);")
    private val DEC_ENTITY_REGEX = Regex("&#(\\d+);")

    fun parseSyncedLyrics(raw: String): List<LyricsLine> = parseLyrics(raw)

    fun parseLyrics(lyrics: String): List<LyricsLine> {
        val unescaped = lyrics.trim()

        val decoded = decodeHtmlEntities(unescaped)

        val lines = decoded.lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("[offset:") }
            .map { it.trim() }

        val isRichSync = lines.any { line ->
            RICH_SYNC_LINE_REGEX.matches(line) && RICH_SYNC_WORD_REGEX.containsMatchIn(line)
        }
        val hasWordData = lines.any { WORD_DATA_LINE_REGEX.matches(it) }

        return when {
            isRichSync -> parseRichSyncLyrics(lines)
            hasWordData -> parseStandardWithWordData(lines)
            else -> parseStandardLyrics(lines)
        }.sortedBy { it.timeMs }
    }

    private fun parseRichSyncLyrics(lines: List<String>): List<LyricsLine> {
        val result = mutableListOf<LyricsLine>()
        lines.forEachIndexed { index, line ->
            val match = RICH_SYNC_LINE_REGEX.matchEntire(line) ?: return@forEachIndexed
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fractionRaw = match.groupValues[3]
            val fraction = fractionRaw.toLongOrNull() ?: 0L
            val millis = if (fractionRaw.length == 3) fraction else fraction * 10
            val lineTimeMs = minutes * DateUtils.MINUTE_IN_MILLIS + seconds * DateUtils.SECOND_IN_MILLIS + millis

            var content = match.groupValues[4].trimStart()

            val agentMatch = AGENT_REGEX.find(content)
            val agent = agentMatch?.groupValues?.get(1)
            if (agentMatch != null) {
                content = content.replaceFirst(AGENT_REGEX, "")
            }

            val isBackground = BACKGROUND_REGEX.containsMatchIn(content)
            if (isBackground) {
                content = content.replaceFirst(BACKGROUND_REGEX, "")
            }

            val words = parseRichSyncWords(content, index, lines)
            val plainText = content.replace(Regex("""<\d{1,2}:\d{2}\.\d{2,3}>\s*"""), "").trim()

            if (plainText.isNotBlank()) {
                result.add(LyricsLine(timeMs = lineTimeMs, text = plainText, words = words, agent = agent, isBackground = isBackground))
            }
        }
        return result
    }

    private fun parseRichSyncWords(content: String, currentIndex: Int, allLines: List<String>): List<LyricsWord> {
        val matches = RICH_SYNC_WORD_REGEX.findAll(content).toList()
        if (matches.isEmpty()) return emptyList()
        val words = mutableListOf<LyricsWord>()
        matches.forEachIndexed { i, match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fractionRaw = match.groupValues[3]
            val fraction = fractionRaw.toLongOrNull() ?: 0L
            val startMs = if (fractionRaw.length == 3) minutes * 60_000L + seconds * 1_000L + fraction
                          else minutes * 60_000L + seconds * 1_000L + fraction * 10L
            val wordText = match.groupValues[4]
            val endMs = if (i < matches.size - 1) {
                val next = matches[i + 1]
                val nMin = next.groupValues[1].toLongOrNull() ?: 0L
                val nSec = next.groupValues[2].toLongOrNull() ?: 0L
                val nFracRaw = next.groupValues[3]
                val nFrac = nFracRaw.toLongOrNull() ?: 0L
                if (nFracRaw.length == 3) nMin * 60_000L + nSec * 1_000L + nFrac
                else nMin * 60_000L + nSec * 1_000L + nFrac * 10L
            } else {
                getNextLineStartTime(currentIndex, allLines) ?: (startMs + 500L)
            }
            if (wordText.isNotBlank()) {
                words.add(LyricsWord(text = wordText, startMs = startMs, endMs = endMs))
            }
        }
        return words
    }

    private fun getNextLineStartTime(currentIndex: Int, allLines: List<String>): Long? {
        if (currentIndex + 1 >= allLines.size) return null
        val next = allLines[currentIndex + 1]
        val match = RICH_SYNC_LINE_REGEX.matchEntire(next) ?: return null
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fractionRaw = match.groupValues[3]
        val fraction = fractionRaw.toLongOrNull() ?: 0L
        return if (fractionRaw.length == 3) minutes * 60_000L + seconds * 1_000L + fraction
               else minutes * 60_000L + seconds * 1_000L + fraction * 10L
    }

    private fun parseStandardWithWordData(lines: List<String>): List<LyricsLine> {
        val result = mutableListOf<LyricsLine>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (WORD_DATA_LINE_REGEX.matches(line)) { i++; continue }
            val parsed = parseLineWithMultipleTimestamps(line) ?: run { i++; continue }
            val wordTimestamps = if (i + 1 < lines.size) {
                val next = lines[i + 1]
                if (WORD_DATA_LINE_REGEX.matches(next)) parseWordData(next) else null
            } else null
            if (wordTimestamps != null && wordTimestamps.isNotEmpty()) {
                parsed.forEach { entry -> result.add(entry.copy(words = wordTimestamps)) }
            } else {
                result.addAll(parsed)
            }
            i++
        }
        return result
    }

    private fun parseWordData(line: String): List<LyricsWord>? {
        val data = line.removeSurrounding("<", ">").trim()
        if (data.isBlank()) return null
        return try {
            data.split("|").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 3) {
                    LyricsWord(text = parts[0],
                               startMs = (parts[1].toDoubleOrNull() ?: 0.0).times(1000).toLong(),
                               endMs = (parts[2].toDoubleOrNull() ?: 0.0).times(1000).toLong())
                } else null
            }.takeIf { it.isNotEmpty() }
        } catch (e: Exception) { null }
    }

    private fun parseStandardLyrics(lines: List<String>): List<LyricsLine> {
        val result = mutableListOf<LyricsLine>()
        for (line in lines) {
            val parsed = parseLineWithMultipleTimestamps(line) ?: continue
            result.addAll(parsed)
        }
        return result
    }

    private fun parseLineWithMultipleTimestamps(line: String): List<LyricsLine>? {
        if (line.isEmpty()) return null
        val match = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val timeMatches = TIME_TAG_REGEX.findAll(line).toList()
        if (timeMatches.isEmpty()) return null
        var text = match.groupValues[4].trim()

        val agentMatch = AGENT_REGEX.find(text)
        val agent = agentMatch?.groupValues?.get(1)
        if (agentMatch != null) text = text.replaceFirst(AGENT_REGEX, "")

        val isBackground = BACKGROUND_REGEX.containsMatchIn(text)
        if (isBackground) text = text.replaceFirst(BACKGROUND_REGEX, "")

        return timeMatches.map { tm ->
            val min = tm.groupValues[1].toLongOrNull() ?: 0L
            val sec = tm.groupValues[2].toLongOrNull() ?: 0L
            val milStr = tm.groupValues[3]
            var mil = milStr.toLongOrNull() ?: 0L
            if (milStr.length == 2) mil *= 10L
            val timeMs = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
            LyricsLine(timeMs = timeMs, text = text, agent = agent, isBackground = isBackground)
        }
    }

    private fun decodeHtmlEntities(text: String): String {
        var result = text
        result = HEX_ENTITY_REGEX.replace(result) { match ->
            val hex = match.groupValues[1].toIntOrNull(16)
            try {
                if (hex != null && hex in 0..0x10FFFF) String(Character.toChars(hex)) else match.value
            } catch (e: IllegalArgumentException) { match.value }
        }
        result = DEC_ENTITY_REGEX.replace(result) { match ->
            val dec = match.groupValues[1].toIntOrNull()
            try {
                if (dec != null && dec in 0..0x10FFFF) String(Character.toChars(dec)) else match.value
            } catch (e: IllegalArgumentException) { match.value }
        }
        result = result.replace("&#x27;", "\u0027")
        result = result.replace("&#34;", "\u0022")
        result = result.replace("&#60;", "\u003C")
        result = result.replace("&#62;", "\u003E")
        result = result.replace("&#xA0;", "\u00A0")
        result = result.replace("&amp;", "\u0026")
        return result
    }

    private val TIMESTAMP_PARSE_REGEX = Regex("""\[?(\d{1,2}):(\d{2})\.(\d{2,3})\]?""")

    fun parseLrcTimestamp(timestamp: String): Long? {
        return TIMESTAMP_PARSE_REGEX.find(timestamp)?.let { match ->
            val (min, sec, ms) = match.destructured
            val mil = if (ms.length == 2) (ms.toLongOrNull() ?: 0L) * 10 else (ms.toLongOrNull() ?: 0L)
            (min.toLongOrNull() ?: 0L) * 60_000 + (sec.toLongOrNull() ?: 0L) * 1_000 + mil
        }
    }

    fun toLrc(lines: List<LyricsLine>): String {
        return lines.joinToString("\n") { line ->
            val min = line.timeMs / 60_000
            val sec = (line.timeMs % 60_000) / 1_000
            val ms = line.timeMs % 1_000
            "[${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}.${ms.toString().padStart(3, '0')}]${line.text}"
        }
    }
}
