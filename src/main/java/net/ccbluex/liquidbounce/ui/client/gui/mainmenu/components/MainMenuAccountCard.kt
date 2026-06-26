/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import java.awt.Color

/**
 * Card showing the currently logged-in account and its type.
 */
object MainMenuAccountCard {

    private val cardWidth = 180f
    private val cardHeight = 60f

    fun draw(x: Float, y: Float) {
        RenderUtils.drawRoundedCornerRect(x, y, x + cardWidth, y + cardHeight, 6f, Color(30, 30, 40, 220).rgb)
        RenderUtils.drawRoundedCornerRect(x, y, x + cardWidth, y + 2f, 6f, Color(80, 80, 100).rgb)

        val mc = Minecraft.getMinecraft()
        val username = mc.session.username
        val isPremium = mc.session.token.length >= 32
        val typeKey = if (isPremium) "ui.mainmenu.account.premium" else "ui.mainmenu.account.cracked"
        val typeStr = LanguageManager.getAndFormat(typeKey)

        // Title
        val title = LanguageManager.getAndFormat("ui.mainmenu.account")
        FontLoaders.F16.drawString(title, x + 10, y + 8, Color(156, 163, 175).rgb, false)
        // Username
        FontLoaders.F20.drawString(username, x + 10, y + 24, Color.WHITE.rgb, false)
        // Type badge
        val typeColor = if (isPremium) Color(80, 200, 120).rgb else Color(200, 150, 80).rgb
        val typeWidth = FontLoaders.F14.getStringWidth(typeStr)
        RenderUtils.drawRoundedCornerRect(
            x + cardWidth - typeWidth - 18,
            y + cardHeight - 22,
            x + cardWidth - 8,
            y + cardHeight - 8,
            4f,
            Color(255, 255, 255, 20).rgb
        )
        FontLoaders.F14.drawString(
            typeStr,
            x + cardWidth - typeWidth - 13,
            y + cardHeight - 19,
            typeColor,
            false
        )
    }

    fun width(): Float = cardWidth
    fun height(): Float = cardHeight
}
