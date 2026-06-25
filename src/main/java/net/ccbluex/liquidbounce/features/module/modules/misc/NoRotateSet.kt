/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue

class NoRotateSet : Module(name = "NoRotateSet", category = ModuleCategory.MISC) {
    val noLoadingValue = BoolValue("NoLoading", true)
    val overwriteTeleportValue = BoolValue("SilentConfirm", true)
    val rotateValue = BoolValue("SilentConfirmSetRotation", true).displayable { overwriteTeleportValue.get() }
}