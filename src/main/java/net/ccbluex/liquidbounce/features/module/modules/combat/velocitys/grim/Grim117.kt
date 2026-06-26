/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.grim

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.EnumFacing

/**
 * Skidded from edFDP (mode name "Grim1.17").
 * On velocity packet: send 4x C06PosLook + C07 STOP_DESTROY_BLOCK, then cancel.
 */
class Grim117 : VelocityMode("Grim1.17") {
    override fun onVelocityPacket(event: PacketEvent) {
        val player = mc.thePlayer
        repeat(4) {
            mc.netHandler.addToSendQueue(C03PacketPlayer.C06PacketPlayerPosLook(
                player.posX, player.posY, player.posZ,
                player.rotationYaw, player.rotationPitch, player.onGround
            ))
        }
        mc.netHandler.addToSendQueue(C07PacketPlayerDigging(
            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
            player.position, EnumFacing.DOWN
        ))
        event.cancelEvent()
    }
}
