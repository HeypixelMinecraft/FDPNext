package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.utils.render.RenderUtils

/**
 * Sigma 风格 Tab 按钮 - 250x40 左侧栏标签
 */
class SigmaMusicTabButton(
    val id: String,
    var x: Float,
    var y: Float,
    val width: Float = 250f,
    val height: Float = 40f,
    val label: String,
    private val onClick: () -> Unit
) {
    var hovered = false
    var selected = false

    fun draw(partialTicks: Float) {
        // 背景
        if (selected) {
            RenderUtils.drawRoundedRect(x, y, x + width, y + height, 6f, SigmaMusicResources.TAB_SELECTED)
        } else if (hovered) {
            RenderUtils.drawRoundedRect(x, y, x + width, y + height, 6f, SigmaMusicResources.TAB_HOVER)
        }

        // 标签文字
        val font = SigmaMusicResources.helveticaLight14()
        SigmaMusicTextHelper.drawString(
            label, x + 20, y + 12,
            SigmaMusicResources.LIGHT_GREYISH_BLUE, font, 14f
        )
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
}
