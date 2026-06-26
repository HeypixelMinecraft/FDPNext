/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

/**
 * Card showing GitHub release + community info.
 * Star count requires network access, so we display static repo link + latest tag.
 */
object MainMenuCommunityCard {

    private val cardWidth = 180f
    private val cardHeight = 60f

    private val repoUrl = "https://github.com/HeypixelMinecraft/FDPNext"

    fun draw(x: Float, y: Float) {
        RenderUtils.drawRoundedCornerRect(x, y, x + cardWidth, y + cardHeight, 6f, Color(30, 30, 40, 220).rgb)
        RenderUtils.drawRoundedCornerRect(x, y, x + cardWidth, y + 2f, 6f, Color(120, 80, 160).rgb)

        // Title
        val title = LanguageManager.getAndFormat("ui.mainmenu.community")
        FontLoaders.F16.drawString(title, x + 10, y + 8, Color(156, 163, 175).rgb, false)

        // Latest release tag
        val releaseLabel = LanguageManager.getAndFormat("ui.mainmenu.release", FDPNext.CLIENT_VERSION)
        FontLoaders.F18.drawString(releaseLabel, x + 10, y + 22, Color.WHITE.rgb, false)

        // Repo url (truncated if needed)
        val urlLabel = "GitHub"
        val urlWidth = FontLoaders.F14.getStringWidth(urlLabel)
        RenderUtils.drawRoundedCornerRect(
            x + cardWidth - urlWidth - 18,
            y + cardHeight - 22,
            x + cardWidth - 8,
            y + cardHeight - 8,
            4f,
            Color(255, 255, 255, 20).rgb
        )
        FontLoaders.F14.drawString(
            urlLabel,
            x + cardWidth - urlWidth - 13,
            y + cardHeight - 19,
            Color(120, 180, 255).rgb,
            false
        )
    }

    fun width(): Float = cardWidth
    fun height(): Float = cardHeight
}
