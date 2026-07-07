package net.ccbluex.liquidbounce.skid.sigma

interface Transform {
    fun transform(real: FloatArray): Array<FloatArray>
    fun transform(real: FloatArray, imaginary: FloatArray): Array<FloatArray>
    fun inverseTransform(real: FloatArray, imaginary: FloatArray): Array<FloatArray>
}
