/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.grim

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S12PacketEntityVelocity

/**
 * Skidded from edFDP GrimVerticalVelocity2.
 * Like V1 but only requires serverSprintState; uses ShitCode flag + optional CallEvent.
 * motionX/Z *= 0.07776
 */
class GrimVerticalVelocity2 : VelocityMode("GrimVertical2") {
    private var attack = false

    private val swingValue = BoolValue("${valuePrefix}Swing", false)
    private val sendc0fValue = BoolValue("${valuePrefix}C0F", false)
    private val c0fPacketAmount = IntegerValue("${valuePrefix}C0FPacketAmount", 0, 1, 20)
        .displayable { sendc0fValue.get() }
    private val c02PacketAmount = IntegerValue("${valuePrefix}C02C0APacketAmount", 0, 1, 20)
    private val swingAmount = IntegerValue("${valuePrefix}SwingAmount", 1, 1, 10)
        .displayable { swingValue.get() }
    private val callEvent = BoolValue("${valuePrefix}CallEvent", false)

    override fun onUpdate(event: UpdateEvent) {
        if (!attack) return
        val player = mc.thePlayer ?: return
        FDPNext.shitCode.setAttack(true)
        val killAura = FDPNext.moduleManager[KillAura::class.java]!!
        repeat(c02PacketAmount.get()) {
            val target = killAura.currentTarget ?: return@repeat
            attackEntity(target)
        }
        player.motionX *= 0.07776
        player.motionZ *= 0.07776
        velocity.velocityInput = false
        attack = false
        FDPNext.shitCode.setAttack(false)
    }

    private fun runSwing() {
        mc.thePlayer.swingItem()
    }

    private fun attackEntity(entity: EntityLivingBase) {
        val event = AttackEvent(entity as Entity)
        if (callEvent.get()) {
            FDPNext.eventManager.callEvent(event)
            if (event.isCancelled) return
        }
        runSwing()
        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
    }

    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        val player = mc.thePlayer ?: return
        if (packet.motionX == 0 && packet.motionZ == 0) return
        val world = mc.theWorld ?: return
        val entity = world.getEntityByID(packet.entityID) ?: return
        if (entity != player) return

        velocity.velocityInput = true

        val killAura = FDPNext.moduleManager[KillAura::class.java]!!
        if (!killAura.state || killAura.currentTarget == null) return
        val target = killAura.currentTarget!!
        if (player.getDistanceToEntityBox(target) > 3.0) return
        if (player.serverSprintState) {
            repeat(c0fPacketAmount.get()) {
                if (sendc0fValue.get()) {
                    mc.netHandler.addToSendQueue(C0FPacketConfirmTransaction(100, 100, true))
                }
            }
            repeat(swingAmount.get()) {
                if (swingValue.get()) mc.thePlayer.swingItem()
            }
            attack = true
        }
    }
}
