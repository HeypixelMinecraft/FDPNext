package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.skid.sigma.SongInfo
import net.ccbluex.liquidbounce.utils.render.ImageUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MusicThumbnailButton(
    val track: SongInfo,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    private val onClick: () -> Unit
) {
    var hovered = false

    private var coverTexture: ResourceLocation? = null
    private var coverLoaded = false
    private var coverLoading = false

    fun draw(partialTicks: Float) {
        val bgColor = if (hovered) 0x30FFFFFF.toInt() else 0x18FFFFFF
        RenderUtils.drawRoundedRect(x, y, x + width, y + height, 4f, bgColor)

        if (coverTexture != null && coverLoaded) {
            GlStateManager.enableBlend()
            Minecraft.getMinecraft().textureManager.bindTexture(coverTexture)
            GuiMusicPlayer.drawModalRect(x + 4, y + 4, 48f, 48f)
            GlStateManager.disableBlend()
        } else {
            RenderUtils.drawRoundedRect(x + 4, y + 4, x + 52, y + 52, 4f, 0x30FFFFFF.toInt())
            val iconColor = 0x60FFFFFF
            RenderUtils.drawCircle(x + 28, y + 28, 8f, iconColor)
            RenderUtils.drawRoundedRect(x + 24, y + 22, x + 28, y + 34, 2f, 0x80FFFFFF.toInt())
        }

        val title = track.title
        val artist = if (track.artist.isNotEmpty()) track.artist else ""
        val maxTextWidth = width - 60

        MusicPlayerTextHelper.drawText(x + 58, y + 8, truncateText(title, 14, maxTextWidth.toInt()), 0xFFEEEEEE.toInt(), 14)
        if (artist.isNotEmpty()) {
            MusicPlayerTextHelper.drawText(x + 58, y + 26, truncateText(artist, 12, maxTextWidth.toInt()), 0xFF999999.toInt(), 12)
        }

        if (track.durationMs > 0) {
            val dur = formatDuration(track.durationMs)
            MusicPlayerTextHelper.drawText(x + width - 30, y + height - 16, dur, 0xFF777777.toInt(), 12)
        }
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
        if (coverLoading || coverLoaded) return
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
                        val resized = ImageUtils.resizeImage(img, 48, 48)
                        val dynamicTexture = DynamicTexture(resized)
                        val loc = ResourceLocation("fdpnext", "cover_${track.neteaseSongId}_${track.title.hashCode()}")
                        Minecraft.getMinecraft().textureManager.loadTexture(loc, dynamicTexture)
                        coverTexture = loc
                        coverLoaded = true
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            } finally {
                coverLoading = false
            }
        }
    }

    private fun truncateText(text: String, fontSize: Int, maxWidth: Int): String {
        if (text.isEmpty()) return ""
        val measuredWidth = MusicPlayerTextHelper.getStringWidth(text, fontSize)
        if (measuredWidth <= maxWidth) return text

        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (MusicPlayerTextHelper.getStringWidth(text.substring(0, mid), fontSize) <= maxWidth - 12) {
                lo = mid
            } else {
                hi = mid - 1
            }
        }
        return if (lo > 0) text.substring(0, lo) + "..." else ""
    }

    companion object {
        private val coverExecutor = Executors.newFixedThreadPool(4)

        private fun formatDuration(ms: Long): String {
            val totalSec = ms / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "$min:${sec.toString().padStart(2, '0')}"
        }
    }
}
