package net.ccbluex.liquidbounce.ui.client.gui.music

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Sigma 风格音乐播放器文本辅助
 * 使用 Java AWT + Helvetica 字体渲染到纹理（1:1 还原 Sigma）
 */
object SigmaMusicTextHelper {

    private data class CachedText(
        val texture: ResourceLocation,
        val width: Int,
        val height: Int,
        val dynamicTexture: DynamicTexture
    )

    private val cache = mutableMapOf<String, CachedText>()
    private val lock = Any()

    private const val MAX_CACHE_SIZE = 128

    /**
     * 渲染文本到纹理并绘制到屏幕
     */
    fun drawString(text: String, x: Float, y: Float, color: Int, font: Font, fontSize: Float) {
        if (text.isEmpty()) return
        val key = "${text}_${color}_${font.name}_${fontSize}"
        val cached = synchronized(lock) { cache[key] }

        val ct = cached ?: createTextTexture(text, color, font, fontSize, key)

        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.color(1f, 1f, 1f, 1f)
        Minecraft.getMinecraft().textureManager.bindTexture(ct.texture)
        // 缩放回 GL 像素
        val drawW = ct.width / 2f
        val drawH = ct.height / 2f
        GuiMusicPlayer.drawModalRect(x, y, drawW, drawH)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun createTextTexture(text: String, color: Int, font: Font, fontSize: Float, key: String): CachedText {
        // 使用 2x 分辨率以保证清晰度
        val renderFont = font.deriveFont(fontSize * 2f)
        val tmpImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val tmpG = tmpImg.createGraphics()
        tmpG.font = renderFont
        tmpG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        tmpG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val frc = tmpG.fontMetrics
        val strW = frc.stringWidth(text)
        val strH = frc.height
        tmpG.dispose()

        val pad = 4
        val imgW = (strW + pad * 2).coerceAtLeast(8)
        val imgH = (strH + pad * 2).coerceAtLeast(8)

        val img = BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.font = renderFont
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

        val a = (color shr 24) and 0xFF
        val r = (color shr 16) and 0xFF
        val gg = (color shr 8) and 0xFF
        val b = color and 0xFF
        g.color = java.awt.Color(r, gg, b, a)
        g.drawString(text, pad.toFloat(), (frc.ascent + pad).toFloat())
        g.dispose()

        val dyn = DynamicTexture(img)
        val location = ResourceLocation("fdpnext", "musictext_${key.hashCode()}")

        try {
            Minecraft.getMinecraft().textureManager.loadTexture(location, dyn)
        } catch (_: Exception) {}

        val ct = CachedText(location, imgW, imgH, dyn)
        synchronized(lock) {
            if (cache.size >= MAX_CACHE_SIZE) {
                val firstKey = cache.keys.first()
                val removed = cache.remove(firstKey)
                removed?.let {
                    try {
                        Minecraft.getMinecraft().textureManager.deleteTexture(it.texture)
                    } catch (_: Exception) {}
                }
            }
            cache[key] = ct
        }
        return ct
    }

    /**
     * 获取文本像素宽度（不渲染）
     */
    fun getStringWidth(text: String, font: Font, fontSize: Float): Int {
        if (text.isEmpty()) return 0
        val renderFont = font.deriveFont(fontSize)
        val tmpImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = tmpImg.createGraphics()
        g.font = renderFont
        val w = g.fontMetrics.stringWidth(text)
        g.dispose()
        return w
    }

    /**
     * 清空缓存
     */
    fun clearCache() {
        synchronized(lock) {
            cache.values.forEach { ct ->
                try {
                    Minecraft.getMinecraft().textureManager.deleteTexture(ct.texture)
                } catch (_: Exception) {}
            }
            cache.clear()
        }
    }

    /**
     * 简单的字符串宽度估算（用于布局）
     */
    fun getStringWidthApprox(text: String, fontSize: Float): Int {
        return (text.length * fontSize * 0.55f).toInt()
    }
}
