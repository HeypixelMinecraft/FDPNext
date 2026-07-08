package net.ccbluex.liquidbounce.skid.sigma

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.BooleanControl
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

object SigmaMusicManager {

    private var playlist = Playlist("Default")
    private var currentIndex = 0
    @Volatile var isPlaying = false; private set
    private var volume = 50
    private var repeatMode = AudioRepeatMode.REPEAT
    private var audioThread: Thread? = null
    private var sourceDataLine: SourceDataLine? = null
    private val shouldStop = AtomicBoolean(false)
    @Volatile private var lastSetVolume = -1

    @Volatile var songTitle = ""; private set
    @Volatile var currentPositionMs = 0L; private set
    @Volatile var durationMs = 0L; private set

    private var currentLyrics: List<LrcParser.LyricLine>? = null
    private var currentYrcLyrics: List<YrcParser.YrcLine>? = null
    private val amplitudes = CopyOnWriteArrayList<Double>()
    private val visualizerData = CopyOnWriteArrayList<DoubleArray>()

    private var smoothedLyricProgress = 0.0f
    private var lastLyricText = ""
    private var lastProgressUpdateTime = 0L

    fun playFile(file: File) {
        val song = SongInfo.fromLocalFile(file)
        playlist.clear()
        playlist.addTrack(song)
        currentIndex = 0
        isPlaying = true
        startPlayback()
    }

    fun playSong(songId: Long, title: String) {
        val song = SongInfo(
            title = title,
            neteaseSongId = songId
        )
        playlist.clear()
        playlist.addTrack(song)
        currentIndex = 0
        isPlaying = true
        startPlayback()
    }

    fun playPlaylist(tracks: List<SongInfo>, startIndex: Int = 0) {
        playlist.clear()
        playlist.tracks.addAll(tracks)
        currentIndex = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        isPlaying = true
        startPlayback()
    }

    fun playNeteasePlaylist(tracks: List<NeteaseApiSearch.NeteaseTrack>, startIndex: Int = 0) {
        val songs = tracks.map { SongInfo.fromNeteaseTrack(it) }
        playPlaylist(songs, startIndex)
    }

    fun pause() {
        isPlaying = false
        shouldStop.set(true)
        sourceDataLine?.flush()
    }

    fun resume() {
        if (playlist.size > 0) {
            shouldStop.set(false)
            isPlaying = true
        }
    }

    fun stop() {
        isPlaying = false
        shouldStop.set(true)
        audioThread?.interrupt()
        audioThread = null
        try { sourceDataLine?.close() } catch (_: Exception) {}
        sourceDataLine = null
        songTitle = ""
        currentPositionMs = 0
        durationMs = 0
        currentLyrics = null
        currentYrcLyrics = null
        visualizerData.clear()
        amplitudes.clear()
    }

    fun next() {
        if (playlist.size == 0) return
        currentIndex = (currentIndex + 1) % playlist.size
        startPlayback()
    }

    fun previous() {
        if (playlist.size == 0) return
        currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        startPlayback()
    }

    fun setVolume(vol: Int) {
        volume = vol.coerceIn(0, 100)
    }

    fun getVolume(): Int = volume

    fun setRepeatMode(mode: AudioRepeatMode) {
        repeatMode = mode
    }

    fun getRepeatMode(): AudioRepeatMode = repeatMode

    fun getAmplitudes(): List<Double> = amplitudes.toList()

    fun hasVisualizerData(): Boolean = visualizerData.isNotEmpty()

    fun requestStop(): Boolean = shouldStop.get()

    fun getCurrentLyric(): String {
        val lyrics = currentLyrics ?: return ""
        if (lyrics.isEmpty()) return ""
        val current = currentPositionMs
        for (i in lyrics.indices) {
            if (lyrics[i].timestamp > current) {
                return if (i > 0) lyrics[i - 1].content else ""
            }
        }
        return lyrics.last().content
    }

    fun getLyricProgress(): Float {
        val lyrics = currentLyrics ?: return 0.0f
        if (lyrics.isEmpty()) return 0.0f

        val current = currentPositionMs
        var rawProgress = 0.0f

        for (i in lyrics.indices) {
            if (lyrics[i].timestamp > current) {
                if (i > 0) {
                    val start = lyrics[i - 1].timestamp
                    val end = lyrics[i].timestamp
                    val fillSpan = (end - start - 500L).coerceAtLeast(1)
                    rawProgress = ((current - start).toFloat() / fillSpan).coerceIn(0.0f, 1.0f)
                }
                break
            }
            if (i == lyrics.size - 1) rawProgress = 1.0f
        }

        val currentText = getCurrentLyric()
        if (currentText != lastLyricText) {
            lastLyricText = currentText
            smoothedLyricProgress = 0.0f
        }

        val now = System.currentTimeMillis()
        if (lastProgressUpdateTime == 0L) lastProgressUpdateTime = now
        val deltaMs = minOf(now - lastProgressUpdateTime, 100L)
        lastProgressUpdateTime = now

        val speed = 12.0f
        val alpha = (1.0 - Math.exp(-speed * deltaMs / 1000.0f.toDouble())).toFloat()
        smoothedLyricProgress += (rawProgress - smoothedLyricProgress) * alpha

        if (Math.abs(rawProgress - smoothedLyricProgress) < 0.005f) {
            smoothedLyricProgress = rawProgress
        }

        return smoothedLyricProgress
    }

    fun loadLocalMusicFromDir(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        val supported = listOf("mp3", "wav", "ogg")
        val files = dir.listFiles()?.filter { file ->
            supported.any { file.extension.equals(it, ignoreCase = true) }
        }?.sortedBy { it.name.lowercase() } ?: return

        playlist.clear()
        files.forEach { playlist.addTrack(SongInfo.fromLocalFile(it)) }
        currentIndex = 0
    }

    fun searchAndLoadNetease(keyword: String): List<NeteaseApiSearch.NeteaseTrack> {
        val tracks = NeteaseApiSearch.search(keyword)
        if (tracks.isNotEmpty()) {
            val songs = tracks.map { SongInfo.fromNeteaseTrack(it) }
            playlist.clear()
            playlist.tracks.addAll(songs)
            currentIndex = 0
        }
        return tracks
    }

    fun loadNeteasePlaylist(playlistId: Long) {
        val tracks = NeteaseRequestApi.getPlaylistDetail(playlistId)
        if (tracks.isNotEmpty()) {
            val songs = tracks.map { SongInfo.fromNeteaseTrack(it) }
            playlist.clear()
            playlist.tracks.addAll(songs)
            currentIndex = 0
        }
    }

    fun loadNeteaseHotSongs() = loadNeteasePlaylist(NeteaseConstants.PLAYLIST_HOT_SONGS)
    fun loadNeteaseNewSongs() = loadNeteasePlaylist(NeteaseConstants.PLAYLIST_NEW_SONGS)
    fun loadNeteaseSurge() = loadNeteasePlaylist(NeteaseConstants.PLAYLIST_SURGE)

    fun getPlaylistSize(): Int = playlist.size
    fun getCurrentIndex(): Int = currentIndex

    fun getTrackAt(index: Int): SongInfo? {
        return if (index in 0 until playlist.size) playlist.tracks[index] else null
    }

    fun getCurrentTrack(): SongInfo? = getTrackAt(currentIndex)

    private fun startPlayback() {
        shouldStop.set(true)
        audioThread?.interrupt()

        shouldStop.set(false)
        audioThread = Thread({
            try {
                val track = getCurrentTrack() ?: return@Thread
                songTitle = track.displayTitle
                currentPositionMs = 0
                durationMs = track.durationMs
                currentLyrics = null
                currentYrcLyrics = null
                visualizerData.clear()
                amplitudes.clear()
                lastSetVolume = -1

                if (track.isLocal && track.localFile != null) {
                    playLocalFile(track.localFile)
                } else if (track.isNetease) {
                    playNeteaseSong(track)
                }
            } catch (e: Exception) {
                if (!Thread.currentThread().isInterrupted) {
                    System.err.println("[SigmaMusicManager] Playback error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }, "SigmaMusicPlayback")
        audioThread!!.isDaemon = true
        audioThread!!.start()
    }

    private fun playLocalFile(file: File) {
        val stream = file.inputStream()
        playMp3Stream(stream, file)
    }

    private fun playNeteaseSong(track: SongInfo) {
        val songUrl = NeteaseApiSearch.getSongUrl(track.neteaseSongId)
        val urlToPlay = songUrl ?: NeteaseApiSearch.getSimpleSongUrl(track.neteaseSongId)

        val lrcText = NeteaseApiSearch.getLyrics(track.neteaseSongId)
        if (lrcText != null) {
            if (YrcParser.isYrc(lrcText)) {
                currentYrcLyrics = YrcParser.parse(lrcText)
                currentLyrics = currentYrcLyrics?.map { LrcParser.LyricLine(it.startTime, it.content) }
            } else {
                currentLyrics = LrcParser.parseString(lrcText)
                currentYrcLyrics = null
            }
        }

        val conn = URL(urlToPlay).openConnection()
        conn.connectTimeout = 14000
        conn.readTimeout = 14000
        conn.setRequestProperty("User-Agent", NeteaseConstants.UA_PC_BROWSER)

        val inputStream = conn.getInputStream()

        playMp3Stream(inputStream, null)
    }

    private fun playMp3Stream(inputStream: InputStream, localFile: File?) {
        var localBitstream: Bitstream? = null
        var localLine: SourceDataLine? = null
        try {
            val bitstream = Bitstream(inputStream)
            localBitstream = bitstream
            val mp3Decoder = Decoder()

            if (localFile != null) {
                loadLyricsFromFile(localFile)
            }

            val firstHeader: Header = bitstream.readFrame() ?: return

            val sampleRate = firstHeader.frequency()
            val channels = if (firstHeader.mode() == Header.SINGLE_CHANNEL) 1 else 2

            val mp3Format = AudioFormat(sampleRate.toFloat(), 16, channels, true, false)
            val line = AudioSystem.getSourceDataLine(mp3Format)
            localLine = line
            sourceDataLine = line
            line.open(mp3Format)
            line.start()

            isPlaying = true
            var frameHeader: Header? = firstHeader
            var frameCount = 0
            val msPerFrame = firstHeader.ms_per_frame()
            var pcmBytes: ByteArray? = null
            var framesSinceVisualizer = 0

            while (frameHeader != null && !shouldStop.get() && !Thread.currentThread().isInterrupted) {
                val output = mp3Decoder.decodeFrame(frameHeader, bitstream) ?: break
                val sampleBuf = output as javazoom.jl.decoder.SampleBuffer
                val samples = sampleBuf.getBuffer()
                val len = sampleBuf.getBufferLength()

                if (pcmBytes == null || pcmBytes.size != len * 2) {
                    pcmBytes = ByteArray(len * 2)
                }

                for (i in 0 until len) {
                    pcmBytes!![i * 2] = (samples[i].toInt() and 0xFF).toByte()
                    pcmBytes!![i * 2 + 1] = (samples[i].toInt() shr 8 and 0xFF).toByte()
                }

                line.write(pcmBytes!!, 0, pcmBytes!!.size)

                frameCount++
                currentPositionMs = (frameCount * msPerFrame).toLong()

                if (durationMs <= 0 || localFile != null) {
                    durationMs = (frameCount * msPerFrame).toLong()
                }

                framesSinceVisualizer++
                if (framesSinceVisualizer >= 3) {
                    framesSinceVisualizer = 0
                    processVisualizerData(pcmBytes!!, mp3Format)
                }

                if (volume != lastSetVolume) {
                    lastSetVolume = volume
                    adjustAudioVolume(line, volume)
                }

                bitstream.closeFrame()
                frameHeader = bitstream.readFrame()
            }

            line.drain()
            line.close()
            bitstream.close()
            inputStream.close()
            sourceDataLine = null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            if (!Thread.currentThread().isInterrupted) {
                System.err.println("[SigmaMusicManager] MP3 playback error: ${e.message}")
                e.printStackTrace()
            }
        } finally {
            try { localLine?.close() } catch (_: Exception) {}
            try { localBitstream?.close() } catch (_: Exception) {}
            try { inputStream.close() } catch (_: Exception) {}
            sourceDataLine = null
        }

        if (!shouldStop.get()) {
            if (repeatMode == AudioRepeatMode.LOOP_CURRENT) {
                startPlayback()
            } else if (repeatMode == AudioRepeatMode.REPEAT) {
                next()
            }
        }
    }

    private fun loadLyricsFromFile(audioFile: File) {
        try {
            val name = audioFile.name
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex < 0) {
                currentLyrics = null
                currentYrcLyrics = null
                return
            }
            val lrcName = name.substring(0, dotIndex) + ".lrc"
            val lrcFile = File(audioFile.parent, lrcName)
            if (lrcFile.exists()) {
                currentLyrics = LrcParser.parse(lrcFile)
                currentYrcLyrics = null
            } else {
                currentLyrics = null
                currentYrcLyrics = null
            }
        } catch (e: Exception) {
            currentLyrics = null
            currentYrcLyrics = null
        }
    }

    private fun processVisualizerData(pcmBytes: ByteArray, audioFormat: AudioFormat) {
        try {
            val pcmFloat = AudioMathHelper.convertToPCMFloatArray(pcmBytes, audioFormat)
            val n = pcmFloat.size
            if (n >= 2) {
                var p = 2
                while (p < n) p = p shl 1

                val paddedPcm = FloatArray(p)
                System.arraycopy(pcmFloat, 0, paddedPcm, 0, n)

                val fft = JavaFFT(paddedPcm.size)
                val transformed = fft.transform(paddedPcm)

                visualizerData.add(AudioMathHelper.calculateAmplitudes(transformed[0], transformed[1]))
                if (visualizerData.size > 18) {
                    visualizerData.removeAt(0)
                }

                if (visualizerData.isNotEmpty()) {
                    val latest = visualizerData[0]
                    while (amplitudes.size < latest.size && amplitudes.size < 1024) {
                        amplitudes.add(0.0)
                    }
                    for (i in latest.indices) {
                        if (i >= amplitudes.size) break
                        val target = latest[i]
                        val current = amplitudes[i]
                        val diff = current - target
                        amplitudes[i] = minOf(2.256E7, maxOf(0.0, current - diff * 0.335f.coerceAtMost(1.0f)))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun adjustAudioVolume(line: SourceDataLine, vol: Int) {
        try {
            val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
            val muteControl = line.getControl(BooleanControl.Type.MUTE) as? BooleanControl

            if (vol == 0) {
                muteControl?.value = true
            } else {
                muteControl?.value = false
                gainControl?.value = (Math.log(vol.toDouble() / 100.0) / Math.log(10.0) * 20.0).toFloat()
            }
        } catch (_: Exception) {}
    }
}
