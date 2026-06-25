/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.file.configs

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.ui.client.hud.Config
import java.io.File

class HudConfig(file: File) : FileConfig(file) {
    override fun loadConfig(config: String) {
        FDPNext.hud.clearElements()
        FDPNext.hud = Config(config).toHUD()
    }

    override fun saveConfig(): String {
        return Config(FDPNext.hud).toJson()
    }
}