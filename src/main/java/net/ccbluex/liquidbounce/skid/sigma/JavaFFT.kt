package net.ccbluex.liquidbounce.skid.sigma

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class JavaFFT(val numberOfSamples: Int) : Transform {

    private val reverseIndices: IntArray
    private val frequencies: FloatArray

    init {
        if (!isPowerOfTwo(numberOfSamples)) throw IllegalArgumentException("N is not a power of 2")
        if (numberOfSamples <= 0) throw IllegalArgumentException("N must be greater than 0")
        val numberOfBits = getNumberOfNeededBits(numberOfSamples)
        reverseIndices = IntArray(numberOfSamples)
        for (i in 0 until numberOfSamples) {
            reverseIndices[i] = fastReverseBits(i, numberOfBits)
        }
        frequencies = FloatArray(numberOfSamples)
        for (index in 0 until numberOfSamples) {
            frequencies[index] = if (index <= numberOfSamples / 2) {
                index.toFloat() / numberOfSamples
            } else {
                -((numberOfSamples - index).toFloat() / numberOfSamples)
            }
        }
    }

    override fun transform(real: FloatArray): Array<FloatArray> {
        val out = Array(3) { FloatArray(real.size) }
        transform(false, real, null, out[0], out[1])
        out[2] = frequencies.clone()
        return out
    }

    override fun transform(real: FloatArray, imaginary: FloatArray): Array<FloatArray> {
        val out = Array(3) { FloatArray(real.size) }
        transform(false, real, imaginary, out[0], out[1])
        out[2] = frequencies.clone()
        return out
    }

    override fun inverseTransform(real: FloatArray, imaginary: FloatArray): Array<FloatArray> {
        val out = Array(2) { FloatArray(real.size) }
        transform(true, real, imaginary, out[0], out[1])
        return out
    }

    fun transform(
        inverse: Boolean,
        realIn: FloatArray,
        imaginaryIn: FloatArray?,
        realOut: FloatArray,
        imaginaryOut: FloatArray
    ) {
        if (realIn.size != numberOfSamples) {
            throw IllegalArgumentException("Number of samples must be $numberOfSamples for this instance of JavaFFT")
        }

        for (i in 0 until numberOfSamples) {
            realOut[reverseIndices[i]] = realIn[i]
        }
        if (imaginaryIn != null) {
            for (i in 0 until numberOfSamples) {
                imaginaryOut[reverseIndices[i]] = imaginaryIn[i]
            }
        }

        var blockEnd = 1
        val angleNumerator = if (inverse) -2.0 * PI else 2.0 * PI

        var blockSize = 2
        while (blockSize <= numberOfSamples) {
            val deltaAngle = angleNumerator / blockSize
            val sm2 = -sin(-2 * deltaAngle)
            val sm1 = -sin(-deltaAngle)
            val cm2 = cos(-2 * deltaAngle)
            val cm1 = cos(-deltaAngle)
            val w = 2 * cm1

            var i = 0
            while (i < numberOfSamples) {
                var ar2 = cm2
                var ar1 = cm1
                var ai2 = sm2
                var ai1 = sm1

                var j = i
                var n = 0
                while (n < blockEnd) {
                    val ar0 = w * ar1 - ar2
                    ar2 = ar1
                    ar1 = ar0

                    val ai0 = w * ai1 - ai2
                    ai2 = ai1
                    ai1 = ai0

                    val k = j + blockEnd
                    val tr = ar0 * realOut[k] - ai0 * imaginaryOut[k]
                    val ti = ar0 * imaginaryOut[k] + ai0 * realOut[k]

                    realOut[k] = (realOut[j] - tr).toFloat()
                    imaginaryOut[k] = (imaginaryOut[j] - ti).toFloat()

                    realOut[j] += tr.toFloat()
                    imaginaryOut[j] += ti.toFloat()

                    j++
                    n++
                }
                i += blockSize
            }
            blockEnd = blockSize
            blockSize = blockSize shl 1
        }

        if (inverse) {
            for (i in 0 until numberOfSamples) {
                realOut[i] /= numberOfSamples.toFloat()
                imaginaryOut[i] /= numberOfSamples.toFloat()
            }
        }
    }

    companion object {
        private const val MAX_FAST_BITS = 16
        private val FFT_BIT_TABLE = Array(MAX_FAST_BITS) { IntArray(0) }

        init {
            var len = 2
            for (b in 1..MAX_FAST_BITS) {
                FFT_BIT_TABLE[b - 1] = IntArray(len)
                for (i in 0 until len) {
                    FFT_BIT_TABLE[b - 1][i] = reverseBits(i, b)
                }
                len = len shl 1
            }
        }

        private fun getNumberOfNeededBits(powerOfTwo: Int): Int {
            var i = 0
            while (true) {
                if (powerOfTwo and (1 shl i) != 0) return i
                i++
            }
        }

        private fun reverseBits(index: Int, numberOfBits: Int): Int {
            var ind = index
            var rev = 0
            for (i in 0 until numberOfBits) {
                rev = rev shl 1 or (ind and 1)
                ind = ind shr 1
            }
            return rev
        }

        private fun fastReverseBits(index: Int, numberOfBits: Int): Int {
            return if (numberOfBits <= MAX_FAST_BITS) {
                FFT_BIT_TABLE[numberOfBits - 1][index]
            } else {
                reverseBits(index, numberOfBits)
            }
        }

        private fun isPowerOfTwo(number: Int): Boolean = number and (number - 1) == 0
    }
}
