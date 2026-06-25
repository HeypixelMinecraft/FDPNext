/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.cape

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage

class SingleImageCape(override val name: String, val image: BufferedImage) : ICape {
    override val cape = ResourceLocation("FDPNext/cape/${name.lowercase().replace(" ","_")}")

    init {
        Minecraft.getMinecraft().textureManager.loadTexture(cape, DynamicTexture(image))
    }

    override fun finalize() {
        Minecraft.getMinecraft().textureManager.deleteTexture(cape)
    }
}