package net.ccbluex.liquidbounce.features.module.modules.movement.flys.vulcan

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import kotlin.math.sqrt

class AugustusVulcanFly : FlyMode("AugustusVulcan") {

    private val speedValue = FloatValue("${valuePrefix}Speed", 2.0f, 0.1f, 5.0f)
    private val verticalValue = BoolValue("${valuePrefix}Vertical", false)
    private val strictValue = BoolValue("${valuePrefix}Strict", false)

    private var isSuccess = false
    private var waitTicks = 0
    private var doCancel = false
    private var stage = FlyStage.FLYING
    private var startX = 0.0
    private var startZ = 0.0
    private var startY = 0.0

    override fun onEnable() {
        waitTicks = 0
        doCancel = false
        stage = FlyStage.FLYING
        isSuccess = false
        startX = mc.thePlayer.posX
        startY = mc.thePlayer.posY
        startZ = mc.thePlayer.posZ
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        if (!isSuccess) {
            mc.thePlayer.setPosition(startX, startY, startZ)
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        when (stage) {
            FlyStage.FLYING -> {
                MovementUtils.resetMotion(false)
                MovementUtils.strafe(speedValue.get())
                doCancel = true

                if (mc.gameSettings.keyBindSneak.pressed) {
                    MovementUtils.strafe(0.45f)
                    if (verticalValue.get()) {
                        mc.thePlayer.motionY = -speedValue.get().toDouble()
                    }
                }

                if (verticalValue.get()) {
                    if (mc.gameSettings.keyBindJump.pressed) {
                        mc.thePlayer.motionY = speedValue.get().toDouble()
                    } else if (!mc.gameSettings.keyBindSneak.pressed) {
                        mc.thePlayer.motionY = 0.0
                    }
                }

                if (mc.gameSettings.keyBindSneak.pressed && mc.thePlayer.ticksExisted % 2 == 1) {
                    val fixedY = mc.thePlayer.posY - (mc.thePlayer.posY % 1)
                    val underBlock = BlockUtils.getBlock(BlockPos(mc.thePlayer.posX, fixedY - 1, mc.thePlayer.posZ))

                    if (underBlock != null && underBlock.isFullBlock) {
                        stage = FlyStage.WAIT_APPLY
                        MovementUtils.resetMotion(true)
                        mc.thePlayer.jumpMovementFactor = 0.00f
                        doCancel = false
                        mc.thePlayer.onGround = false

                        var fixedX = mc.thePlayer.posX - (mc.thePlayer.posX % 1)
                        var fixedZ = mc.thePlayer.posZ - (mc.thePlayer.posZ % 1)

                        if (fixedX > 0) fixedX += 0.5 else fixedX -= 0.5
                        if (fixedZ > 0) fixedZ += 0.5 else fixedZ -= 0.5

                        mc.thePlayer.setPosition(fixedX, fixedY, fixedZ)
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, fixedY, mc.thePlayer.posZ, true))
                        doCancel = true
                    }
                }
            }

            FlyStage.WAIT_APPLY -> {
                waitTicks++
                doCancel = false

                if (waitTicks == 60) {
                    stage = FlyStage.FLYING
                    waitTicks = 0
                }

                mc.timer.timerSpeed = 1f
                MovementUtils.resetMotion(true)
                mc.thePlayer.jumpMovementFactor = 0.00f

                val fixedY = mc.thePlayer.posY - (mc.thePlayer.posY % 1)

                if (strictValue.get()) {
                    mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, fixedY - 1024, mc.thePlayer.posZ, true))
                } else {
                    val hasCollision10 = !mc.theWorld.getCollisionBoxes(mc.thePlayer.entityBoundingBox.offset(0.0, -10.0, 0.0)).isEmpty()
                    val hasCollision12 = !mc.theWorld.getCollisionBoxes(mc.thePlayer.entityBoundingBox.offset(0.0, -12.0, 0.0)).isEmpty()

                    if (!hasCollision10 && !hasCollision12) {
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, fixedY - 10, mc.thePlayer.posZ, true))
                    } else {
                        mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, fixedY - 1024, mc.thePlayer.posZ, true))
                    }
                }
                doCancel = true
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        when (val packet = event.packet) {
            is C03PacketPlayer -> {
                if (doCancel) {
                    event.cancelEvent()
                    doCancel = false
                }
                packet.onGround = true
            }

            is S08PacketPlayerPosLook -> {
                if (stage == FlyStage.WAIT_APPLY) {
                    val distance = sqrt(
                        (packet.x - mc.thePlayer.posX) * (packet.x - mc.thePlayer.posX) +
                        (packet.y - mc.thePlayer.posY) * (packet.y - mc.thePlayer.posY) +
                        (packet.z - mc.thePlayer.posZ) * (packet.z - mc.thePlayer.posZ)
                    )

                    if (distance < 1.4) {
                        isSuccess = true
                        fly.state = false
                        return
                    }
                }
                event.cancelEvent()
            }
        }
    }

    enum class FlyStage {
        FLYING,
        WAIT_APPLY
    }
}
