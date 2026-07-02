/*
 * FDPNext Hacked Client
 * Adapted from FDPClient MurderDetector.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemBow
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumFacing
import java.awt.Color

object MurderDetector : Module(
    name = "MurderDetector",
    category = ModuleCategory.MISC,
    description = "Detects murderers by checking held knife items."
) {

    private val showTextValue = BoolValue("ShowText", true)
    private val chatValue = BoolValue("Chat", true)
    private val notificationValue = BoolValue("Notification", true)
    private val autoShootValue = BoolValue("AutoShoot", false)
    private val silentAimValue = BoolValue("SilentAim", true).displayable { autoShootValue.get() }
    private val predictValue = BoolValue("Predict", true).displayable { autoShootValue.get() }
    private val throughWallsValue = BoolValue("ThroughWalls", false).displayable { autoShootValue.get() }
    private val maxShootDistanceValue = FloatValue("ShootRange", 55F, 5F, 120F).displayable { autoShootValue.get() }
    private val chargeTicksValue = IntegerValue("ChargeTicks", 20, 3, 25).displayable { autoShootValue.get() }
    private val predictSizeValue = FloatValue("PredictSize", 2F, 0.1F, 5F).displayable { autoShootValue.get() && predictValue.get() }

    private var murder1: EntityPlayer? = null
    private var murder2: EntityPlayer? = null
    private var currentShootTarget: EntityPlayer? = null

    private val murderItems = setOf(
        267, 272, 256, 280, 271, 268, 273, 369, 277, 359,
        400, 285, 398, 357, 279, 283, 276, 293, 421, 333,
        409, 349, 364, 382, 351, 340, 406, 396, 260, 2258,
        76, 32, 19, 122, 175, 405, 130
    )

    override fun onDisable() {
        clearMurderers()
        stopUsingBow()
    }

    @EventTarget
    @Suppress("UNUSED_PARAMETER")
    fun onWorld(event: WorldEvent) {
        clearMurderers()
        stopUsingBow()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return
        if (mc.thePlayer.ticksExisted % 2 == 0) return

        for (player in mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player == murder1 || player == murder2) continue
            if (!isHoldingMurderItem(player)) continue

            when {
                murder1 == null -> {
                    murder1 = player
                    alert(player)
                }

                murder2 == null -> {
                    murder2 = player
                    alert(player)
                }
            }
        }
    }

    @EventTarget
    @Suppress("UNUSED_PARAMETER")
    fun onUpdate(event: UpdateEvent) {
        if (!autoShootValue.get() || mc.thePlayer == null || mc.theWorld == null) {
            stopUsingBow()
            return
        }

        val target = getShootTarget()
        if (target == null) {
            currentShootTarget = null
            stopUsingBow()
            return
        }

        val bowSlot = findBowSlot()
        if (bowSlot == null || !hasArrow()) {
            currentShootTarget = null
            stopUsingBow()
            return
        }

        currentShootTarget = target
        mc.thePlayer.inventory.currentItem = bowSlot
        RotationUtils.faceBow(target, silentAimValue.get(), predictValue.get(), predictSizeValue.get())

        val heldItem = mc.thePlayer.heldItem
        if (heldItem?.item !is ItemBow) return

        mc.gameSettings.keyBindUseItem.pressed = true
        if (!mc.thePlayer.isUsingItem) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, heldItem)
        }

        if (mc.thePlayer.isUsingItem && mc.thePlayer.itemInUseDuration >= chargeTicksValue.get()) {
            mc.gameSettings.keyBindUseItem.pressed = false
            mc.thePlayer.stopUsingItem()
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN)
            )
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!showTextValue.get()) return

        val firstLine = murder1?.let { "Murderer1: \u00A7e${it.name}" } ?: "Murderer1: \u00A7cNone"
        val secondLine = murder2?.let { "Murderer2: \u00A7e${it.name}" } ?: "Murderer2: \u00A7cNone"
        val centerX = event.scaledResolution.scaledWidth / 2F
        val font = mc.fontRendererObj

        font.drawStringWithShadow(
            firstLine,
            centerX - font.getStringWidth(firstLine) / 2F,
            66.5F,
            Color.WHITE.rgb
        )
        font.drawStringWithShadow(
            secondLine,
            centerX - font.getStringWidth(secondLine) / 2F,
            77.5F,
            Color.WHITE.rgb
        )
    }

    private fun isHoldingMurderItem(player: EntityPlayer): Boolean {
        val heldItem = player.heldItem ?: return false
        return heldItem.displayName.contains("Knife", ignoreCase = true) ||
            murderItems.contains(Item.getIdFromItem(heldItem.item))
    }

    private fun getShootTarget(): EntityPlayer? {
        return listOfNotNull(murder1, murder2)
            .filter { !it.isDead && it.health > 0F }
            .filter { mc.thePlayer.getDistanceToEntity(it) <= maxShootDistanceValue.get() }
            .filter { throughWallsValue.get() || mc.thePlayer.canEntityBeSeen(it) }
            .minByOrNull { mc.thePlayer.getDistanceToEntity(it) }
    }

    private fun findBowSlot(): Int? {
        for (slot in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(slot) ?: continue
            if (stack.item is ItemBow) {
                return slot
            }
        }
        return null
    }

    private fun hasArrow(): Boolean {
        if (mc.playerController.isInCreativeMode) return true
        return mc.thePlayer.inventory.mainInventory.any { it?.item == Items.arrow }
    }

    fun getMurderers(): List<EntityPlayer> {
        return listOfNotNull(murder1, murder2).filter { !it.isDead && it.health > 0F }
    }

    fun hasDetectedMurderer(player: EntityPlayer): Boolean {
        return player == murder1 || player == murder2
    }

    private fun stopUsingBow() {
        mc.gameSettings.keyBindUseItem.pressed = false
        if (mc.thePlayer?.heldItem?.item is ItemBow && mc.thePlayer.isUsingItem) {
            mc.thePlayer.stopUsingItem()
        }
    }

    private fun alert(player: EntityPlayer) {
        val message = "\u00A7e${player.name}\u00A7r is Murderer!"

        if (chatValue.get()) {
            mc.thePlayer.addChatMessage(ChatComponentText(message))
        }

        if (notificationValue.get()) {
            FDPNext.hud.addNotification(
                Notification("ALERT!", "${player.name} is Murderer!", NotifyType.INFO, 6000)
            )
        }
    }

    private fun clearMurderers() {
        murder1 = null
        murder2 = null
        currentShootTarget = null
    }
}
