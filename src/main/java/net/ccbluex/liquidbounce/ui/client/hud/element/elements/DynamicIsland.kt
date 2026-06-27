/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ColorValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.LiquidGlassShader
import net.minecraft.util.ResourceLocation
import java.awt.Color

/**
 * iOS-style "Dynamic Island" (灵动岛): a horizontal rounded pill at the top of the HUD with a
 * LiquidGlass refraction background, showing client logo + name, player name, server IP
 * (or "SinglePlayer"), and the live ping.
 */
@ElementInfo(name = "DynamicIsland")
class DynamicIsland : Element() {

    private val logoValue = BoolValue("Logo", true)
    private val clientNameValue = BoolValue("ClientName", true)
    private val playerNameValue = BoolValue("PlayerName", true)
    private val serverIpValue = BoolValue("ServerIP", true)
    private val pingValue = BoolValue("Ping", true)

    private val glassValue = BoolValue("Glass", true)
    private val roundnessValue = FloatValue("Roundness", 1f, 0f, 1f)
    private val heightValue = FloatValue("Height", 18f, 12f, 30f)
    private val textColorValue = ColorValue("TextColor", Color(235, 238, 245).rgb)

    private val logo = ResourceLocation("FDPNext/misc/logo.png")

    private val pad = 7f
    private val gap = 7f

    override fun createElement(): Boolean {
        // Default to roughly top-center; user can drag in the HUD designer.
        side = Side(Side.Horizontal.MIDDLE, Side.Vertical.UP)
        x = 85.0
        y = 4.0
        return true
    }

    override fun drawElement(partialTicks: Float): Border {
        val font = Fonts.font35
        val h = heightValue.get()
        val logoSize = (h - 6f)
        val textColor = textColorValue.get()

        val parts = ArrayList<Pair<String, Int>>()
        if (clientNameValue.get()) parts.add(FDPNext.CLIENT_NAME to textColor)
        if (playerNameValue.get()) parts.add(mc.session.username to textColor)
        if (serverIpValue.get()) parts.add((mc.currentServerData?.serverIP ?: "SinglePlayer") to textColor)
        if (pingValue.get()) {
            val ping = currentPing()
            parts.add("${ping}ms" to pingColor(ping))
        }

        // Measure pill width
        var w = pad
        if (logoValue.get()) w += logoSize + gap
        parts.forEachIndexed { i, p ->
            w += font.getStringWidth(p.first)
            if (i != parts.lastIndex) w += gap
        }
        w += pad

        val roundness = roundnessValue.get()

        // Background
        if (glassValue.get() && LiquidGlassShader.isAvailable()) {
            LiquidGlassShader.draw(renderX, renderY, w, h, scale, roundness)
        } else {
            BlurUtils.draw((renderX * scale).toFloat(), (renderY * scale).toFloat(), w * scale, h * scale, 6f)
            RenderUtils.drawRoundedCornerRect(0f, 0f, w, h, (h / 2f) * roundness, Color(18, 18, 26, 170).rgb)
        }

        // Foreground row
        var cx = pad
        if (logoValue.get()) {
            RenderUtils.drawImage(logo, cx.toInt(), ((h - logoSize) / 2f).toInt(), logoSize.toInt(), logoSize.toInt())
            cx += logoSize + gap
        }
        // +3 compensates GameFontRenderer.drawString's internal `y - 3F`, so text centers on the logo.
        val textY = (h - font.height) / 2f + 3f
        parts.forEachIndexed { i, p ->
            font.drawString(p.first, cx, textY, p.second)
            cx += font.getStringWidth(p.first)
            if (i != parts.lastIndex) cx += gap
        }

        return Border(0f, 0f, w, h)
    }

    private fun currentPing(): Int {
        val player = mc.thePlayer ?: return 0
        return mc.netHandler?.getPlayerInfo(player.uniqueID)?.responseTime?.coerceAtLeast(0) ?: 0
    }

    private fun pingColor(ping: Int): Int = when {
        ping < 80 -> Color(80, 220, 120)
        ping < 150 -> Color(230, 200, 80)
        else -> Color(220, 90, 90)
    }.rgb
}
