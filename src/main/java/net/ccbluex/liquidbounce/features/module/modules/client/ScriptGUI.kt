/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.gui.modernui.GuiScriptLoadMenu

class ScriptGUI : Module("ScriptGUI", category = ModuleCategory.CLIENT, canEnable = false) {
    override fun onEnable() {
        mc.displayGuiScreen(GuiScriptLoadMenu())
    }
}
