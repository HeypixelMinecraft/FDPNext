package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.utils.render.RenderUtils

/**
 * Sigma 风格音量滑块 - 4x40 垂直
 */
class SigmaMusicVolumeSlider(
    var x: Float,
    var y: Float,
    val width: Float = 4f,
    val height: Float = 40f
) {
    var volume: Float = 0.5f  // 0.0 ~ 1.0 (Sigma: 0=静音, 1=最大)
    var dragging: Boolean = false

    fun draw(partialTicks: Float) {
        // 背景
        RenderUtils.drawRoundedRect(x, y, x + width, y + height, 2f, 0x60FFFFFF)
        // 当前音量（从底部向上填充）
        if (volume > 0f) {
            val fillHeight = height * volume
            RenderUtils.drawRoundedRect(
                x, y + height - fillHeight,
                x + width, y + height,
                2f, SigmaMusicResources.LIGHT_GREYISH_BLUE
            )
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            dragging = true
            updateVolume(mouseY)
            return true
        }
        return false
    }

    fun mouseReleased() {
        dragging = false
    }

    fun updateVolume(mouseY: Int) {
        val relativeY = (mouseY - y).coerceIn(0f, height)
        // Sigma: 1.0 = 顶部 = 静音, 0.0 = 底部 = 最大音量
        volume = 1f - (relativeY / height)
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in (x - 5)..(x + width + 5) && mouseY.toFloat() in y..(y + height)
    }
}
