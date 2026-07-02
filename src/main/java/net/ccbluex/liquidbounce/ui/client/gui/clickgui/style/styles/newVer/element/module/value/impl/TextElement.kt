package net.ccbluex.liquidbounce.ui.client.gui.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.ui.client.gui.newVer.ColorManager
import net.ccbluex.liquidbounce.ui.client.gui.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ChatAllowedCharacters
import org.lwjgl.input.Keyboard
import java.awt.Color

class TextElement(private val savedValue: TextValue) : ValueElement<String>(savedValue) {
    private var focused = false

    init {
        valueHeight = 24F
    }

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        val boxWidth = (width * 0.45F).coerceAtLeast(180F)
        val boxLeft = x + width - boxWidth - 10F
        val boxRight = x + width - 10F
        val boxTop = y + 3F
        val boxBottom = y + 21F
        val hovered = MouseUtils.mouseWithinBounds(mouseX, mouseY, boxLeft, boxTop, boxRight, boxBottom)

        Fonts.font40.drawString(value.name, x + 10F, y + 12F - Fonts.font40.FONT_HEIGHT / 2F + 2F, -1)
        RenderUtils.originalRoundedRect(boxLeft, boxTop, boxRight, boxBottom, 4F, if (focused || hovered) ColorManager.buttonOutline.rgb else ColorManager.button.rgb)
        RenderUtils.originalRoundedRect(boxLeft + 1F, boxTop + 1F, boxRight - 1F, boxBottom - 1F, 4F, ColorManager.button.rgb)

        val rawText = savedValue.get()
        val visibleText = if (shouldMask() && !focused && rawText.isNotEmpty()) "*".repeat(rawText.length.coerceAtMost(24)) else rawText
        val displayText = trimToWidth(visibleText, boxWidth - 16F)
        Fonts.font40.drawString(displayText.ifEmpty { if (focused) "" else "Click to set" }, boxLeft + 6F, y + 12F - Fonts.font40.FONT_HEIGHT / 2F + 2F, if (rawText.isEmpty() && !focused) Color(140, 140, 140).rgb else -1)

        if (focused && System.currentTimeMillis() / 500L % 2L == 0L) {
            val cursorX = (boxLeft + 6F + Fonts.font40.getStringWidth(displayText)).coerceAtMost(boxRight - 6F)
            RenderUtils.newDrawRect(cursorX, boxTop + 4F, cursorX + 1F, boxBottom - 4F, accentColor.rgb)
        }

        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        val boxWidth = (width * 0.45F).coerceAtLeast(180F)
        focused = MouseUtils.mouseWithinBounds(mouseX, mouseY, x + width - boxWidth - 10F, y + 3F, x + width - 10F, y + 21F)
    }

    override fun onKeyPress(typed: Char, keyCode: Int): Boolean {
        if (!focused) return false

        when (keyCode) {
            Keyboard.KEY_ESCAPE, Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                focused = false
                return true
            }
            Keyboard.KEY_BACK -> {
                val text = savedValue.get()
                if (text.isNotEmpty()) savedValue.set(text.dropLast(1))
                return true
            }
            Keyboard.KEY_V -> {
                if (GuiScreen.isCtrlKeyDown()) {
                    savedValue.set(savedValue.get() + GuiScreen.getClipboardString())
                    return true
                }
            }
        }

        if (ChatAllowedCharacters.isAllowedCharacter(typed)) {
            savedValue.set(savedValue.get() + typed)
            return true
        }

        return true
    }

    private fun shouldMask(): Boolean {
        val lower = value.name.lowercase()
        return lower.contains("key") || lower.contains("token") || lower.contains("password") || lower.contains("secret")
    }

    private fun trimToWidth(text: String, maxWidth: Float): String {
        if (Fonts.font40.getStringWidth(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.isNotEmpty() && Fonts.font40.getStringWidth("...$trimmed") > maxWidth) {
            trimmed = trimmed.drop(1)
        }
        return "...$trimmed"
    }
}