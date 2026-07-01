/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components.MainMenuBackground
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.opengl.GL11.*

class ModernGuiMainMenu : GuiScreen() {

    private val buttons = mutableListOf<MainMenuButton>()

    init {
        buttons.addAll(listOf(
            MainMenuButton("singleplayer", "S", "ui.mainmenu.singleplayer") {
                mc.displayGuiScreen(GuiMainMenu())
            },
            MainMenuButton("multiplayer", "M", "ui.mainmenu.multiplayer") {
                mc.displayGuiScreen(net.minecraft.client.gui.GuiMultiplayer(this))
            },
            MainMenuButton("options", "O", "ui.mainmenu.options") {
                mc.displayGuiScreen(net.minecraft.client.gui.GuiOptions(this, mc.gameSettings))
            },
            MainMenuButton("quit", "Q", "ui.mainmenu.quit") {
                mc.shutdown()
            }
        ))
    }

    override fun initGui() {
        val buttonWidth = 200
        val buttonHeight = 20
        val startY = height / 2 - 50

        buttonList.clear()
        buttons.forEachIndexed { index, btn ->
            buttonList.add(GuiButton(
                index,
                width / 2 - buttonWidth / 2,
                startY + index * 25,
                buttonWidth,
                buttonHeight,
                LanguageManager.get(btn.i18nKey)
            ))
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw background
        MainMenuBackground.draw(mouseX, mouseY, width, height)

        // Draw title
        val title = "FDPNext"
        val titleWidth = mc.fontRendererObj.getStringWidth(title)
        mc.fontRendererObj.drawStringWithShadow(
            title,
            (width - titleWidth) / 2f,
            height / 4f,
            0xFFFFFF
        )

        // Draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        if (button.id < buttons.size) {
            buttons[button.id].action.invoke()
        }
    }
}