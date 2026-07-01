/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu

import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule
import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components.MainMenuBackground
import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components.MainMenuContentPanel
import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components.MainMenuHeader
import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components.MainMenuSidebar
import net.ccbluex.liquidbounce.utils.render.shader.shaders.AuroraShader
import net.minecraft.client.gui.GuiLanguage
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.client.gui.GuiYesNoCallback
import net.minecraftforge.fml.client.GuiModList
import java.io.IOException

/**
 * Modern dark-themed main menu with sidebar navigation, version info,
 * account/community cards and an animated particle background.
 *
 * Layout:
 *   +-------------------------------------------------+
 *   | FDPNext                       v0.1.0-beta@hash   |
 *   | ------                                          |
 *   | [SinglePlayer]    +-----------+ +-----------+   |
 *   | [MultiPlayer ]    | Account   | | Community |   |
 *   | [AltManager ]    +-----------+ +-----------+   |
 *   | [Mods       ]                                    |
 *   | [Options    ]   (animated particle background)   |
 *   | [Languages  ]                                    |
 *   | [Quit       ]                                    |
 *   +-------------------------------------------------+
 */
class ModernGuiMainMenu : GuiScreen(), GuiYesNoCallback {

    private lateinit var sidebar: MainMenuSidebar

    override fun initGui() {
        AuroraShader.resetTime()
        val buttons = listOf(
            MainMenuButton("singleplayer", ">", "ui.mainmenu.singleplayer") {
                mc.displayGuiScreen(GuiSelectWorld(this))
            },
            MainMenuButton("multiplayer", ">", "ui.mainmenu.multiplayer") {
                mc.displayGuiScreen(GuiMultiplayer(this))
            },
            MainMenuButton("altmanager", ">", "ui.mainmenu.altmanager") {
                mc.displayGuiScreen(GuiAltManager(this))
            },
            MainMenuButton("mods", ">", "ui.mainmenu.mods") {
                mc.displayGuiScreen(GuiModList(this))
            },
            MainMenuButton("options", ">", "ui.mainmenu.options") {
                mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            },
            MainMenuButton("languages", ">", "ui.mainmenu.languages") {
                mc.displayGuiScreen(GuiLanguage(this, mc.gameSettings, mc.languageManager))
            },
            MainMenuButton("quit", ">", "ui.mainmenu.quit") {
                mc.shutdown()
            }
        )
        sidebar = MainMenuSidebar(buttons, { accentColor() }, { /* selection callback */ })
        super.initGui()
    }

    private fun accentColor(): Int =
        try { ClickGUIModule.generateColor().rgb } catch (_: Throwable) { 0xFF6B9D.toInt() }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        MainMenuBackground.draw(mouseX, mouseY, width, height)
        MainMenuHeader.draw(width)

        sidebar.draw(mouseX, mouseY)

        // Content panel to the right of the sidebar
        val contentX = 12f + 100f + 24f // sidebar x + width + gap
        val contentY = 60f
        MainMenuContentPanel.draw(contentX, contentY)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        sidebar.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }
}