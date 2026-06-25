/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue

class TrueSight : Module(name = "TrueSight", category = ModuleCategory.RENDER) {
    val barriersValue = BoolValue("Barriers", true)
    val entitiesValue = BoolValue("Entities", true)
}