/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.client.gui.inventory.GuiInventory

class NoInvClose : Module(name = "NoInvClose", category = ModuleCategory.MISC) {
    @EventTarget
    fun onPacket(event: PacketEvent){
        if (mc.theWorld == null || mc.thePlayer == null) return
        
        if (event.packet is S2EPacketCloseWindow && mc.currentScreen is GuiInventory) event.cancelEvent()
    }
}