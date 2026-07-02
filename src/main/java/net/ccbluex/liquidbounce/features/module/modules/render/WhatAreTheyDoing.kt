/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.watut.WatutActivity
import net.ccbluex.liquidbounce.utils.watut.WatutGuiState
import net.ccbluex.liquidbounce.utils.watut.WatutManager
import net.ccbluex.liquidbounce.utils.watut.WatutPreviewQuality
import net.ccbluex.liquidbounce.utils.watut.WatutStatus
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S3FPacketCustomPayload
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

class WhatAreTheyDoing : Module(
    name = "WhatAreTheyDoing",
    description = "Shows WATUT-style player activity, GUI previews, particles and arm poses.",
    category = ModuleCategory.RENDER
) {
    private val syncValue = BoolValue("Sync", true)
    private val screenPreviewValue = BoolValue("ScreenPreview", true)
    private val particlesValue = BoolValue("Particles", true)
    private val armPoseValue = BoolValue("ArmPose", true)
    private val showTextValue = BoolValue("ShowText", true)
    private val showOwnStatusValue = BoolValue("ShowOwnStatus", false)
    private val idleSecondsValue = IntegerValue("IdleSeconds", 20, 5, 120)
    private val maxDistanceValue = FloatValue("MaxDistance", 64F, 8F, 128F)
    private val previewScaleValue = FloatValue("PreviewScale", 1F, 0.5F, 2F)
    private val previewFpsValue = IntegerValue("PreviewFps", 2, 1, 10)
    private val previewQualityValue = ListValue("PreviewQuality", arrayOf("Low", "Medium", "High"), "Medium")

    override fun onEnable() {
        syncSettings()
        WatutManager.enabled = true
    }

    override fun onDisable() {
        WatutManager.enabled = false
        WatutManager.reset()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        WatutManager.reset()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        syncSettings()
        WatutManager.updateLocal()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (event.type == PacketEvent.Type.RECEIVE && packet is S3FPacketCustomPayload) {
            WatutManager.handlePayload(packet)
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val world = mc.theWorld ?: return
        val player = mc.thePlayer ?: return

        for (entity in world.playerEntities) {
            if (entity !is EntityPlayer || entity.isDead || entity.isInvisible) {
                continue
            }

            if (entity == player && !showOwnStatusValue.get()) {
                continue
            }

            if (player.getDistanceToEntity(entity) > maxDistanceValue.get()) {
                continue
            }

            val status = WatutManager.statusFor(entity.uniqueID) ?: continue
            if (!status.visible) {
                continue
            }

            renderWatut(entity, status, event.partialTicks)
        }
    }

    private fun syncSettings() {
        WatutManager.syncEnabled = syncValue.get()
        WatutManager.screenPreviewEnabled = screenPreviewValue.get()
        WatutManager.particlesEnabled = particlesValue.get()
        WatutManager.armPoseEnabled = armPoseValue.get()
        WatutManager.showOwnStatus = showOwnStatusValue.get()
        WatutManager.idleSeconds = idleSecondsValue.get()
        WatutManager.maxDistance = maxDistanceValue.get()
        WatutManager.previewFps = previewFpsValue.get()
        WatutManager.previewQuality = WatutPreviewQuality.byName(previewQualityValue.get())
    }

    private fun renderWatut(entity: EntityPlayer, status: WatutStatus, partialTicks: Float) {
        val renderManager = mc.renderManager
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.renderPosX
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.renderPosY + entity.eyeHeight + 0.65
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.renderPosZ

        GL11.glPushMatrix()
        GL11.glTranslated(x, y, z)
        GL11.glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        GL11.glRotatef(renderManager.playerViewX, 1F, 0F, 0F)

        RenderUtils.enableGlCap(GL11.GL_BLEND)
        RenderUtils.disableGlCap(GL11.GL_LIGHTING, GL11.GL_DEPTH_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        if (screenPreviewValue.get() && status.screen != null) {
            renderScreen(status)
        }

        if (particlesValue.get()) {
            renderParticles(status)
        }

        if (showTextValue.get()) {
            renderLabel(status)
        }

        RenderUtils.resetCaps()
        GL11.glPopMatrix()
    }

    private fun renderLabel(status: WatutStatus) {
        val font = Fonts.font40 ?: Fonts.minecraftFont
        val text = statusText(status)
        val scale = 0.018F
        GL11.glPushMatrix()
        GL11.glScalef(-scale, -scale, scale)

        val width = font.getStringWidth(text) / 2F
        RenderUtils.quickDrawRect(-width - 5F, -4F, width + 5F, font.FONT_HEIGHT + 4F, Color(0, 0, 0, 120).rgb)
        font.drawString(text, -width + 1F, 1.5F, Color.WHITE.rgb, true)

        GL11.glPopMatrix()
    }

    private fun renderScreen(status: WatutStatus) {
        val screen = status.screen ?: return
        val scale = previewScaleValue.get()
        val width = 1.25F * scale
        val height = width * screen.height.toFloat() / screen.width.toFloat()
        val yOffset = -height - 0.15F

        GL11.glPushMatrix()
        GL11.glTranslatef(0F, yOffset, 0F)
        GlStateManager.color(1F, 1F, 1F, 0.92F)
        GlStateManager.enableTexture2D()
        mc.textureManager.bindTexture(screen.location)

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos((-width / 2).toDouble(), height.toDouble(), 0.0).tex(0.0, 0.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), height.toDouble(), 0.0).tex(1.0, 0.0).endVertex()
        worldRenderer.pos((width / 2).toDouble(), 0.0, 0.0).tex(1.0, 1.0).endVertex()
        worldRenderer.pos((-width / 2).toDouble(), 0.0, 0.0).tex(0.0, 1.0).endVertex()
        tessellator.draw()

        GlStateManager.disableTexture2D()
        RenderUtils.quickDrawRect(-width / 2, 0F, width / 2, 0.018F, Color(70, 160, 255, 210).rgb)
        RenderUtils.quickDrawRect(-width / 2, height - 0.018F, width / 2, height, Color(70, 160, 255, 210).rgb)
        RenderUtils.quickDrawRect(-width / 2, 0F, -width / 2 + 0.018F, height, Color(70, 160, 255, 210).rgb)
        RenderUtils.quickDrawRect(width / 2 - 0.018F, 0F, width / 2, height, Color(70, 160, 255, 210).rgb)

        val mx = -width / 2 + width * status.mouseX
        val my = height * (1F - status.mouseY)
        val cursorSize = if (status.mousePressed) 0.055F else 0.04F
        RenderUtils.quickDrawRect(mx - cursorSize, my - cursorSize, mx + cursorSize, my + cursorSize, Color(255, 255, 255, 230).rgb)
        GlStateManager.enableTexture2D()
        GL11.glPopMatrix()
    }

    private fun renderParticles(status: WatutStatus) {
        GlStateManager.disableTexture2D()
        val ticks = (System.currentTimeMillis() % 10_000L) / 1000.0
        val color = when (status.activity) {
            WatutActivity.TYPING -> Color(120, 190, 255, 180)
            WatutActivity.IN_GUI -> Color(130, 255, 190, 150)
            WatutActivity.IDLE -> Color(180, 180, 180, 130)
            WatutActivity.ACTIVE -> Color(255, 255, 255, 80)
        }

        val amount = when (status.activity) {
            WatutActivity.TYPING -> 5
            WatutActivity.IN_GUI -> 4
            WatutActivity.IDLE -> 3
            WatutActivity.ACTIVE -> 0
        }

        for (i in 0 until amount) {
            val angle = ticks * 1.5 + i * 1.7
            val radius = 0.18 + i * 0.025
            val px = cos(angle).toFloat() * radius.toFloat()
            val py = (-0.35F + sin(angle * 1.4).toFloat() * 0.08F) - i * 0.035F
            val size = 0.025F + status.typingAmplifier * 0.02F
            RenderUtils.quickDrawRect(px - size, py - size, px + size, py + size, color.rgb)
        }
        GlStateManager.enableTexture2D()
    }

    private fun statusText(status: WatutStatus): String {
        return when (status.activity) {
            WatutActivity.TYPING -> "Typing..."
            WatutActivity.IDLE -> "Idle"
            WatutActivity.IN_GUI -> guiText(status.guiState)
            WatutActivity.ACTIVE -> guiText(status.guiState).takeIf { it != "None" } ?: "Active"
        }
    }

    private fun guiText(state: WatutGuiState): String {
        return when (state) {
            WatutGuiState.NONE -> "None"
            WatutGuiState.CHAT -> "Chat"
            WatutGuiState.INVENTORY -> "Inventory"
            WatutGuiState.CHEST -> "Chest"
            WatutGuiState.CRAFTING -> "Crafting"
            WatutGuiState.PAUSE -> "Menu"
            WatutGuiState.SIGN -> "Sign"
            WatutGuiState.BOOK -> "Book"
            WatutGuiState.ANVIL -> "Anvil"
            WatutGuiState.ENCHANTING -> "Enchanting"
            WatutGuiState.FURNACE -> "Furnace"
            WatutGuiState.DISPENSER -> "Dispenser"
            WatutGuiState.HOPPER -> "Hopper"
            WatutGuiState.BEACON -> "Beacon"
            WatutGuiState.MERCHANT -> "Trading"
            WatutGuiState.OTHER -> "GUI"
        }
    }
}
