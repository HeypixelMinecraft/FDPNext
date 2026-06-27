/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music.api

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.ui.music.MusicSource
import net.ccbluex.liquidbounce.ui.music.MusicTrack
import net.ccbluex.liquidbounce.utils.misc.HttpUtils

/**
 * 酷狗音乐 API
 *
 * 直接调用 kugou.com 官方接口
 * - 搜索: mobilecdn.kugou.com/new/appclient/search/specialkey
 * - 歌曲Hash: 从搜索结果获取
 * - 歌曲URL: wwwapi.kugou.com/yy/index.php?r=play/getdata (需要 hash + album_id)
 * - 备用URL: trackercdn.kugou.com/i/v2/ (通过 hash 获取播放 URL)
 */
object KugouMusicApi {

    /**
     * 搜索歌曲
     * @param keyword 关键词
     * @param limit 返回数量
     */
    fun search(keyword: String, limit: Int = 20): List<MusicTrack> {
        return try {
            val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
            // 使用 mobilecdn 接口搜索
            val url = "http://mobilecdn.kugou.com/new/appclient/search/specialkey?key=$encoded&size=$limit&isCluster=1"
            val resp = HttpUtils.get(url)
            val json = JsonParser().parse(resp).asJsonObject
            if (json.has("data") && !json.get("data").isJsonNull) {
                val data = json.getAsJsonObject("data")
                val songs = data.getAsJsonArray("lists") ?: return emptyList()
                songs.map { it.asJsonObject }.map { song ->
                    val hash = song.get("FileHash")?.asString ?: song.get("hash")?.asString ?: ""
                    val name = song.get("SongName")?.asString ?: song.get("songname")?.asString ?: "未知"
                    val artist = song.get("SingerName")?.asString ?: song.get("singername")?.asString ?: "未知"
                    val album = song.get("AlbumName")?.asString ?: ""
                    val duration = (song.get("Duration")?.asLong ?: 0L) * 1000L
                    val albumId = song.get("AlbumID")?.asString ?: song.get("album_id")?.asString ?: ""
                    MusicTrack(
                        id = hash,
                        name = name,
                        artist = artist,
                        album = album,
                        duration = duration,
                        source = MusicSource.KUGOU,
                        streamUrl = "", // 需要通过 getStreamUrl 异步获取
                        coverUrl = "",
                        externalUrl = "https://www.kugou.com/song/#hash=$hash&album_id=$albumId"
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 通过 hash 获取歌曲播放 URL
     * @param hash 歌曲 FileHash
     * @param albumId 专辑 ID
     * @return 播放 URL，失败返回 null
     */
    fun getStreamUrl(hash: String, albumId: String = ""): String? {
        return try {
            // 方法1: 使用 wwwapi 接口
            val key = generateKey(hash)
            val url = "https://wwwapi.kugou.com/yy/index.php?r=play/getdata&hash=$hash&key=$key&album_id=$albumId&_=${System.currentTimeMillis()}"
            val resp = HttpUtils.get(url)
            val json = JsonParser().parse(resp).asJsonObject
            if (json.has("err_code") && json.get("err_code").asInt == 0) {
                val data = json.getAsJsonObject("data")
                val playUrl = data?.get("play_url")?.asString
                if (!playUrl.isNullOrEmpty() && playUrl != "null") return playUrl
                val playBackupUrl = data?.get("play_backup_url")?.asString
                if (!playBackupUrl.isNullOrEmpty()) return playBackupUrl
            }
            // 方法2: 使用 trackercdn 接口
            getStreamUrlV2(hash)
        } catch (e: Exception) {
            try { getStreamUrlV2(hash) } catch (_: Exception) { null }
        }
    }

    /**
     * 备用方法：通过 trackercdn 获取播放 URL
     */
    private fun getStreamUrlV2(hash: String): String? {
        return try {
            val key = generateKey(hash)
            val url = "https://trackercdn.kugou.com/i/v2/?key=$key&hash=$hash&appid=1005&pid=2&cmd=25&behavior=play"
            val resp = HttpUtils.get(url)
            val json = JsonParser().parse(resp).asJsonObject
            if (json.has("status") && json.get("status").asInt == 1) {
                val urls = json.getAsJsonArray("urls")
                urls?.firstOrNull()?.asJsonObject?.get("url")?.asString
            } else null
        } catch (e: Exception) { null }
    }

    /**
     * 生成酷狗 API 所需的 key（hash 的 MD5 小写）
     * 酷狗使用 hash 本身的 md5 作为 key
     */
    private fun generateKey(hash: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val bytes = md.digest(hash.lowercase().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
