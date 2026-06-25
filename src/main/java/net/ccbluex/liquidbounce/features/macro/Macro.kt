package net.ccbluex.liquidbounce.features.macro

import net.ccbluex.liquidbounce.FDPNext

class Macro(val key: Int, val command: String) {
    fun exec() {
        FDPNext.commandManager.executeCommands(command)
    }
}