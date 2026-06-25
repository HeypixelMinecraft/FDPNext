/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.features.command.Command

class PrefixCommand : Command("prefix", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("prefix <character>")
            return
        }

        val prefix = args[1]

        if (prefix.length > 1) {
            alert("§cPrefix can only be one character long!")
            return
        }

        FDPNext.commandManager.prefix = prefix.single()
        FDPNext.fileManager.saveConfig(FDPNext.fileManager.specialConfig)

        alert("Successfully changed command prefix to '§8$prefix§3'")
    }
}