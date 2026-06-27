/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music

import net.ccbluex.liquidbounce.ui.music.api.KugouMusicApi
import net.ccbluex.liquidbounce.ui.music.api.NeteaseMusicApi
import net.ccbluex.liquidbounce.ui.music.api.QishuiMusicApi
import net.ccbluex.liquidbounce.ui.music.api.YoutubeMusicApi
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 音乐播放器管理器
 *
 * 负责管理播放队列、当前播放、源切换、播放控制
 */
object MusicPlayer {

    val engine = MusicPlayerEngine()

    /** 播放队列 */
    val playlist = CopyOnWriteArrayList<MusicTrack>()

    /** 当前播放索引 */
    @Volatile var currentIndex: Int = -1
        private set

    /** 当前播放的曲目 */
    val currentTrack: MusicTrack? get() = if (currentIndex in playlist.indices) playlist[currentIndex] else null

    /** 当前音乐源 */
    @Volatile var currentSource: MusicSource = MusicSource.NETEASE

    /** 是否循环播放 */
    @Volatile var loop: Boolean = false

    /** 搜索结果 */
    val searchResults = CopyOnWriteArrayList<MusicTrack>()

    /** 是否正在搜索 */
    @Volatile var searching: Boolean = false

    /** 搜索回调 */
    var onSearchComplete: ((List<MusicTrack>) -> Unit)? = null

    /** 播放状态变化回调 */
    var onTrackChanged: ((MusicTrack?) -> Unit)? = null
    var onPlayStateChanged: (() -> Unit)? = null

    /** 当前歌词行列表（按时间升序，空列表表示无歌词） */
    @Volatile var currentLyrics: List<LyricLine> = emptyList()
        private set

    /** 是否正在获取歌词 */
    @Volatile var fetchingLyrics: Boolean = false
        private set

    /** 歌词获取完成回调 */
    var onLyricsUpdated: (() -> Unit)? = null

    init {
        engine.onFinish = {
            // 播放完毕，自动下一首
            if (loop) {
                currentTrack?.let { playTrack(it) }
            } else {
                next()
            }
        }
        engine.onError = { msg ->
            ClientUtils.logWarn("[MusicPlayer] $msg")
        }
    }

    /**
     * 搜索歌曲
     */
    fun search(keyword: String) {
        if (keyword.isBlank()) return
        searching = true
        Thread {
            try {
                val results = when (currentSource) {
                    MusicSource.NETEASE -> NeteaseMusicApi.search(keyword)
                    MusicSource.KUGOU -> KugouMusicApi.search(keyword)
                    MusicSource.YOUTUBE_MUSIC -> YoutubeMusicApi.search(keyword)
                    MusicSource.QISHUI -> QishuiMusicApi.search(keyword)
                }
                searchResults.clear()
                searchResults.addAll(results)
            } catch (e: Exception) {
                ClientUtils.logWarn("[MusicPlayer] 搜索失败: ${e.message}")
            } finally {
                searching = false
                onSearchComplete?.invoke(searchResults.toList())
            }
        }.apply { isDaemon = true; name = "FDPNext-MusicSearch" }.start()
    }

    /**
     * 播放指定曲目
     */
    fun playTrack(track: MusicTrack) {
        stop()
        // 酷狗/汽水/YouTube 需要先获取流 URL
        val streamUrl = when (track.source) {
            MusicSource.KUGOU -> if (track.streamUrl.isEmpty()) KugouMusicApi.getStreamUrl(track.id, "") ?: ""
            else track.streamUrl
            MusicSource.YOUTUBE_MUSIC -> if (track.streamUrl.isEmpty()) YoutubeMusicApi.getStreamUrl(track.id) ?: ""
            else track.streamUrl
            MusicSource.QISHUI -> if (track.streamUrl.isEmpty()) QishuiMusicApi.getStreamUrl(track.id) ?: ""
            else track.streamUrl
            else -> track.streamUrl
        }
        if (streamUrl.isEmpty()) {
            ClientUtils.logWarn("[MusicPlayer] 无法获取播放URL: ${track.name}")
            return
        }
        engine.play(streamUrl, track)
        // 异步获取歌词
        fetchLyrics(track)
        onTrackChanged?.invoke(track)
        onPlayStateChanged?.invoke()
    }

    /**
     * 异步获取歌词
     */
    fun fetchLyrics(track: MusicTrack) {
        currentLyrics = emptyList()
        fetchingLyrics = true
        onLyricsUpdated?.invoke()
        Thread {
            try {
                val lyrics = when (track.source) {
                    MusicSource.NETEASE -> NeteaseMusicApi.getLyrics(track.id)
                    MusicSource.KUGOU -> KugouMusicApi.getLyrics(track.id, track.name, track.duration)
                    MusicSource.YOUTUBE_MUSIC -> YoutubeMusicApi.getLyrics(track.id)
                    MusicSource.QISHUI -> QishuiMusicApi.getLyrics(track.id)
                }
                currentLyrics = lyrics
            } catch (e: Exception) {
                ClientUtils.logWarn("[MusicPlayer] 获取歌词失败: ${e.message}")
                currentLyrics = emptyList()
            } finally {
                fetchingLyrics = false
                onLyricsUpdated?.invoke()
            }
        }.apply { isDaemon = true; name = "FDPNext-MusicLyrics" }.start()
    }

    /**
     * 根据当前播放进度计算高亮的歌词行索引
     *
     * @return 当前应高亮的歌词行索引，无歌词返回 -1
     */
    fun currentLyricIndex(): Int {
        val lyrics = currentLyrics
        if (lyrics.isEmpty()) return -1
        // YouTube Music 等无时间戳歌词（timeMs 全为 0）：不进行同步高亮
        if (lyrics.all { it.timeMs == 0L }) return -1
        val pos = engine.positionMs
        var idx = -1
        for (i in lyrics.indices) {
            if (lyrics[i].timeMs <= pos) idx = i else break
        }
        return idx
    }

    /**
     * 播放搜索结果中指定索引的曲目（并加入播放队列）
     */
    fun playFromSearch(index: Int) {
        if (index !in searchResults.indices) return
        playlist.clear()
        playlist.addAll(searchResults)
        currentIndex = index
        playTrack(playlist[index])
    }

    /**
     * 暂停
     */
    fun pause() {
        engine.pause()
        onPlayStateChanged?.invoke()
    }

    /**
     * 恢复
     */
    fun resume() {
        currentTrack?.let { track ->
            if (engine.isPaused) {
                val url = when (track.source) {
                    MusicSource.KUGOU -> if (track.streamUrl.isEmpty()) KugouMusicApi.getStreamUrl(track.id, "") ?: ""
                    else track.streamUrl
                    MusicSource.YOUTUBE_MUSIC -> if (track.streamUrl.isEmpty()) YoutubeMusicApi.getStreamUrl(track.id) ?: ""
                    else track.streamUrl
                    MusicSource.QISHUI -> if (track.streamUrl.isEmpty()) QishuiMusicApi.getStreamUrl(track.id) ?: ""
                    else track.streamUrl
                    else -> track.streamUrl
                }
                engine.resume(url, track)
                onPlayStateChanged?.invoke()
            }
        }
    }

    /**
     * 停止
     */
    fun stop() {
        engine.stop()
        currentLyrics = emptyList()
        onLyricsUpdated?.invoke()
        onPlayStateChanged?.invoke()
    }

    /**
     * 下一首
     */
    fun next() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex + 1 < playlist.size) currentIndex + 1 else 0
        playTrack(playlist[currentIndex])
    }

    /**
     * 上一首
     */
    fun previous() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex - 1 >= 0) currentIndex - 1 else playlist.size - 1
        playTrack(playlist[currentIndex])
    }

    /**
     * 切换音乐源
     */
    fun switchSource(newSource: MusicSource) {
        if (currentSource == newSource) return
        stop()
        currentSource = newSource
        searchResults.clear()
        playlist.clear()
        currentIndex = -1
        currentLyrics = emptyList()
        onLyricsUpdated?.invoke()
    }

    /**
     * 设置音量
     */
    fun setVolume(vol: Float) {
        engine.volume = vol
    }

    val isPlaying: Boolean get() = engine.isPlaying
    val isPaused: Boolean get() = engine.isPaused
}
