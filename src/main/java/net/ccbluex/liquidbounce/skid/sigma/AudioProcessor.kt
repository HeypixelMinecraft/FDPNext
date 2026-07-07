package net.ccbluex.liquidbounce.skid.sigma

interface AudioProcessor {
    fun processBuffer(data: ByteArray, offset: Int, length: Int)
}
