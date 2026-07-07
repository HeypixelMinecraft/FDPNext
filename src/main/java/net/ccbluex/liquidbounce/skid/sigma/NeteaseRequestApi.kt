package net.ccbluex.liquidbounce.skid.sigma

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object NeteaseRequestApi {

    private const val BASE = "https://music.163.com"

    data class ToplistInfo(
        val id: Long,
        val name: String,
        val coverUrl: String,
        val updateFrequency: String
    )

    fun getPlaylistDetail(playlistId: Long, limit: Int = 50, offset: Int = 0): List<NeteaseApiSearch.NeteaseTrack> {
        val tracks = mutableListOf<NeteaseApiSearch.NeteaseTrack>()
        try {
            val data = JsonObject().apply {
                addProperty("id", playlistId)
                addProperty("n", limit)
                addProperty("s", 0)
                addProperty("csrf_token", "")
            }

            val enc = NeteaseApiEncrypt.encrypt(data.toString())
            val body = "params=${encUrl(enc[0])}&encSecKey=${encUrl(enc[1])}"

            val resp = NeteaseApiSearch.invokePostRequest("$BASE/weapi/v6/playlist/detail", body)
            val json = JsonParser().parse(resp).asJsonObject

            if (json.get("code").asInt != 200) {
                System.err.println("[NeteaseRequestApi] getPlaylistDetail bad code: ${json.get("code")}")
                return tracks
            }

            val playlist = json.getAsJsonObject("playlist") ?: return tracks

            var tracksArr: JsonArray? = null
            if (playlist.has("tracks") && !playlist.get("tracks").isJsonNull) {
                tracksArr = playlist.getAsJsonArray("tracks")
            }

            if (tracksArr == null || tracksArr.size() == 0) {
                var trackIds: JsonArray? = null
                if (playlist.has("trackIds") && !playlist.get("trackIds").isJsonNull) {
                    trackIds = playlist.getAsJsonArray("trackIds")
                }

                if (trackIds == null || trackIds.size() == 0) return tracks

                val ids = mutableListOf<Long>()
                val start = minOf(offset, trackIds.size())
                val end = minOf(start + limit, trackIds.size())
                for (i in start until end) {
                    ids.add(trackIds[i].asJsonObject.get("id").asLong)
                }
                if (ids.isNotEmpty()) {
                    return NeteaseApiSearch.getSongDetail(*ids.toLongArray())
                }
                return tracks
            }

            val start = minOf(offset, tracksArr.size())
            val end = minOf(start + limit, tracksArr.size())
            for (i in start until end) {
                tracks.add(parseSongObject(tracksArr[i].asJsonObject))
            }
        } catch (e: Exception) {
            System.err.println("[NeteaseRequestApi] getPlaylistDetail failed: ${e.message}")
            e.printStackTrace()
        }
        return tracks
    }

    fun getPlaylistDetail(playlistId: Long): List<NeteaseApiSearch.NeteaseTrack> {
        return getPlaylistDetail(playlistId, 50, 0)
    }

    fun getToplist(): List<ToplistInfo> {
        val list = mutableListOf<ToplistInfo>()
        try {
            val data = JsonObject().apply {
                addProperty("csrf_token", "")
            }

            val enc = NeteaseApiEncrypt.encrypt(data.toString())
            val body = "params=${encUrl(enc[0])}&encSecKey=${encUrl(enc[1])}"

            val resp = NeteaseApiSearch.invokePostRequest("$BASE/weapi/toplist", body)
            val json = JsonParser().parse(resp).asJsonObject
            if (json.get("code").asInt != 200) return list

            val toplistArr = json.getAsJsonArray("list") ?: return list

            for (elem in toplistArr) {
                val item = elem.asJsonObject
                val id = item.get("id").asLong
                val name = item.get("name").asString
                val coverUrl = if (item.has("coverImgUrl")) item.get("coverImgUrl").asString else ""
                val updateTime = if (item.has("updateFrequency")) item.get("updateFrequency").asString else ""
                list.add(ToplistInfo(id, name, coverUrl, updateTime))
            }
        } catch (e: Exception) {
            System.err.println("[NeteaseRequestApi] getToplist failed: ${e.message}")
            e.printStackTrace()
        }
        return list
    }

    fun parseSongObject(song: JsonObject): NeteaseApiSearch.NeteaseTrack {
        val id = song.get("id").asLong
        val name = song.get("name").asString

        val artist = when {
            song.has("ar") && song.getAsJsonArray("ar").size() > 0 ->
                song.getAsJsonArray("ar").get(0).asJsonObject.get("name").asString
            song.has("artists") && song.getAsJsonArray("artists").size() > 0 ->
                song.getAsJsonArray("artists").get(0).asJsonObject.get("name").asString
            else -> "Unknown"
        }

        var album = ""
        var coverUrl = ""
        if (song.has("al") && !song.get("al").isJsonNull) {
            val al = song.getAsJsonObject("al")
            if (al.has("name")) album = al.get("name").asString
            if (al.has("picUrl")) coverUrl = al.get("picUrl").asString
        } else if (song.has("album") && !song.get("album").isJsonNull) {
            val alb = song.getAsJsonObject("album")
            if (alb.has("name")) album = alb.get("name").asString
            if (alb.has("picUrl")) coverUrl = alb.get("picUrl").asString
        }

        val duration = when {
            song.has("dt") -> song.get("dt").asLong
            song.has("duration") -> song.get("duration").asLong
            else -> 0L
        }

        return NeteaseApiSearch.NeteaseTrack(id, name, artist, album, duration, coverUrl)
    }

    private fun encUrl(s: String): String {
        return java.net.URLEncoder.encode(s, "UTF-8")
    }
}
