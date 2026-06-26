/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.GL20

/**
 * Aurora / flowing gradient background shader.
 * Uses fbm noise to produce organic, slowly-moving color bands
 * in deep dark + blue/purple/cyan/green tones.
 */
object AuroraShader : Shader("aurora.frag") {

    private var startTime = System.currentTimeMillis()

    override fun setupUniforms() {
        setupUniform("resolution")
        setupUniform("time")
    }

    override fun updateUniforms() {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        GL20.glUniform2f(getUniform("resolution"), mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
        GL20.glUniform1f(getUniform("time"), elapsed)
    }

    /** Reset the time origin (e.g. when the menu is re-opened) */
    fun resetTime() {
        startTime = System.currentTimeMillis()
    }
}
