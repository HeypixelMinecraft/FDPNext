/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.grim

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.util.EnumFacing
import net.minecraft.util.BlockPos

/**
 * Skidded from edFDP (mode name "GrimC07").
 * Cancels S12/S27 then sends C07 STOP_DESTROY_BLOCK to flag-dodge Grim.
 */
class GrimVelocity2 : VelocityMode("GrimC07") {
    private val alwaysValue = BoolValue("Always", true)
    private val onlyAirValue = BoolValue("OnlyBreakAir", true)
    private val worldValue = BoolValue("BreakOnWorld", false)
    private val sendC03Value = BoolValue("SendC03", false)
    private val c06Value = BoolValue("Send1.17C06", false)
    private val flagPauseValue = IntegerValue("FlagPause-Time", 50, 0, 5000)

    private var gotVelo = false
    private var flagTimer = MSTimer()

    override fun onEnable() {
        gotVelo = false
        flagTimer.reset()
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            flagTimer.reset()
        }
        if (!flagTimer.hasTimePassed(flagPauseValue.get().toLong())) {
            gotVelo = false
            return
        }
        if (packet is S12PacketEntityVelocity) {
            val id = packet.entityID
            val player = mc.thePlayer ?: return
            if (id == player.entityId) {
                event.cancelEvent()
                gotVelo = true
                return
            }
        }
        if (packet is S27PacketExplosion) {
            event.cancelEvent()
            gotVelo = true
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        if (!flagTimer.hasTimePassed(flagPauseValue.get().toLong())) {
            gotVelo = false
            return
        }
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        if (!gotVelo && !alwaysValue.get()) return
        val pos = BlockPos(player.posX, player.posY, player.posZ)
        if (checkBlock(pos) || checkBlock(pos.up())) {
            gotVelo = false
        }
    }

    private fun checkBlock(pos: BlockPos): Boolean {
        if (!onlyAirValue.get() || mc.theWorld.isAirBlock(pos)) {
            if (sendC03Value.get()) {
                if (c06Value.get()) {
                    mc.netHandler.addToSendQueue(C03PacketPlayer.C06PacketPlayerPosLook(
                        mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                        mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround
                    ))
                } else {
                    mc.netHandler.addToSendQueue(C03PacketPlayer(mc.thePlayer.onGround))
                }
            }
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.DOWN
            ))
            if (worldValue.get()) {
                mc.theWorld.setBlockToAir(pos)
            }
            return true
        }
        return false
    }
}
