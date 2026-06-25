/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory

class NoClickDelay : Module(name = "NoClickDelay", category = ModuleCategory.COMBAT) {

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (mc.thePlayer != null && mc.theWorld != null) {
            mc.leftClickCounter = 0
        }
    }
}