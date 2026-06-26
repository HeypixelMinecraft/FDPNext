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
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.Vec3
import org.apache.commons.lang3.RandomUtils.nextInt

/**
 * Skidded from edFDP GrimVerticalVelocity5.
 * Most configurable variant: smartVelo (dynamic motion multiplier), spoofJump,
 * sprintSpoof (START/STOP_SPRINTING swap), playerJump, callEvent, random C0F.
 */
class GrimVerticalVelocity5 : VelocityMode("GrimVertical5") {
    private var attack = false

    private val smartVelo = BoolValue("${valuePrefix}SmartVelo", false)
    private val sendc0fValue = BoolValue("${valuePrefix}C0F", false)
    private val c0fPacketAmount = IntegerValue("${valuePrefix}C0FPacketAmount", 0, 1, 20)
        .displayable { sendc0fValue.get() }
    private val c02PacketAmount = IntegerValue("${valuePrefix}C02PacketAmount", 0, 1, 20)
    private val sprintSpoof = BoolValue("${valuePrefix}SpoofSprint", false)
    private val playerJump = BoolValue("${valuePrefix}PlayerJump", false)
    private val spoofJump = BoolValue("${valuePrefix}SpoofJump", false)
    private val callEvent = BoolValue("${valuePrefix}CallEvent", false)

    private var motionXZ = 0.01
    private var lastSprinting = false

    override fun onEnable() {
        motionXZ = 0.01
    }

    override fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer
        if (attack) {
            val killAura = FDPNext.moduleManager[KillAura::class.java]!!
            repeat(c02PacketAmount.get()) {
                val target = killAura.currentTarget ?: return@repeat
                attackEntity(target)
            }
            if (smartVelo.get()) {
                player.motionX *= motionXZ
                player.motionZ *= motionXZ
            } else {
                player.motionX *= 0.07776
                player.motionZ *= 0.07776
            }
            Velocity.velocityInput = false
            attack = false
        } else if (player.hurtTime == 6 && player.onGround && !mc.gameSettings.keyBindJump.isKeyDown && spoofJump.get()) {
            mc.thePlayer.movementInput.jump = true
        }
    }

    private fun runSwing() {
        mc.netHandler.addToSendQueue(C0APacketAnimation())
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

    private fun getMotionNoXZ(packet: S12PacketEntityVelocity): Double {
        val strength = Vec3(packet.motionX.toDouble(), packet.motionY.toDouble(), packet.motionZ.toDouble()).lengthVector()
        return when {
            strength >= 20000.0 -> if (mc.thePlayer.onGround) 0.06425 else 0.075
            strength >= 5000.0 -> if (mc.thePlayer.onGround) 0.02625 else 0.0552
            else -> 0.0175
        }
    }

    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            val player = mc.thePlayer ?: return
            if (packet.motionX == 0 && packet.motionZ == 0) return
            val world = mc.theWorld ?: return
            val entity = world.getEntityByID(packet.entityID) ?: return
            if (entity != player) return

            if (player.onGround && player.hurtTime > 0 && playerJump.get()) {
                player.jump()
            }
            Velocity.velocityInput = true
            motionXZ = getMotionNoXZ(packet)

            val killAura = FDPNext.moduleManager[KillAura::class.java]!!
            if (!killAura.state || killAura.currentTarget == null) return
            val target = killAura.currentTarget!!
            if (player.getDistanceToEntityBox(target) > 3.0) return

            if (player.isSprinting && player.serverSprintState && MovementUtils.isMoving()) {
                repeat(c0fPacketAmount.get()) {
                    if (sendc0fValue.get()) {
                        mc.netHandler.addToSendQueue(C0FPacketConfirmTransaction(
                            nextInt(102, 1000024123), nextInt(102, 1000024123).toShort(), true
                        ))
                    }
                }
                attack = true
            } else if (sprintSpoof.get()) {
                repeat(c0fPacketAmount.get()) {
                    if (sendc0fValue.get()) {
                        mc.netHandler.addToSendQueue(C0FPacketConfirmTransaction(
                            nextInt(102, 1000024123), nextInt(102, 1000024123).toShort(), true
                        ))
                    }
                }
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                mc.thePlayer.setSprinting(false)
                attack = true
                mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            }
        } else if (packet is C0BPacketEntityAction && Velocity.velocityInput && sprintSpoof.get()) {
            val action = packet.getAction()
            if (action == C0BPacketEntityAction.Action.START_SPRINTING) {
                if (lastSprinting) FDPNext.eventManager.callEvent(event)
                lastSprinting = true
            } else if (action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                if (!lastSprinting) FDPNext.eventManager.callEvent(event)
                lastSprinting = false
            }
        }
    }
}
