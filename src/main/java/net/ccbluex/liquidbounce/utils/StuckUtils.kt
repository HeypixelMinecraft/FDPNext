/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.minecraft.network.play.client.C03PacketPlayer

/**
 * Stuck utility - sends position packets to keep player in place.
 */
object StuckUtils : Listenable, MinecraftInstance() {

    init {
        FDPNext.eventManager.registerListener(this)
    }

    var stucking = false
        private set

    fun stuck() {
        stucking = true
    }

    fun stopStuck() {
        stucking = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (stucking && mc.thePlayer != null) {
            mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
        }
    }

    override fun handleEvents() = true
}
