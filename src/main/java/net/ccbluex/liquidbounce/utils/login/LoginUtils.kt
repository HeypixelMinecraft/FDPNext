/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.login

import me.liuli.elixir.account.CrackedAccount
import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.util.Session

object LoginUtils : MinecraftInstance() {
    fun loginCracked(username: String) {
        mc.session = CrackedAccount().also { it.name = username }.session.let { Session(it.username, it.uuid, it.token, it.type) }
        FDPNext.eventManager.callEvent(SessionEvent())
    }

    fun randomCracked() {
        var name = GuiAltManager.randomAltField.text

        while (name.contains("%n") || name.contains("%s")) {
            if (name.contains("%n")) {
                name = name.replaceFirst("%n", RandomUtils.nextInt(0, 9).toString())
            }

            if (name.contains("%s")) {
                name = name.replaceFirst("%s", RandomUtils.randomString(1))
            }
        }

        loginCracked(name)
    }
}