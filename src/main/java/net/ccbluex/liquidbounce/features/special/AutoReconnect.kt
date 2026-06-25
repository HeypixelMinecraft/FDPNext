/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.special

object AutoReconnect {
    const val MAX = 60000
    const val MIN = 1000

    var isEnabled = true
    var delay = 5000
        set(value) {
            isEnabled = value < MAX

            field = value
        }
}