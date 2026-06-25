/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.WorldEvent

class MemoryFix : Module(name = "MemoryFix",  category = ModuleCategory.MISC) {
    override fun onEnable() {
        Runtime.getRuntime().gc()
    }
    
    @EventTarget
    fun onWorld(event: WorldEvent) {
        Runtime.getRuntime().gc()
    }
}
