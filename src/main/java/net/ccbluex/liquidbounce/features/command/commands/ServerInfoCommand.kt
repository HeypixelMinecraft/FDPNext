/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

class ServerInfoCommand : Command("serverinfo", arrayOf("si")) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (mc.currentServerData == null) {
            alert("This command does not work in single player.")
            return
        }

        val data = mc.currentServerData ?: return

        alert("Server infos:")
        alert("§7Name: §8${data.serverName}")
        alert("§7IP: §8${data.serverIP}")
        alert("§7Players: §8${data.populationInfo}")
        alert("§7MOTD: §8${data.serverMOTD}")
        alert("§7ServerVersion: §8${data.gameVersion}")
        alert("§7ProtocolVersion: §8${data.version}")
        alert("§7Ping: §8${data.pingToServer}")
    }
}