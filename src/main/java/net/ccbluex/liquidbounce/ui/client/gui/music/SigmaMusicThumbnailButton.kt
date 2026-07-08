package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.skid.sigma.SongInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sigma 风格缩略图按钮 - 183x220 网格按钮
 * 紫色渐变背景 + 中心音乐图标 + 底部标题
 */
class SigmaMusicThumbnailButton(
    val track: SongInfo,
    var x: Float,
    var y: Float,
    val width: Float = 183f,
    val height: Float = 220f,
    private val onClick: () -> Unit
) {
    var hovered = false

    private val coverKey = "${track.neteaseSongId}_${track.title.hashCode()}"
    private var coverLoading = false
    private val hasPendingCover = AtomicBoolean(false)

    fun draw(partialTicks: Float) {
        // 紫色渐变背景（Sigma 默认）
        drawGradientBackground()

        // 绘制封面
        val coverTexture = CoverTextureManager.get(coverKey)
        if (coverTexture != null) {
            GlStateManager.enableBlend()
            Minecraft.getMinecraft().textureManager.bindTexture(coverTexture)
            GuiMusicPlayer.drawModalRect(x + 4, y + 4, 183 - 8f, 183 - 8f)
            GlStateManager.disableBlend()
        } else {
            // 默认紫色占位 + 中心音乐图标
            RenderUtils.drawRoundedRect(x + 4, y + 4, x + width - 4, y + 4 + (width - 8), 8f, 0xCC2D1B3D.toInt())
            // 中心音乐图标（白色音符）
            drawMusicIcon(x + width / 2, y + 4 + (width - 8) / 2, (width - 8) * 0.3f, 0xFFEEEEEE.toInt())
        }

        // 标题区域
        val titleY = y + 4 + (width - 8) + 8
        val titleHeight = height - (width - 8) - 12

        // 标题文本
        val title = track.title
        val font = SigmaMusicResources.helveticaLight14()
        SigmaMusicTextHelper.drawString(
            truncateText(title, 14, (width - 16).toInt()),
            x + 8, titleY, SigmaMusicResources.LIGHT_GREYISH_BLUE, font, 14f
        )

        // 艺术家
        if (track.artist.isNotEmpty()) {
            val artistFont = SigmaMusicResources.helveticaLight14()
            SigmaMusicTextHelper.drawString(
                truncateText(track.artist, 12, (width - 16).toInt()),
                x + 8, titleY + 20, SigmaMusicResources.TEXT_SECONDARY, artistFont, 12f
            )
        }

        // 悬停高亮
        if (hovered) {
            RenderUtils.drawRoundedRect(x, y, x + width, y + height, 14f, 0x30FFFFFF.toInt())
        }
    }

    private fun drawGradientBackground() {
        // 紫红色渐变（Sigma 默认）
        RenderUtils.drawGradientRect(
            x.toDouble(), y.toDouble(),
            (x + width).toDouble(), (y + height).toDouble(),
            false,
            0xEE1B0E3D.toInt(),
            0xEE3D1B5C.toInt()
        )
    }

    /**
     * 绘制音乐图标（简化音符图案）
     */
    private fun drawMusicIcon(cx: Float, cy: Float, size: Float, color: Int) {
        // 简化的播放三角形
        val halfSize = size / 2
        val a = -0.5f
        val b = 0.5f
        val points = floatArrayOf(
            cx + halfSize * a, cy + halfSize,
            cx + halfSize * a, cy - halfSize,
            cx + halfSize * b, cy
        )
        // 简单的三角形绘制
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        val a1 = (color shr 24) and 0xFF
        val r = (color shr 16) and 0xFF
        val gg = (color shr 8) and 0xFF
        val b2 = color and 0xFF
        GL11.glColor4f(r / 255f, gg / 255f, b2 / 255f, a1 / 255f)
        GL11.glBegin(GL11.GL_TRIANGLES)
        GL11.glVertex2f(points[0], points[1])
        GL11.glVertex2f(points[2], points[3])
        GL11.glVertex2f(points[4], points[5])
        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            onClick()
            return true
        }
        return false
    }

    fun updateHover(mouseX: Int, mouseY: Int) {
        hovered = isHovered(mouseX, mouseY)
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in x..(x + width) && mouseY.toFloat() in y..(y + height)
    }

    fun loadCoverAsync() {
        if (coverLoading || CoverTextureManager.contains(coverKey)) return
        val coverUrl = track.coverUrl
        if (coverUrl.isEmpty()) return

        coverLoading = true
        coverExecutor.submit {
            try {
                val url = URL(coverUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (conn.responseCode in 200..399) {
                    val img = javax.imageio.ImageIO.read(conn.inputStream)
                    if (img != null) {
                        CoverTextureManager.getOrLoad(coverKey) { img }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            } finally {
                coverLoading = false
            }
        }
    }

    private fun truncateText(text: String, fontSize: Float, maxWidth: Int): String {
        if (text.isEmpty()) return ""
        val font = SigmaMusicResources.helveticaLight14()
        val measuredWidth = SigmaMusicTextHelper.getStringWidth(text, font, fontSize)
        if (measuredWidth <= maxWidth) return text

        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (SigmaMusicTextHelper.getStringWidth(text.substring(0, mid), font, fontSize) <= maxWidth - 12) {
                lo = mid
            } else {
                hi = mid - 1
            }
        }
        return if (lo > 0) text.substring(0, lo) + "..." else ""
    }

    companion object {
        private val coverExecutor = Executors.newFixedThreadPool(4)
    }
}
