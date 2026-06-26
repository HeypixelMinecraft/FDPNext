/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

import net.ccbluex.liquidbounce.utils.render.GLUtils
import net.ccbluex.liquidbounce.utils.render.ParticleUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.AuroraShader
import org.lwjgl.opengl.GL11.*

/**
 * Aurora / flowing gradient background shader + particle overlay.
 */
object MainMenuBackground {

    fun draw(mouseX: Int, mouseY: Int, width: Int, height: Int) {
        // Draw aurora shader over full screen
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        AuroraShader.startShader()
        GLUtils.drawQuads(0f, 0f, width.toFloat(), height.toFloat())
        AuroraShader.stopShader()

        glDisable(GL_BLEND)

        // Particle overlay on top
        ParticleUtils.drawParticles(mouseX, mouseY)
    }
}
