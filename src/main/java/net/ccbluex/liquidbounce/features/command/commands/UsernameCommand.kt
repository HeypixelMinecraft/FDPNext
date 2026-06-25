/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

class UsernameCommand : Command("username", arrayOf("name")) {
    override fun execute(args: Array<String>) {
        alert("Username: " + mc.thePlayer.name)
    }
}