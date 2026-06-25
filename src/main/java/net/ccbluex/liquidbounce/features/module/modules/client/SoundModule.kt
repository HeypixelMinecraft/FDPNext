/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

object SoundModule : Module(name = "SoundModules", category = ModuleCategory.CLIENT, canEnable = false) {
    val toggleIgnoreScreenValue = BoolValue("ToggleIgnoreScreen", false)
    private val toggleSoundValue = ListValue("ToggleSound", arrayOf("None", "Click", "Custom"), "Click")

    fun playSound(enable: Boolean) {
        when (toggleSoundValue.get().lowercase()) {
            "click" -> {
                mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("random.click"), 1F))
            }

            "custom" -> {
                if (enable) {
                    FDPNext.tipSoundManager.enableSound.asyncPlay()
                } else {
                    FDPNext.tipSoundManager.disableSound.asyncPlay()
                }
            }
        }
    }
}