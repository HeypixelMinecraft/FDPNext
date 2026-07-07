package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.skid.sigma.SigmaMusicManager
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class MusicVolumeSlider(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float
) {
    private var dragging = false

    fun draw(partialTicks: Float) {
        val volume = SigmaMusicManager.getVolume() / 100f

        val bgColor = 0x40FFFFFF
        val fillColor = 0xFF2080FF.toInt()

        RenderUtils.drawRoundedRect(x, y, x + width, y + height, height / 2, bgColor)

        if (volume > 0f) {
            RenderUtils.drawRoundedRect(x, y, x + width * volume, y + height, height / 2, fillColor)
        }

        val thumbX = x + width * volume
        RenderUtils.drawCircle(thumbX, y + height / 2, 3f, 0xFFFFFFFF.toInt())
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            dragging = true
            updateVolume(mouseX)
            return true
        }
        return false
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (dragging && state == 0) {
            dragging = false
        }
    }

    fun mouseMoved(mouseX: Int, mouseY: Int) {
        if (dragging) {
            updateVolume(mouseX)
        }
    }

    fun mouseScrolled(delta: Int) {
        val current = SigmaMusicManager.getVolume()
        val newVol = (current + if (delta > 0) 5 else -5).coerceIn(0, 100)
        SigmaMusicManager.setVolume(newVol)
    }

    private fun updateVolume(mouseX: Int) {
        val vol = (((mouseX.toFloat() - x) / width) * 100).toInt().coerceIn(0, 100)
        SigmaMusicManager.setVolume(vol)
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in x..(x + width) && mouseY.toFloat() in (y - 4)..(y + height + 4)
    }
}
