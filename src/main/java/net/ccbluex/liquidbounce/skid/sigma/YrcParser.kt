package net.ccbluex.liquidbounce.skid.sigma

import java.util.Collections
import java.util.regex.Pattern

object YrcParser {

    data class YrcWord(val start: Long, val duration: Long, val text: String)

    data class YrcLine(val startTime: Long, val content: String, val words: List<YrcWord>) : Comparable<YrcLine> {
        override fun compareTo(other: YrcLine): Int = startTime.compareTo(other.startTime)
    }

    private val WORD_PATTERN = Pattern.compile("\\[(\\d+),(\\d+)]([^\\[]*)")

    fun parse(yrcText: String?): List<YrcLine> {
        if (yrcText.isNullOrBlank()) return emptyList()

        val lines = mutableListOf<YrcLine>()
        val rawLines = yrcText.split("\n")

        for (rawLine in rawLines) {
            if (rawLine.isBlank()) continue

            val words = mutableListOf<YrcWord>()
            val contentBuilder = StringBuilder()
            var lineStart = -1L

            val matcher = WORD_PATTERN.matcher(rawLine)
            while (matcher.find()) {
                val start = matcher.group(1)!!.toLong()
                val duration = matcher.group(2)!!.toLong()
                val word = matcher.group(3) ?: continue
                if (word.isEmpty()) continue

                if (lineStart < 0) lineStart = start
                words.add(YrcWord(start, duration, word))
                contentBuilder.append(word)
            }

            if (words.isNotEmpty()) {
                val content = contentBuilder.toString().trim()
                if (content.isNotEmpty()) {
                    lines.add(YrcLine(lineStart, content, words))
                }
            }
        }

        Collections.sort(lines)
        return lines
    }

    fun isYrc(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        return WORD_PATTERN.matcher(text).find()
    }
}
