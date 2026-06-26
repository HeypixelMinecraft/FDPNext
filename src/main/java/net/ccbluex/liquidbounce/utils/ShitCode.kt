/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils

/**
 * Simple shared attack-flag holder, skidded from edFDP.
 * Used by GrimVerticalVelocity2 to communicate attack state across components.
 */
class ShitCode {
    @Volatile
    var attack: Boolean = false
        private set

    fun setAttack(b: Boolean) {
        attack = b
    }

    fun getAttack(): Boolean = attack
}
