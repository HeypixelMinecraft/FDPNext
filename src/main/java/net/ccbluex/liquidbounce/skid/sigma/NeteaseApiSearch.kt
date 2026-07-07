package net.ccbluex.liquidbounce.skid.sigma

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NeteaseApiSearch {

    private const val BASE_URL = "https://music.163.com"

    data class NeteaseTrack(
        val id: Long,
        val name: String,
        val artist: String,
        val album: String,
        val duration: Long,
        val coverUrl: String
    ) {
        fun getDisplayTitle(): String = "$artist - $name"
    }

    data class NeteaseSongUrl(
        val id: Long,
        val url: String?,
        val br: Int,
        val type: String
    )

    fun search(keyword: String, limit: Int = 30, offset: Int = 0): List<NeteaseTrack> {
        val tracks = mutableListOf<NeteaseTrack>()
        try {
            val data = JsonObject().apply {
                addProperty("s", keyword)
                addProperty("type", 1)
                addProperty("limit", limit)
                addProperty("offset", offset)
                addProperty("total", true)
                addProperty("csrf_token", "")
            }

            val encrypted = NeteaseApiEncrypt.encrypt(data.toString())
            val body = "params=${URLEncoder.encode(encrypted[0], "UTF-8")}&encSecKey=${URLEncoder.encode(encrypted[1], "UTF-8")}"

            val response = postRequest("$BASE_URL/weapi/search/get", body)
            val json = JsonParser().parse(response).asJsonObject

            if (json.get("code").asInt != 200) {
                System.err.println("[NeteaseSearch] Bad response code: ${json.get("code")}")
                return tracks
            }

            val result = json.getAsJsonObject("result") ?: return tracks
            if (!result.has("songs")) return tracks

            val songs = result.getAsJsonArray("songs")
            for (elem in songs) {
                val song = elem.asJsonObject
                val id = song.get("id").asLong
                val name = song.get("name").asString

                val artist = when {
                    song.has("artists") && song.getAsJsonArray("artists").size() > 0 ->
                        song.getAsJsonArray("artists").get(0).asJsonObject.get("name").asString
                    song.has("ar") && song.getAsJsonArray("ar").size() > 0 ->
                        song.getAsJsonArray("ar").get(0).asJsonObject.get("name").asString
                    else -> "Unknown"
                }

                val album = when {
                    song.has("album") && !song.get("album").isJsonNull ->
                        song.getAsJsonObject("album").get("name").asString
                    song.has("al") && !song.get("al").isJsonNull ->
                        song.getAsJsonObject("al").get("name").asString
                    else -> ""
                }

                var duration = if (song.has("duration")) song.get("duration").asLong else 0L
                if (duration == 0L && song.has("dt")) duration = song.get("dt").asLong

                val coverUrl = when {
                    song.has("al") && !song.get("al").isJsonNull && song.getAsJsonObject("al").has("picUrl") ->
                        song.getAsJsonObject("al").get("picUrl").asString
                    song.has("album") && !song.get("album").isJsonNull && song.getAsJsonObject("album").has("picUrl") ->
                        song.getAsJsonObject("album").get("picUrl").asString
                    else -> ""
                }

                tracks.add(NeteaseTrack(id, name, artist, album, duration, coverUrl))
            }
        } catch (e: Exception) {
            System.err.println("[NeteaseSearch] search failed: ${e.message}")
            e.printStackTrace()
        }
        return tracks
    }

    fun getSongUrls(vararg songIds: Long): List<NeteaseSongUrl> {
        val urls = mutableListOf<NeteaseSongUrl>()
        try {
            val ids = JsonArray()
            for (id in songIds) ids.add(JsonPrimitive(id))

            val data = JsonObject().apply {
                add("ids", ids)
                addProperty("level", "standard")
                addProperty("encodeType", "mp3")
                addProperty("csrf_token", "")
            }

            val encrypted = NeteaseApiEncrypt.encrypt(data.toString())
            val body = "params=${URLEncoder.encode(encrypted[0], "UTF-8")}&encSecKey=${URLEncoder.encode(encrypted[1], "UTF-8")}"

            val response = postRequest("$BASE_URL/weapi/song/enhance/player/url/v1", body)
            val json = JsonParser().parse(response).asJsonObject

            if (json.get("code").asInt != 200) {
                System.err.println("[NeteaseSearch] getSongUrls bad code: ${json.get("code")}")
                return urls
            }

            val dataArr = json.getAsJsonArray("data")
            for (elem in dataArr) {
                val item = elem.asJsonObject
                val id = item.get("id").asLong
                val songUrl = if (item.has("url") && !item.get("url").isJsonNull) item.get("url").asString else null
                val br = if (item.has("br")) item.get("br").asInt else 0
                val type = if (item.has("type") && !item.get("type").isJsonNull) item.get("type").asString else "mp3"

                if (songUrl != null && songUrl.isNotEmpty()) {
                    urls.add(NeteaseSongUrl(id, songUrl, br, type))
                }
            }
        } catch (e: Exception) {
            System.err.println("[NeteaseSearch] getSongUrls failed: ${e.message}")
            e.printStackTrace()
        }
        return urls
    }

    fun getSongUrl(songId: Long): String? {
        val urls = getSongUrls(songId)
        return urls.firstOrNull()?.url
    }

    fun getSimpleSongUrl(songId: Long): String {
        return "${NeteaseConstants.SIMPLE_SONG_URL_PREFIX}$songId.mp3"
    }

    fun getSongDetail(vararg songIds: Long): List<NeteaseTrack> {
        val tracks = mutableListOf<NeteaseTrack>()
        try {
            val c = JsonArray()
            val ids = JsonArray()
            for (id in songIds) {
                val obj = JsonObject().apply { addProperty("id", id) }
                c.add(obj)
                ids.add(JsonPrimitive(id))
            }

            val data = JsonObject().apply {
                addProperty("c", c.toString())
                addProperty("ids", ids.toString())
                addProperty("csrf_token", "")
            }

            val encrypted = NeteaseApiEncrypt.encrypt(data.toString())
            val body = "params=${URLEncoder.encode(encrypted[0], "UTF-8")}&encSecKey=${URLEncoder.encode(encrypted[1], "UTF-8")}"

            val response = postRequest("$BASE_URL/weapi/v3/song/detail", body)
            val json = JsonParser().parse(response).asJsonObject

            if (json.has("songs")) {
                val songs = json.getAsJsonArray("songs")
                for (elem in songs) {
                    val song = elem.asJsonObject
                    val id = song.get("id").asLong
                    val name = song.get("name").asString

                    val artist = if (song.has("ar") && song.getAsJsonArray("ar").size() > 0) {
                        song.getAsJsonArray("ar").get(0).asJsonObject.get("name").asString
                    } else "Unknown"

                    var album = ""
                    var coverUrl = ""
                    if (song.has("al") && !song.get("al").isJsonNull) {
                        val al = song.getAsJsonObject("al")
                        if (al.has("name")) album = al.get("name").asString
                        if (al.has("picUrl")) coverUrl = al.get("picUrl").asString
                    }

                    val duration = if (song.has("dt")) song.get("dt").asLong else 0L
                    tracks.add(NeteaseTrack(id, name, artist, album, duration, coverUrl))
                }
            }
        } catch (e: Exception) {
            System.err.println("[NeteaseSearch] getSongDetail failed: ${e.message}")
            e.printStackTrace()
        }
        return tracks
    }

    fun getLyrics(songId: Long): String? {
        try {
            val data = JsonObject().apply {
                addProperty("id", songId)
                addProperty("lv", -1)
                addProperty("tv", -1)
                addProperty("csrf_token", "")
            }

            val encrypted = NeteaseApiEncrypt.encrypt(data.toString())
            val body = "params=${URLEncoder.encode(encrypted[0], "UTF-8")}&encSecKey=${URLEncoder.encode(encrypted[1], "UTF-8")}"

            val response = postRequest("$BASE_URL/weapi/song/lyric", body)
            val json = JsonParser().parse(response).asJsonObject

            if (json.has("yrc") && !json.get("yrc").isJsonNull) {
                val yrc = json.getAsJsonObject("yrc")
                if (yrc.has("lyric") && !yrc.get("lyric").isJsonNull) {
                    val yrcText = yrc.get("lyric").asString
                    if (!yrcText.isEmpty()) return yrcText
                }
            }

            if (json.has("lrc") && !json.get("lrc").isJsonNull) {
                val lrc = json.getAsJsonObject("lrc")
                if (lrc.has("lyric") && !lrc.get("lyric").isJsonNull) {
                    return lrc.get("lyric").asString
                }
            }
        } catch (e: Exception) {
            System.err.println("[NeteaseSearch] getLyrics failed: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

    fun invokePostRequest(urlStr: String, body: String): String = postRequest(urlStr, body)

    private fun postRequest(urlStr: String, body: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", NeteaseConstants.UA_PC_BROWSER)
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("Referer", "https://music.163.com/")
        conn.setRequestProperty("Origin", "https://music.163.com")

        try {
            val os: OutputStream = conn.outputStream
            os.write(body.toByteArray(StandardCharsets.UTF_8))
            os.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val status = conn.responseCode
        val inputStream = if (status in 200..399) conn.inputStream else conn.errorStream

        val sb = StringBuilder()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
        }
        conn.disconnect()
        return sb.toString()
    }
}
