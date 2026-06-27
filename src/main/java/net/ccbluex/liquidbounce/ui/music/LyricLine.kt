/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music

/**
 * 歌词单行数据
 *
 * @param timeMs 该行歌词的开始时间（毫秒）
 * @param text   歌词文本
 */
data class LyricLine(val timeMs: Long, val text: String)

/**
 * LRC 歌词解析工具
 *
 * 支持标准 LRC 格式：[mm:ss.xx]歌词内容
 * 支持同一行多个时间戳：[00:01.00][00:15.00]歌词
 */
object LrcParser {

    private val TIME_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    /**
     * 解析 LRC 文本为歌词行列表（按时间升序）
     */
    fun parse(lrc: String): List<LyricLine> {
        if (lrc.isBlank()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        for (raw in lrc.lines()) {
            val text = raw.trim()
            if (text.isEmpty()) continue
            val matches = TIME_TAG.findAll(text).toList()
            if (matches.isEmpty()) continue
            // 提取时间戳之后的内容作为歌词文本
            val lastMatch = matches.last()
            val content = text.substring(lastMatch.range.last + 1).trim()
            // 跳过纯元数据行（如 [ti:]、[ar:]、[al:]、[by:]、[offset:]）
            if (content.isEmpty()) continue
            for (m in matches) {
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val msPart = m.groupValues[3]
                val ms = when {
                    msPart.length == 1 -> msPart.toLongOrNull()?.times(100) ?: 0L
                    msPart.length == 2 -> msPart.toLongOrNull()?.times(10) ?: 0L
                    else -> msPart.toLongOrNull() ?: 0L
                }
                val totalMs = min * 60_000L + sec * 1000L + ms
                lines.add(LyricLine(totalMs, content))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
