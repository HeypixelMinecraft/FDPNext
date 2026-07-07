package net.ccbluex.liquidbounce.ui.client.gui.music

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.LinkedHashMap

object MusicPlayerTextHelper {

    private const val MAX_CACHE_SIZE = 256

    private val textCache = object : LinkedHashMap<String, CachedText>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedText>): Boolean {
            if (size > MAX_CACHE_SIZE) {
                eldest.value.release()
                return true
            }
            return false
        }
    }

    private var cjkFont14: Font? = null
    private var cjkFont20: Font? = null
    private var cjkFont28: Font? = null

    private fun getCjkFont14(): Font {
        if (cjkFont14 == null) cjkFont14 = pickCjkFont(14)
        return cjkFont14!!
    }

    private fun getCjkFont20(): Font {
        if (cjkFont20 == null) cjkFont20 = pickCjkFont(20)
        return cjkFont20!!
    }

    private fun getCjkFont28(): Font {
        if (cjkFont28 == null) cjkFont28 = pickCjkFont(28)
        return cjkFont28!!
    }

    fun drawText(x: Float, y: Float, text: String, color: Int, fontSize: Int = 14) {
        if (text.isEmpty()) return
        val font = when {
            fontSize >= 24 -> getCjkFont28()
            fontSize >= 18 -> getCjkFont20()
            else -> getCjkFont14()
        }
        val cacheKey = "${text}|${fontSize}"
        var cached = textCache[cacheKey]
        if (cached == null) {
            cached = renderToTexture(font, text)
            if (cached != null) {
                textCache[cacheKey] = cached
            } else return
        }

        val alpha = (color shr 24 and 0xFF) / 255.0f
        val r = (color shr 16 and 0xFF) / 255.0f
        val g = (color shr 8 and 0xFF) / 255.0f
        val b = (color and 0xFF) / 255.0f

        GlStateManager.enableBlend()
        GlStateManager.disableDepth()
        GlStateManager.color(r, g, b, alpha)
        Minecraft.getMinecraft().textureManager.bindTexture(cached.textureLocation)
        GuiMusicPlayer.drawModalRect(x, y, cached.width.toFloat(), cached.height.toFloat())
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    fun getStringWidth(text: String, fontSize: Int = 14): Int {
        if (text.isEmpty()) return 0
        val font = when {
            fontSize >= 24 -> getCjkFont28()
            fontSize >= 18 -> getCjkFont20()
            else -> getCjkFont14()
        }
        val measureImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = measureImg.createGraphics()
        g.font = font
        val fm = g.fontMetrics
        val w = fm.stringWidth(text)
        g.dispose()
        return w
    }

    fun getStringHeight(fontSize: Int = 14): Int {
        val font = when {
            fontSize >= 24 -> getCjkFont28()
            fontSize >= 18 -> getCjkFont20()
            else -> getCjkFont14()
        }
        val measureImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = measureImg.createGraphics()
        g.font = font
        val fm = g.fontMetrics
        val h = fm.height
        g.dispose()
        return h
    }

    private fun renderToTexture(font: Font, text: String): CachedText? {
        try {
            val measureImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            val gMeasure = measureImg.createGraphics()
            gMeasure.font = font
            val fm: FontMetrics = gMeasure.fontMetrics
            val textWidth = fm.stringWidth(text).coerceAtLeast(1)
            val textHeight = fm.height.coerceAtLeast(1)
            gMeasure.dispose()

            val imgW = textWidth + 2
            val imgH = textHeight + 2

            val image = BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB)
            val g2d = image.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.font = font
            g2d.color = Color.WHITE
            g2d.drawString(text, 1, fm.ascent + 1)
            g2d.dispose()

            val texture = DynamicTexture(image)
            val location = ResourceLocation("fdpnext", "music_text_${text.hashCode()}")
            Minecraft.getMinecraft().textureManager.loadTexture(location, texture)

            return CachedText(location, imgW, imgH)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun pickCjkFont(size: Int): Font {
        val candidates = arrayOf("HarmonyOS Sans SC", "Microsoft YaHei", "SimHei", "NSimSun", "Dialog")
        val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
        val available = ge.availableFontFamilyNames

        for (name in candidates) {
            for (avail in available) {
                if (avail.equals(name, ignoreCase = true)) {
                    return Font(name, Font.PLAIN, size)
                }
            }
        }
        return Font(Font.SANS_SERIF, Font.PLAIN, size)
    }

    fun flushCache() {
        textCache.values.forEach { it.release() }
        textCache.clear()
    }

    private class CachedText(
        val textureLocation: ResourceLocation,
        val width: Int,
        val height: Int
    ) {
        fun release() {
            try {
                Minecraft.getMinecraft().textureManager.deleteTexture(textureLocation)
            } catch (_: Exception) {}
        }
    }
}
