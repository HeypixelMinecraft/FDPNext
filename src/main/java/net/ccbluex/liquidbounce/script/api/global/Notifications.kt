/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.script.api.global

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType

object Notifications {

    @Suppress("unused")
    @JvmStatic
    fun create(name: String?, content: String?, notify: String?, time: Int?) {
        var notifytype = NotifyType.WARNING
        when(notify?.lowercase()) {
            "success" -> notifytype = NotifyType.SUCCESS

            "info" -> notifytype = NotifyType.INFO

            "error" -> notifytype = NotifyType.ERROR

            "warning" -> notifytype = NotifyType.WARNING
        }
        FDPNext.hud.addNotification(Notification(name ?: "ScriptAPI", content ?: "Notification register failed", notifytype, time ?: 1000))
    }
}