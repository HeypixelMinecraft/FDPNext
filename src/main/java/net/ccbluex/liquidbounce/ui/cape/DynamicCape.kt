/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.cape

import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage

abstract class DynamicCape(override val name: String) : ICape {

    protected val frames = mutableListOf<BufferedImage>()
    protected val delays = mutableListOf<Int>()
    protected var playTime = 0
    protected val path = "FDPNext/cape/${name.lowercase().replace(" ","_")}_frame"

    override val cape: ResourceLocation
        get() {
            val frameTime = System.currentTimeMillis() % playTime
            var frameId = 0
            for(i in delays.indices) {
                if(frameTime < delays[i]) {
                    break
                }
                frameId = i
            }
            return ResourceLocation(path + frameId)
        }

    override fun finalize() {
        val mc = Minecraft.getMinecraft()
        for (i in 0 until frames.size) {
            mc.textureManager.deleteTexture(ResourceLocation(path + i))
        }
    }
}