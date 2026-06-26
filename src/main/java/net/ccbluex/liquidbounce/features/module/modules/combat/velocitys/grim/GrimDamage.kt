/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.grim

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation

/**
 * Skidded from edFDP. When hurtTime == 9, spams attack packets on nearby target
 * and reduces horizontal motion by 0.07776 to dampen knockback.
 */
class GrimDamage : VelocityMode("GrimDamage") {
    override fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        if (player.hurtTime == 9) {
            val target = FDPNext.combatManager.getNearByEntity(3.0f) ?: return
            repeat(12) {
                mc.netHandler.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
                mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
            player.motionX *= 0.07776
            player.motionZ *= 0.07776
        }
    }
}
