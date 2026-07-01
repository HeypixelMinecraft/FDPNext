/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.grim

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S12PacketEntityVelocity

/**
 * Skidded from SilenceFix GrimVelocity.
 * Old Grim AC bypass via multi-attack + cancel.
 *
 * Cancels S12 velocity, sends 6 C0A+C02 attack packets in rapid succession
 * to trigger Grim's attack-reduce velocity, then applies custom reduced motion.
 */
class GrimSilenceFix : VelocityMode("GrimSilenceFix") {
    private var velocityReceived = false
    private var target: EntityLivingBase? = null
    private var needRestoreSprint = false
    private var savedVelocityPacket: S12PacketEntityVelocity? = null

    private val attackCountValue = IntegerValue("${valuePrefix}AttackCount", 6, 1, 15)
    private val horizontalValue = BoolValue("${valuePrefix}Horizontal", true)
    private val horizontalFactorValue = FloatValue("${valuePrefix}HorFactor", 0.07765f, 0f, 1f)
    private val verticalValue = BoolValue("${valuePrefix}Vertical", false)
    private val sendC0AValue = BoolValue("${valuePrefix}SendC0A", true)

    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        val player = mc.thePlayer ?: return
        if (packet.motionX == 0 && packet.motionZ == 0 && packet.motionY == 0) return
        val world = mc.theWorld ?: return
        val entity = world.getEntityByID(packet.entityID) ?: return
        if (entity != player) return

        val killAura = FDPNext.moduleManager[KillAura::class.java]!!
        val auraTarget = killAura.currentTarget
        if (!killAura.state || auraTarget == null) return
        if (player.getDistanceToEntityBox(auraTarget) > 3.5) return

        target = auraTarget
        savedVelocityPacket = packet
        velocityReceived = true
        event.cancelEvent()
    }

    override fun onUpdate(event: UpdateEvent) {
        if (!velocityReceived) return
        val player = mc.thePlayer ?: return
        val t = target ?: return
        velocityReceived = false
        target = null

        // Track original sprint state for restoration
        needRestoreSprint = !player.serverSprintState

        // If not sprinting, spoof START_SPRINTING
        if (needRestoreSprint) {
            player.sendQueue.addToSendQueue(
                C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING)
            )
        }

        // Multi-attack: send C0A + C02 in rapid succession
        val count = attackCountValue.get()
        repeat(count) {
            if (sendC0AValue.get()) {
                player.sendQueue.addToSendQueue(C0APacketAnimation())
            }
            player.sendQueue.addToSendQueue(
                C02PacketUseEntity(t, C02PacketUseEntity.Action.ATTACK)
            )
        }

        // Set sprinting true server-side
        player.isSprinting = true

        // Apply reduced velocity
        val packet = savedVelocityPacket
        if (packet != null) {
            val motionX = (packet.motionX / 8000.0) * if (horizontalValue.get()) horizontalFactorValue.get().toDouble() else 1.0
            val motionY = (packet.motionY / 8000.0) * if (verticalValue.get()) horizontalFactorValue.get().toDouble() else 1.0
            val motionZ = (packet.motionZ / 8000.0) * if (horizontalValue.get()) horizontalFactorValue.get().toDouble() else 1.0
            player.motionX = motionX
            player.motionY = motionY
            player.motionZ = motionZ
            savedVelocityPacket = null
        }

        // Restore sprint state if it was changed
        if (needRestoreSprint) {
            player.sendQueue.addToSendQueue(
                C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING)
            )
        }

        velocity.velocityInput = false
    }
}