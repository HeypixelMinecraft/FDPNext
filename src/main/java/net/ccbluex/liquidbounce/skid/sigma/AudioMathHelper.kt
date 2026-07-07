package net.ccbluex.liquidbounce.skid.sigma

import javax.sound.sampled.AudioFormat

object AudioMathHelper {

    fun convertToPCMFloatArray(audioBytes: ByteArray, audioFormat: AudioFormat): FloatArray {
        val pcmValues = FloatArray(audioBytes.size / audioFormat.frameSize)
        val frameSize = audioFormat.frameSize

        for (i in audioBytes.indices step frameSize) {
            val sample = if (!audioFormat.isBigEndian) {
                bytesToIntLE(audioBytes, i, frameSize)
            } else {
                bytesToIntBE(audioBytes, i, frameSize)
            }
            pcmValues[i / frameSize] = sample.toFloat() / 32768.0f
        }
        return pcmValues
    }

    fun calculateAmplitudes(realPart: FloatArray, imaginaryPart: FloatArray): DoubleArray {
        val amplitudes = DoubleArray(realPart.size / 2)
        for (i in amplitudes.indices) {
            amplitudes[i] = Math.sqrt(
                (realPart[i] * realPart[i] + imaginaryPart[i] * imaginaryPart[i]).toDouble()
            )
        }
        return amplitudes
    }

    private fun bytesToIntLE(byteArray: ByteArray, startIndex: Int, length: Int): Int {
        var result = 0
        for (i in 0 until length) {
            val currentByte = byteArray[startIndex + i].toInt() and 0xFF
            result += currentByte shl (8 * i)
        }
        return result
    }

    private fun bytesToIntBE(byteArray: ByteArray, startIndex: Int, length: Int): Int {
        var result = 0
        for (i in 0 until length) {
            val currentByte = byteArray[startIndex + i].toInt() and 0xFF
            result += currentByte shl (8 * (length - i - 1))
        }
        return result
    }
}
