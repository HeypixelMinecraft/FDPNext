package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.skid.sigma.SigmaMusicManager
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class MusicProgressBar(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float
) {
    private var dragging = false
    private var hoverProgress = -1f

    fun draw(partialTicks: Float) {
        val durationMs = SigmaMusicManager.durationMs
        val positionMs = SigmaMusicManager.currentPositionMs

        val progress = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        val displayProgress = if (dragging && hoverProgress >= 0) hoverProgress else progress

        val bgColor = 0x40FFFFFF
        val fillColor = 0xFF2080FF.toInt()
        val bufferedColor = 0x602080FF.toInt()

        RenderUtils.drawRoundedRect(x, y, x + width, y + height, height / 2, bgColor)

        if (displayProgress > 0f) {
            RenderUtils.drawRoundedRect(x, y, x + width * displayProgress, y + height, height / 2, fillColor)
        }

        val thumbX = x + width * displayProgress
        val thumbRadius = 3f
        RenderUtils.drawCircle(thumbX, y + height / 2, thumbRadius, 0xFFFFFFFF.toInt())
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            dragging = true
            updateHoverProgress(mouseX)
            return true
        }
        return false
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (dragging && state == 0) {
            if (hoverProgress >= 0) {
                val durationMs = SigmaMusicManager.durationMs
                if (durationMs > 0) {
                    val targetMs = (hoverProgress * durationMs).toLong()
                    seekTo(targetMs)
                }
            }
            dragging = false
            hoverProgress = -1f
        }
    }

    fun mouseMoved(mouseX: Int, mouseY: Int) {
        if (dragging) {
            updateHoverProgress(mouseX)
        }
    }

    private fun updateHoverProgress(mouseX: Int) {
        hoverProgress = ((mouseX.toFloat() - x) / width).coerceIn(0f, 1f)
    }

    private fun seekTo(targetMs: Long) {
        val current = SigmaMusicManager.currentPositionMs
        if (targetMs > current) {
            val steps = ((targetMs - current) / 50).toInt().coerceAtLeast(1)
            Thread {
                for (i in 0 until steps) {
                    if (!SigmaMusicManager.isPlaying) break
                    Thread.sleep(50)
                }
            }.start()
        }
    }

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in x..(x + width) && mouseY.toFloat() in (y - 4)..(y + height + 4)
    }
}
