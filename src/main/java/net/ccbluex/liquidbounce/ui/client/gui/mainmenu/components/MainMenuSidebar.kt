/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.gui.mainmenu.MainMenuButton
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import java.awt.Color

/**
 * Vertical sidebar with the main navigation buttons.
 * Selected button is highlighted with the accent color; hover state animates.
 */
class MainMenuSidebar(
    private val buttons: List<MainMenuButton>,
    private val accentColor: () -> Int,
    private val onSelect: (MainMenuButton) -> Unit
) {
    var selectedId: String = buttons.firstOrNull()?.id ?: ""
        private set

    // Per-button hover alpha [0.0, 1.0] for smooth animation
    private val hoverAlphas = FloatArray(buttons.size)

    private val x: Int get() = 12
    private val y: Int get() = 60
    private val width: Int get() = 100
    private val buttonHeight: Int get() = 24
    private val gap: Int get() = 4
    private val hoverSpeed: Float = 0.15f
    fun buttonRect(index: Int): Rect {
        val by = y + index * (buttonHeight + gap)
        return Rect(x.toFloat(), by.toFloat(), (x + width).toFloat(), (by + buttonHeight).toFloat())
    }

    fun draw(mouseX: Int, mouseY: Int) {
        // Sidebar background panel
        RenderUtils.drawRoundedCornerRect(
            (x - 4).toFloat(),
            (y - 8).toFloat(),
            (x + width + 4).toFloat(),
            (y + buttons.size * (buttonHeight + gap) + 4).toFloat(),
            6f,
            Color(30, 30, 40, 220).rgb
        )

        buttons.forEachIndexed { index, btn ->
            val r = buttonRect(index)
            val hovered = r.contains(mouseX, mouseY)
            val selected = btn.id == selectedId

            // Animate hover alpha toward target (1.0 = hovered, 0.0 = not hovered)
            val target = if (hovered) 1.0f else 0.0f
            hoverAlphas[index] += (target - hoverAlphas[index]) * hoverSpeed
            // Snap when close enough to avoid endless micro-interpolation
            if (kotlin.math.abs(hoverAlphas[index] - target) < 0.005f) {
                hoverAlphas[index] = target
            }
            val alpha = hoverAlphas[index]

            if (alpha > 0.01f) {
                val bg = if (selected) {
                    val c = accentColor()
                    Color(c shr 16 and 0xFF, c shr 8 and 0xFF, c and 0xFF, (alpha * 255).toInt().coerceIn(0, 255)).rgb
                } else {
                    Color(255, 255, 255, (alpha * 30).toInt().coerceIn(0, 255)).rgb
                }
                RenderUtils.drawRoundedCornerRect(r.x, r.y, r.x2, r.y2, 4f, bg)
            }

            // Icon
            FontLoaders.F18.drawString(
                btn.icon,
                r.x + 8,
                r.y + (buttonHeight - 8) / 2f,
                if (selected) Color.WHITE.rgb else Color(180, 180, 190).rgb,
                false
            )
            // Label
            val label = LanguageManager.getAndFormat(btn.i18nKey)
            FontLoaders.F18.drawString(
                label,
                r.x + 24,
                r.y + (buttonHeight - 8) / 2f,
                if (selected) Color.WHITE.rgb else Color(200, 200, 210).rgb,
                false
            )
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        buttons.forEachIndexed { index, btn ->
            if (buttonRect(index).contains(mouseX, mouseY)) {
                selectedId = btn.id
                onSelect(btn)
                btn.action()
                return true
            }
        }
        return false
    }

    data class Rect(val x: Float, val y: Float, val x2: Float, val y2: Float) {
        fun contains(mx: Int, my: Int): Boolean = mx >= x && mx <= x2 && my >= y && my <= y2
    }
}
