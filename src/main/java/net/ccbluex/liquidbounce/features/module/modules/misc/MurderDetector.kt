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
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.util.ChatComponentText
import java.awt.Color

object MurderDetector : Module(
    name = "MurderDetector",
    category = ModuleCategory.MISC,
    description = "Detects murderers by checking held knife items."
) {

    private val showTextValue = BoolValue("ShowText", true)
    private val chatValue = BoolValue("Chat", true)
    private val notificationValue = BoolValue("Notification", true)

    private var murder1: EntityPlayer? = null
    private var murder2: EntityPlayer? = null

    private val murderItems = setOf(
        267, 272, 256, 280, 271, 268, 273, 369, 277, 359,
        400, 285, 398, 357, 279, 283, 276, 293, 421, 333,
        409, 349, 364, 382, 351, 340, 406, 396, 260, 2258,
        76, 32, 19, 122, 175, 405, 130
    )

    override fun onDisable() {
        clearMurderers()
    }

    @EventTarget
    @Suppress("UNUSED_PARAMETER")
    fun onWorld(event: WorldEvent) {
        clearMurderers()
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
    }
}
