/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class HiveHopSpeed : SpeedMode("HiveHop") {
    override fun onEnable() {
        mc.thePlayer.speedInAir = 0.0425f
        mc.timer.timerSpeed = 1.04f
    }

    override fun onDisable() {
        mc.thePlayer.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }

    override fun onUpdate() {
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.3
            mc.thePlayer.speedInAir = 0.0425f
            mc.timer.timerSpeed = 1.04f
            MovementUtils.strafe()
        } else {
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.motionX = mc.thePlayer.motionZ
            mc.thePlayer.speedInAir = 0.02f
            mc.timer.timerSpeed = 1f
        }
    }
}