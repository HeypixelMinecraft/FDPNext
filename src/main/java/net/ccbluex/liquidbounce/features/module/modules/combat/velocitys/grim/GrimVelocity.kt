/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.grim

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S32PacketConfirmTransaction

/**
 * Skidded from edFDP. Cancels S12 + S32 transactions to bypass Grim.
 */
class GrimVelocity : VelocityMode("Grim") {
    private var cancelPacket = 6
    private var resetPersec = 8
    private var grimTCancel = 0
    private var updates = 0

    override fun onEnable() {
        grimTCancel = 0
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            event.cancelEvent()
            grimTCancel = cancelPacket
        }
        if (packet is S32PacketConfirmTransaction && grimTCancel > 0) {
            event.cancelEvent()
            grimTCancel--
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        updates++
        if (resetPersec > 0) {
            if (updates >= 0 || updates >= resetPersec) {
                updates = 0
                if (grimTCancel > 0) {
                    grimTCancel--
                }
            }
        }
    }
}
