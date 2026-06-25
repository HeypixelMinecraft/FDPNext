/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue

object Performance : Module("Performance", category = ModuleCategory.CLIENT) {
    @JvmField
    var staticParticleColorValue = BoolValue("StaticParticleColor", false)
    @JvmField
    var fastEntityLightningValue = BoolValue("FastEntityLightning", false)
    @JvmField
    var fastBlockLightningValue = BoolValue("FastBlockLightning", false)
}

