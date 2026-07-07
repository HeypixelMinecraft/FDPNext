package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.gui.music.GuiMusicPlayer
import org.lwjgl.input.Keyboard

object MusicPlayerModule : Module(
    name = "MusicPlayer",
    category = ModuleCategory.CLIENT,
    keyBind = Keyboard.KEY_M,
    canEnable = false
) {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.displayGuiScreen(GuiMusicPlayer())
        state = false
    }
}
