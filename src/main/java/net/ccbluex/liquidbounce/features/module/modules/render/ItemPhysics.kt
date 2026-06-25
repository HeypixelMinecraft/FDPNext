/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.FloatValue

class ItemPhysics : Module(name = "ItemPhysics", category = ModuleCategory.RENDER) {
    val itemWeight = FloatValue("Weight", 0.5F, 0F, 1F)
    override val tag: String?
        get() = "${itemWeight.get()}"
}