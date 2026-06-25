/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import org.lwjgl.input.Keyboard

class KeyBindManager : Module("KeyBindManager", category = ModuleCategory.CLIENT, keyBind = Keyboard.KEY_RMENU, canEnable = false) {
    override fun onEnable() {
        mc.displayGuiScreen(FDPNext.keyBindManager)
    }
}