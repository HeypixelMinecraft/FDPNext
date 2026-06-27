/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.command.commands

import de.florianmichael.vialoadingbase.ViaLoadingBase
import net.ccbluex.liquidbounce.features.command.Command

/**
 * Reports built-in multi-version (ViaMCP) status and how to use it.
 *
 * Java multi-version is built in via ViaMCP — pick the target version with the slider on the
 * multiplayer screen. Bedrock can't run in-process on Java 8; it goes through ViaProxy (docs).
 */
class ViaCommand : Command("via", arrayOf("viaversion", "multiversion")) {

    override fun execute(args: Array<String>) {
        alert("§bMulti-version (built-in ViaMCP)")

        val target = try {
            ViaLoadingBase.getInstance()?.targetVersion?.name
        } catch (_: Throwable) {
            null
        }

        if (target != null) {
            chat(" §7Target version: §a$target §7(change it with the slider on the multiplayer screen)")
        } else {
            chat(" §cViaMCP not initialized.")
        }

        val data = mc.currentServerData
        if (data != null) {
            chat(" §7Connected server: §8${data.gameVersion} §7(protocol §8${data.version}§7)")
        }

        alert("§bUsage")
        chat(" §7Java versions: set the target with the slider in the multiplayer screen, then connect.")
        chat(" §7Bedrock: run §8ViaProxy§7 (Java 21) locally and connect FDPNext to §8127.0.0.1§7.")
        chat(" §7Full guide: §8MULTIVERSION.md§7 in the repo.")
    }
}
