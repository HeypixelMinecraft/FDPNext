/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 *
 * Skidded from Gapple.kt
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.StuckUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shadowRenderUtils
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.network.play.client.*
import java.awt.Color

/**
 * Gapple - Auto eat golden apple when health is low
 */
object Gapple : Module(
    name = "Gapple",
    description = "Automatically eat golden apple when health is low",
    category = ModuleCategory.PLAYER
) {

    private val heal = IntegerValue("health", 20, 0, 40)
    private val sendDelay = IntegerValue("SendDelay", 3, 1, 10)
    private val stuck = BoolValue("Stuck", false)
    private val stopMove = BoolValue("StopMove", false)
    private val noCancelC02 = BoolValue("NoCancelC02", false)
    private val noC02 = BoolValue("NoC02", false)
    private val autoGapple = BoolValue("AutoGapple", false)

    private var slot = -1
    private var c03s = 0
    private var c02s = 0
    private var canStart = false

    var eating = false
    var pulsing = false
    var target: EntityLivingBase? = null

    override fun onEnable() {
        c03s = 0
        slot = InventoryUtils.findItem(36, 45, Items.golden_apple)
        if (slot != -1) {
            slot = slot - 36
        }
    }

    override fun onDisable() {
        eating = false

        if (canStart) {
            pulsing = false
            eating = false
            BlinkUtils.setBlinkState(off = true, release = true)
        }

        if (stuck.get()) {
            StuckUtils.stopStuck()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.health < heal.get()) {
            if (!eating) {
                val killAura = FDPNext.moduleManager.getModule(net.ccbluex.liquidbounce.features.module.modules.combat.KillAura::class.java)
                target = killAura?.currentTarget

                c03s = 0

                slot = InventoryUtils.findItem(36, 45, Items.golden_apple)

                if (slot != -1) {
                    slot = slot - 36
                }
            }

            if (mc.thePlayer == null || mc.thePlayer.isDead) {
                BlinkUtils.setBlinkState(off = true, release = true)
                state = false
                return
            }
            if (slot == -1) {
                state = false
                return
            }
            if (eating) {
                if (stuck.get()) {
                    StuckUtils.stuck()
                }
                if (!BlinkUtils.movingPacketStat) {
                    BlinkUtils.setBlinkState(all = true)
                    canStart = true
                }
            } else {
                eating = true
            }
            if (c03s >= 32) {
                eating = false
                pulsing = true
                BlinkUtils.setBlinkState(off = true, release = true)
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
                mc.netHandler.addToSendQueue(
                    C08PacketPlayerBlockPlacement(
                        mc.thePlayer.inventoryContainer.getSlot(slot + 36).stack
                    )
                )
                BlinkUtils.setBlinkState(off = true, release = true)
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                pulsing = false
                if (autoGapple.get()) {
                    c03s = 0
                    slot = InventoryUtils.findItem(36, 45, Items.golden_apple)
                    if (slot != -1) {
                        slot = slot - 36
                    }
                } else {
                    state = false
                }
                return
            }

            if ((mc.thePlayer.ticksExisted % sendDelay.get()) == 0) {
                BlinkUtils.releasePacket()
            }
        } else {
            eating = false

            if (canStart) {
                pulsing = false
                eating = false
                BlinkUtils.setBlinkState(off = true, release = true)
            }

            if (stuck.get()) {
                StuckUtils.stopStuck()
            }
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val scaledScreen = ScaledResolution(mc)
        drawProgressBar(scaledScreen.scaledWidth.toFloat(), scaledScreen.scaledHeight.toFloat())
    }

    private fun drawProgressBar(width: Float, height: Float) {
        val progressLength = 140F
        val startY = height / 4 * 3
        val startX = width / 2 - progressLength / 2

        val progressRatio = (c03s.toFloat() / 32F).coerceIn(0f, 1f)
        val currentProgress = progressLength * progressRatio

        // Shadow effect
        shadowRenderUtils.drawGlowWithCustomAlpha(startX - 2, startY - 2, progressLength + 4, 11F, 0.3F)

        // Background
        RenderUtils.drawRoundedRect(startX, startY, startX + progressLength, startY + 7F, 2F, Color(0, 0, 0, 128).rgb)

        // Progress bar
        if (currentProgress != 0f) {
            RenderUtils.drawRoundedRect(startX, startY, startX + currentProgress, startY + 7F, 2F, Color(76, 157, 240, 255).rgb)
        }
    }
}
