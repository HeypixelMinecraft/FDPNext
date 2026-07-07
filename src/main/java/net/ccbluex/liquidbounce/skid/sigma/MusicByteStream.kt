package net.ccbluex.liquidbounce.skid.sigma

import java.io.ByteArrayOutputStream

class MusicByteStream(val musicStream: MusicStream) : ByteArrayOutputStream() {
    val buffer: ByteArray get() = buf
}
