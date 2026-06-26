/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

import net.ccbluex.liquidbounce.utils.render.ParticleUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

/**
 * Animated dark background: vertical gradient + particle field.
 * Colors follow the LiquidBounce.next-ish dark theme.
 */
object MainMenuBackground {

    private val TOP = Color(15, 15, 20).rgb
    private val BOTTOM = Color(26, 26, 36).rgb

    fun draw(mouseX: Int, mouseY: Int, width: Int, height: Int) {
        RenderUtils.drawGradientRect(0, 0, width, height, TOP, BOTTOM)
        ParticleUtils.drawParticles(mouseX, mouseY)
    }
}
