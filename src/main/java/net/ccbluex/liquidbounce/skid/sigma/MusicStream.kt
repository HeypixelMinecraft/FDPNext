package net.ccbluex.liquidbounce.skid.sigma

import java.io.InputStream

class MusicStream(inputStream: InputStream, val audioProcessor: AudioProcessor?) : InputStream() {
    val byteStream: MusicByteStream = MusicByteStream(this)
    @Volatile var bufferEnd: Int = 0
    @Volatile var bufferPosition: Int = 0
    @Volatile var endOfStream: Boolean = false
    private var streamingThread: Thread? = null

    init {
        streamingThread = Thread(AudioStreamer(this, inputStream), "AudioStreamer")
        streamingThread!!.isDaemon = true
        streamingThread!!.start()
    }

    override fun available(): Int = bufferEnd - bufferPosition

    override fun read(): Int {
        if (endOfStream && bufferPosition >= bufferEnd) return -1
        while (bufferEnd <= bufferPosition || byteStream.buffer.size <= bufferPosition) {
            if (endOfStream) return -1
        }
        return byteStream.buffer[bufferPosition++].toInt() and 0xFF
    }

    override fun read(data: ByteArray, offset: Int, length: Int): Int {
        while (bufferEnd < bufferPosition + length) {
            if (endOfStream) return -1
        }
        System.arraycopy(byteStream.buffer, bufferPosition, data, offset, length)
        bufferPosition += length
        return length
    }

    override fun reset() {
        bufferPosition = 0
    }

    override fun skip(bytes: Long): Long {
        bufferPosition += bytes.toInt()
        return bytes
    }

    override fun close() {
        byteStream.close()
        streamingThread?.interrupt()
        streamingThread = null
        bufferEnd = 0
        bufferPosition = 0
        endOfStream = false
        super.close()
    }
}
