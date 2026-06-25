/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.features.command.Command

class FakeNameCommand : Command("SetFakeName", emptyArray()){
    override fun execute(args: Array<String>) {
        if(args.size > 2) {
            val module = FDPNext.moduleManager.getModule(args[1]) ?: return
            module.name = args[2]
        } else
            chatSyntax("SetFakeName <Module> <Name>")
    }
    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size) {
            1 -> FDPNext.moduleManager.modules
                    .map { it.name }
                    .filter { it.startsWith(moduleName, true) }
                    .toList()
            else -> emptyList()
        }
    }
}