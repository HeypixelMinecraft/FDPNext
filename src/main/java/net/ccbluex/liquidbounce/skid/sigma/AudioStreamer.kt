package net.ccbluex.liquidbounce.skid.sigma

import java.io.InputStream

class AudioStreamer(val musicStream: MusicStream, private val inputStream: InputStream) : Runnable {
    override fun run() {
        val buffer = ByteArray(16384)
        try {
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1 && !Thread.interrupted()) {
                if (bytesRead > 0) {
                    musicStream.byteStream.write(buffer, 0, bytesRead)
                    musicStream.bufferEnd += bytesRead
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!Thread.interrupted()) {
            musicStream.endOfStream = true
            musicStream.audioProcessor?.processBuffer(
                musicStream.byteStream.buffer, 0, musicStream.bufferEnd
            )
        }
    }
}
