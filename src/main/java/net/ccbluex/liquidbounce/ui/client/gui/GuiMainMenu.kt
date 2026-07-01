/*
 * FDPNext Hacked Client
 * A Super Skid Minecraft Client based on FDP v5.3.5.
 * https://github.com/UnlegitMC/FDPNext/
 */
package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.SigmaGuiMainMenu
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiYesNoCallback


class GuiMainMenu : GuiScreen(), GuiYesNoCallback {
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        mc.displayGuiScreen(SigmaGuiMainMenu())
    }

}
