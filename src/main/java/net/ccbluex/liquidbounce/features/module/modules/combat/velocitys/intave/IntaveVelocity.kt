/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.intave

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.abs
import kotlin.math.sign

/**
 * Intave Velocity bypass.
 *
 * Based on intave's Physics.java velocity check:
 *   if (checkVelocity && !elytraFlying
 *       && ticksPast(EXTERNAL_VELOCITY) < 10
 *       && !receivedFlyingPacketIn(2)) {  // <- 2 tick 内有 flying 包则跳过整个检查
 *       ...
 *   }
 *
 * Two-stage bypass:
 *  1. On receiving S12 velocity, immediately inject 2 flying packets so the
 *     `receivedFlyingPacketIn(2)` condition is true and the entire velocity
 *     check is skipped.
 *  2. For the first 2 ticks after velocity, freeze horizontal motion and
 *     clamp vertical motion below 0.015 to hit the
 *     `verticalViolationIncrease = 0` branch (actuallyMoved == false).
 */
class IntaveVelocity : VelocityMode("Intave") {

    /** 收到 velocity 后的剩余 tick 计数 */
    private var velocityTick = -1

    /** 注入的 flying 包数量（对应 receivedFlyingPacketIn(2)） */
    private val flyingInjectCount = 2

    /** 检查窗口（intave 内部为 10 tick） */
    private val checkWindow = 10

    /** actuallyMoved 阈值：predictedX/Z < 0.01 走 else 分支 */
    private val actuallyMovedThreshold = 0.01

    /** 垂直偏差阈值：differenceY < 0.015 时 verticalViolationIncrease = 0 */
    private val verticalClamp = 0.015

    override fun onEnable() {
        velocityTick = -1
    }

    override fun onDisable() {
        velocityTick = -1
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        // 仅处理作用于自身的 velocity 包
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            velocityTick = 0
            // 绕过 1：立即注入 flying 包，触发 receivedFlyingPacketIn(2) 跳过 velocity 检查
            injectFlyingPackets(flyingInjectCount)
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        if (velocityTick < 0) return
        val player = mc.thePlayer ?: return

        // 绕过 2：前 2 tick 停止水平移动，让 actuallyMoved = false
        if (velocityTick < 2) {
            player.motionX = 0.0
            player.motionZ = 0.0
            // 垂直方向限制 < 0.015，触发 verticalViolationIncrease = 0
            if (abs(player.motionY) > verticalClamp) {
                player.motionY = sign(player.motionY) * verticalClamp
            }
        }

        // 10 tick 后重置，velocity 检查窗口已过
        if (velocityTick >= checkWindow) {
            velocityTick = -1
        } else {
            velocityTick++
        }
    }

    /**
     * 注入指定数量的 flying 包（C04 PlayerPosition）。
     * intave 的 receivedFlyingPacketIn(n) 检查过去 n tick 内是否有 flying 包到达，
     * 注入后该条件为 true，整个 velocity 检查被跳过。
     */
    private fun injectFlyingPackets(count: Int) {
        val player = mc.thePlayer ?: return
        val posX = player.posX
        val posY = player.posY
        val posZ = player.posZ
        val onGround = player.onGround
        val netHandler = mc.netHandler

        repeat(count) {
            val packet: Packet<*> = C03PacketPlayer.C04PacketPlayerPosition(posX, posY, posZ, onGround)
            netHandler.addToSendQueue(packet)
        }
    }
}
