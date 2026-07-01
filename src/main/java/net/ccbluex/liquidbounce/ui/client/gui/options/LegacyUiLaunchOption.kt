package net.ccbluex.liquidbounce.ui.client.gui.options

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.ui.client.gui.EnumLaunchFilter
import net.ccbluex.liquidbounce.ui.client.gui.LaunchFilterInfo
import net.ccbluex.liquidbounce.ui.client.gui.LaunchOption
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule
import net.ccbluex.liquidbounce.ui.client.gui.ClickGuiConfig
import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.ModernGuiMainMenu
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui
import java.io.File

@LaunchFilterInfo([EnumLaunchFilter.MODERN_UI])
object modernuiLaunchOption : LaunchOption() {

    @JvmStatic
    lateinit var clickGui: ClickGui

    @JvmStatic
    lateinit var clickGuiConfig: ClickGuiConfig

    override fun start() {
        FDPNext.mainMenu = ModernGuiMainMenu()
        FDPNext.moduleManager.registerModule(ClickGUIModule)

        clickGui = ClickGui()
        clickGuiConfig = ClickGuiConfig(
            File(
                FDPNext.fileManager.dir,
                "clickgui.json"
            )
        )
        FDPNext.fileManager.loadConfig(clickGuiConfig)
    }

    override fun stop() {
        FDPNext.fileManager.saveConfig(clickGuiConfig)
    }
}