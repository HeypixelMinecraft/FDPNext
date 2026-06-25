/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.special

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object AutoDisable : Listenable {
    private const val name = "AutoDisable"

    @EventTarget
    fun onWorld(event: WorldEvent) {
        FDPNext.moduleManager.modules
            .filter { it.state && it.autoDisable == Module.EnumAutoDisableType.RESPAWN && it.triggerType == Module.EnumTriggerType.TOGGLE }
            .forEach { module ->
                module.state = false
                FDPNext.hud.addNotification(Notification(this.name, "Disabled ${module.name} due world Changed.", NotifyType.WARNING, 2000))
            }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            FDPNext.moduleManager.modules
                .filter { it.state && it.autoDisable == Module.EnumAutoDisableType.FLAG && it.triggerType == Module.EnumTriggerType.TOGGLE }
                .forEach { module ->
                    module.state = false
                    FDPNext.hud.addNotification(Notification(this.name, "Disabled ${module.name} due flags.", NotifyType.WARNING, 2000))
                }
        }
    }

    fun handleGameEnd() {
        FDPNext.moduleManager.modules
            .filter { it.state && it.autoDisable == Module.EnumAutoDisableType.GAME_END }
            .forEach { module ->
                module.state = false
                FDPNext.hud.addNotification(Notification(this.name, "Disabled ${module.name} due to game end.", NotifyType.WARNING, 2000))
            }
    }

    override fun handleEvents() = true
}
