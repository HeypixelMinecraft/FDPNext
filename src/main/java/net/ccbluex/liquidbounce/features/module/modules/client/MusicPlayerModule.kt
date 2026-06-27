/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.music.GuiMusicPlayer
import net.ccbluex.liquidbounce.ui.music.MusicPlayer
import net.ccbluex.liquidbounce.ui.music.MusicSource

/**
 * 音乐播放器模块
 *
 * 提供游戏内快捷键控制和配置
 */
class MusicPlayerModule : Module(
    name = "MusicPlayer",
    category = ModuleCategory.CLIENT,
    defaultOn = false,
    keyBind = org.lwjgl.input.Keyboard.KEY_NONE
) {

    val sourceValue = ListValue("Source", arrayOf("Netease", "Kugou", "YoutubeMusic"), "Netease")
    val volumeValue = FloatValue("Volume", 0.5f, 0f, 1f)
    val loopValue = BoolValue("Loop", false)

    private var guiRequested = false

    override fun onEnable() {
        guiRequested = true
    }

    override fun onDisable() {
        // 停止播放但不保存配置
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        // 应用配置（每 tick 检查，确保值同步）
        MusicPlayer.setVolume(volumeValue.get())
        MusicPlayer.loop = loopValue.get()

        // 源切换
        val src = when (sourceValue.get()) {
            "Netease" -> MusicSource.NETEASE
            "Kugou" -> MusicSource.KUGOU
            "YoutubeMusic" -> MusicSource.YOUTUBE_MUSIC
            else -> MusicSource.NETEASE
        }
        if (MusicPlayer.currentSource != src) {
            MusicPlayer.switchSource(src)
        }

        // 打开 GUI（在主线程 tick 中执行，确保 Minecraft 上下文正确）
        if (guiRequested) {
            guiRequested = false
            mc.displayGuiScreen(GuiMusicPlayer(mc.currentScreen))
        }
    }
}
