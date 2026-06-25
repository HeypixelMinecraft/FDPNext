/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.timer

import net.ccbluex.liquidbounce.utils.misc.RandomUtils

object TimeUtils {
    fun randomDelay(minDelay: Int, maxDelay: Int): Long {
        return RandomUtils.nextInt(minDelay, maxDelay).toLong()
    }

    fun randomClickDelay(minCPS: Int, maxCPS: Int): Long {
        return (Math.random() * (1000 / minCPS - 1000 / maxCPS + 1) + 1000 / maxCPS).toLong()
    }
}