/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.random.Random

/**
 * FairFight Velocity bypass.
 *
 * 基于爬取的 FairFight 源码分析 (https://github.com/dw1e/FairFight):
 *
 * 1. VelocityA.java (垂直击退):
 *    - tickOfCheck = 4, 只检查前 4 tick
 *    - 漏洞: 第 5 tick 起完全跳过检查
 *    - isJumped() 时 predictedY = attributeJump (跳跃重置垂直预测)
 *
 * 2. VelocityB.java (水平击退):
 *    - tickOfCheck = 4, 只检查前 4 tick
 *    - 漏洞 1: getTickSinceAttack() == 1 时 predictedXZ *= 0.6 (攻击衰减)
 *    - 漏洞 2: isJumped() && isSprinting() 时 predictedXZ -= 0.2 (跳跃重置)
 *    - 漏洞 3: buffer.add() > 4 才 flag, 5 次违规才触发
 *
 * 3. VelocityC.java (跳跃重置成功率):
 *    - maxVL = 8, 成功率 >= 80% 才 flag
 *    - 漏洞: 保持成功率 < 80% 可完全绕过 (每 5 次失败 1 次)
 *
 * 4. VelocityD.java (全击退模拟):
 *    - buffer size 5, buffer.add() > 3 才 flag
 *    - 漏洞: offsetMotion 允许 0.03 偏差
 *
 * 绕过策略:
 *   - 收到 velocity 后, 前 4 tick 正常接收 (避免 VelocityA/B buffer 累积)
 *   - 若冲刺状态, 立即跳跃 (利用 VelocityB: predictedXZ -= 0.2)
 *   - 若刚攻击过, 攻击后 predictedXZ *= 0.6 (利用 VelocityB: attack reduce)
 *   - 第 5 tick 起自由减少水平 motion (检查已跳过)
 *   - 跳跃重置成功率控制在 75% (低于 80% 阈值)
 */
class FairFightVelocity : VelocityMode("FairFight") {

    // ===== 跳跃重置配置 (绕过 VelocityB) =====
    private val jumpResetValue = BoolValue("${valuePrefix}JumpReset", true)
    private val jumpChanceValue = FloatValue("${valuePrefix}JumpChance", 75.0f, 0.0f, 100.0f)
        .displayable { jumpResetValue.get() }
    // VelocityC: 成功率 < 80% 才不 flag, 默认 75%

    // ===== 攻击衰减配置 (绕过 VelocityB) =====
    private val attackReduceValue = BoolValue("${valuePrefix}AttackReduce", true)
    private val attackReduceFactorValue = FloatValue("${valuePrefix}AttackReduceFactor", 0.6f, 0.1f, 1.0f)
        .displayable { attackReduceValue.get() }
    private val lastAttackTimeValue = IntegerValue("${valuePrefix}LastAttackTime", 2000, 1, 10000)
        .displayable { attackReduceValue.get() }

    // ===== 后期自由减少配置 (绕过 VelocityA/B tickOfCheck=4) =====
    private val lateReduceValue = BoolValue("${valuePrefix}LateReduce", true)
    private val lateReduceFactorValue = FloatValue("${valuePrefix}LateReduceFactor", 0.5f, 0.0f, 1.0f)
        .displayable { lateReduceValue.get() }
    private val lateReduceTickValue = IntegerValue("${valuePrefix}LateReduceTick", 5, 1, 20)
        .displayable { lateReduceValue.get() }

    // ===== 垂直配置 (绕过 VelocityA) =====
    private val verticalReduceValue = BoolValue("${valuePrefix}VerticalReduce", false)
    private val verticalReduceFactorValue = FloatValue("${valuePrefix}VerticalReduceFactor", 0.8f, 0.0f, 1.0f)
        .displayable { verticalReduceValue.get() }

    /** 上次攻击时间戳 (ms) */
    private val lastAttackTimer = MSTimer()

    /** velocity 接收后的 tick 计数 */
    private var velocityTickCount = 0

    /** 是否收到 velocity */
    private var receivedVelocity = false

    /** 跳跃重置统计 (绕过 VelocityC) */
    private var jumpSuccessCount = 0
    private var jumpTotalCount = 0

    override fun onEnable() {
        resetState()
    }

    override fun onDisable() {
        resetState()
    }

    private fun resetState() {
        velocityTickCount = 0
        receivedVelocity = false
        jumpSuccessCount = 0
        jumpTotalCount = 0
    }

    /**
     * 攻击事件: 记录攻击时间。
     * 利用 FairFight VelocityB: getTickSinceAttack() == 1 时 predictedXZ *= 0.6
     */
    override fun onAttack(event: AttackEvent) {
        lastAttackTimer.reset()
    }

    /**
     * Velocity 包处理: 记录接收状态, 可选垂直减少。
     */
    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet as? S12PacketEntityVelocity ?: return
        val player = mc.thePlayer ?: return

        receivedVelocity = true
        velocityTickCount = 0

        // 垂直减少 (VelocityA: 前 4 tick 检查, isJumped 重置 predictedY)
        if (verticalReduceValue.get()) {
            val factor = verticalReduceFactorValue.get().toDouble()
            packet.motionY = (packet.getMotionY() * factor).toInt()
        }
    }

    /**
     * Velocity 触发逻辑。
     * Velocity.kt 的 onUpdate 在 velocityInput 时调用此方法。
     */
    override fun onVelocity(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        if (!receivedVelocity) return

        velocityTickCount++

        // ===== 阶段 1: 前 4 tick (VelocityA/B 检查窗口内) =====
        if (velocityTickCount in 1..4) {
            // 绕过 VelocityB: 跳跃重置 (isJumped && isSprinting → predictedXZ -= 0.2)
            if (jumpResetValue.get() && player.onGround && player.isSprinting) {
                // VelocityC: 控制成功率 < 80%
                jumpTotalCount++
                val targetSuccessRate = jumpChanceValue.get() / 100.0f
                val currentRate = if (jumpTotalCount > 0) jumpSuccessCount.toFloat() / jumpTotalCount else 0f

                // 按概率跳跃, 保持成功率低于 80%
                if (currentRate < targetSuccessRate || Random.nextFloat() > 0.2f) {
                    player.jump()
                    jumpSuccessCount++
                }

                // 重置统计 (VelocityC: 10 次后检查)
                if (jumpTotalCount >= 10) {
                    jumpSuccessCount = 0
                    jumpTotalCount = 0
                }
            }

            // 绕过 VelocityB: 攻击衰减 (getTickSinceAttack() == 1 → predictedXZ *= 0.6)
            if (attackReduceValue.get() && velocityTickCount == 1) {
                if (!lastAttackTimer.hasTimePassed(lastAttackTimeValue.get().toLong())) {
                    val factor = attackReduceFactorValue.get().toDouble()
                    player.motionX *= factor
                    player.motionZ *= factor
                }
            }
        }

        // ===== 阶段 2: 第 5 tick 起 (VelocityA/B 检查已跳过, tickOfCheck=4) =====
        if (lateReduceValue.get() && velocityTickCount >= lateReduceTickValue.get()) {
            val factor = lateReduceFactorValue.get().toDouble()
            // 自由减少水平 velocity, FairFight 不会再检查
            player.motionX *= factor
            player.motionZ *= factor
        }

        // 10 tick 后重置 (Velocity 检查窗口完全结束)
        if (velocityTickCount >= 10) {
            receivedVelocity = false
            velocityTickCount = 0
        }
    }
}
