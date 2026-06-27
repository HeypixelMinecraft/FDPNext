/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music.api

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.ui.music.LyricLine
import net.ccbluex.liquidbounce.ui.music.MusicSource
import net.ccbluex.liquidbounce.ui.music.MusicTrack
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * YouTube Music API
 *
 * 直接调用 YouTube Music 官方 InnerTube 接口（基于 ytmusicapi 调研）
 * - 搜索: POST https://music.youtube.com/youtubei/v1/search?alt=json
 *   使用 WEB_REMIX 客户端，无需认证（仅需 visitor_id，匿名也可工作）
 * - 播放流 URL: POST https://www.youtube.com/youtubei/v1/player
 *   使用 ANDROID 客户端（返回的 URL 通常预签名，无需 deciphering）
 *
 * 响应解析：
 * - 搜索结果位于 contents.tabbedSearchResultsRenderer.tabs[].tabRenderer.content
 *   .sectionListRenderer.contents[]，每项为 musicShelfRenderer（分类列表）
 *   或 musicCardShelfRenderer（顶部结果）
 * - 列表项 musicResponsiveListItemRenderer 的 overlay.musicItemThumbnailOverlayRenderer
 *   .content.musicPlayButtonRenderer.playNavigationEndpoint.watchEndpoint.videoId 即 videoId
 * - 流 URL 位于 streamingData.adaptiveFormats[].url（audio/webm 或 audio/mp4）
 */
object YoutubeMusicApi {

    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?alt=json"
    private const val PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
    private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next?alt=json"
    private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?alt=json"

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0"
    private const val YTM_CLIENT_VERSION = "1.20260627.01.00"
    private const val ANDROID_CLIENT_VERSION = "19.09.37"

    /**
     * 搜索歌曲
     * @param keyword 关键词
     * @param limit 返回数量
     */
    fun search(keyword: String, limit: Int = 20): List<MusicTrack> {
        return try {
            val body = """
                {"query":${toJsonString(keyword)},"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"$YTM_CLIENT_VERSION","hl":"zh","gl":"CN"}}}
            """.trimIndent()

            val resp = postJson(SEARCH_URL, body)
            val json = JsonParser().parse(resp).asJsonObject

            val results = mutableListOf<MusicTrack>()
            val tabs = json.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()

            for (tab in tabs) {
                val tabRenderer = tab.asJsonObject?.getAsJsonObject("tabRenderer") ?: continue
                val sectionList = tabRenderer.getAsJsonObject("content")
                    ?.getAsJsonObject("sectionListRenderer")
                    ?.getAsJsonArray("contents") ?: continue

                for (section in sectionList) {
                    val sectionObj = section.asJsonObject
                    // 处理 musicShelfRenderer（分类列表，如 Songs/Videos）
                    val shelf = sectionObj.getAsJsonObject("musicShelfRenderer")
                    if (shelf != null) {
                        val category = shelf.getAsJsonArray("contents") ?: continue
                        for (item in category) {
                            if (results.size >= limit) break
                            val track = parseListItem(item.asJsonObject)
                            if (track != null) results.add(track)
                        }
                    }
                    // 处理 musicCardShelfRenderer（顶部结果卡片）
                    val card = sectionObj.getAsJsonObject("musicCardShelfRenderer")
                    if (card != null) {
                        val track = parseCardShelf(card)
                        if (track != null && results.size < limit) results.add(track)
                    }
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析 musicResponsiveListItemRenderer
     */
    private fun parseListItem(item: com.google.gson.JsonObject): MusicTrack? {
        val renderer = item.getAsJsonObject("musicResponsiveListItemRenderer") ?: return null
        val videoId = extractVideoId(renderer) ?: return null
        val (title, artist, album) = extractFlexColumns(renderer)
        val duration = extractDuration(renderer)
        val thumbnail = extractThumbnail(renderer)
        return MusicTrack(
            id = videoId,
            name = title,
            artist = artist,
            album = album,
            duration = duration,
            source = MusicSource.YOUTUBE_MUSIC,
            streamUrl = "", // 需要通过 getStreamUrl 异步获取
            coverUrl = thumbnail,
            externalUrl = "https://music.youtube.com/watch?v=$videoId"
        )
    }

    /**
     * 解析 musicCardShelfRenderer（顶部结果）
     */
    private fun parseCardShelf(card: com.google.gson.JsonObject): MusicTrack? {
        val title = card.getAsJsonObject("title")
            ?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject?.get("text")?.asString ?: "未知"
        val subtitle = card.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
            ?.joinToString("") { it.asJsonObject.get("text").asString } ?: "未知"
        val videoId = card.getAsJsonObject("thumbnailOverlay")
            ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
            ?.getAsJsonObject("content")
            ?.getAsJsonObject("musicPlayButtonRenderer")
            ?.getAsJsonObject("playNavigationEndpoint")
            ?.getAsJsonObject("watchEndpoint")
            ?.get("videoId")?.asString ?: return null
        val thumbnail = card.getAsJsonObject("thumbnail")
            ?.getAsJsonObject("musicThumbnailRenderer")
            ?.getAsJsonObject("thumbnail")
            ?.getAsJsonArray("thumbnails")
            ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
        return MusicTrack(
            id = videoId,
            name = title,
            artist = subtitle,
            album = "",
            duration = 0L,
            source = MusicSource.YOUTUBE_MUSIC,
            streamUrl = "",
            coverUrl = thumbnail,
            externalUrl = "https://music.youtube.com/watch?v=$videoId"
        )
    }

    /**
     * 从 renderer 中提取 videoId
     */
    private fun extractVideoId(renderer: com.google.gson.JsonObject): String? {
        val overlay = renderer.getAsJsonObject("overlay")
            ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
            ?.getAsJsonObject("content")
            ?.getAsJsonObject("musicPlayButtonRenderer")
            ?.getAsJsonObject("playNavigationEndpoint")
            ?.getAsJsonObject("watchEndpoint") ?: return null
        return overlay.get("videoId")?.asString
    }

    /**
     * 提取 flexColumns 中的标题、艺术家、专辑
     */
    private fun extractFlexColumns(renderer: com.google.gson.JsonObject): Triple<String, String, String> {
        val columns = renderer.getAsJsonArray("flexColumns") ?: return Triple("未知", "未知", "")
        val title = StringBuilder()
        val artist = StringBuilder()
        var album = ""
        for (i in 0 until columns.size()) {
            val textRuns = columns[i].asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs") ?: continue
            val text = textRuns.joinToString("") { it.asJsonObject.get("text").asString }
            when (i) {
                0 -> title.append(text)
                1 -> artist.append(text)
                2 -> album = text
            }
        }
        return Triple(
            if (title.isEmpty()) "未知" else title.toString(),
            if (artist.isEmpty()) "未知" else artist.toString(),
            album
        )
    }

    /**
     * 提取时长（毫秒）
     */
    private fun extractDuration(renderer: com.google.gson.JsonObject): Long {
        return try {
            val fixedColumns = renderer.getAsJsonArray("fixedColumns") ?: return 0L
            val text = fixedColumns.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
                ?.firstOrNull()?.asJsonObject?.get("text")?.asString ?: return 0L
            // 格式 "3:45" 或 "1:23:45"
            val parts = text.split(":").map { it.toLongOrNull() ?: 0L }
            val ms = when (parts.size) {
                2 -> parts[0] * 60 + parts[1]
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                else -> 0L
            }
            ms * 1000
        } catch (_: Exception) { 0L }
    }

    /**
     * 提取缩略图 URL
     */
    private fun extractThumbnail(renderer: com.google.gson.JsonObject): String {
        return try {
            renderer.getAsJsonObject("thumbnail")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
                ?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""
        } catch (_: Exception) { "" }
    }

    /**
     * 通过 videoId 获取可播放的音频流 URL
     * 使用 ANDROID 客户端（返回的 URL 通常预签名，无需 deciphering）
     *
     * @param videoId YouTube 视频 ID
     * @return 音频流 URL，失败返回 null
     */
    fun getStreamUrl(videoId: String): String? {
        return try {
            val body = """
                {"videoId":${toJsonString(videoId)},"context":{"client":{"clientName":"ANDROID","clientVersion":"$ANDROID_CLIENT_VERSION","androidSdkVersion":30,"hl":"zh","gl":"CN"}}}
            """.trimIndent()

            val resp = postJson(PLAYER_URL, body)
            val json = JsonParser().parse(resp).asJsonObject
            val streamingData = json.getAsJsonObject("streamingData") ?: return null

            // 优先选择 audio/mp4 或 audio/webm 的自适应格式
            val adaptiveFormats = streamingData.getAsJsonArray("adaptiveFormats") ?: return null
            // 找 audio 流（mimeType 以 audio/ 开头），优先 mp4
            var best: String? = null
            var bestBitrate = -1
            for (fmt in adaptiveFormats) {
                val fmtObj = fmt.asJsonObject
                val mimeType = fmtObj.get("mimeType")?.asString ?: continue
                if (!mimeType.startsWith("audio/")) continue
                val url = fmtObj.get("url")?.asString ?: continue
                val bitrate = fmtObj.get("bitrate")?.asInt ?: 0
                // 优先 mp4（aac），其次 webm（opus）
                val isMp4 = mimeType.contains("audio/mp4") || mimeType.contains("audio/m4a")
                val score = bitrate + (if (isMp4) 100000 else 0)
                if (score > bestBitrate) {
                    bestBitrate = score
                    best = url
                }
            }
            // 兜底：formats 数组
            if (best == null) {
                val formats = streamingData.getAsJsonArray("formats")
                if (formats != null && formats.size() > 0) {
                    best = formats[0].asJsonObject.get("url")?.asString
                }
            }
            best
        } catch (e: Exception) { null }
    }

    /**
     * 获取歌词（纯文本）
     *
     * 流程：
     * 1. 调用 /next?videoId={id} 获取 watch next 面板，提取歌词 tab 的 browseId
     *    （browsable endpoint 位于 tabs[].tabRenderer.endpoint.browseEndpoint.browseId，
     *     tab 标题为 "Lyrics"）
     * 2. 调用 /browse?browseId={browseId} 获取歌词段落，
     *    文本位于 contents.sectionListRenderer.contents[].musicDescriptionShelfRenderer.description.runs[].text
     *
     * 注意：YouTube Music 歌词没有时间戳，转换为 LyricLine 时 timeMs 全部设为 0，
     *      歌词面板会以纯文本方式展示（不进行高亮同步）。
     *
     * @param videoId YouTube 视频 ID
     * @return 歌词行列表，失败返回空列表
     */
    fun getLyrics(videoId: String): List<LyricLine> {
        return try {
            val nextBody = """
                {"videoId":${toJsonString(videoId)},"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"$YTM_CLIENT_VERSION","hl":"zh","gl":"CN"}}}
            """.trimIndent()
            val nextResp = postJson(NEXT_URL, nextBody)
            val nextJson = JsonParser().parse(nextResp).asJsonObject

            // 查找歌词 tab：tabs[].tabRenderer.title == "Lyrics" 且存在 endpoint.browseEndpoint
            val tabs = nextJson.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnMusicWatchNextResultsRenderer")
                ?.getAsJsonObject("tabbedRenderer")
                ?.getAsJsonObject("watchNextTabbedResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()
            var lyricsBrowseId: String? = null
            for (tab in tabs) {
                val tabRenderer = tab.asJsonObject?.getAsJsonObject("tabRenderer") ?: continue
                val title = tabRenderer.get("title")?.asString ?: continue
                if (!title.equals("Lyrics", ignoreCase = true)) continue
                val browseId = tabRenderer.getAsJsonObject("endpoint")
                    ?.getAsJsonObject("browseEndpoint")
                    ?.get("browseId")?.asString
                if (!browseId.isNullOrEmpty()) {
                    lyricsBrowseId = browseId
                    break
                }
            }
            if (lyricsBrowseId == null) return emptyList()

            val browseBody = """
                {"browseId":${toJsonString(lyricsBrowseId)},"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"$YTM_CLIENT_VERSION","hl":"zh","gl":"CN"}}}
            """.trimIndent()
            val browseResp = postJson(BROWSE_URL, browseBody)
            val browseJson = JsonParser().parse(browseResp).asJsonObject

            // 在 sectionListRenderer.contents[] 中查找 musicDescriptionShelfRenderer
            val contents = browseJson.getAsJsonObject("contents")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()
            for (section in contents) {
                val desc = section.asJsonObject?.getAsJsonObject("musicDescriptionShelfRenderer") ?: continue
                val runs = desc.getAsJsonObject("description")?.getAsJsonArray("runs") ?: continue
                val text = runs.joinToString("") { it.asJsonObject.get("text").asString }
                if (text.isBlank()) continue
                // 按行拆分，每行作为一个 LyricLine（timeMs=0，无时间同步）
                return text.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { LyricLine(0L, it) }
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 发送 POST JSON 请求
     */
    private fun postJson(url: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 30000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/")
            doOutput = true
        }
        try {
            val os: OutputStream = conn.outputStream
            os.write(body.toByteArray(Charsets.UTF_8))
            os.flush()
        } catch (e: Exception) {
            // 可能是 4xx，但仍可能有响应体
        }
        return try {
            conn.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            conn.errorStream?.bufferedReader()?.readText() ?: throw e
        }
    }

    /**
     * 转义 JSON 字符串
     */
    private fun toJsonString(s: String): String {
        val escaped = s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
