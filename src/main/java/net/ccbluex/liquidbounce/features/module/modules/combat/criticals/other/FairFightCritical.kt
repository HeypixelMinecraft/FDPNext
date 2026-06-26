package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.other

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.CriticalMode

/**
 * FairFight Criticals bypass.
 *
 * 基于爬取的 FairFight 源码分析 (https://github.com/dw1e/FairFight):
 *
 * 1. FlyA.java (Y轴重力):
 *    - threshold = offsetMotion ? 0.05 : 1E-5
 *    - 漏洞: offsetMotion 模式下允许 0.05 垂直偏差
 *    - 绕过: 使用 < 0.05 的 Y 偏移
 *
 * 2. FlyB.java (1/64地面):
 *    - getTickSinceVelocity() == 1 && velocityY % 0.015625 == 0.0 时 exempt
 *    - 漏洞: velocityY 为 0.015625 倍数时豁免
 *    - 绕过: Y 偏移使用 0.015625 的倍数
 *
 * 3. FlyC.java (相同Y轴):
 *    - deltaY == lastDeltaY 时检查 (step/velocity 豁免)
 *    - 漏洞: 连续相同 Y 会触发, 但交替 Y 值可绕过
 *    - 绕过: 每次攻击交替 Y 偏移值
 *
 * 4. FlyD.java (跳跃高度):
 *    - isOffsetYMotion() && deltaY - 0.4044449F < 1E-7F 时豁免 (低跳)
 *    - 漏洞: offsetYMotion 模式下接近 0.4044449 的 Y 不检测
 *
 * 5. VelocityA.java (垂直击退):
 *    - buffer.add() > tickOfCheck (4) 才 flag
 *    - 漏洞: 4 次违规才触发, 间歇性可绕过
 *
 * 绕过策略:
 *   - Y 偏移全部使用 0.015625 倍数且 < 0.05 (FlyA + FlyB)
 *   - 每次攻击交替不同的 Y 值 (FlyC)
 *   - 每 4 次攻击跳过 1 次 (避免 buffer 累积到 5)
 *   - 使用 3 种不同的 Y 模式轮换
 */
class FairFightCritical : CriticalMode("FairFight") {
    private var attacks = 0

    override fun onEnable() {
        attacks = 0
    }

    override fun onAttack(event: AttackEvent) {
        attacks++

        // FairFight buffer 管理: 每 4 次攻击跳过 1 次, 避免累积到 5 次 flag
        // (FlyA/VelocityA: buffer.add() > 4 才 flag)
        if (attacks % 5 == 0) {
            critical.antiDesync = false
            return
        }

        // FairFight FlyA: offsetMotion 允许 0.05, 使用 < 0.05 的 Y 偏移
        // FairFight FlyB: velocityY % 0.015625 == 0 豁免, 全部使用 0.015625 倍数
        // FairFight FlyC: deltaY == lastDeltaY 触发, 3 种模式轮换避免连续相同
        when (attacks % 3) {
            1 -> {
                // 模式 A: 0.015625 → 0.03125 → 0.015625 → 0
                critical.sendCriticalPacket(yOffset = 0.015625, ground = true)
                critical.sendCriticalPacket(yOffset = 0.03125, ground = false)
                critical.sendCriticalPacket(yOffset = 0.015625, ground = false)
                critical.sendCriticalPacket(ground = false)
            }
            2 -> {
                // 模式 B: 0.03125 → 0.046875 → 0.015625 → 0 (交替 Y 避免 FlyC)
                critical.sendCriticalPacket(yOffset = 0.03125, ground = true)
                critical.sendCriticalPacket(yOffset = 0.046875, ground = false)
                critical.sendCriticalPacket(yOffset = 0.015625, ground = false)
                critical.sendCriticalPacket(ground = false)
            }
            else -> {
                // 模式 C: 0.046875 → 0.015625 → 0.03125 → 0 (第三种组合)
                critical.sendCriticalPacket(yOffset = 0.046875, ground = true)
                critical.sendCriticalPacket(yOffset = 0.015625, ground = false)
                critical.sendCriticalPacket(yOffset = 0.03125, ground = false)
                critical.sendCriticalPacket(ground = false)
            }
        }

        if (attacks > 100) attacks = 1
    }
}
