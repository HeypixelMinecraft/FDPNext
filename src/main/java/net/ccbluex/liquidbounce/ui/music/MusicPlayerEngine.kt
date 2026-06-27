/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music

import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.advanced.AdvancedPlayer
import javazoom.jl.player.advanced.PlaybackEvent
import javazoom.jl.player.advanced.PlaybackListener
import java.io.InputStream
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 基于 JLayer 的 MP3 流式播放引擎
 *
 * 支持：流式播放、暂停/恢复、停止、进度回调
 * 注意：JLayer 1.0.1 的 PlaybackListener 只有 playbackStarted/Finished，
 * 进度通过定时器线程模拟更新（基于 MP3 ~38.28 帧/秒，每帧约 26ms）
 */
class MusicPlayerEngine {

    private var player: AdvancedPlayer? = null
    private var playThread: Thread? = null
    private var progressThread: Thread? = null
    private var inputStream: InputStream? = null

    private val playing = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val elapsedMs = AtomicLong(0L)
    private val totalMs = AtomicLong(0L)

    /** 0.0 ~ 1.0（JLayer 不直接支持音量，此值仅记录供 UI 显示） */
    @Volatile var volume: Float = 0.5f
        set(value) { field = value.coerceIn(0f, 1f) }

    var onProgress: ((currentMs: Long, totalMs: Long) -> Unit)? = null
    var onFinish: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val isPlaying: Boolean get() = playing.get() && !paused.get()
    val isPaused: Boolean get() = paused.get()

    /** 当前播放位置（毫秒），供外部读取用于歌词同步 */
    val positionMs: Long get() = elapsedMs.get()
    /** 总时长（毫秒），0 表示未知 */
    val durationMs: Long get() = totalMs.get()

    /** 当前播放 URL，用于 resume 时重新连接 */
    private var currentUrl: String = ""

    /**
     * 开始播放指定 URL 的音频流
     */
    fun play(url: String) {
        stop()
        currentUrl = url
        elapsedMs.set(0L)
        playThread = Thread({
            try {
                val conn = URL(url).openConnection()
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36")
                conn.connectTimeout = 10000
                conn.readTimeout = 30000
                val stream = conn.getInputStream()
                inputStream = stream

                // 估算总时长：contentLength / (128kbps/8) = 秒数
                val contentLength = conn.contentLength.toLong()
                if (contentLength > 0) {
                    // 假设 128kbps：每秒 16000 字节
                    totalMs.set((contentLength / 16000.0 * 1000).toLong())
                }

                val p = AdvancedPlayer(stream)
                player = p
                playing.set(true)
                paused.set(false)

                p.playBackListener = object : PlaybackListener() {
                    override fun playbackStarted(e: PlaybackEvent?) {
                        startProgressTimer()
                    }

                    override fun playbackFinished(e: PlaybackEvent?) {
                        playing.set(false)
                        stopProgressTimer()
                        if (!paused.get()) {
                            onFinish?.invoke()
                        }
                    }
                }

                p.play()
            } catch (e: JavaLayerException) {
                playing.set(false)
                stopProgressTimer()
                onError?.invoke("播放错误: ${e.message}")
            } catch (e: Exception) {
                playing.set(false)
                stopProgressTimer()
                onError?.invoke("错误: ${e.message}")
            }
        }, "FDPNext-MusicPlayer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * 启动进度定时器（每 250ms 更新一次）
     */
    private fun startProgressTimer() {
        progressThread = Thread({
            try {
                while (playing.get() && !paused.get()) {
                    Thread.sleep(250)
                    elapsedMs.addAndGet(250)
                    onProgress?.invoke(elapsedMs.get(), totalMs.get())
                }
            } catch (_: InterruptedException) {
                // 暂停/停止时被中断属正常流程，无需上报
            }
        }, "FDPNext-MusicProgress").apply {
            isDaemon = true
            start()
        }
    }

    private fun stopProgressTimer() {
        progressThread?.interrupt()
        progressThread = null
    }

    /**
     * 暂停播放
     * JLayer 不原生支持暂停，这里停止播放但保留已播放时长
     */
    fun pause() {
        if (!playing.get() || paused.get()) return
        paused.set(true)
        playing.set(false)
        stopProgressTimer()
        try { player?.close() } catch (_: Exception) {}
    }

    /**
     * 恢复播放
     * 由于 MP3 流不支持随机访问，恢复时重新连接流并跳过已播放的时长
     * 注意：JLayer 的 play(Int, Int) 通过帧号跳过，但流式播放无法精确 seek，
     * 这里采用简单方案：重新从头播放（保留进度显示）
     */
    fun resume(url: String) {
        if (!paused.get()) return
        paused.set(false)
        // 重新播放（无法精确 seek，从头开始但保留进度计数）
        val savedElapsed = elapsedMs.get()
        playThread = Thread({
            try {
                val conn = URL(url).openConnection()
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36")
                conn.connectTimeout = 10000
                conn.readTimeout = 30000
                val stream = conn.getInputStream()
                inputStream = stream

                val p = AdvancedPlayer(stream)
                player = p
                playing.set(true)
                elapsedMs.set(savedElapsed)

                p.playBackListener = object : PlaybackListener() {
                    override fun playbackStarted(e: PlaybackEvent?) { startProgressTimer() }
                    override fun playbackFinished(e: PlaybackEvent?) {
                        playing.set(false)
                        stopProgressTimer()
                        if (!paused.get()) onFinish?.invoke()
                    }
                }

                p.play()
            } catch (e: Exception) {
                playing.set(false)
                stopProgressTimer()
                onError?.invoke("恢复错误: ${e.message}")
            }
        }, "FDPNext-MusicPlayer-Resume").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * 停止播放并释放资源
     */
    fun stop() {
        playing.set(false)
        paused.set(false)
        elapsedMs.set(0L)
        stopProgressTimer()
        try { player?.close() } catch (_: Exception) {}
        try { inputStream?.close() } catch (_: Exception) {}
        player = null
        inputStream = null
        playThread?.interrupt()
        playThread = null
    }
}
