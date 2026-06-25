/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.misc

import java.util.*

object RandomUtils {

    fun nextBoolean(): Boolean {
        return Random().nextBoolean()
    }

    @JvmStatic
    fun nextInt(startInclusive: Int = 0, endExclusive: Int = Int.MAX_VALUE) =
        if (endExclusive - startInclusive <= 0) startInclusive else startInclusive + Random().nextInt(endExclusive - startInclusive)

    fun nextDouble(startInclusive: Double = 0.0, endInclusive: Double = 1.0) =
        if (startInclusive == endInclusive || endInclusive - startInclusive <= 0.0) startInclusive else startInclusive + (endInclusive - startInclusive) * Math.random()

    fun nextFloat(startInclusive: Float = 0f, endInclusive: Float = 1f) =
        if (startInclusive == endInclusive || endInclusive - startInclusive <= 0f) startInclusive else (startInclusive + (endInclusive - startInclusive) * Math.random()).toFloat()

    fun randomNumber(length: Int) = random(length, "123456789")

    fun randomString(length: Int) = random(length, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")

    fun random(length: Int, chars: String)= random(length, chars.toCharArray())

    fun random(length: Int, chars: CharArray): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until length) stringBuilder.append(chars[Random().nextInt(chars.size)])
        return stringBuilder.toString()
    }
}