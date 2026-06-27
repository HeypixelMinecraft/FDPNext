/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GLContext

/**
 * Liquid Glass shader implementation for FDPNext
 *
 * Based on Jacquesqwq/LiquidGlassShader V2 Tinted + V3 mipmap blur.
 * Renders a glass material with refraction, chromatic dispersion, fresnel,
 * tint and mipmap-based blur on a rectangular quad.
 *
 * Requires OpenGL 3.0+ (for glGenerateMipmap) and GL_ARB_shader_texture_lod.
 * Falls back to direct sampling (no blur) if GL30 is unavailable.
 */
object LiquidGlass : MinecraftInstance() {

    private val liquidGlassShader = ShaderUtil("shaders/liquidGlass.frag", "shaders/vertex.vsh")
    private val mipmapCopyShader = ShaderUtil("shaders/mipmapCopy.frag", "shaders/vertex.vsh")

    private var blurFramebuffer: Framebuffer? = null
    private var hasGL30: Boolean = false
    private var hasTextureLod: Boolean = false
    private var initialized = false

    private fun initCapabilities() {
        if (initialized) return
        val caps = GLContext.getCapabilities()
        hasGL30 = caps.OpenGL30
        hasTextureLod = caps.GL_ARB_shader_texture_lod
        initialized = true
    }

    private fun ensureFramebuffer() {
        // Use scaled resolution to match HUD rendering coordinate space.
        // gl_FragCoord is in scaled coords, so uScreenSize must also be scaled.
        val sr = net.minecraft.client.gui.ScaledResolution(mc)
        val width = sr.scaledWidth
        val height = sr.scaledHeight
        val fb = blurFramebuffer
        if (fb == null || fb.framebufferWidth != width || fb.framebufferHeight != height) {
            fb?.deleteFramebuffer()
            val newFb = Framebuffer(width, height, false)
            newFb.setFramebufferFilter(GL11.GL_LINEAR)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, newFb.framebufferTexture)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
            blurFramebuffer = newFb
        }
    }

    /**
     * Copies the main framebuffer into the blur framebuffer and generates
     * a mipmap chain so the glass shader can sample blurred texels.
     * Call this once per frame before any draw() calls.
     */
    fun updateBlurTexture() {
        initCapabilities()
        if (!hasGL30 || !hasTextureLod) return

        ensureFramebuffer()
        val fb = blurFramebuffer ?: return

        fb.framebufferClear()
        fb.bindFramebuffer(false)
        GlStateManager.disableBlend()

        mipmapCopyShader.init()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.framebuffer.framebufferTexture)
        mipmapCopyShader.setUniformi("textureIn", 0)
        ShaderUtil.drawQuads()
        mipmapCopyShader.unload()

        fb.unbindFramebuffer()
        GlStateManager.enableBlend()

        // Generate mipmap chain on the blur texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fb.framebufferTexture)
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)

        mc.framebuffer.bindFramebuffer(true)
    }

    /**
     * Draw liquid glass effect on a rectangular quad.
     *
     * The quad is drawn at (x, y) with the given size in the current GL
     * coordinate system (affected by modelview matrix). The shader uses
     * gl_FragCoord for screen-space sampling, so it works correctly even
     * with GL translations applied.
     *
     * @param x quad X in current GL coordinate system
     * @param y quad Y in current GL coordinate system
     * @param width quad width
     * @param height quad height
     * @param powerFactor superellipse power (higher = rounder corners). 4.0 = rounded rect
     * @param noise glass grain noise amount
     * @param refractionPower refraction strength
     * @param tintR tint color red (0-1)
     * @param tintG tint color green (0-1)
     * @param tintB tint color blue (0-1)
     * @param tintStrength tint blend strength
     * @param chromaStrength chromatic aberration strength
     * @param darkness darkness factor (0 = bright, 1 = dark)
     * @param blurRadius mipmap LOD level for blur (0 = no blur)
     * @param globalAlpha overall alpha multiplier
     */
    fun draw(
        x: Float, y: Float, width: Float, height: Float,
        powerFactor: Float = 4.0f,
        noise: Float = 0.03f,
        refractionPower: Float = 0.75f,
        tintR: Float = 0.82f,
        tintG: Float = 0.88f,
        tintB: Float = 1.0f,
        tintStrength: Float = 0.12f,
        chromaStrength: Float = 0.001f,
        darkness: Float = 0.0f,
        blurRadius: Float = 2.0f,
        globalAlpha: Float = 1.0f
    ) {
        initCapabilities()

        // Use blur framebuffer texture if GL30 is available, otherwise fall back to main framebuffer
        val sourceTexture = if (hasGL30 && hasTextureLod) {
            blurFramebuffer?.framebufferTexture ?: mc.framebuffer.framebufferTexture
        } else {
            mc.framebuffer.framebufferTexture
        }

        liquidGlassShader.init()

        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GlStateManager.bindTexture(sourceTexture)

        liquidGlassShader.setUniformi("uBlurTex", 0)
        liquidGlassShader.setUniformf("uScreenSize", mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
        liquidGlassShader.setUniformf("uPowerFactor", powerFactor)
        liquidGlassShader.setUniformf("uNoise", noise)
        liquidGlassShader.setUniformf("uRefractionPower", refractionPower)
        liquidGlassShader.setUniformf("uTintColor", tintR, tintG, tintB)
        liquidGlassShader.setUniformf("uTintStrength", tintStrength)
        liquidGlassShader.setUniformf("uChromaStrength", chromaStrength)
        liquidGlassShader.setUniformf("uDarkness", darkness)
        liquidGlassShader.setUniformf("uBlurRadius", if (hasGL30 && hasTextureLod) blurRadius else 0.0f)
        liquidGlassShader.setUniformf("uGlobalAlpha", globalAlpha)

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

        // Draw quad in current GL coordinate system
        ShaderUtil.drawQuads(x, y, width, height)

        liquidGlassShader.unload()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.bindTexture(0)
    }
}
