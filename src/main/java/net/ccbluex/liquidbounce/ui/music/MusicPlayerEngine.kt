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
import net.ccbluex.liquidbounce.FDPNext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 基于 JLayer 的 MP3 播放引擎（先缓存后播放）
 *
 * 播放流程：先把远程 MP3 完整下载到 FDPNext-1.8/MusicTemp 下的缓存文件，
 * 再从本地文件读取播放。相比直接流式播放更稳定（不受网络抖动影响），
 * 且暂停/恢复无需重新下载。
 *
 * 注意：JLayer 1.0.1 的 PlaybackListener 只有 playbackStarted/Finished，
 * 进度通过定时器线程模拟更新（基于 MP3 ~38.28 帧/秒，每帧约 26ms）
 */
class MusicPlayerEngine {

    private var player: AdvancedPlayer? = null
    private var playThread: Thread? = null
    private var progressThread: Thread? = null
    private var inputStream: InputStream? = null

    /** 当前曲目已缓存到本地的文件，用于暂停/恢复时复用，避免重复下载 */
    @Volatile private var cachedFile: File? = null

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

    /**
     * 开始播放指定 URL 的音频：先下载到本地缓存，再从文件播放
     *
     * @param track 曲目信息，用于按「曲目身份」缓存并写入 SQLite 缓存库（可空）
     */
    fun play(url: String, track: MusicTrack? = null) {
        stop()
        elapsedMs.set(0L)
        playThread = Thread({
            try {
                val file = cacheToFile(url, track)
                cachedFile = file
                paused.set(false)
                playFile(file, fromBeginning = true)
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
     * 解析出可供播放的本地 MP3 文件：
     * 1. 本地路径直接返回；
     * 2. 提供 track 时，优先命中 SQLite 缓存库；未命中则下载并登记映射；
     * 3. 无 track 时回退到按 URL 哈希缓存。
     */
    private fun cacheToFile(url: String, track: MusicTrack?): File {
        // 已是本地文件：直接使用
        if (!url.startsWith("http", ignoreCase = true)) {
            val local = if (url.startsWith("file:", ignoreCase = true)) File(URL(url).toURI()) else File(url)
            if (local.isFile) return local
        }

        val tempDir = FDPNext.fileManager.musicTempDir
        if (!tempDir.exists()) tempDir.mkdirs()

        // 按曲目身份缓存（跨会话复用），命中缓存库直接返回
        if (track != null) {
            MusicCacheDatabase.lookup(track)?.let { return it }
            val fileName = md5("${track.source.name}:${track.id}") + ".mp3"
            val file = download(url, File(tempDir, fileName))
            MusicCacheDatabase.store(track, file)
            return file
        }

        // 回退：按 URL 哈希缓存
        val target = File(tempDir, md5(url) + ".mp3")
        if (target.isFile && target.length() > 0) return target
        return download(url, target)
    }

    /**
     * 下载 URL 到目标文件：先写入 .part 临时文件，完成后改名，
     * 避免下载中断的半成品被当作有效缓存。
     */
    private fun download(url: String, target: File): File {
        val part = File(target.parentFile, target.name + ".part")
        runCatching { part.delete() }

        val conn = URL(url).openConnection()
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36")
        conn.connectTimeout = 10000
        conn.readTimeout = 30000
        conn.getInputStream().use { input ->
            FileOutputStream(part).use { output -> input.copyTo(output, 8192) }
        }

        runCatching { target.delete() }
        // 改名失败（跨卷等情况）则退而使用 .part 文件本身
        return if (part.renameTo(target)) target else part
    }

    /**
     * 从本地缓存文件播放（阻塞当前线程直至播放结束或被停止）
     */
    private fun playFile(file: File, fromBeginning: Boolean) {
        val stream = BufferedInputStream(FileInputStream(file))
        inputStream = stream

        // 用文件大小估算总时长（假设 128kbps：每秒 16000 字节）
        if (fromBeginning && file.length() > 0) {
            totalMs.set((file.length() / 16000.0 * 1000).toLong())
        }

        val p = AdvancedPlayer(stream)
        player = p
        playing.set(true)

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
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return BigInteger(1, digest).toString(16).padStart(32, '0')
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
    fun resume(url: String, track: MusicTrack? = null) {
        if (!paused.get()) return
        paused.set(false)
        // 复用本地缓存重播（无法精确 seek，从头开始但保留进度计数）
        val savedElapsed = elapsedMs.get()
        playThread = Thread({
            try {
                val file = cachedFile?.takeIf { it.isFile } ?: cacheToFile(url, track).also { cachedFile = it }
                elapsedMs.set(savedElapsed)
                playFile(file, fromBeginning = false)
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
