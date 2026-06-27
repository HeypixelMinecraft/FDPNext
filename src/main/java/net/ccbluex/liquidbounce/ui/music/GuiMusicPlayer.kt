/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music

import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.ui.music.MusicPlayer as Player
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * 音乐播放器 GUI 界面
 */
class GuiMusicPlayer(private val prevGui: GuiScreen?) : GuiScreen() {

    private lateinit var searchField: GuiTextField
    private val searchResultsList = mutableListOf<MusicTrack>()
    private var scrollOffset = 0
    private val listX = 10
    private val listY = 50
    private val listWidth = 380
    private val itemHeight = 32
    private val visibleItems = 8

    // 歌词面板
    private var lyricScroll = 0
    private val lyricVisibleLines = 12
    private val lyricLineHeight = 16

    private val sourceButtons = mutableListOf<GuiButton>()

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        searchField = GuiTextField(0, fontRendererObj, 10, 30, 300, 18)
        searchField.maxStringLength = 100
        searchField.isFocused = true

        buttonList.clear()
        // 源切换按钮
        sourceButtons.clear()
        var bx = 320
        MusicSource.values().forEachIndexed { i, src ->
            val btn = GuiButton(i + 1, bx, 28, 60, 20, src.displayName)
            sourceButtons.add(btn)
            buttonList.add(btn)
            bx += 64
        }

        // 控制按钮
        buttonList.add(GuiButton(10, 10, height - 70, 50, 20, "<<<"))
        buttonList.add(GuiButton(11, 65, height - 70, 80, 20, LanguageManager.get("ui.music.playPause")))
        buttonList.add(GuiButton(12, 150, height - 70, 50, 20, ">>>"))
        buttonList.add(GuiButton(13, 205, height - 70, 60, 20, LanguageManager.get("ui.music.stop")))
        buttonList.add(GuiButton(14, 270, height - 70, 60, 20, if (Player.loop) LanguageManager.get("ui.music.loopOn") else LanguageManager.get("ui.music.loopOff")))
        buttonList.add(GuiButton(0, width - 80, height - 70, 70, 20, LanguageManager.get("ui.back")))

        // 注册回调
        Player.onSearchComplete = { results ->
            searchResultsList.clear()
            searchResultsList.addAll(results)
            scrollOffset = 0
        }
        Player.onTrackChanged = { _ -> lyricScroll = 0 }
        Player.onPlayStateChanged = { }
        Player.onLyricsUpdated = { lyricScroll = 0 }

        // 初始化搜索结果
        searchResultsList.clear()
        searchResultsList.addAll(Player.searchResults)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        // 标题
        Fonts.font40.drawCenteredString(LanguageManager.get("ui.music.title"), width / 2f, 5f, Color.WHITE.rgb)

        // 当前播放状态
        val track = Player.currentTrack
        val stateText = when {
            Player.isPlaying -> LanguageManager.get("ui.music.playing")
            Player.isPaused -> LanguageManager.get("ui.music.paused")
            else -> LanguageManager.get("ui.music.stopped")
        }
        val trackText = if (track != null) "${track.name} - ${track.artist} [${track.source.displayName}]" else LanguageManager.get("ui.music.noTrack")
        Fonts.font35.drawString("$stateText | $trackText", 10, height - 95, Color.WHITE.rgb)

        // 进度条
        val progressBarX = 10
        val progressBarY = height - 82
        val progressBarWidth = width - 20
        RenderUtils.drawRoundedCornerRect(
            progressBarX.toFloat(), progressBarY.toFloat(),
            progressBarX + progressBarWidth.toFloat(), progressBarY + 8f,
            4f, Color(60, 60, 60).rgb
        )

        // 搜索框
        searchField.drawTextBox()
        Fonts.font35.drawString(LanguageManager.get("ui.music.search"), 10, 18, Color.WHITE.rgb)

        // 搜索结果列表
        drawSearchList(mouseX, mouseY)

        // 歌词面板
        drawLyricsPanel(mouseX, mouseY)

        // 源按钮高亮
        sourceButtons.forEachIndexed { i, btn ->
            btn.packedFGColour = if (Player.currentSource == MusicSource.values()[i]) Color(100, 200, 100).rgb else Color.WHITE.rgb
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawSearchList(mouseX: Int, mouseY: Int) {
        val listHeight = visibleItems * itemHeight
        // 背景
        RenderUtils.drawRoundedCornerRect(
            listX.toFloat(), listY.toFloat(),
            (listX + listWidth).toFloat(), (listY + listHeight).toFloat(),
            4f, Color(30, 30, 30, 200).rgb
        )

        // 列表项
        for (i in 0 until visibleItems) {
            val idx = i + scrollOffset
            if (idx >= searchResultsList.size) break
            val track = searchResultsList[idx]
            val y = listY + i * itemHeight

            val isCurrent = (track == Player.currentTrack)
            val isHover = mouseX in listX..(listX + listWidth) && mouseY in y..(y + itemHeight)

            if (isCurrent) RenderUtils.drawRoundedCornerRect(
                listX.toFloat(), y.toFloat(),
                (listX + listWidth).toFloat(), (y + itemHeight).toFloat(),
                2f, Color(80, 80, 200, 120).rgb
            )
            else if (isHover) RenderUtils.drawRoundedCornerRect(
                listX.toFloat(), y.toFloat(),
                (listX + listWidth).toFloat(), (y + itemHeight).toFloat(),
                2f, Color(255, 255, 255, 40).rgb
            )

            // 索引号
            Fonts.font35.drawString("${idx + 1}.", listX + 4, y + 4, Color(180, 180, 180).rgb)
            // 曲名
            Fonts.font35.drawString(track.name, listX + 25, y + 4,
                if (isCurrent) Color(100, 200, 255).rgb else Color.WHITE.rgb)
            // 艺术家
            Fonts.font40.drawString(track.artist, listX + 25, y + 18, Color(180, 180, 180).rgb)
            // 来源
            Fonts.font35.drawString("[${track.source.displayName}]", listX + listWidth - 70, y + 8, Color(150, 150, 150).rgb)
        }

        // 搜索中提示
        if (Player.searching) {
            Fonts.font35.drawCenteredString(LanguageManager.get("ui.music.searching"), (listX + listWidth / 2).toFloat(), (listY + listHeight / 2).toFloat(), Color.YELLOW.rgb)
        } else if (searchResultsList.isEmpty()) {
            Fonts.font35.drawCenteredString(LanguageManager.get("ui.music.noResults"), (listX + listWidth / 2).toFloat(), (listY + listHeight / 2).toFloat(), Color.GRAY.rgb)
        }

        // 滚动条
        if (searchResultsList.size > visibleItems) {
            val scrollBarHeight = (visibleItems.toFloat() / searchResultsList.size * listHeight).toInt().coerceAtLeast(10)
            val scrollBarY = listY + (scrollOffset.toFloat() / searchResultsList.size * listHeight).toInt()
            RenderUtils.drawRoundedCornerRect(
                (listX + listWidth - 6).toFloat(), scrollBarY.toFloat(),
                (listX + listWidth - 2).toFloat(), (scrollBarY + scrollBarHeight).toFloat(),
                2f, Color(150, 150, 150).rgb
            )
        }
    }

    /**
     * 绘制歌词面板
     *
     * 布局：位于搜索列表右侧，宽度自适应到屏幕边缘。
     * - 网易云/酷狗：LRC 带时间戳，当前行高亮，自动滚动让当前行显示在中间。
     * - YouTube Music：纯文本无时间戳，所有行普通显示，仅支持手动滚动。
     */
    private fun drawLyricsPanel(mouseX: Int, mouseY: Int) {
        val panelX = listX + listWidth + 10
        val panelY = listY
        val panelWidth = (width - panelX - 10).coerceAtLeast(120)
        val panelHeight = visibleItems * itemHeight

        // 背景
        RenderUtils.drawRoundedCornerRect(
            panelX.toFloat(), panelY.toFloat(),
            (panelX + panelWidth).toFloat(), (panelY + panelHeight).toFloat(),
            4f, Color(30, 30, 30, 200).rgb
        )

        // 标题
        Fonts.font35.drawString(LanguageManager.get("ui.music.lyrics"), panelX + 6, panelY - 12, Color.WHITE.rgb)

        val lyrics = Player.currentLyrics

        // 加载中提示
        if (Player.fetchingLyrics) {
            Fonts.font35.drawCenteredString(
                LanguageManager.get("ui.music.lyricsLoading"),
                (panelX + panelWidth / 2).toFloat(),
                (panelY + panelHeight / 2).toFloat(),
                Color.YELLOW.rgb
            )
            return
        }

        // 无歌词提示
        if (lyrics.isEmpty()) {
            Fonts.font35.drawCenteredString(
                LanguageManager.get("ui.music.lyricsNone"),
                (panelX + panelWidth / 2).toFloat(),
                (panelY + panelHeight / 2).toFloat(),
                Color.GRAY.rgb
            )
            return
        }

        // 当前行索引（带时间戳歌词才同步高亮）
        val currentIdx = Player.currentLyricIndex()

        // 自动滚动：让当前行显示在面板中间
        if (currentIdx >= 0) {
            val target = (currentIdx - lyricVisibleLines / 2).coerceAtLeast(0).coerceAtMost((lyrics.size - lyricVisibleLines).coerceAtLeast(0))
            lyricScroll = target
        }

        // 裁剪到面板内部
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        val scale = mc.displayHeight.toFloat() / height.toFloat()
        GL11.glScissor(
            (panelX * scale).toInt(),
            (mc.displayHeight - (panelY + panelHeight) * scale).toInt(),
            (panelWidth * scale).toInt(),
            (panelHeight * scale).toInt()
        )

        // 绘制可见歌词行
        val startY = panelY + 4
        val textX = panelX + 8
        val maxTextWidth = panelWidth - 16
        for (i in 0 until lyricVisibleLines) {
            val idx = i + lyricScroll
            if (idx >= lyrics.size) break
            val line = lyrics[idx]
            val y = startY + i * lyricLineHeight
            val isCurrent = (idx == currentIdx)
            val color = when {
                isCurrent -> Color(255, 220, 80).rgb
                idx < currentIdx -> Color(140, 140, 140).rgb
                else -> Color(220, 220, 220).rgb
            }
            val displayText = clipText(line.text, maxTextWidth)
            // 当前行加阴影
            if (isCurrent) {
                Fonts.font35.drawString(displayText, textX + 1, y + 1, Color(0, 0, 0, 180).rgb)
            }
            Fonts.font35.drawString(displayText, textX, y, color)
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        // 滚动条
        if (lyrics.size > lyricVisibleLines) {
            val scrollBarHeight = (lyricVisibleLines.toFloat() / lyrics.size * panelHeight).toInt().coerceAtLeast(10)
            val scrollBarY = panelY + (lyricScroll.toFloat() / lyrics.size * panelHeight).toInt()
            RenderUtils.drawRoundedCornerRect(
                (panelX + panelWidth - 6).toFloat(), scrollBarY.toFloat(),
                (panelX + panelWidth - 2).toFloat(), (scrollBarY + scrollBarHeight).toFloat(),
                2f, Color(150, 150, 150).rgb
            )
        }
    }

    /**
     * 截断文本到指定宽度（超出加省略号）
     */
    private fun clipText(text: String, maxWidth: Int): String {
        if (Fonts.font35.getStringWidth(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && Fonts.font35.getStringWidth(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "..."
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            in 1..MusicSource.values().size -> {
                Player.switchSource(MusicSource.values()[button.id - 1])
            }
            10 -> Player.previous()
            11 -> {
                if (Player.isPlaying) Player.pause()
                else if (Player.isPaused) Player.resume()
                else if (Player.currentTrack != null) Player.playTrack(Player.currentTrack!!)
            }
            12 -> Player.next()
            13 -> Player.stop()
            14 -> {
                Player.loop = !Player.loop
                button.displayString = if (Player.loop) LanguageManager.get("ui.music.loopOn") else LanguageManager.get("ui.music.loopOff")
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        searchField.mouseClicked(mouseX, mouseY, mouseButton)

        // 点击列表项播放
        if (mouseButton == 0 && mouseX in listX..(listX + listWidth)) {
            for (i in 0 until visibleItems) {
                val idx = i + scrollOffset
                if (idx >= searchResultsList.size) break
                val y = listY + i * itemHeight
                if (mouseY in y..(y + itemHeight)) {
                    Player.playFromSearch(idx)
                    break
                }
            }
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val wheel = org.lwjgl.input.Mouse.getEventDWheel()
        if (wheel == 0) return
        val mouseX = org.lwjgl.input.Mouse.getEventX() * width / mc.displayWidth
        val mouseY = height - org.lwjgl.input.Mouse.getEventY() * height / mc.displayHeight - 1

        // 鼠标在搜索列表上：滚动搜索结果
        val listRight = listX + listWidth
        if (mouseX in listX..listRight && mouseY in listY..(listY + visibleItems * itemHeight)) {
            val maxScroll = (searchResultsList.size - visibleItems).coerceAtLeast(0)
            scrollOffset = if (wheel > 0) (scrollOffset - 1).coerceAtLeast(0)
            else (scrollOffset + 1).coerceAtMost(maxScroll)
            return
        }

        // 鼠标在歌词面板上：滚动歌词
        val panelX = listX + listWidth + 10
        val panelWidth = (width - panelX - 10).coerceAtLeast(120)
        val panelHeight = visibleItems * itemHeight
        if (mouseX in panelX..(panelX + panelWidth) && mouseY in listY..(listY + panelHeight)) {
            val lyricsSize = Player.currentLyrics.size
            val maxLyricScroll = (lyricsSize - lyricVisibleLines).coerceAtLeast(0)
            // 滚动幅度 3 行，更顺畅
            val step = 3
            lyricScroll = if (wheel > 0) (lyricScroll - step).coerceAtLeast(0)
            else (lyricScroll + step).coerceAtMost(maxLyricScroll)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (searchField.isFocused) {
            if (keyCode == Keyboard.KEY_RETURN) {
                Player.search(searchField.text.trim())
                return
            }
            searchField.textboxKeyTyped(typedChar, keyCode)
            return
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        Player.onSearchComplete = null
        Player.onTrackChanged = null
        Player.onPlayStateChanged = null
        Player.onLyricsUpdated = null
    }
}
