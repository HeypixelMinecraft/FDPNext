package net.ccbluex.liquidbounce.ui.client.gui.music

import net.ccbluex.liquidbounce.skid.sigma.NeteaseApiSearch
import net.ccbluex.liquidbounce.skid.sigma.SigmaMusicManager
import net.ccbluex.liquidbounce.skid.sigma.SongInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.min

/**
 * Sigma Jello Music Player - 1:1 还原 Sigma 客户端的 MusicPlayer GUI
 *
 * 布局（800x600 固定）：
 * - 左侧 250px tab 栏（Search/Bundled Music/Local Music/网易云热歌/网易云新歌/我喜欢的音乐）
 * - 右侧内容区：3列网格缩略图（每张 183x220）
 * - 顶部 64px 头部（"Jello" + "music" 字体）
 * - 底部 94px 控制栏（专辑封面 114x114 + 播放控制 + 音量 + 进度条 + 循环）
 */
class GuiMusicPlayer : GuiScreen() {

    companion object {
        const val PANEL_WIDTH = 800
        const val PANEL_HEIGHT = 600
        const val TAB_WIDTH = 250
        const val TAB_HEIGHT = 40
        const val HEADER_HEIGHT = 64
        const val BOTTOM_BAR_HEIGHT = 94
        const val GRID_THUMBNAIL_WIDTH = 183
        const val GRID_THUMBNAIL_HEIGHT = 220
        const val GRID_PADDING_X = 65
        const val GRID_PADDING_Y = 10
        const val GRID_X_STEP = 183
        const val GRID_Y_STEP = 210
        const val ALBUM_SIZE = 114

        /**
         * 绘制纹理矩形
         */
        fun drawModalRect(x: Float, y: Float, width: Float, height: Float) {
            val tessellator = Tessellator.getInstance()
            val worldrenderer = tessellator.worldRenderer
            GlStateManager.enableTexture2D()
            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
            worldrenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0)
                .tex(0.0, 1.0).endVertex()
            worldrenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0)
                .tex(1.0, 1.0).endVertex()
            worldrenderer.pos((x + width).toDouble(), y.toDouble(), 0.0)
                .tex(1.0, 0.0).endVertex()
            worldrenderer.pos(x.toDouble(), y.toDouble(), 0.0)
                .tex(0.0, 0.0).endVertex()
            tessellator.draw()
        }

        fun drawModalRect(x: Double, y: Double, width: Double, height: Double) {
            val tessellator = Tessellator.getInstance()
            val worldrenderer = tessellator.worldRenderer
            GlStateManager.enableTexture2D()
            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
            worldrenderer.pos(x, y + height, 0.0).tex(0.0, 1.0).endVertex()
            worldrenderer.pos(x + width, y + height, 0.0).tex(1.0, 1.0).endVertex()
            worldrenderer.pos(x + width, y, 0.0).tex(1.0, 0.0).endVertex()
            worldrenderer.pos(x, y, 0.0).tex(0.0, 0.0).endVertex()
            tessellator.draw()
        }

        fun drawImage(resource: ResourceLocation, x: Float, y: Float, width: Float, height: Float, color: Int) {
            GlStateManager.pushMatrix()
            GlStateManager.enableBlend()
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
            GlStateManager.color(
                ((color shr 16) and 0xFF) / 255f,
                ((color shr 8) and 0xFF) / 255f,
                (color and 0xFF) / 255f,
                ((color shr 24) and 0xFF) / 255f
            )
            Minecraft.getMinecraft().textureManager.bindTexture(resource)
            drawModalRect(x, y, width, height)
            GlStateManager.disableBlend()
            GlStateManager.color(1f, 1f, 1f, 1f)
            GlStateManager.popMatrix()
        }
    }

    private var panelX = 0f
    private var panelY = 0f

    // 拖动相关
    private var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dragX = 0f
    private var dragY = 0f

    // Tab 列表（搜索 + 5个源）
    private val tabs = mutableListOf<SigmaMusicTabButton>()
    private var currentTab: SigmaMusicTabButton? = null

    // 内容面板
    private val currentTracks = mutableListOf<SongInfo>()
    private val thumbnailButtons = mutableListOf<SigmaMusicThumbnailButton>()
    private val allTracks = mutableListOf<SongInfo>()

    // 搜索
    private var searchBox: SigmaMusicSearchBox? = null
    private var showSearchBox: Boolean = false

    // 控制栏
    private var progressBar: SigmaMusicProgressBar? = null
    private var volumeSlider: SigmaMusicVolumeSlider? = null

    private val musicManager = SigmaMusicManager

    // 动画
    private var panelOpacity = 0f

    init {
        // 初始化所有源 tabs
        initializeTabs()
    }

    private fun initializeTabs() {
        tabs.clear()
        tabs.add(SigmaMusicTabButton("search", 0f, 0f, TAB_WIDTH.toFloat(), TAB_HEIGHT.toFloat(), "Search") { btn ->
            showSearchBox = true
            currentTab = btn
        })
        tabs.add(SigmaMusicTabButton("bundled", 0f, 0f, TAB_WIDTH.toFloat(), TAB_HEIGHT.toFloat(), "Bundled Music") { btn ->
            showSearchBox = false
            currentTab = btn
            loadBundledMusic()
        })
        tabs.add(SigmaMusicTabButton("local", 0f, 0f, TAB_WIDTH.toFloat(), TAB_HEIGHT.toFloat(), "Local Music") { btn ->
            showSearchBox = false
            currentTab = btn
            loadLocalMusic()
        })
        tabs.add(SigmaMusicTabButton("netease_hot", 0f, 0f, TAB_WIDTH.toFloat(), TAB_HEIGHT.toFloat(), "网易云热歌") { btn ->
            showSearchBox = false
            currentTab = btn
            loadNeteaseHot()
        })
        tabs.add(SigmaMusicTabButton("netease_new", 0f, 0f, TAB_WIDTH.toFloat(), TAB_HEIGHT.toFloat(), "网易云新歌") { btn ->
            showSearchBox = false
            currentTab = btn
            loadNeteaseNew()
        })
        // 默认选中第一个非搜索 tab
        currentTab = tabs[1]
        currentTab?.selected = true
    }

    override fun initGui() {
        val sr = ScaledResolution(mc)
        val screenWidth = sr.scaledWidth
        val screenHeight = sr.scaledHeight

        // 响应式面板位置（保持 800x600 尺寸）
        panelX = ((screenWidth - PANEL_WIDTH) / 2f).coerceAtLeast(0f)
        panelY = ((screenHeight - PANEL_HEIGHT) / 2f).coerceAtLeast(0f)

        // 重新定位 tabs
        tabs.forEachIndexed { i, tab ->
            tab.x = panelX
            tab.y = panelY + HEADER_HEIGHT + 14 + i * TAB_HEIGHT
        }

        // 初始化搜索框
        searchBox = SigmaMusicSearchBox(
            panelX + TAB_WIDTH, panelY + HEADER_HEIGHT,
            (PANEL_WIDTH - TAB_WIDTH).toFloat(), HEADER_HEIGHT.toFloat(),
            "Search..."
        )

        // 初始化进度条（位于内容区底部、控制栏上方）
        progressBar = SigmaMusicProgressBar(
            panelX + TAB_WIDTH, panelY + PANEL_HEIGHT - BOTTOM_BAR_HEIGHT - 5,
            (PANEL_WIDTH - TAB_WIDTH).toFloat(), 5f
        )

        // 初始化音量滑块（右侧）
        volumeSlider = SigmaMusicVolumeSlider(
            panelX + PANEL_WIDTH - 19f, panelY + PANEL_HEIGHT - BOTTOM_BAR_HEIGHT + 14,
            4f, 40f
        )

        // 加载默认音乐
        if (currentTracks.isEmpty()) {
            loadBundledMusic()
        }

        panelOpacity = 0f
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // 背景虚化
        drawDefaultBackground()

        // 面板透明度动画
        panelOpacity = min(1f, panelOpacity + partialTicks * 4f)
        val alpha = (panelOpacity * 255).toInt().coerceIn(0, 255)

        // 主面板背景（深色，14px 圆角，Sigma 风格）
        val panelBg = (alpha shl 24) or (0x131313 and 0x00FFFFFF)
        RenderUtils.drawRoundedRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 14f, panelBg)

        // 绘制内容
        drawHeader()
        drawTabs(mouseX, mouseY)
        drawContentArea(mouseX, mouseY)
        drawBottomBar(mouseX, mouseY)
    }

    private fun drawHeader() {
        // "Jello" 大字体 (Helvetica 40px)
        SigmaMusicTextHelper.drawString(
            "Jello",
            panelX + 55, panelY + 20,
            SigmaMusicResources.LIGHT_GREYISH_BLUE,
            SigmaMusicResources.helveticaLight40(), 40f
        )
        // "music" 小字体 (Helvetica 20px)
        SigmaMusicTextHelper.drawString(
            "music",
            panelX + 55 + 80, panelY + 40,
            SigmaMusicResources.LIGHT_GREYISH_BLUE,
            SigmaMusicResources.helveticaLight20(), 20f
        )

        // 标题分隔线
        RenderUtils.drawRect(panelX, panelY + HEADER_HEIGHT, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT + 1, 0x30FFFFFF)
    }

    private fun drawTabs(mouseX: Int, mouseY: Int) {
        tabs.forEach { tab ->
            tab.updateHover(mouseX, mouseY)
            tab.draw(0f)
        }
    }

    private fun drawContentArea(mouseX: Int, mouseY: Int) {
        val contentX = panelX + TAB_WIDTH
        val contentY = panelY + HEADER_HEIGHT + 14
        val contentWidth = PANEL_WIDTH - TAB_WIDTH
        val contentHeight = PANEL_HEIGHT - HEADER_HEIGHT - BOTTOM_BAR_HEIGHT - 28

        if (showSearchBox && searchBox != null) {
            searchBox?.draw(0f)
            // 搜索结果显示
            val filtered = if (searchBox?.text?.isNotEmpty() == true) {
                allTracks.filter { it.title.contains(searchBox!!.text, ignoreCase = true) }
            } else allTracks
            drawThumbnailGrid(filtered, contentX, contentY, mouseX, mouseY)
        } else {
            drawThumbnailGrid(currentTracks, contentX, contentY, mouseX, mouseY)
        }
    }

    private fun drawThumbnailGrid(
        tracks: List<SongInfo>,
        x: Float, y: Float,
        mouseX: Int, mouseY: Int
    ) {
        thumbnailButtons.clear()

        tracks.forEachIndexed { index, track ->
            val col = index % 3
            val row = index / 3

            val tx = x + GRID_PADDING_X + col * GRID_X_STEP
            val ty = y + GRID_PADDING_Y + row * GRID_Y_STEP

            // 只在可见范围内绘制
            if (ty + GRID_THUMBNAIL_HEIGHT < y) return@forEachIndexed
            if (ty > y + (PANEL_HEIGHT - HEADER_HEIGHT - BOTTOM_BAR_HEIGHT - 28)) return@forEachIndexed

            val btn = SigmaMusicThumbnailButton(track, tx, ty) {
                playSong(track)
            }
            btn.updateHover(mouseX, mouseY)
            btn.draw(0f)
            btn.loadCoverAsync()
            thumbnailButtons.add(btn)
        }
    }

    private fun drawBottomBar(mouseX: Int, mouseY: Int) {
        val barY = panelY + PANEL_HEIGHT - BOTTOM_BAR_HEIGHT
        val barX = panelX
        val barW = PANEL_WIDTH.toFloat()

        // 控制栏背景
        RenderUtils.drawRect(barX, barY, barX + barW, panelY + PANEL_HEIGHT, SigmaMusicResources.DEEP_TEAL)
        RenderUtils.drawRect(barX, barY, barX + barW, barY + 1, 0x30FFFFFF)

        // 专辑封面（左侧 114x114）
        drawImage(
            SigmaMusicResources.artworkImage,
            barX + (TAB_WIDTH - ALBUM_SIZE) / 2f, barY + (BOTTOM_BAR_HEIGHT - ALBUM_SIZE) / 2f - 10,
            ALBUM_SIZE.toFloat(), ALBUM_SIZE.toFloat(),
            SigmaMusicResources.LIGHT_GREYISH_BLUE
        )

        // 播放控制按钮
        val controlCenterX = barX + (TAB_WIDTH + (barW - TAB_WIDTH) / 2f)
        val playY = barY + 27

        val isPlaying = musicManager.isPlaying
        val playIcon = if (isPlaying) SigmaMusicResources.pauseIcon else SigmaMusicResources.playIcon
        drawImage(playIcon, controlCenterX - 19, playY, 38f, 38f, SigmaMusicResources.LIGHT_GREYISH_BLUE)

        drawImage(
            SigmaMusicResources.forwardsIcon,
            controlCenterX + 95, barY + 23, 46f, 46f, SigmaMusicResources.LIGHT_GREYISH_BLUE
        )
        drawImage(
            SigmaMusicResources.backwardsIcon,
            controlCenterX - 141, barY + 23, 46f, 46f, SigmaMusicResources.LIGHT_GREYISH_BLUE
        )

        // 循环按钮（左侧）
        drawImage(SigmaMusicResources.repeatIcon, barX + 14, barY + 34, 27f, 20f, SigmaMusicResources.LIGHT_GREYISH_BLUE)

        // 音量滑块（右侧）
        volumeSlider?.draw(0f)

        // 进度条
        progressBar?.let { pb ->
            val dur = musicManager.durationMs
            pb.progress = if (dur > 0) {
                (musicManager.currentPositionMs.toFloat() / dur).coerceIn(0f, 1f)
            } else 0f
            pb.draw(0f)
        }

        // 进度时间文本
        val elapsed = formatTime(musicManager.currentPositionMs)
        val total = formatTime(musicManager.durationMs)
        val font14 = SigmaMusicResources.helveticaLight14()
        SigmaMusicTextHelper.drawString(
            elapsed, barX + TAB_WIDTH + 14, panelY + PANEL_HEIGHT - 32,
            SigmaMusicResources.LIGHT_GREYISH_BLUE, font14, 14f
        )
        val totalWidth = SigmaMusicTextHelper.getStringWidth(total, font14, 14f)
        SigmaMusicTextHelper.drawString(
            total, barX + barW - 14 - totalWidth, panelY + PANEL_HEIGHT - 32,
            SigmaMusicResources.LIGHT_GREYISH_BLUE, font14, 14f
        )

        // 当前播放歌曲标题（专辑封面下方）
        val songTitle = musicManager.songTitle.ifEmpty { "Jello Music" }
        SigmaMusicTextHelper.drawString(
            truncateText(songTitle, 14, TAB_WIDTH - 30),
            barX + 15, barY + 20,
            SigmaMusicResources.LIGHT_GREYISH_BLUE,
            SigmaMusicResources.helveticaLight14(), 14f
        )
    }

    private fun playSong(track: SongInfo) {
        musicManager.playPlaylist(listOf(track), 0)
    }

    // 音乐源加载
    private fun loadBundledMusic() {
        currentTracks.clear()
        // 内置音乐 - 从运行目录 music 加载
        val bundledDir = java.io.File("music")
        if (bundledDir.exists() && bundledDir.isDirectory) {
            bundledDir.listFiles { f -> f.extension == "mp3" || f.extension == "nbs" }?.forEach { file ->
                currentTracks.add(SongInfo.fromLocalFile(file))
            }
        }
        // 占位
        if (currentTracks.isEmpty()) {
            currentTracks.add(SongInfo(title = "Remember This", artist = "Sigma Client", durationMs = 180000L))
        }
        allTracks.clear()
        allTracks.addAll(currentTracks)
    }

    private fun loadLocalMusic() {
        currentTracks.clear()
        val localDir = java.io.File(mc.mcDataDir, "music")
        if (localDir.exists() && localDir.isDirectory) {
            localDir.listFiles { f -> f.extension == "mp3" || f.extension == "nbs" }?.forEach { file ->
                currentTracks.add(SongInfo.fromLocalFile(file))
            }
        }
        allTracks.clear()
        allTracks.addAll(currentTracks)
    }

    private fun loadNeteaseHot() {
        currentTracks.clear()
        Thread {
            val tracks = try {
                NeteaseApiSearch.search("热歌", limit = 30)
            } catch (_: Exception) { emptyList() }
            tracks.forEach { currentTracks.add(SongInfo.fromNeteaseTrack(it)) }
            allTracks.clear()
            allTracks.addAll(currentTracks)
        }.start()
        repeat(6) { i ->
            currentTracks.add(SongInfo(title = "网易云热歌 ${i + 1}", artist = "Loading..."))
        }
    }

    private fun loadNeteaseNew() {
        currentTracks.clear()
        Thread {
            val tracks = try {
                NeteaseApiSearch.search("新歌", limit = 30)
            } catch (_: Exception) { emptyList() }
            tracks.forEach { currentTracks.add(SongInfo.fromNeteaseTrack(it)) }
            allTracks.clear()
            allTracks.addAll(currentTracks)
        }.start()
        repeat(6) { i ->
            currentTracks.add(SongInfo(title = "网易云新歌 ${i + 1}", artist = "Loading..."))
        }
    }

    // 输入处理
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            return
        }
        if (searchBox?.keyTyped(typedChar, keyCode) == true) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // 标题栏拖动
        if (mouseButton == 0 && isInHeader(mouseX, mouseY) && !isInTabArea(mouseX, mouseY)) {
            dragging = true
            dragOffsetX = mouseX - panelX
            dragOffsetY = mouseY - panelY
            dragX = panelX
            dragY = panelY
            return
        }

        // Tabs
        tabs.forEach { tab ->
            if (tab.mouseClicked(mouseX, mouseY, mouseButton)) {
                tabs.forEach { it.selected = (it == tab) }
                return
            }
        }

        // 搜索框
        searchBox?.mouseClicked(mouseX, mouseY, mouseButton)

        // 缩略图
        thumbnailButtons.forEach { btn ->
            if (btn.mouseClicked(mouseX, mouseY, mouseButton)) return
        }

        // 控制栏按钮（粗略判断）
        if (isInBottomBar(mouseX, mouseY)) {
            handleBottomBarClick(mouseX, mouseY, mouseButton)
            return
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun handleBottomBarClick(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val barY = panelY + PANEL_HEIGHT - BOTTOM_BAR_HEIGHT
        val barX = panelX
        val barW = PANEL_WIDTH.toFloat()
        val controlCenterX = barX + (TAB_WIDTH + (barW - TAB_WIDTH) / 2f)

        // 播放/暂停
        if (isInRect(mouseX, mouseY, controlCenterX - 19, barY + 27, 38f, 38f)) {
            if (musicManager.isPlaying) musicManager.pause() else musicManager.resume()
            return
        }
        // 下一首
        if (isInRect(mouseX, mouseY, controlCenterX + 95, barY + 23, 46f, 46f)) {
            musicManager.next()
            return
        }
        // 上一首
        if (isInRect(mouseX, mouseY, controlCenterX - 141, barY + 23, 46f, 46f)) {
            musicManager.previous()
            return
        }
        // 循环
        if (isInRect(mouseX, mouseY, barX + 14, barY + 34, 27f, 20f)) {
            // Sigma 循环模式切换: NO_REPEAT -> REPEAT -> LOOP_CURRENT
            musicManager.setRepeatMode(musicManager.getRepeatMode().getNext())
            return
        }
        // 进度条
        progressBar?.mouseClicked(mouseX, mouseY, mouseButton)
        // 音量
        volumeSlider?.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun isInRect(mx: Int, my: Int, x: Float, y: Float, w: Float, h: Float): Boolean {
        return mx.toFloat() in x..(x + w) && my.toFloat() in y..(y + h)
    }

    private fun isInHeader(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in panelX..(panelX + PANEL_WIDTH) &&
            mouseY.toFloat() in panelY..(panelY + HEADER_HEIGHT)
    }

    private fun isInTabArea(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in panelX..(panelX + TAB_WIDTH) &&
            mouseY.toFloat() in (panelY + HEADER_HEIGHT)..(panelY + PANEL_HEIGHT - BOTTOM_BAR_HEIGHT)
    }

    private fun isInBottomBar(mouseX: Int, mouseY: Int): Boolean {
        return mouseX.toFloat() in panelX..(panelX + PANEL_WIDTH) &&
            mouseY.toFloat() in (panelY + PANEL_HEIGHT - BOTTOM_BAR_HEIGHT)..(panelY + PANEL_HEIGHT)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging = false
        progressBar?.mouseReleased()
        volumeSlider?.mouseReleased()
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (dragging) {
            val sr = ScaledResolution(mc)
            val maxX = sr.scaledWidth - PANEL_WIDTH
            val maxY = sr.scaledHeight - PANEL_HEIGHT
            panelX = (mouseX - dragOffsetX).coerceIn(0f, maxX.toFloat())
            panelY = (mouseY - dragOffsetY).coerceIn(0f, maxY.toFloat())
            // 重新定位子元素
            initGui()
        }
        if (progressBar?.dragging == true) progressBar?.updateProgress(mouseX)
        if (volumeSlider?.dragging == true) {
            volumeSlider?.updateVolume(mouseY)
            // Sigma 音量映射：1.0=最大(100), 0.0=静音(0)
            musicManager.setVolume(((1f - (volumeSlider?.volume ?: 0.5f)) * 100f).toInt())
        }
    }

    override fun doesGuiPauseGame(): Boolean = false

    override fun onGuiClosed() {
        SigmaMusicTextHelper.clearCache()
        super.onGuiClosed()
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "$min:${sec.toString().padStart(2, '0')}"
    }

    private fun truncateText(text: String, fontSize: Float, maxWidth: Int): String {
        if (text.isEmpty()) return ""
        val font = SigmaMusicResources.helveticaLight14()
        val measuredWidth = SigmaMusicTextHelper.getStringWidth(text, font, fontSize)
        if (measuredWidth <= maxWidth) return text
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (SigmaMusicTextHelper.getStringWidth(text.substring(0, mid), font, fontSize) <= maxWidth - 12) lo = mid else hi = mid - 1
        }
        return if (lo > 0) text.substring(0, lo) + "..." else ""
    }
}
