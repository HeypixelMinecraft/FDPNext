package net.ccbluex.liquidbounce.features.module.modules.movement.flys.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity

/**
 * FairFight Fly bypass.
 *
 * 基于爬取的 FairFight 源码分析 (https://github.com/dw1e/FairFight):
 *
 * 1. FlyA.java (Y轴重力):
 *    threshold = offsetMotion ? 0.05 : 1E-5
 *    漏洞: offsetMotion 模式下允许 0.05 垂直偏差
 *
 * 2. FlyB.java (1/64地面):
 *    getTickSinceVelocity() == 1 && velocityY % 0.015625 == 0.0 时 exempt
 *    漏洞: velocityY 为 0.015625 倍数时豁免 (利用 velocity 窗口)
 *
 * 3. FlyC.java (相同Y轴):
 *    deltaY == lastDeltaY 时检查 (step/velocity 豁免)
 *    漏洞: 交替 Y 值可绕过连续相同检测
 *
 * 4. FlyD.java (跳跃高度):
 *    isOffsetYMotion() && deltaY - 0.4044449F < 1E-7F 时豁免 (低跳)
 *    漏洞: offsetYMotion 模式下接近 0.4044449 不检测
 *
 * 5. VelocityA.java (垂直击退):
 *    tickOfCheck = 4, buffer.add() > 4 才 flag
 *    漏洞: 4 次内不 flag, 可利用 velocity 窗口自由移动
 *
 * 绕过策略:
 *   - Y 偏移使用 0.015625 倍数且 < 0.05 (FlyA + FlyB)
 *   - 交替正负 Y 偏移避免连续相同 (FlyC)
 *   - 每 5 tick 重置一次, 避免 buffer 累积
 *   - 利用 velocity 窗口 (tickOfCheck=4) 间歇性自由移动
 */
class FairFightFly : FlyMode("FairFight") {

    // ===== 模式 =====
    private val bypassModeValue = ListValue("${valuePrefix}BypassMode",
        arrayOf("Offset", "Velocity", "Packet"), "Offset")

    // ===== 速度 =====
    private val hSpeedValue = FloatValue("${valuePrefix}Horizontal", 1.0f, 0.1f, 3.0f)
    private val vSpeedValue = FloatValue("${valuePrefix}Vertical", 0.4f, 0.05f, 2.0f)

    // ===== Offset 模式参数 =====
    private val offsetStepValue = FloatValue("${valuePrefix}OffsetStep", 0.04f, 0.005f, 0.05f)
        .displayable { bypassModeValue.equals("Offset") }
    private val offsetIntervalValue = IntegerValue("${valuePrefix}OffsetInterval", 5, 1, 20)
        .displayable { bypassModeValue.equals("Offset") }

    // ===== Velocity 模式参数 =====
    private val velFreeTicksValue = IntegerValue("${valuePrefix}VelFreeTicks", 4, 1, 10)
        .displayable { bypassModeValue.equals("Velocity") }
    private val velRestTicksValue = IntegerValue("${valuePrefix}VelRestTicks", 6, 1, 20)
        .displayable { bypassModeValue.equals("Velocity") }

    // ===== 通用 =====
    private val autoDisableValue = BoolValue("${valuePrefix}AutoDisable", true)
    private val debugValue = BoolValue("${valuePrefix}Debug", false)

    private var tickCounter = 0
    private var velocityTickCount = 0
    private var receivedVelocity = false
    private var lastYOffset = 0.0
    private var phase = 0

    override fun onEnable() {
        tickCounter = 0
        velocityTickCount = 0
        receivedVelocity = false
        lastYOffset = 0.0
        phase = 0
        MovementUtils.resetMotion(true)
    }

    override fun onDisable() {
        MovementUtils.resetMotion(true)
        mc.timer.timerSpeed = 1.0f
    }

    override fun onUpdate(event: UpdateEvent) {
        tickCounter++
        if (receivedVelocity) velocityTickCount++

        when (bypassModeValue.get().lowercase()) {
            "offset" -> updateOffsetMode()
            "velocity" -> updateVelocityMode()
            "packet" -> updatePacketMode()
        }
    }

    /**
     * Offset 模式: 利用 FlyA offsetMotion 0.05 阈值
     * 每 [offsetIntervalValue] tick 发送一个 < 0.05 的 Y 偏移包, 交替正负避免 FlyC
     */
    private fun updateOffsetMode() {
        // FairFight FlyA: offsetMotion 允许 0.05, 使用 < 0.05 的 Y 偏移
        // FairFight FlyB: velocityY % 0.015625 == 0 豁免, 使用 0.015625 倍数
        val step = offsetStepValue.get().toDouble()
        val interval = offsetIntervalValue.get()

        if (tickCounter % interval == 0) {
            // 交替正负 Y 偏移避免 FlyC (deltaY == lastDeltaY)
            val yOffset = if (phase % 2 == 0) step else -step
            phase++

            // 确保是 0.015625 的倍数 (FlyB 豁免)
            val alignedOffset = Math.round(yOffset / 0.015625) * 0.015625
            lastYOffset = alignedOffset

            val newY = mc.thePlayer.posY + alignedOffset
            PacketUtils.sendPacketNoEvent(
                C04PacketPlayerPosition(mc.thePlayer.posX, newY, mc.thePlayer.posZ, false)
            )
        }

        // 水平移动
        MovementUtils.resetMotion(true)
        MovementUtils.strafe(hSpeedValue.get())

        // 垂直控制
        if (mc.gameSettings.keyBindJump.isKeyDown) {
            mc.thePlayer.motionY = vSpeedValue.get().toDouble()
        } else if (mc.gameSettings.keyBindSneak.isKeyDown) {
            mc.thePlayer.motionY = -vSpeedValue.get().toDouble()
        } else {
            mc.thePlayer.motionY = 0.0
        }

        // 每 5 tick 重置 buffer (避免 FlyA buffer.add() > 4 flag)
        if (tickCounter % 5 == 0) {
            PacketUtils.sendPacketNoEvent(
                C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true)
            )
        }
    }

    /**
     * Velocity 模式: 利用 VelocityA tickOfCheck=4 + buffer > 4
     * velocity 后前 4 tick 自由移动, 之后 6 tick 正常下落让 buffer 衰减
     */
    private fun updateVelocityMode() {
        if (receivedVelocity && velocityTickCount in 1..velFreeTicksValue.get()) {
            // velocity 窗口内自由移动 (FairFight VelocityA 只检查前 4 tick)
            MovementUtils.resetMotion(true)
            MovementUtils.strafe(hSpeedValue.get())

            if (mc.gameSettings.keyBindJump.isKeyDown) {
                mc.thePlayer.motionY = vSpeedValue.get().toDouble()
            } else if (mc.gameSettings.keyBindSneak.isKeyDown) {
                mc.thePlayer.motionY = -vSpeedValue.get().toDouble()
            } else {
                mc.thePlayer.motionY = 0.0
            }

            // 持续发送小偏移包维持高度 (FlyA offsetMotion)
            if (tickCounter % 3 == 0) {
                val yOffset = if (phase % 2 == 0) 0.03125 else -0.03125
                phase++
                val newY = mc.thePlayer.posY + yOffset
                PacketUtils.sendPacketNoEvent(
                    C04PacketPlayerPosition(mc.thePlayer.posX, newY, mc.thePlayer.posZ, false)
                )
            }
        } else if (receivedVelocity && velocityTickCount > velFreeTicksValue.get() + velRestTicksValue.get()) {
            // 窗口结束, 重置等待下一次 velocity
            receivedVelocity = false
            velocityTickCount = 0
        } else if (receivedVelocity) {
            // 休息阶段: 正常下落, 让 buffer 衰减
            mc.thePlayer.motionY = -0.0784
        } else {
            // 等待 velocity 触发, 小幅悬停
            MovementUtils.resetMotion(true)
            mc.thePlayer.motionY = 0.0
        }
    }

    /**
     * Packet 模式: 利用 FlyD offsetYMotion 低跳豁免
     * 接近 0.4044449 的 Y 不检测, 发送低跳包链
     */
    private fun updatePacketMode() {
        if (tickCounter % 10 == 0) {
            // FairFight FlyD: deltaY 接近 0.4044449 时豁免
            // 发送低跳序列
            PacketUtils.sendPacketNoEvent(
                C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.4044449, mc.thePlayer.posZ, false)
            )
            PacketUtils.sendPacketNoEvent(
                C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true)
            )
            phase++
        }

        MovementUtils.resetMotion(true)
        MovementUtils.strafe(hSpeedValue.get())

        if (mc.gameSettings.keyBindJump.isKeyDown) {
            mc.thePlayer.motionY = vSpeedValue.get().toDouble() * 0.5
        } else if (mc.gameSettings.keyBindSneak.isKeyDown) {
            mc.thePlayer.motionY = -vSpeedValue.get().toDouble() * 0.5
        } else {
            mc.thePlayer.motionY = 0.0
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        // 监听 velocity 包, 触发 velocity 窗口
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            if (bypassModeValue.equals("Velocity")) {
                receivedVelocity = true
                velocityTickCount = 0
                // FairFight FlyB: velocityY % 0.015625 == 0 豁免, 修改 velocityY 为 0.015625 倍数
                packet.motionY = (Math.round(packet.getMotionY() / 0.015625) * 0.015625).toInt()
            }
        }

        // S08 flag 处理
        if (packet is S08PacketPlayerPosLook) {
            if (debugValue.get()) {
                ClientUtils.displayAlert("FairFight Fly: S08 flag received")
            }
            if (autoDisableValue.get()) {
                fly.state = false
            }
        }

        // Velocity 模式下取消重力包
        if (bypassModeValue.equals("Velocity") && receivedVelocity && velocityTickCount in 1..velFreeTicksValue.get()) {
            if (packet is C03PacketPlayer && packet !is C04PacketPlayerPosition) {
                event.cancelEvent()
            }
        }
    }
}
