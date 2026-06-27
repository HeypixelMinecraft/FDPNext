/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.font.FontLoaders
import java.awt.Color

/**
 * Top-left logo + top-right version string.
 */
object MainMenuHeader {

    fun draw(width: Int) {
        // Logo (top-left)
        FontLoaders.F40.drawString(
            FDPNext.CLIENT_NAME,
            18f,
            18f,
            Color.WHITE.rgb,
            false
        )

        // Website link (top-right)
        val website = "https://${FDPNext.CLIENT_WEBSITE}/"
        val wWidth = FontLoaders.F16.getStringWidth(website)
        FontLoaders.F16.drawString(
            website,
            (width - wWidth - 12).toFloat(),
            18f,
            Color(156, 163, 175).rgb,
            false
        )

        // Version + commit (top-right, under the website link — kept clear of the sidebar)
        val versionLine = "${FDPNext.CLIENT_VERSION}@${FDPNext.CLIENT_COMMIT}"
        val vWidth = FontLoaders.F16.getStringWidth(versionLine)
        FontLoaders.F16.drawString(
            versionLine,
            (width - vWidth - 12).toFloat(),
            18f + 16f,
            Color(156, 163, 175).rgb,
            false
        )
    }
}
