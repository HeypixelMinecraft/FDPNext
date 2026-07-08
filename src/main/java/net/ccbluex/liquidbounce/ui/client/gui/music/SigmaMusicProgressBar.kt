package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.utils.render.RenderUtils

/**
 * Sigma 风格进度条 - 底部控制栏进度
 */
class SigmaMusicProgressBar(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float = 5f
) {
    var progress: Float = 0f  // 0.0 ~ 1.0
    var dragging: Boolean = false

    fun draw(partialTicks: Float) {
        // 背景
        RenderUtils.drawRoundedRect(x, y, x + width, y + height, 2.5f, 0x60FFFFFF)
        // 进度
        if (progress > 0f) {
            RenderUtils.drawRoundedRect(x, y, x + width * progress, y + height, 2.5f, SigmaMusicResources.LIGHT_GREYISH_BLUE)
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            dragging = true
            updateProgress(mouseX)
            return true
        }
        return false
    }

    fun mouseReleased() {
        dragging = false
    }

    fun updateProgress(mouseX: Int) {
        val relativeX = (mouseX - x).coerceIn(0f, width)
        progress = relativeX / width
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in (x - 5)..(x + width + 5) && mouseY.toFloat() in (y - 5)..(y + height + 5)
    }
}
