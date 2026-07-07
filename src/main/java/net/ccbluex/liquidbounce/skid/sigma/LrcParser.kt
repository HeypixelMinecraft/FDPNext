package net.ccbluex.liquidbounce.skid.sigma

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.regex.Pattern

object LrcParser {

    data class LyricLine(val timestamp: Long, val content: String) : Comparable<LyricLine> {
        override fun compareTo(other: LyricLine): Int = timestamp.compareTo(other.timestamp)
    }

    fun parse(file: File): List<LyricLine> {
        if (!file.exists()) return emptyList()
        try {
            return parse(FileInputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    fun parseString(lrcText: String?): List<LyricLine> {
        if (lrcText.isNullOrEmpty()) return emptyList()
        return parse(lrcText.byteInputStream(StandardCharsets.UTF_8))
    }

    fun parse(inputStream: InputStream?): List<LyricLine> {
        val lyrics = mutableListOf<LyricLine>()
        if (inputStream == null) return lyrics

        try {
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                val pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val matcher = pattern.matcher(line!!)
                    if (matcher.find()) {
                        val minutes = matcher.group(1)!!.toLong()
                        val seconds = matcher.group(2)!!.toLong()
                        val millisStr = matcher.group(3)!!
                        var millis = millisStr.toLong()
                        if (millisStr.length == 2) millis *= 10

                        val totalMillis = minutes * 60000 + seconds * 1000 + millis
                        val content = matcher.group(4)!!.trim()
                        if (content.isNotEmpty()) {
                            lyrics.add(LyricLine(totalMillis, content))
                        }
                    }
                }
                Collections.sort(lyrics)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lyrics
    }
}
