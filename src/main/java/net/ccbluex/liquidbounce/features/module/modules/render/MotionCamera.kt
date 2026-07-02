/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.FloatValue

class MotionCamera : Module(name = "MotionCamera", category = ModuleCategory.RENDER) {
    val interpolation = FloatValue("Interpolation", 0.5f, 0.05f, 1f)
}
