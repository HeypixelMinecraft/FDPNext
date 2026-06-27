/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * GrimCombat - Skidded from AirClient (lmx0721/AirClient) Velocity "GrimCombat" mode.
 * On strong horizontal knockback, spams sprint-reset attacks on the target to
 * negate the velocity against Grim, then dampens the residual knockback on ground.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.grim

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.WorldSettings
import kotlin.math.floor
import kotlin.math.sqrt

class GrimCombat : VelocityMode("GrimCombat") {

    private val range = FloatValue("${valuePrefix}Range", 3.5f, 0f, 6f)
    private val attackCount = IntegerValue("${valuePrefix}AttackCounts", 12, 1, 16)
    private val fireCheck = BoolValue("${valuePrefix}FireCheck", false)
    private val waterCheck = BoolValue("${valuePrefix}WaterCheck", false)
    private val fallCheck = BoolValue("${valuePrefix}FallCheck", false)
    private val consumeCheck = BoolValue("${valuePrefix}ConsumableCheck", false)
    private val raycast = BoolValue("${valuePrefix}RayCast", false)
    private val debug = BoolValue("${valuePrefix}Debug", false)

    private var attacked = false

    override fun onEnable() {
        attacked = false
    }

    override fun onDisable() {
        attacked = false
    }

    /**
     * Called only for S12 velocity packets that belong to the local player.
     */
    override fun onVelocityPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet as? S12PacketEntityVelocity ?: return

        if (mc.currentScreen is GuiGameOver) return
        if (mc.playerController.currentGameType === WorldSettings.GameType.SPECTATOR) return
        if (player.isOnLadder || (player.isBurning && fireCheck.get()) || (player.isInWater && waterCheck.get())) return
        if (player.fallDistance > 1.5f && fallCheck.get()) return
        if (player.isUsingItem && consumeCheck.get()) return
        if (isInsideSoulSand()) return

        val horizontalStrength = sqrt(
            packet.motionX.toDouble() * packet.motionX + packet.motionZ.toDouble() * packet.motionZ
        )
        if (horizontalStrength <= 1000.0) return

        val target = findTarget(player) ?: return
        val sprinting = player.serverSprintState

        if (!sprinting) {
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING))
        }

        repeat(attackCount.get()) {
            mc.netHandler.addToSendQueue(C0APacketAnimation())
            mc.netHandler.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
        }

        if (!sprinting) {
            mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING))
        }

        attacked = true
        event.cancelEvent()

        if (debug.get()) {
            ClientUtils.displayChatMessage("§7[GrimCombat] negated knockback (${attackCount.get()} hits)")
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        if (!attacked) return

        if (player.hurtTime > 0 && player.onGround) {
            player.addVelocity(-1.3E-10, -1.3E-10, -1.3E-10)
            player.isSprinting = false
        }
        if (player.hurtTime == 0) {
            attacked = false
        }
    }

    private fun findTarget(player: Entity): Entity? {
        mc.objectMouseOver?.let { mouse ->
            if (mouse.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                val hit = mouse.entityHit
                if (hit is EntityLivingBase && player.getDistanceToEntityBox(hit) <= range.get()) {
                    return hit
                }
            }
        }

        if (!raycast.get()) {
            FDPNext.moduleManager[KillAura::class.java]?.currentTarget?.takeIf {
                EntityUtils.isSelected(it, true) && player.getDistanceToEntityBox(it) <= range.get()
            }?.let { return it }
        }

        return null
    }

    private fun isInsideSoulSand(): Boolean {
        val world = mc.theWorld ?: return false
        val player = mc.thePlayer ?: return false
        val box = player.entityBoundingBox.contract(0.001, 0.001, 0.001)

        val minX = floor(box.minX).toInt()
        val maxX = floor(box.maxX + 1.0).toInt()
        val minY = floor(box.minY).toInt()
        val maxY = floor(box.maxY + 1.0).toInt()
        val minZ = floor(box.minZ).toInt()
        val maxZ = floor(box.maxZ + 1.0).toInt()

        for (x in minX until maxX) {
            for (y in minY until maxY) {
                for (z in minZ until maxZ) {
                    if (world.getBlockState(BlockPos(x, y, z)).block === net.minecraft.init.Blocks.soul_sand) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
