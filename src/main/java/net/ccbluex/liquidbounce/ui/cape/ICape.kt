/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.cape

import net.minecraft.util.ResourceLocation

interface ICape {

    val name: String

    val cape: ResourceLocation

    fun finalize()
}