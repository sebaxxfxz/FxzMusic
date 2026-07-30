package com.fxzmusic.app.service.lyrics

import com.fxzmusic.app.data.LyricsLine
import com.fxzmusic.app.data.LyricsWord
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

object TtmlParser {

    fun parse(ttmlXml: String): List<LyricsLine> {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Exception) {}
            try {
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            } catch (_: Exception) {}
            try {
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            } catch (_: Exception) {}
            factory.isExpandEntityReferences = false
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(ByteArrayInputStream(ttmlXml.toByteArray(Charsets.UTF_8)))
            val body = document.getElementsByTagNameNS("*", "body").item(0)
                ?: document.getElementsByTagName("body").item(0) ?: return emptyList()
            val divs = body.childNodes
            val lines = mutableListOf<LyricsLine>()

            for (i in 0 until divs.length) {
                val div = divs.item(i)
                val divLocalName = div.localName ?: div.nodeName
                if (divLocalName != "div") continue

                val ps = div.childNodes
                for (j in 0 until ps.length) {
                    val p = ps.item(j)
                    val pLocalName = p.localName ?: p.nodeName
                    if (pLocalName != "p") continue

                    val pBegin = p.attributes?.getNamedItemNS("*", "begin")?.nodeValue
                        ?: p.attributes?.getNamedItem("begin")?.nodeValue
                    val lineStartMs = parseTimeToMs(pBegin) ?: continue

                    val spans = p.childNodes
                    val words = mutableListOf<LyricsWord>()
                    val lineTextParts = mutableListOf<String>()

                    for (k in 0 until spans.length) {
                        val span = spans.item(k)
                        val spanName = span.nodeName
                        val localName = span.localName

                        val spanLocalName = span.localName ?: spanName
                        if (spanLocalName == "span") {
                            val role = span.attributes?.getNamedItemNS("*", "role")?.nodeValue
                                ?: span.attributes?.getNamedItem("role")?.nodeValue
                            if (role == "x-translation" || role == "x-roman") continue

                            val spanBegin = span.attributes?.getNamedItemNS("*", "begin")?.nodeValue
                                ?: span.attributes?.getNamedItem("begin")?.nodeValue
                            val spanEnd = span.attributes?.getNamedItemNS("*", "end")?.nodeValue
                                ?: span.attributes?.getNamedItem("end")?.nodeValue
                            val spanText = span.textContent ?: ""

                            if (spanText.isBlank()) continue

                            val startMs = parseTimeToMs(spanBegin) ?: lineStartMs
                            val endMs = parseTimeToMs(spanEnd) ?: (startMs + 500)

                            words.add(LyricsWord(text = spanText, startMs = startMs, endMs = endMs))
                            lineTextParts.add(spanText)
                        }
                    }

                    if (lineTextParts.isEmpty()) {
                        val fallbackText = p.textContent?.trim() ?: ""
                        if (fallbackText.isNotEmpty() && words.isEmpty()) {
                            lines.add(LyricsLine(timeMs = lineStartMs, text = fallbackText))
                        }
                    } else {
                        lines.add(LyricsLine(timeMs = lineStartMs, text = words.joinToString("") { it.text }, words = words))
                    }
                }
            }

            lines.sortedBy { it.timeMs }
        } catch (e: Exception) {
            android.util.Log.w("TtmlParser", "Failed to parse TTML", e)
            emptyList()
        }
    }

    private fun parseTimeToMs(time: String?): Long? {
        if (time.isNullOrBlank()) return null
        return try {
            val cleaned = time.trim()
            when {
                cleaned.contains(":") -> {
                    val parts = cleaned.split(":")
                    when (parts.size) {
                        2 -> {
                            val min = parts[0].toLong()
                            val sec = parts[1].toDouble()
                            (min * 60 * 1000 + sec * 1000).toLong()
                        }
                        3 -> {
                            val hr = parts[0].toLong()
                            val min = parts[1].toLong()
                            val sec = parts[2].toDouble()
                            (hr * 3600 * 1000 + min * 60 * 1000 + sec * 1000).toLong()
                        }
                        else -> null
                    }
                }
                else -> (cleaned.toDouble() * 1000).toLong()
            }
        } catch (e: Exception) {
            null
        }
    }
}
