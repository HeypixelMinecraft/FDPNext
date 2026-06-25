/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue

class KeepSprint : Module(name = "KeepSprint", category = ModuleCategory.COMBAT) {
    val s = FloatValue("Motion", 0.0F , 0.0F, 1.0F)
    val aws = BoolValue("AlwaysSprint", false)
}
