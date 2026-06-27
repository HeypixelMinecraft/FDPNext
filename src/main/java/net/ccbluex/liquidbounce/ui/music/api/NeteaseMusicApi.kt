/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music.api

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.ui.music.LrcParser
import net.ccbluex.liquidbounce.ui.music.LyricLine
import net.ccbluex.liquidbounce.ui.music.MusicSource
import net.ccbluex.liquidbounce.ui.music.MusicTrack
import net.ccbluex.liquidbounce.utils.misc.HttpUtils

/**
 * 网易云音乐 API
 *
 * 直接调用 music.163.com 官方接口
 * - 搜索: /api/search/get
 * - 歌曲URL: /song/media/outer/url (直链 302 跳转)
 * - 详情: /api/song/detail
 */
object NeteaseMusicApi {

    private const val BASE = "https://music.163.com"

    /**
     * 搜索歌曲
     * @param keyword 关键词
     * @param limit 返回数量
     */
    fun search(keyword: String, limit: Int = 20): List<MusicTrack> {
        return try {
            val url = "$BASE/api/search/get/web?csrf_token=&hlpretag=&hlposttag=&s=${java.net.URLEncoder.encode(keyword, "UTF-8")}&type=1&offset=0&total=true&limit=$limit"
            val resp = HttpUtils.request(url, "POST", "")
            val json = JsonParser().parse(resp).asJsonObject
            if (json.has("code") && json.get("code").asInt == 200) {
                val songs = json.getAsJsonObject("result")?.getAsJsonArray("songs") ?: return emptyList()
                songs.map { it.asJsonObject }.map { song ->
                    val id = song.get("id").asString
                    val name = song.get("name").asString
                    val artists = song.getAsJsonArray("artists")?.joinToString(", ") { it.asJsonObject.get("name").asString } ?: "未知"
                    val album = song.getAsJsonObject("album")?.get("name")?.asString ?: ""
                    val duration = song.get("duration")?.asLong ?: 0L
                    MusicTrack(
                        id = id,
                        name = name,
                        artist = artists,
                        album = album,
                        duration = duration,
                        source = MusicSource.NETEASE,
                        streamUrl = "$BASE/song/media/outer/url?id=$id.mp3",
                        coverUrl = song.getAsJsonObject("album")?.get("picUrl")?.asString ?: "",
                        externalUrl = "$BASE/song?id=$id"
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取歌曲详情（补充封面等信息）
     */
    fun getSongDetail(id: String): MusicTrack? {
        return try {
            val url = "$BASE/api/song/detail/?ids=$id"
            val resp = HttpUtils.get(url)
            val json = JsonParser().parse(resp).asJsonObject
            val songs = json.getAsJsonArray("songs") ?: return null
            if (songs.size() == 0) return null
            val song = songs[0].asJsonObject
            MusicTrack(
                id = id,
                name = song.get("name").asString,
                artist = song.getAsJsonArray("artists")?.joinToString(", ") { it.asJsonObject.get("name").asString } ?: "未知",
                album = song.getAsJsonObject("album")?.get("name")?.asString ?: "",
                duration = song.get("duration")?.asLong ?: 0L,
                source = MusicSource.NETEASE,
                streamUrl = "$BASE/song/media/outer/url?id=$id.mp3",
                coverUrl = song.getAsJsonObject("album")?.get("picUrl")?.asString ?: "",
                externalUrl = "$BASE/song?id=$id"
            )
        } catch (e: Exception) { null }
    }

    /**
     * 获取歌词（LRC 格式）
     *
     * 接口: /api/song/lyric?os=pc&id={id}&lv=-1&kv=-1&tv=-1
     * 返回 JSON，lrc.lyric 字段为 LRC 文本
     *
     * @param id 网易云歌曲 ID
     * @return 歌词行列表，失败返回空列表
     */
    fun getLyrics(id: String): List<LyricLine> {
        return try {
            val url = "$BASE/api/song/lyric?os=pc&id=$id&lv=-1&kv=-1&tv=-1"
            val resp = HttpUtils.get(url)
            val json = JsonParser().parse(resp).asJsonObject
            val lrcText = json.getAsJsonObject("lrc")?.get("lyric")?.asString
                ?: json.getAsJsonObject("klyric")?.get("lyric")?.asString
                ?: ""
            LrcParser.parse(lrcText)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
