/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flys.vulcan

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer

class VulcanFlight : FlyMode("Vulcan") {

    private var ticks = 0

    override fun onEnable() {
        ticks = 0
        // 发送位置包，Y轴-2，用于绕过 Vulcan 检测
        mc.netHandler.addToSendQueue(C03PacketPlayer.C06PacketPlayerPosLook(
            mc.thePlayer.posX,
            mc.thePlayer.posY - 2,
            mc.thePlayer.posZ,
            mc.thePlayer.rotationYaw,
            mc.thePlayer.rotationPitch,
            false
        ))
    }

    override fun onDisable() {
        // 停止移动
        MovementUtils.resetMotion(true)
    }

    @EventTarget
    override fun onMotion(event: MotionEvent) {
        if (!event.isPre()) return

        val speed = 1f

        // 设置垂直运动
        mc.thePlayer.motionY = -1E-10 +
            (if (mc.gameSettings.keyBindJump.isKeyDown) speed.toDouble() else 0.0) -
            (if (mc.gameSettings.keyBindSneak.isKeyDown) speed.toDouble() else 0.0)

        // 增加 tick 计数
        ticks++
        if (ticks >= 8) {
            // 达到最大 tick 数，停止飞行
            MovementUtils.resetMotion(true)
            fly.state = false
        }
    }

    @EventTarget
    override fun onMove(event: MoveEvent) {
        val speed = 1f
        // 设置水平移动速度
        MovementUtils.strafe(speed)
        event.zeroXZ()
    }
}