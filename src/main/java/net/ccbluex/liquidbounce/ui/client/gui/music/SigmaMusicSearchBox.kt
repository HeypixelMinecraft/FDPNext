package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard

/**
 * Sigma 风格搜索框
 */
class SigmaMusicSearchBox(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float = 40f,
    val hint: String = "Search..."
) {
    var text: String = ""
    var focused: Boolean = false
    private var cursorCooldown: Long = 0L

    fun draw(partialTicks: Float) {
        // 背景
        val bgColor = if (focused) 0xFF1F1F1F.toInt() else 0xFF131313.toInt()
        RenderUtils.drawRoundedRect(x, y, x + width, y + height, 6f, bgColor)

        // 文本或提示
        val displayText = if (text.isEmpty()) hint else text
        val textColor = if (text.isEmpty()) SigmaMusicResources.TEXT_SECONDARY else SigmaMusicResources.LIGHT_GREYISH_BLUE
        val font = SigmaMusicResources.helveticaLight14()
        SigmaMusicTextHelper.drawString(displayText, x + 12, y + 13, textColor, font, 14f)

        // 光标
        if (focused) {
            val now = System.currentTimeMillis()
            if ((now / 500) % 2 == 0L) {
                val textWidth = SigmaMusicTextHelper.getStringWidth(text, font, 14f)
                RenderUtils.drawRect(x + 12 + textWidth, y + 10, x + 13 + textWidth, y + 30, SigmaMusicResources.LIGHT_GREYISH_BLUE)
            }
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!focused) return false
        when (keyCode) {
            Keyboard.KEY_BACK -> {
                if (text.isNotEmpty()) text = text.substring(0, text.length - 1)
                return true
            }
            Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                focused = false
                return true
            }
        }
        if (typedChar.isLetterOrDigit() || typedChar == ' ' || typedChar in "._-\u4e00\u9fa5") {
            if (text.length < 100) text += typedChar
            return true
        }
        return false
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            val wasFocused = focused
            focused = isHovered(mouseX, mouseY)
            return focused || wasFocused
        }
        return false
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in x..(x + width) && mouseY.toFloat() in y..(y + height)
    }
}
