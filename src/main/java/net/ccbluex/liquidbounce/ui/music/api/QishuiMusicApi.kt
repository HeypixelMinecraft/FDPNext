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
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 汽水音乐 API
 *
 * 反编译自 Soda Music PC 客户端 v3.5.1 (asar + source map)
 *
 * 架构：
 * - Base URL: https://api.qishui.com
 * - 公共查询参数 (TT Common Params): aid / app_name / device_id / iid / version_name 等
 * - 搜索: GET /luna/pc/search/{search_type}  (search_type ∈ all/track/album/artist/playlist)
 * - 播放信息: POST /luna/pc/track_v2 (Body JSON: track_id/media_type/queue_type/scene_name/play_count)
 *   响应中直接包含 lyric.content (LRC 格式) 和 track_player.url_player_info
 * - 真实播放 URL: GET track_player.url_player_info → Result.Data.PlayInfoList[0].MainPlayUrl
 *
 * 风控注意：
 * - device_id / iid 需要持久化（首次启动通过 device_register 接口获取，这里用固定值兜底）
 * - ttwid 通过响应 Set-Cookie 返回，会自动保存到 cookieStore
 * - 触发风控时响应头会有 bdturing-verify，需要人工/打码通过（这里直接返回失败）
 */
object QishuiMusicApi {

    private const val BASE = "https://api.qishui.com"
    private const val APP_ID = "386088"
    private const val APP_NAME = "luna_pc"
    private const val VERSION_NAME = "3.5.1"
    private const val VERSION_CODE = "30050100"
    private const val CHANNEL = "official"
    private const val BUILD_MODE = "master"
    // 汽水音乐 PC 客户端 User-Agent
    private const val USER_AGENT = "LunaPC/3.5.1(408871041)"
    // 用字符拼接避免源码中出现 * / 序列（Kotlin 解析器在某些上下文会误判）
    private val ACCEPT_HEADER = "application/json, text/plain, " + "*" + "/" + "*"

    // 持久化的设备标识（来自客户端首次启动注册；可自行替换为新注册的值）
    @Volatile var deviceId: String = "3805838440943668"
    @Volatile var installId: String = "2380871729681244"

    // ttwid cookie（首次请求后由服务器 Set-Cookie 返回，会自动被 CookieManager 缓存）
    @Volatile var ttwid: String = ""

    /**
     * 搜索歌曲
     *
     * GET /luna/pc/search/track?q=<keyword>&cursor=0&search_id=<uuid>&search_method=input
     *
     * @param keyword 关键词
     * @param limit   最大返回数（汽水音乐单页固定 20 条，超过需要翻页，这里简化处理）
     */
    fun search(keyword: String, limit: Int = 20): List<MusicTrack> {
        return try {
            val encoded = URLEncoder.encode(keyword, "UTF-8")
            val searchId = java.util.UUID.randomUUID().toString()
            // search_type=track 只搜歌曲；若想综合搜索可改为 all
            val path = "/luna/pc/search/track"
            val query = buildString {
                appendCommonParams()
                append("&q=").append(encoded)
                append("&cursor=0")
                append("&search_id=").append(searchId)
                append("&search_method=input")
                append("&debug_params=")
                append("&from_search_id=")
                append("&search_scene=")
            }
            val url = "$BASE$path?$query"
            val resp = httpGet(url)
            val json = JsonParser().parse(resp as String).asJsonObject
            val statusCode = json.get("status_code")?.asInt ?: -1
            if (statusCode != 0) {
                ClientUtils.logWarn("[Qishui] 搜索失败 status_code=$statusCode, msg=${json.getAsJsonObject("status_info")?.get("status_msg")}")
                return emptyList()
            }
            val groups = json.getAsJsonArray("result_groups") ?: return emptyList()
            val results = mutableListOf<MusicTrack>()
            // 遍历所有分组，提取 track 类型
            for (i in 0 until groups.size()) {
                val group = groups[i].asJsonObject
                val groupId = group.get("id")?.asString ?: continue
                val data = group.getAsJsonArray("data") ?: continue
                for (j in 0 until data.size()) {
                    if (results.size >= limit) break
                    val item = data[j].asJsonObject
                    val entity = item.getAsJsonObject("entity") ?: continue
                    val track = entity.getAsJsonObject("track") ?: continue
                    val t = parseTrack(track) ?: continue
                    results.add(t)
                }
                if (results.size >= limit) break
            }
            results
        } catch (e: Exception) {
            ClientUtils.logWarn("[Qishui] 搜索异常: ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取播放 URL
     *
     * 流程：
     * 1. POST /luna/pc/track_v2 → 获取 track_player.url_player_info
     * 2. GET url_player_info → 解析 Result.Data.PlayInfoList[0].MainPlayUrl
     *
     * @param trackId 歌曲 ID (track.id)
     * @return 可直接播放的 MP3/M4A URL，失败返回 null
     */
    fun getStreamUrl(trackId: String): String? {
        return try {
            val trackV2Resp = getTrackV2(trackId) ?: return null
            val trackPlayer = trackV2Resp.getAsJsonObject("track_player") ?: return null
            // 优先：直接是 video_model（JSON 字符串内含 URL）
            val videoModel = trackPlayer.get("video_model")?.takeIf { !it.isJsonNull }?.asString
            if (!videoModel.isNullOrEmpty()) {
                try {
                    val vm = JsonParser().parse(videoModel as String).asJsonObject
                    val videoList = vm.getAsJsonArray("video_list")
                    if (videoList != null && videoList.size() > 0) {
                        val mainUrl = videoList[0].asJsonObject.get("main_url")?.asString
                        if (!mainUrl.isNullOrEmpty()) return mainUrl
                    }
                } catch (_: Exception) { }
            }
            // 兜底：url_player_info → RPCVideoModel
            val urlPlayerInfo = trackPlayer.get("url_player_info")?.takeIf { !it.isJsonNull }?.asString
            if (urlPlayerInfo.isNullOrEmpty()) return null
            val resp = httpGet(urlPlayerInfo)
            val rpcJson = JsonParser().parse(resp as String).asJsonObject
            val playInfoList = rpcJson
                .getAsJsonObject("Result")
                ?.getAsJsonObject("Data")
                ?.getAsJsonArray("PlayInfoList")
                ?: return null
            if (playInfoList.size() == 0) return null
            // 选取第一个有 MainPlayUrl 的项（通常是标准音质）
            for (i in 0 until playInfoList.size()) {
                val item = playInfoList[i].asJsonObject
                val mainUrl = item.get("MainPlayUrl")?.takeIf { !it.isJsonNull }?.asString
                if (!mainUrl.isNullOrEmpty()) return mainUrl
            }
            null
        } catch (e: Exception) {
            ClientUtils.logWarn("[Qishui] 获取播放URL异常: ${e.message}")
            null
        }
    }

    /**
     * 调用 GetTrackV2 接口
     *
     * POST /luna/pc/track_v2
     * Body: { media_type, track_id, queue_type, scene_name, play_count }
     *
     * 响应中包含:
     * - lyric.content (LRC 格式歌词，可直接解析)
     * - track_player.url_player_info (下一步要请求的 URL)
     * - track (完整曲目信息)
     */
    fun getTrackV2(trackId: String): com.google.gson.JsonObject? {
        return try {
            val path = "/luna/pc/track_v2"
            val query = buildString { appendCommonParams() }
            val url = "$BASE$path?$query"
            // 注意：POST body 中的字段在 query 中也会被自动剔除（参考 request.ts 实现）
            val body = com.google.gson.JsonObject().apply {
                addProperty("media_type", "audio")
                addProperty("track_id", trackId)
                addProperty("queue_type", "discovery_playlist")
                addProperty("scene_name", "personal_recommend")
                addProperty("play_count", 1)
            }.toString()
            val resp = httpPostJson(url, body)
            val json = JsonParser().parse(resp as String).asJsonObject
            val statusCode = json.get("status_code")?.asInt ?: -1
            if (statusCode != 0) {
                ClientUtils.logWarn("[Qishui] GetTrackV2 失败 status_code=$statusCode, msg=${json.getAsJsonObject("status_info")?.get("status_msg")}")
                return null
            }
            json
        } catch (e: Exception) {
            ClientUtils.logWarn("[Qishui] GetTrackV2 异常: ${e.message}")
            null
        }
    }

    /**
     * 获取歌词（LRC 格式）
     *
     * 汽水音乐的歌词直接随 GetTrackV2 响应返回，无需额外请求！
     *
     * 响应结构:
     * lyric.content = "[00:12.34]歌词第一行\n[00:15.67]..."  (LRC 格式)
     * lyric.translations.cn = "[00:12.34]翻译..."  (可选，LRC 格式)
     * lyric.lyric_contributor.user.nickname = "..."  (可选)
     *
     * @param trackId 歌曲 ID
     * @return 歌词行列表（带时间戳，已排序），失败返回空列表
     */
    fun getLyrics(trackId: String): List<LyricLine> {
        return try {
            val trackV2Resp = getTrackV2(trackId) ?: return emptyList()
            val lyric = trackV2Resp.getAsJsonObject("lyric") ?: return emptyList()
            val lrcText = lyric.get("content")?.takeIf { !it.isJsonNull }?.asString ?: return emptyList()
            if (lrcText.isBlank()) return emptyList()
            LrcParser.parse(lrcText)
        } catch (e: Exception) {
            ClientUtils.logWarn("[Qishui] 获取歌词异常: ${e.message}")
            emptyList()
        }
    }

    /**
     * 解析 track JSON 为 MusicTrack
     *
     * Track 关键字段（来自 entity/track.ts + playableFromTrack）:
     * - id, name, sub_name, media_type, duration (秒), vocal
     * - artists[]: { id, name, url_avatar }
     * - album: { id, name, url_cover }
     */
    private fun parseTrack(track: com.google.gson.JsonObject): MusicTrack? {
        return try {
            val id = track.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return null
            val name = track.get("name")?.takeIf { !it.isJsonNull }?.asString ?: "未知"
            // 艺术家
            val artists = track.getAsJsonArray("artists")?.joinToString(", ") {
                it.asJsonObject.get("name")?.takeIf { !it.isJsonNull }?.asString ?: ""
            } ?: "未知"
            // 专辑
            val album = track.getAsJsonObject("album")?.get("name")?.takeIf { !it.isJsonNull }?.asString ?: ""
            // 封面
            val coverUrl = track.getAsJsonObject("album")
                ?.getAsJsonObject("url_cover")
                ?.get("url")?.takeIf { !it.isJsonNull }?.asString ?: ""
            // 时长（汽水音乐 duration 字段单位是秒，需要 ×1000 转毫秒）
            val durationSec = track.get("duration")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
            val durationMs = if (durationSec in 1L..7200L) durationSec * 1000L else durationSec
            MusicTrack(
                id = id,
                name = name,
                artist = artists,
                album = album,
                duration = durationMs,
                source = MusicSource.QISHUI,
                streamUrl = "", // 通过 getStreamUrl 异步获取
                coverUrl = coverUrl,
                externalUrl = "https://lf-music.qishui.com/song/$id"
            )
        } catch (e: Exception) {
            null
        }
    }

    // ============ HTTP 工具 ============

    /**
     * 构造公共查询参数 (TT Common Params)
     *
     * 这些参数会自动附加到所有 luna/pc 请求上（参考 RequestService.getTTCommonParams）
     */
    private fun StringBuilder.appendCommonParams() {
        append("aid=").append(APP_ID)
        append("&app_name=").append(APP_NAME)
        append("&region=cn&geo_region=cn&os_region=cn&sim_region=")
        append("&device_id=").append(deviceId)
        append("&cdid=")
        append("&iid=").append(installId)
        append("&version_name=").append(VERSION_NAME)
        append("&version_code=").append(VERSION_CODE)
        append("&channel=").append(CHANNEL)
        append("&build_mode=").append(BUILD_MODE)
        append("&network_carrier=")
        append("&ac=wifi")
        append("&tz_name=").append(URLEncoder.encode("Asia/Shanghai", "UTF-8"))
        append("&resolution=")
        append("&device_platform=windows")
        append("&device_type=Windows")
        append("&os_version=").append(URLEncoder.encode("Windows 11 Pro", "UTF-8"))
        append("&fp=").append(deviceId)
    }

    /**
     * HTTP GET 请求（带 cookie 和 UA）
     */
    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", ACCEPT_HEADER)
        conn.setRequestProperty("Referer", "https://api.qishui.com/")
        if (ttwid.isNotEmpty()) {
            conn.setRequestProperty("Cookie", "ttwid=" + ttwid)
        }
        try {
            val code = conn.responseCode
            extractTtwid(conn)
            if (code != 200) {
                ClientUtils.logWarn("[Qishui] HTTP $code GET $url")
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw RuntimeException("HTTP $code: $err")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * HTTP POST 请求（JSON body）
     */
    private fun httpPostJson(url: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.requestMethod = "POST"
        conn.connectTimeout = 5000
        conn.readTimeout = 15000
        conn.doOutput = true
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", ACCEPT_HEADER)
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Referer", "https://api.qishui.com/")
        if (ttwid.isNotEmpty()) {
            conn.setRequestProperty("Cookie", "ttwid=" + ttwid)
        }
        try {
            DataOutputStream(conn.outputStream).use { it.writeBytes(body) }
            val code = conn.responseCode
            extractTtwid(conn)
            if (code != 200) {
                ClientUtils.logWarn("[Qishui] HTTP $code POST $url")
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw RuntimeException("HTTP $code: $err")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 从响应头 Set-Cookie 中提取 ttwid 并缓存
     */
    private fun extractTtwid(conn: HttpURLConnection) {
        try {
            val cookies = conn.getHeaderFields()["Set-Cookie"] ?: return
            for (cookie in cookies) {
                val match = Regex("ttwid=([^;]+)").find(cookie)
                if (match != null) {
                    ttwid = match.groupValues[1]
                    return
                }
            }
        } catch (_: Exception) { }
    }
}
