package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.utils.render.RenderUtils

class MusicTabButton(
    val label: String,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    private val onClick: () -> Unit
) {
    var hovered = false
    var selected = false
    var hoverAlpha = 0f

    fun draw(partialTicks: Float) {
        val bgColor = if (selected) {
            0x602080FF.toInt()
        } else if (hovered) {
            0x40FFFFFF
        } else {
            0x00000000
        }

        if (bgColor != 0x00000000) {
            RenderUtils.drawRect(x, y, x + width, y + height, bgColor)
        }

        if (selected) {
            RenderUtils.drawRect(x, y, x + 2, y + height, 0xFF2080FF.toInt())
        }

        val textColor = if (selected) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
        MusicPlayerTextHelper.drawText(x + 8, y + (height - 14) / 2, label, textColor, 14)
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            onClick()
        }
    }

    fun updateHover(mouseX: Int, mouseY: Int) {
        hovered = isHovered(mouseX, mouseY)
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in x..(x + width) && mouseY.toFloat() in y..(y + height)
    }
}
