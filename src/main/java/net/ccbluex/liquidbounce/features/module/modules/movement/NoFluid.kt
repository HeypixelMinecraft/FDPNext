/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue

object NoFluid : Module(name = "NoFluid",  category = ModuleCategory.MOVEMENT) {

    val waterValue = BoolValue("Water", true)
    val lavaValue = BoolValue("Lava", true)
}