package net.ccbluex.liquidbounce.ui.sound

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.utils.FileUtils
import java.io.File

class TipSoundManager {
    var enableSound: TipSoundPlayer
    var disableSound: TipSoundPlayer

    init {
        val enableSoundFile = File(FDPNext.fileManager.soundsDir, "enable.wav")
        val disableSoundFile = File(FDPNext.fileManager.soundsDir, "disable.wav")

        if (!enableSoundFile.exists()) {
            FileUtils.unpackFile(enableSoundFile, "assets/minecraft/FDPNext/sound/enable.wav")
        }
        if (!disableSoundFile.exists()) {
            FileUtils.unpackFile(disableSoundFile, "assets/minecraft/FDPNext/sound/disable.wav")
        }

        enableSound = TipSoundPlayer(enableSoundFile)
        disableSound = TipSoundPlayer(disableSoundFile)
    }
}