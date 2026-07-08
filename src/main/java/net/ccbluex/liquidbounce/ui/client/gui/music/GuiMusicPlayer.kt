package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.skid.sigma.AudioRepeatMode
import net.ccbluex.liquidbounce.skid.sigma.SigmaMusicManager
import net.ccbluex.liquidbounce.skid.sigma.SongInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Desktop
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class GuiMusicPlayer : GuiScreen() {

    companion object {
        private const val TAB_WIDTH = 170
        private const val BOTTOM_BAR_HEIGHT = 60
        private const val HEADER_HEIGHT = 40

        fun drawModalRect(x: Float, y: Float, w: Float, h: Float) {
            val tess = Tessellator.getInstance()
            val wr: WorldRenderer = tess.worldRenderer
            wr.begin(7, DefaultVertexFormats.POSITION_TEX)
            wr.pos(x.toDouble(), (y + h).toDouble(), 0.0).tex(0.0, 1.0).endVertex()
            wr.pos((x + w).toDouble(), (y + h).toDouble(), 0.0).tex(1.0, 1.0).endVertex()
            wr.pos((x + w).toDouble(), y.toDouble(), 0.0).tex(1.0, 0.0).endVertex()
            wr.pos(x.toDouble(), y.toDouble(), 0.0).tex(0.0, 0.0).endVertex()
            tess.draw()
        }
    }

    private var panelX = 0f
    private var panelY = 0f
    private var panelWidth = 800
    private var panelHeight = 600

    private val tabs = mutableListOf<MusicTabButton>()
    private var activeTabIndex = -1

    private lateinit var progressBar: MusicProgressBar
    private lateinit var volumeSlider: MusicVolumeSlider
    private lateinit var searchBox: MusicSearchBox

    private val thumbnails = CopyOnWriteArrayList<MusicThumbnailButton>()
    private var scrollOffset = 0
    private val thumbnailColumns = 3
    private val thumbnailWidth = 230f
    private val thumbnailHeight = 56f
    private val thumbnailPadding = 8f

    private var isDragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    private var showSearch = false
    private var searchText = ""
    private var searchResults = CopyOnWriteArrayList<SongInfo>()
    private var searchThumbnails = CopyOnWriteArrayList<MusicThumbnailButton>()
    private var isSearching = false

    private val contentExecutor = Executors.newSingleThreadExecutor()

    private var hoverPlayPause = false
    private var hoverNext = false
    private var hoverPrev = false
    private var hoverStop = false
    private var hoverRepeat = false
    private var hoverVolume = false
    private var hoverFolder = false
    private var hoverSearch = false

    override fun initGui() {
        // 响应式面板尺寸计算
        val sr = net.minecraft.client.gui.ScaledResolution(mc)
        val screenWidth = sr.scaledWidth
        val screenHeight = sr.scaledHeight
        
        // 计算面板尺寸（85%屏幕尺寸，限制在合理范围内）
        panelWidth = (screenWidth * 0.85f).toInt().coerceIn(400, 900)
        panelHeight = (screenHeight * 0.85f).toInt().coerceIn(300, 650)
        
        // 计算面板位置（居中，确保不超出屏幕）
        panelX = ((screenWidth - panelWidth) / 2f).coerceAtLeast(0f)
        panelY = ((screenHeight - panelHeight) / 2f).coerceAtLeast(0f)

        progressBar = MusicProgressBar(
            panelX + 100, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 10,
            panelWidth - 200f, 8f
        )

        volumeSlider = MusicVolumeSlider(
            panelX + panelWidth - 80, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 20,
            60f, 8f
        )

        searchBox = MusicSearchBox(
            panelX + TAB_WIDTH + 10, panelY + HEADER_HEIGHT + 5,
            panelWidth - TAB_WIDTH - 20f, 20f
        )
        searchBox.init(mc!!, width, height)

        setupTabs()
        loadBundledMusic()
    }

    private fun setupTabs() {
        tabs.clear()
        val tabY = panelY + HEADER_HEIGHT + 5

        tabs.add(MusicTabButton("All Music", panelX, tabY, TAB_WIDTH.toFloat(), 28f) {
            showSearch = false
            activeTabIndex = 0
            loadBundledMusic()
        })

        tabs.add(MusicTabButton("Hot Songs", panelX, tabY + 32, TAB_WIDTH.toFloat(), 28f) {
            showSearch = false
            activeTabIndex = 1
            loadPlaylistAsync { SigmaMusicManager.loadNeteaseHotSongs() }
        })

        tabs.add(MusicTabButton("New Songs", panelX, tabY + 64, TAB_WIDTH.toFloat(), 28f) {
            showSearch = false
            activeTabIndex = 2
            loadPlaylistAsync { SigmaMusicManager.loadNeteaseNewSongs() }
        })

        tabs.add(MusicTabButton("Search", panelX, tabY + 96, TAB_WIDTH.toFloat(), 28f) {
            showSearch = true
            activeTabIndex = 3
            thumbnails.clear()
        })

        if (tabs.isNotEmpty()) {
            tabs[0].selected = true
            activeTabIndex = 0
        }
    }

    private fun loadBundledMusic() {
        val gameDir = Minecraft.getMinecraft().mcDataDir
        val musicDir = File(gameDir, "music")
        if (!musicDir.exists()) musicDir.mkdirs()

        contentExecutor.submit {
            SigmaMusicManager.loadLocalMusicFromDir(musicDir)
            buildThumbnailsFromPlaylist()
        }
    }

    private fun loadPlaylistAsync(loader: () -> Unit) {
        contentExecutor.submit {
            loader()
            Thread.sleep(500)
            buildThumbnailsFromPlaylist()
        }
    }

    private fun buildThumbnailsFromPlaylist() {
        thumbnails.clear()
        scrollOffset = 0
        val size = SigmaMusicManager.getPlaylistSize()

        for (i in 0 until size) {
            val track = SigmaMusicManager.getTrackAt(i) ?: continue
            val col = i % thumbnailColumns
            val row = i / thumbnailColumns

            val tx = panelX + TAB_WIDTH + 10 + col * (thumbnailWidth + thumbnailPadding)
            val ty = panelY + HEADER_HEIGHT + 10 + row * (thumbnailHeight + thumbnailPadding) - scrollOffset

            val thumb = MusicThumbnailButton(track, tx, ty, thumbnailWidth, thumbnailHeight) {
                SigmaMusicManager.playPlaylist(
                    (0 until size).mapNotNull { SigmaMusicManager.getTrackAt(it) },
                    i
                )
            }
            thumb.loadCoverAsync()
            thumbnails.add(thumb)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GlStateManager.pushMatrix()

        drawPanelBackground()
        drawHeader()
        drawTabs(mouseX, mouseY, partialTicks)
        drawContentArea(mouseX, mouseY, partialTicks)
        drawBottomBar(mouseX, mouseY, partialTicks)
        drawOverlay()

        GlStateManager.popMatrix()

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawPanelBackground() {
        RenderUtils.drawRoundedRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 8f, 0xE01A1A2A.toInt())

        RenderUtils.drawRect(panelX, panelY, panelX + panelWidth, panelY + HEADER_HEIGHT, 0x40000000)

        RenderUtils.drawRect(panelX, panelY + panelHeight - BOTTOM_BAR_HEIGHT, panelX + panelWidth, panelY + panelHeight, 0x40000000)

        RenderUtils.drawRect(panelX + TAB_WIDTH, panelY + HEADER_HEIGHT, panelX + TAB_WIDTH + 1, panelY + panelHeight - BOTTOM_BAR_HEIGHT, 0x20FFFFFF)
    }

    private fun drawHeader() {
        MusicPlayerTextHelper.drawText(panelX + 12, panelY + 12, "Music Player", 0xFFEEEEEE.toInt(), 18)

        val searchBtnX = panelX + panelWidth - 60
        val searchBtnY = panelY + 10
        val searchBtnW = 50f
        val searchBtnH = 20f

        val searchBg = if (hoverSearch) 0x302080FF else 0x20FFFFFF
        RenderUtils.drawRoundedRect(searchBtnX, searchBtnY, searchBtnX + searchBtnW, searchBtnY + searchBtnH, 4f, searchBg)
        MusicPlayerTextHelper.drawText(searchBtnX + 10, searchBtnY + 4, "Search", 0xFFEEEEEE.toInt(), 12)
    }

    private fun drawTabs(mouseX: Int, mouseY: Int, partialTicks: Float) {
        for (tab in tabs) {
            tab.updateHover(mouseX, mouseY)
            tab.draw(partialTicks)
        }
    }

    private fun drawContentArea(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        val scaleFactor = net.minecraft.client.gui.ScaledResolution(mc).scaleFactor
        val scissorX = ((panelX + TAB_WIDTH + 1) * scaleFactor).toInt()
        val scissorY = ((height - (panelY + panelHeight - BOTTOM_BAR_HEIGHT)) * scaleFactor).toInt()
        val scissorW = ((panelWidth - TAB_WIDTH - 1) * scaleFactor).toInt()
        val scissorH = ((panelHeight - HEADER_HEIGHT - BOTTOM_BAR_HEIGHT) * scaleFactor).toInt()
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH)

        if (showSearch) {
            searchBox.draw(partialTicks)
        } else {
            for (thumb in thumbnails) {
                thumb.updateHover(mouseX, mouseY)
                thumb.draw(partialTicks)
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    private fun drawBottomBar(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val barY = panelY + panelHeight - BOTTOM_BAR_HEIGHT

        val songTitle = SigmaMusicManager.songTitle
        if (songTitle.isNotEmpty()) {
            MusicPlayerTextHelper.drawText(panelX + 100, barY + 5, truncateText(songTitle, 14, 200), 0xFFEEEEEE.toInt(), 14)
        }

        val positionMs = SigmaMusicManager.currentPositionMs
        val durationMs = SigmaMusicManager.durationMs
        MusicPlayerTextHelper.drawText(
            panelX + 100, barY + 25,
            "${formatTime(positionMs)} / ${formatTime(durationMs)}",
            0xFF999999.toInt(), 12
        )

        val lyric = SigmaMusicManager.getCurrentLyric()
        if (lyric.isNotEmpty()) {
            MusicPlayerTextHelper.drawText(panelX + 280, barY + 25, truncateText(lyric, 12, 250), 0xFF666666.toInt(), 12)
        }

        progressBar.x = panelX + 100
        progressBar.y = barY + 40
        progressBar.draw(partialTicks)

        val controlCenterX = panelX + 50
        val controlY = barY + 12

        drawControlButton(controlCenterX - 20, controlY, "||", hoverPrev) { SigmaMusicManager.previous() }
        drawControlButton(controlCenterX + 10, controlY, if (SigmaMusicManager.isPlaying) ">" else "||", hoverPlayPause) {
            if (SigmaMusicManager.isPlaying) SigmaMusicManager.pause() else SigmaMusicManager.resume()
        }
        drawControlButton(controlCenterX + 40, controlY, ">>", hoverNext) { SigmaMusicManager.next() }
        drawControlButton(controlCenterX + 70, controlY, "[]", hoverStop) { SigmaMusicManager.stop() }

        val repeatMode = SigmaMusicManager.getRepeatMode()
        val repeatText = when (repeatMode) {
            AudioRepeatMode.NO_REPEAT -> "1x"
            AudioRepeatMode.REPEAT -> "R"
            AudioRepeatMode.LOOP_CURRENT -> "L"
        }
        drawControlButton(controlCenterX + 100, controlY, repeatText, hoverRepeat) {
            SigmaMusicManager.setRepeatMode(repeatMode.getNext())
        }

        val volX = panelX + panelWidth - 80
        val volY = barY + 20
        volumeSlider.x = volX
        volumeSlider.y = volY
        volumeSlider.draw(partialTicks)

        val folderX = panelX + panelWidth - 140
        val folderBg = if (hoverFolder) 0x302080FF else 0x20FFFFFF
        RenderUtils.drawRoundedRect(folderX, barY + 5, folderX + 50, barY + 25, 4f, folderBg)
        MusicPlayerTextHelper.drawText(folderX + 6, barY + 9, "Open", 0xFFEEEEEE.toInt(), 12)
    }

    private fun drawControlButton(x: Float, y: Float, text: String, hovered: Boolean, onClick: () -> Unit) {
        val btnW = 24f
        val btnH = 20f
        val bg = if (hovered) 0x402080FF else 0x20FFFFFF
        RenderUtils.drawRoundedRect(x, y, x + btnW, y + btnH, 4f, bg)
        MusicPlayerTextHelper.drawText(x + (btnW - MusicPlayerTextHelper.getStringWidth(text, 12)) / 2, y + 4, text, 0xFFEEEEEE.toInt(), 12)
    }

    private fun drawOverlay() {
        if (SigmaMusicManager.hasVisualizerData() && SigmaMusicManager.isPlaying) {
            val amplitudes = SigmaMusicManager.getAmplitudes()
            if (amplitudes.isNotEmpty()) {
                val barCount = minOf(amplitudes.size, 60)
                val barWidth = (panelWidth - TAB_WIDTH).toFloat() / barCount
                val maxBarHeight = 40f

                for (i in 0 until barCount) {
                    val amp = amplitudes[i].coerceIn(0.0, 1.0E7)
                    val normalizedAmp = (amp / 1.0E7).coerceIn(0.0, 1.0)
                    val barH = (normalizedAmp * maxBarHeight).toFloat()

                    if (barH > 0.5f) {
                        val bx = panelX + TAB_WIDTH + i * barWidth
                        val by = panelY + panelHeight - BOTTOM_BAR_HEIGHT - barH
                        val alpha = (0.3 + 0.5 * normalizedAmp).coerceAtMost(0.8)
                        val color = ((alpha * 255).toInt() shl 24) or 0x2080FF
                        RenderUtils.drawRect(bx, by, bx + barWidth - 1, panelY + panelHeight - BOTTOM_BAR_HEIGHT, color)
                    }
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        if (mouseButton == 0) {
            for (tab in tabs) {
                tab.mouseClicked(mouseX, mouseY, mouseButton)
            }

            if (showSearch) {
                searchBox.mouseClicked(mouseX, mouseY, mouseButton)
            } else {
                for (thumb in thumbnails) {
                    if (thumb.mouseClicked(mouseX, mouseY, mouseButton)) break
                }
            }

            progressBar.mouseClicked(mouseX, mouseY, mouseButton)
            volumeSlider.mouseClicked(mouseX, mouseY, mouseButton)

            if (isControlHovered(mouseX, mouseY, panelX + 30, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 12, 24, 20)) {
                SigmaMusicManager.previous()
            } else if (isControlHovered(mouseX, mouseY, panelX + 60, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 12, 24, 20)) {
                if (SigmaMusicManager.isPlaying) SigmaMusicManager.pause() else SigmaMusicManager.resume()
            } else if (isControlHovered(mouseX, mouseY, panelX + 90, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 12, 24, 20)) {
                SigmaMusicManager.next()
            } else if (isControlHovered(mouseX, mouseY, panelX + 120, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 12, 24, 20)) {
                SigmaMusicManager.stop()
            } else if (isControlHovered(mouseX, mouseY, panelX + 150, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 12, 24, 20)) {
                SigmaMusicManager.setRepeatMode(SigmaMusicManager.getRepeatMode().getNext())
            }

            val searchHeaderX = panelX + panelWidth - 60
            val searchHeaderY = panelY + 10
            if (mouseX.toFloat() in searchHeaderX..(searchHeaderX + 50) && mouseY.toFloat() in searchHeaderY..(searchHeaderY + 20)) {
                showSearch = true
                activeTabIndex = 3
                for (t in tabs) t.selected = false
                if (tabs.size > 3) tabs[3].selected = true
            }

            val folderX = panelX + panelWidth - 140
            val barY = panelY + panelHeight - BOTTOM_BAR_HEIGHT
            if (mouseX.toFloat() in folderX..(folderX + 50) && mouseY.toFloat() in (barY + 5)..(barY + 25)) {
                openMusicFolder()
            }
        }

        if (mouseButton == 1) {
            isDragging = true
            dragOffsetX = mouseX.toFloat() - panelX
            dragOffsetY = mouseY.toFloat() - panelY
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)
        progressBar.mouseReleased(mouseX, mouseY, state)
        volumeSlider.mouseReleased(mouseX, mouseY, state)
        if (state == 1) isDragging = false
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val mouseX = Mouse.getEventX() * width / mc.displayWidth
        val mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1

        if (isDragging) {
            panelX = (mouseX.toFloat() - dragOffsetX).coerceIn(0f, (width - panelWidth).toFloat())
            panelY = (mouseY.toFloat() - dragOffsetY).coerceIn(0f, (height - panelHeight).toFloat())
            updatePositions()
        }

        val dWheel = Mouse.getEventDWheel()
        if (dWheel != 0) {
            if (mouseX.toFloat() in (panelX + TAB_WIDTH)..(panelX + panelWidth)) {
                if (showSearch) {
                    searchBox.mouseScrolled(dWheel)
                } else {
                    scrollThumbnails(dWheel)
                }
            }

            if (isControlHovered(mouseX, mouseY, panelX + panelWidth - 80, panelY + panelHeight - BOTTOM_BAR_HEIGHT + 16, 60, 16)) {
                volumeSlider.mouseScrolled(dWheel)
            }
        }

        progressBar.mouseMoved(mouseX, mouseY)
        volumeSlider.mouseMoved(mouseX, mouseY)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (showSearch) {
            searchBox.keyTyped(typedChar, keyCode)
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
        }
    }

    override fun doesGuiPauseGame(): Boolean = false

    private fun scrollThumbnails(delta: Int) {
        val totalHeight = ((thumbnails.size + thumbnailColumns - 1) / thumbnailColumns) * (thumbnailHeight + thumbnailPadding)
        val visibleHeight = panelHeight - HEADER_HEIGHT - BOTTOM_BAR_HEIGHT - 20
        if (totalHeight <= visibleHeight) return

        val scrollStep = 40f
        scrollOffset = (scrollOffset - if (delta > 0) scrollStep.toInt() else -scrollStep.toInt())
            .coerceIn(0, (totalHeight - visibleHeight).toInt())
        buildThumbnailsFromPlaylist()
    }

    private fun updatePositions() {
        progressBar.x = panelX + 100
        progressBar.y = panelY + panelHeight - BOTTOM_BAR_HEIGHT + 10

        volumeSlider.x = panelX + panelWidth - 80
        volumeSlider.y = panelY + panelHeight - BOTTOM_BAR_HEIGHT + 20

        searchBox.x = panelX + TAB_WIDTH + 10
        searchBox.y = panelY + HEADER_HEIGHT + 5
    }

    private fun isControlHovered(mouseX: Int, mouseY: Int, x: Float, y: Float, w: Int, h: Int): Boolean {
        return mouseX.toFloat() in x..(x + w) && mouseY.toFloat() in y..(y + h)
    }

    private fun openMusicFolder() {
        val gameDir = Minecraft.getMinecraft().mcDataDir
        val musicDir = File(gameDir, "music")
        if (!musicDir.exists()) musicDir.mkdirs()
        try {
            Desktop.getDesktop().open(musicDir)
        } catch (_: Exception) {}
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "$min:${sec.toString().padStart(2, '0')}"
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

    override fun onGuiClosed() {
        super.onGuiClosed()
        MusicPlayerTextHelper.flushCache()
    }
}
