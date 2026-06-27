/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music

/**
 * 音乐来源
 */
enum class MusicSource(val displayName: String) {
    NETEASE("网易云音乐"),
    KUGOU("酷狗音乐"),
    YOUTUBE_MUSIC("YouTube Music")
}

/**
 * 音乐曲目数据模型
 */
data class MusicTrack(
    val id: String,
    val name: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L,          // 毫秒，0 表示未知
    val source: MusicSource,
    val streamUrl: String = "",       // 可播放的流媒体 URL
    val coverUrl: String = "",        // 封面图 URL
    val externalUrl: String = ""      // 原始页面 URL
) {
    override fun toString(): String = "$name - $artist [$source]"
}
