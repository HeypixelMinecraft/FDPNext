/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client.button

import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.util.ResourceLocation

class BadlionTwoButtonRenderer(button: GuiButton) : AbstractButtonRenderer(button) {
    override fun render(mouseX: Int, mouseY: Int, mc: Minecraft) {
        val hoveredimg = ResourceLocation("FDPNext/ui/buttons/bhover.png")
        val elseimg = ResourceLocation("FDPNext/ui/buttons/bbutton.png")
        if(button.hovered) { RenderUtils.drawImage(hoveredimg, button.xPosition, button.yPosition, button.width, button.height) } else { RenderUtils.drawImage(elseimg, button.xPosition, button.yPosition, button.width, button.height) }
    }
}