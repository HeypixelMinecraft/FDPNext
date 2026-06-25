/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.timer

class MSTimer {
    var time = -1L

    fun hasTimePassed(MS: Long): Boolean {
        return System.currentTimeMillis() >= time + MS
    }

    fun hasTimeLeft(MS: Long): Long {
        return MS + time - System.currentTimeMillis()
    }

    fun timePassed(): Long {
        return System.currentTimeMillis() - time
    }

    fun reset() {
        time = System.currentTimeMillis()
    }
}