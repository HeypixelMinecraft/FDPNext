/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.intave

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
 * Intave Velocity bypass (noxz style).
 *
 * 基于爬取的 intave 源码分析:
 *   1. AttackReduceIgnoreHeuristic.java:
 *      - 当玩家攻击对方且冲刺时, intave 激活 ATTACK_REDUCE metric
 *      - Minecraft 1.8 冲刺攻击会重置 sprint, 导致 velocity 自然衰减
 *      - intave 期望玩家在 ATTACK_REDUCE 窗口内 velocity 减少
 *      - 漏洞: ignoredAttackReduce 需累积 vl > 5 才 flag, 且 vl 会衰减
 *      - 利用: 主动在攻击时减少 velocity (factor 0.6), 伪装成正常 attack reduce
 *
 *   2. AttackDispatcher.java L112-113:
 *      if (entity.isPlayer && (f > 0 || f1 > 0) && (isSprinting || itemKnockback > 0)) {
 *          movementData.activeTick(ATTACK_REDUCE);
 *      }
 *      - 触发条件: 攻击玩家 + 伤害>0 + (冲刺 OR 击退附魔)
 *
 *   3. JumpReset: Minecraft 原生 jump reset 机制
 *      - 跳跃会重置水平 velocity, intave 对此有宽容度
 *      - 50% 几率跳跃重置, 配合随机延迟避免 pattern 检测
 *
 * 参数参考 LiquidBounce Next (noxz) 的 Intave mode 默认值:
 *   ReduceOnAttack: Factor 0.6, HurtTime 5..7, LastAttackTimeToReduce 2000ms
 *   JumpReset: Chance 50%, Randomize DelayTicks 0..5
 */
class IntaveVelocity : VelocityMode("Intave") {

    // ===== ReduceOnAttack 配置 =====
    private val reduceOnAttackValue = BoolValue("${valuePrefix}ReduceOnAttack", true)
    private val reduceFactorValue = FloatValue("${valuePrefix}ReduceFactor", 0.6f, 0.6f, 1.0f)
        .displayable { reduceOnAttackValue.get() }
    private val hurtTimeMinValue = IntegerValue("${valuePrefix}HurtTimeMin", 5, 1, 10)
        .displayable { reduceOnAttackValue.get() }
    private val hurtTimeMaxValue = IntegerValue("${valuePrefix}HurtTimeMax", 7, 1, 10)
        .displayable { reduceOnAttackValue.get() }
    private val lastAttackTimeValue = IntegerValue("${valuePrefix}LastAttackTimeToReduce", 2000, 1, 10000)
        .displayable { reduceOnAttackValue.get() }

    // ===== JumpReset 配置 =====
    private val jumpResetValue = BoolValue("${valuePrefix}JumpReset", true)
    private val jumpChanceValue = FloatValue("${valuePrefix}JumpChance", 50.0f, 0.0f, 100.0f)
        .displayable { jumpResetValue.get() }
    private val randomizeValue = BoolValue("${valuePrefix}Randomize", false)
        .displayable { jumpResetValue.get() }
    private val randomDelayMinValue = IntegerValue("${valuePrefix}RandomDelayMin", 0, 0, 10)
        .displayable { jumpResetValue.get() && randomizeValue.get() }
    private val randomDelayMaxValue = IntegerValue("${valuePrefix}RandomDelayMax", 5, 0, 10)
        .displayable { jumpResetValue.get() && randomizeValue.get() }

    /** 上次攻击时间戳 (ms) */
    private val lastAttackTimer = MSTimer()

    /** 收到 velocity 后待跳跃的延迟 tick */
    private var pendingJumpDelay = -1

    override fun onEnable() {
        pendingJumpDelay = -1
    }

    override fun onDisable() {
        pendingJumpDelay = -1
    }

    /**
     * 攻击事件: 记录攻击时间。
     * 对应 intave AttackDispatcher 激活 ATTACK_REDUCE metric 的时机。
     */
    override fun onAttack(event: AttackEvent) {
        lastAttackTimer.reset()
    }

    /**
     * Velocity 触发 (Velocity.kt 的 onUpdate 在 velocityInput 时调用)。
     * 实现 JumpReset: 按概率在 hurtTime 窗口内跳跃重置水平 velocity。
     */
    override fun onVelocity(event: UpdateEvent) {
        if (!jumpResetValue.get()) return
        val player = mc.thePlayer ?: return

        // 延迟跳跃处理 (Randomize)
        if (pendingJumpDelay > 0) {
            pendingJumpDelay--
            if (pendingJumpDelay == 0 && player.onGround) {
                player.jump()
                pendingJumpDelay = -1
            }
            return
        }

        // hurtTime 窗口内尝试 jump reset
        if (player.hurtTime in 1..10 && player.onGround) {
            if (Random.nextFloat() * 100f <= jumpChanceValue.get()) {
                if (randomizeValue.get()) {
                    // 随机延迟, 对应 noxz 的 Randomize DelayTicks 0..5
                    val min = randomDelayMinValue.get().coerceAtMost(randomDelayMaxValue.get())
                    val max = randomDelayMaxValue.get().coerceAtLeast(randomDelayMinValue.get())
                    pendingJumpDelay = if (max <= 0) 0 else Random.nextInt(min, max + 1)
                    if (pendingJumpDelay == 0) {
                        player.jump()
                        pendingJumpDelay = -1
                    }
                } else {
                    player.jump()
                }
            }
        }
    }

    /**
     * Velocity 包处理: 实现 ReduceOnAttack。
     * 在收到 S12 时, 若最近攻击过对方, 按 factor 缩放水平 velocity。
     * 利用 intave 的 ATTACK_REDUCE 期望窗口, 伪装成正常攻击减少。
     */
    override fun onVelocityPacket(event: PacketEvent) {
        if (!reduceOnAttackValue.get()) return
        val packet = event.packet as? S12PacketEntityVelocity ?: return

        // 检查最近攻击时间 (noxz 默认 2000ms)
        if (!lastAttackTimer.hasTimePassed(lastAttackTimeValue.get().toLong())) {
            val player = mc.thePlayer ?: return
            val hurtTime = player.hurtTime
            val minHurt = hurtTimeMinValue.get().coerceAtMost(hurtTimeMaxValue.get())
            val maxHurt = hurtTimeMaxValue.get().coerceAtLeast(hurtTimeMinValue.get())

            // 在 hurtTime 窗口内 (noxz 默认 5..7) 应用 reduce factor
            if (hurtTime in minHurt..maxHurt) {
                val factor = reduceFactorValue.get().toDouble()
                // 缩放水平 velocity (1.8 S12 的 motionX/Z 是 1/8000 block/tick)
                packet.motionX = (packet.getMotionX() * factor).toInt()
                packet.motionZ = (packet.getMotionZ() * factor).toInt()
            }
        }
    }
}
