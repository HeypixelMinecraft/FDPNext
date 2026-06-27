/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * Liquid glass background, adapted from Jacquesqwq/LiquidGlassShader (V2 Clear).
 * Grab-pass: copy the current framebuffer into a private mipmapped texture, then run the
 * glass fragment shader sampling that copy with refraction/grain/rim-glow.
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.GLUtils
import net.ccbluex.liquidbounce.utils.render.shader.Shader
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer

object LiquidGlassShader : Shader("liquidglass.frag") {

    // Private backdrop copy of the scene (separate from mc.framebuffer to avoid read/write hazard)
    private var backdropTex = -1
    private var texW = 0
    private var texH = 0

    // Per-draw glass params (display pixels, bottom-left origin to match gl_FragCoord)
    private var quadCenterX = 0f
    private var quadCenterY = 0f
    private var quadSizeX = 0f
    private var quadSizeY = 0f
    private var radius = 8f

    fun isAvailable(): Boolean = programId != 0

    override fun setupUniforms() {
        setupUniform("uBlurTex")
        setupUniform("uScreenSize")
        setupUniform("uQuadCenter")
        setupUniform("uQuadSize")
        setupUniform("uRadius")
        setupUniform("uNoise")
        setupUniform("uRefractionPower")
        setupUniform("uGlowWeight")
        setupUniform("uGlowBias")
        setupUniform("uGlowEdge0")
        setupUniform("uGlowEdge1")
    }

    override fun updateUniforms() {
        GL20.glUniform1i(getUniform("uBlurTex"), 0)
        GL20.glUniform2f(getUniform("uScreenSize"), mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
        GL20.glUniform2f(getUniform("uQuadCenter"), quadCenterX, quadCenterY)
        GL20.glUniform2f(getUniform("uQuadSize"), quadSizeX, quadSizeY)
        GL20.glUniform1f(getUniform("uRadius"), radius)
        // Glass look defaults from the reference USAGE guide.
        GL20.glUniform1f(getUniform("uNoise"), 0.03f)
        GL20.glUniform1f(getUniform("uRefractionPower"), 0.75f)
        GL20.glUniform1f(getUniform("uGlowWeight"), 0.3f)
        GL20.glUniform1f(getUniform("uGlowBias"), 0f)
        GL20.glUniform1f(getUniform("uGlowEdge0"), 0.06f)
        GL20.glUniform1f(getUniform("uGlowEdge1"), 0f)
    }

    /**
     * Draw the liquid glass background for an HUD element panel.
     *
     * Called from inside the element's transform (HUD applies `scale` then `translate(renderX,
     * renderY)`), so the quad is drawn in local coords [0,0,w,h] while the uniforms are computed
     * from the resulting absolute on-screen position so `gl_FragCoord` lines up.
     *
     * [roundness] is 0..1: 1 = full pill (corner radius = half the shorter side), 0 = sharp rect.
     */
    fun draw(renderX: Double, renderY: Double, w: Float, h: Float, scale: Float, roundness: Float) {
        if (!isAvailable() || w <= 0f || h <= 0f) return

        val displayW = mc.displayWidth
        val displayH = mc.displayHeight
        if (displayW <= 0 || displayH <= 0) return

        val sr = ScaledResolution(mc)
        val factorX = displayW.toFloat() / sr.scaledWidth
        val factorY = displayH.toFloat() / sr.scaledHeight

        // Absolute scaled-resolution rect = (renderX/Y + local) * scale, then to display px.
        val absCenterX = (renderX.toFloat() + w / 2f) * scale
        val absCenterY = (renderY.toFloat() + h / 2f) * scale
        quadSizeX = w * scale * factorX
        quadSizeY = h * scale * factorY
        radius = minOf(quadSizeX, quadSizeY) * 0.5f * roundness.coerceIn(0f, 1f)
        quadCenterX = absCenterX * factorX
        quadCenterY = displayH - absCenterY * factorY

        val oldTexture = glGetInteger(GL_TEXTURE_BINDING_2D)

        // Grab pass: copy the current framebuffer into the backdrop texture + mipmaps.
        ensureTexture(displayW, displayH)
        glBindTexture(GL_TEXTURE_2D, backdropTex)
        GL11.glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, displayW, displayH)
        GL30.glGenerateMipmap(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, 0)

        // Glass pass: render into the current framebuffer, sampling the backdrop copy.
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glColor4f(1f, 1f, 1f, 1f)

        startShader()
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, backdropTex)
        GLUtils.drawQuads(0f, 0f, w, h)
        stopShader()

        glBindTexture(GL_TEXTURE_2D, oldTexture)
        glDisable(GL_BLEND)
    }

    private fun ensureTexture(w: Int, h: Int) {
        if (backdropTex == -1) {
            backdropTex = glGenTextures()
            texW = 0
            texH = 0
        }
        if (texW != w || texH != h) {
            glBindTexture(GL_TEXTURE_2D, backdropTex)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, null as ByteBuffer?)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
            glBindTexture(GL_TEXTURE_2D, 0)
            texW = w
            texH = h
        }
    }
}
