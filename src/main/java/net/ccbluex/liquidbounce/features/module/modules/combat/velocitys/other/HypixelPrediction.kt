package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.abs

class HypixelPrediction : VelocityMode("HypixelPrediction") {

    val reduce = BoolValue("${valuePrefix}Reduce", true)
    val reduceMode = ListValue("${valuePrefix}ReduceMode", arrayOf("Attack", "ReleaseWhenCanAttack", "ReleaseBeforeCanAttack"), "Attack")
        .displayable { reduce.get() }
    val extraAttack = BoolValue("${valuePrefix}ExtraAttack", false)
        .displayable { reduce.get() && reduceMode.get() != "Attack" }
    val reduceWhenCanAttack = BoolValue("${valuePrefix}ReduceWhenCanAttack", true)
        .displayable { reduce.get() && reduceMode.get() == "Attack" }
    val onlySprinting = BoolValue("${valuePrefix}OnlySprinting", true)
        .displayable { reduce.get() && reduceMode.get() == "Attack" }
    val attackTimes = IntegerValue("${valuePrefix}AttackTimes", 1, 1, 5)
        .displayable { reduce.get() && reduceMode.get() == "Attack" }
    val jump = BoolValue("${valuePrefix}Jump", true)
    val delay = BoolValue("${valuePrefix}Delay", false)
    val delayTicks = IntegerValue("${valuePrefix}DelayTicks", 1, 1, 5)
        .displayable { delay.get() && !airBuffer.get() }
    val airBuffer = BoolValue("${valuePrefix}DelayTillOnGround", true)
        .displayable { delay.get() }
    val groundDelay = BoolValue("${valuePrefix}GroundDelay", false)
        .displayable { delay.get() && !airBuffer.get() }
    val rotate = BoolValue("${valuePrefix}Rotate", false)
    val rotateTick = IntegerValue("${valuePrefix}RotateTicks", 3, 1, 12)
        .displayable { rotate.get() }
    val autoMove = BoolValue("${valuePrefix}AutoMove", false)
        .displayable { rotate.get() }
    val fakeCheck = BoolValue("${valuePrefix}FakeCheck", true)
    val debug = BoolValue("${valuePrefix}Debug", false)

    private var hasReceivedVelocity = false
    private var ticksSinceVelocity = -1
    private var reduceTick = 0
    private var knockbackX = 0.0
    private var knockbackZ = 0.0
    private var rotatoTickCounter = 0
    private var targetRotation: Rotation? = null
    private var delayFlag = false
    private var delayTimer = MSTimer()
    private var pressed = false
    private var velocityAttacked = false
    private var extraAttacked = false

    override fun onEnable() {
        resetState()
    }

    override fun onDisable() {
        resetState()
    }

    private fun resetState() {
        hasReceivedVelocity = false
        ticksSinceVelocity = -1
        reduceTick = 0
        knockbackX = 0.0
        knockbackZ = 0.0
        rotatoTickCounter = 0
        targetRotation = null
        delayFlag = false
        pressed = false
        velocityAttacked = false
        extraAttacked = false
    }

    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (mc.thePlayer == null) return
        if ((mc.theWorld?.getEntityByID(packet.entityID) ?: return) != mc.thePlayer) return

        if (!delay.get()) {
            hasReceivedVelocity = true
            ticksSinceVelocity = 0
        }

        if (rotate.get() && packet.getMotionY() > 0) {
            knockbackX = packet.getMotionX() / 8000.0
            knockbackZ = packet.getMotionZ() / 8000.0
            if (abs(knockbackX) > 0.01 || abs(knockbackZ) > 0.01) {
                rotatoTickCounter = 1
            }
        }

        if (delay.get() && !delayFlag) {
            val shouldDelay = (airBuffer.get() && !mc.thePlayer.onGround) ||
                    (delay.get() && !mc.thePlayer.onGround) ||
                    (groundDelay.get() && !airBuffer.get())

            if (shouldDelay) {
                delayFlag = true
                delayTimer.reset()
                dbg("Delay/Buffer Active")
                event.cancelEvent()
                return
            }
        }

        event.cancelEvent()
    }

    override fun onUpdate(event: UpdateEvent) {
        if (ticksSinceVelocity >= 0) {
            ticksSinceVelocity++
        }
        if (ticksSinceVelocity >= 10) {
            ticksSinceVelocity = -1
        }

        if (jump.get()) {
            if (mc.thePlayer == null) return
            if (ticksSinceVelocity >= 0) {
                if (ticksSinceVelocity == 0) {
                    pressed = mc.gameSettings.keyBindJump.isPressed()
                }
                if (ticksSinceVelocity <= 2 && mc.thePlayer.onGround) {
                    mc.gameSettings.keyBindJump.pressed = true
                }
            }
            if (ticksSinceVelocity >= 4 && ticksSinceVelocity <= 9) {
                mc.gameSettings.keyBindJump.pressed = pressed
            }
        }

        if (delayFlag) {
            val delayCondition = when {
                airBuffer.get() && mc.thePlayer.onGround -> true
                delay.get() && !airBuffer.get() && delayTimer.hasTimePassed((delayTicks.get() * 50).toLong()) -> true
                groundDelay.get() && !airBuffer.get() && delayTimer.hasTimePassed((delayTicks.get() * 50).toLong()) -> true
                else -> false
            }

            if (delayCondition) {
                ticksSinceVelocity = 0
                val killAura = FDPNext.moduleManager[KillAura::class.java]
                if (extraAttack.get() && reduce.get() && reduceMode.get() != "Attack" && killAura?.currentTarget != null) {
                    if (!extraAttacked) {
                        extraAttacked = true
                        velocityAttacked = true
                    }
                }
                hasReceivedVelocity = true
                dbg("Delay/Buffer Released")
                delayFlag = false
            }
        }
    }

    override fun onVelocity(event: UpdateEvent) {
        if (!reduce.get() || !hasReceivedVelocity) return

        if (velocityAttacked) {
            val killAura = FDPNext.moduleManager[KillAura::class.java] ?: return
            val target = killAura.currentTarget ?: return
            attackTarget(target)
            mc.thePlayer.motionX *= 0.6
            mc.thePlayer.motionZ *= 0.6
            mc.thePlayer.isSprinting = false
            velocityAttacked = false
        }

        if (reduceTick >= attackTimes.get()) {
            reduceTick = 0
            hasReceivedVelocity = false
            return
        }

        val rayTraced = RaycastUtils.raycastEntity(
            3.0,
            RotationUtils.serverRotation.yaw,
            RotationUtils.serverRotation.pitch
        ) { it is EntityPlayer && it != mc.thePlayer && it.isEntityAlive }

        if (rayTraced is EntityPlayer) {
            val killAura = FDPNext.moduleManager[KillAura::class.java]
            val target = killAura?.currentTarget

            if (target != null) {
                val canAttack = when (reduceMode.get()) {
                    "Attack" -> {
                        if (onlySprinting.get() && !mc.thePlayer.isSprinting) false
                        else reduceWhenCanAttack.get()
                    }
                    else -> true
                }

                if (canAttack) {
                    attackTarget(target)
                    mc.thePlayer.motionX *= 0.6
                    mc.thePlayer.motionZ *= 0.6
                    mc.thePlayer.isSprinting = false
                }
            } else {
                attackTarget(rayTraced)
                mc.thePlayer.motionX *= 0.6
                mc.thePlayer.motionZ *= 0.6
                mc.thePlayer.isSprinting = false
            }
        }

        reduceTick++
    }

    override fun onMotion(event: MotionEvent) {
        if (!event.isPre() || !rotate.get()) return

        val maxTick = rotateTick.get()
        if (rotatoTickCounter in 1..maxTick) {
            if (rotatoTickCounter == 1) {
                val targetX = mc.thePlayer.posX - knockbackX
                val targetZ = mc.thePlayer.posZ - knockbackZ
                targetRotation = RotationUtils.getRotations(targetX, mc.thePlayer.posY, targetZ)
            }

            if (targetRotation != null) {
                RotationUtils.setTargetRotation(targetRotation!!, 2)
            }
        }
    }

    override fun onMove(event: MoveEvent) {
        if (!rotate.get() || rotatoTickCounter <= 0) return
        if (rotatoTickCounter > rotateTick.get()) return

        if (autoMove.get()) {
            mc.thePlayer.movementInput.moveForward = 1.0f
        }

        if (targetRotation != null && RotationUtils.serverRotation != null && MovementUtils.isMoving()) {
            MovementUtils.strafe()
        }
    }

    private fun attackTarget(target: EntityLivingBase) {
        FDPNext.eventManager.callEvent(AttackEvent(target))
        mc.getNetHandler().addToSendQueue(C0APacketAnimation())
        mc.getNetHandler().addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
    }

    private fun dbg(msg: String) {
        if (debug.get()) ClientUtils.displayChatMessage("§7[§bHypixelPrediction§7] §f$msg")
    }
}
