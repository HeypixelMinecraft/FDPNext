/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flys.vulcan

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer

class VulcanFlight : FlyMode("Vulcan") {

    private var ticks = 0

    override fun onEnable() {
        ticks = 0
        mc.netHandler.addToSendQueue(C03PacketPlayer.C06PacketPlayerPosLook(
            mc.thePlayer.posX,
            mc.thePlayer.posY - 2,
            mc.thePlayer.posZ,
            mc.thePlayer.rotationYaw,
            mc.thePlayer.rotationPitch,
            false
        ))
    }

    override fun onDisable() {
        MovementUtils.resetMotion(true)
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val speed = 1f

        mc.thePlayer.motionY = -1E-10 +
            (if (mc.gameSettings.keyBindJump.isKeyDown) speed.toDouble() else 0.0) -
            (if (mc.gameSettings.keyBindSneak.isKeyDown) speed.toDouble() else 0.0)

        if (mc.thePlayer.getDistance(
                mc.thePlayer.lastReportedPosX,
                mc.thePlayer.lastReportedPosY,
                mc.thePlayer.lastReportedPosZ
            ) <= 10 - speed - 0.15
        ) {
            event.cancelEvent()
        } else {
            ticks++
            if (ticks >= 8) {
                MovementUtils.resetMotion(true)
                fly.state = false
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val speed = 1f
        MovementUtils.strafe(speed)
        event.zeroY()
    }
}