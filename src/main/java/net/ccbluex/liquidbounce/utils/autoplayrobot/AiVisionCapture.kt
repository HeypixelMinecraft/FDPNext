/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.autoplayrobot

import net.minecraft.client.Minecraft
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

object AiVisionCapture {
    private val mc: Minecraft
        get() = Minecraft.getMinecraft()

    fun captureJpegDataUrl(targetWidth: Int): String? {
        if (mc.currentScreen != null) {
            return null
        }

        return runCatching {
            val framebuffer = mc.framebuffer ?: return null
            val width = framebuffer.framebufferWidth
            val height = framebuffer.framebufferHeight
            if (width <= 0 || height <= 0) {
                return null
            }

            val buffer = BufferUtils.createByteBuffer(width * height * 3)
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer)

            val source = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = ((y * width) + x) * 3
                    val r = buffer.get(index).toInt() and 0xFF
                    val g = buffer.get(index + 1).toInt() and 0xFF
                    val b = buffer.get(index + 2).toInt() and 0xFF
                    source.setRGB(x, height - y - 1, (r shl 16) or (g shl 8) or b)
                }
            }

            val scaledWidth = targetWidth.coerceIn(160, 640)
            val scaledHeight = (height.toDouble() / width.toDouble() * scaledWidth).toInt().coerceAtLeast(90)
            val scaled = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
            val graphics = scaled.createGraphics()
            graphics.drawImage(source.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_FAST), 0, 0, null)
            graphics.dispose()

            val output = ByteArrayOutputStream()
            ImageIO.write(scaled, "jpg", output)
            "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(output.toByteArray())
        }.getOrNull()
    }
}
