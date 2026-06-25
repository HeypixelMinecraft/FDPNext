/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client.button

import net.ccbluex.liquidbounce.features.module.modules.client.UIEffects
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shadowRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import java.awt.Color
import kotlin.math.sqrt

class RoundedButtonRenderer(button: GuiButton) : AbstractButtonRenderer(button) {
    override fun render(mouseX: Int, mouseY: Int, mc: Minecraft) {
        RenderUtils.drawRoundedCornerRect(button.xPosition.toFloat(), button.yPosition.toFloat(),
            button.xPosition + button.width.toFloat(), button.yPosition + button.height.toFloat(),
            sqrt((button.width * button.height).toDouble()).toFloat() * 0.1f,
            (if (button.hovered) {
                Color(60, 60, 60, 150)
            } else {
                Color(31, 31, 31, 150)
            }).rgb)
        if (UIEffects.buttonShadowValue.equals(true)) {
            shadowRenderUtils.drawShadowWithCustomAlpha(button.xPosition.toFloat(),
                button.yPosition.toFloat(),
                button.width.toFloat(),
                button.height.toFloat(),
                240f)
        }
    }
}
