package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.skid.sigma.NeteaseApiSearch
import net.ccbluex.liquidbounce.skid.sigma.SigmaMusicManager
import net.ccbluex.liquidbounce.skid.sigma.SongInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.util.concurrent.Executors

class MusicSearchBox(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float
) {
    private var searchField: GuiTextField? = null
    private val results = mutableListOf<SongInfo>()
    private val thumbnails = mutableListOf<MusicThumbnailButton>()
    private var searching = false
    private var scrollOffset = 0
    private val maxVisibleResults = 8
    private val resultHeight = 56f
    private var initialized = false

    fun init(mc: Minecraft, screenW: Int, screenH: Int) {
        searchField = GuiTextField(0, mc.fontRendererObj, x.toInt() + 1, y.toInt() + 1, (width - 2).toInt(), 18)
        searchField?.setMaxStringLength(100)
        searchField?.setFocused(false)
        initialized = true
    }

    fun draw(partialTicks: Float) {
        if (!initialized) return

        RenderUtils.drawRoundedRect(x, y, x + width, y + 20, 4f, 0x40FFFFFF)
        searchField?.drawTextBox()

        if (searchField?.text?.isEmpty() == true && !searchField?.isFocused!!) {
            MusicPlayerTextHelper.drawText(x + 8, y + 5, "Search Netease...", 0x60FFFFFF, 14)
        }

        if (results.isNotEmpty()) {
            val startY = y + 24
            val visibleCount = minOf(results.size, maxVisibleResults)

            for (i in 0 until visibleCount) {
                val idx = i + scrollOffset
                if (idx >= results.size) break

                val track = results[idx]
                val ry = startY + i * resultHeight

                val bg = if (i % 2 == 0) 0x08FFFFFF else 0x04FFFFFF
                RenderUtils.drawRect(x, ry, x + width, ry + resultHeight, bg)

                MusicPlayerTextHelper.drawText(x + 8, ry + 8, truncateText(track.title, 14, (width - 100).toInt()), 0xFFEEEEEE.toInt(), 14)
                if (track.artist.isNotEmpty()) {
                    MusicPlayerTextHelper.drawText(x + 8, ry + 26, truncateText(track.artist, 12, (width - 100).toInt()), 0xFF999999.toInt(), 12)
                }
            }

            if (results.size > maxVisibleResults) {
                val barHeight = (maxVisibleResults.toFloat() / results.size * (maxVisibleResults * resultHeight))
                val barY = startY + scrollOffset.toFloat() / results.size * (maxVisibleResults * resultHeight)
                RenderUtils.drawRect(x + width - 3, barY, x + width, barY + barHeight, 0x40FFFFFF)
            }
        } else if (searching) {
            MusicPlayerTextHelper.drawText(x + width / 2 - 30, y + 50, "Searching...", 0xFF999999.toInt(), 14)
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int) {
        searchField?.textboxKeyTyped(typedChar, keyCode)
        if (keyCode == Keyboard.KEY_RETURN && searchField?.text?.isNotEmpty() == true) {
            doSearch(searchField?.text!!)
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        searchField?.mouseClicked(mouseX, mouseY, mouseButton)

        if (mouseButton == 0 && results.isNotEmpty()) {
            val startY = y + 24
            val visibleCount = minOf(results.size, maxVisibleResults)

            for (i in 0 until visibleCount) {
                val idx = i + scrollOffset
                if (idx >= results.size) break
                val ry = startY + i * resultHeight
                if (mouseY.toFloat() in ry..(ry + resultHeight) && mouseX.toFloat() in x..(x + width)) {
                    val track = results[idx]
                    SigmaMusicManager.playSong(track.id, track.displayTitle)
                    return true
                }
            }
        }
        return false
    }

    fun mouseScrolled(delta: Int) {
        if (results.size > maxVisibleResults) {
            scrollOffset = (scrollOffset - if (delta > 0) 2 else -2).coerceIn(0, results.size - maxVisibleResults)
        }
    }

    private fun doSearch(keyword: String) {
        if (searching) return
        searching = true
        results.clear()
        thumbnails.clear()
        scrollOffset = 0

        searchExecutor.submit {
            try {
                val tracks = NeteaseApiSearch.search(keyword, 30)
                val playable = mutableListOf<SongInfo>()
                val ids = tracks.map { it.id }.toLongArray()
                val urlList = NeteaseApiSearch.getSongUrls(*ids)
                val urlMap = urlList.associateBy { it.id }

                for (track in tracks) {
                    val urlInfo = urlMap[track.id]
                    if (urlInfo != null && urlInfo.url != null && urlInfo.url.isNotEmpty()) {
                        playable.add(SongInfo.fromNeteaseTrack(track))
                    }
                }

                synchronized(results) {
                    results.clear()
                    results.addAll(playable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                searching = false
            }
        }
    }

    private fun truncateText(text: String, fontSize: Int, maxWidth: Int): String {
        if (text.isEmpty()) return ""
        val measured = MusicPlayerTextHelper.getStringWidth(text, fontSize)
        if (measured <= maxWidth) return text
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (MusicPlayerTextHelper.getStringWidth(text.substring(0, mid), fontSize) <= maxWidth - 12) lo = mid
            else hi = mid - 1
        }
        return if (lo > 0) text.substring(0, lo) + "..." else ""
    }

    companion object {
        private val searchExecutor = Executors.newSingleThreadExecutor()
    }
}
