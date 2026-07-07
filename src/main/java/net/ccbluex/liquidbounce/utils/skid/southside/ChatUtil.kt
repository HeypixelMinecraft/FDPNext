/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.skid.southside

import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraft.util.ChatComponentText

object ChatUtil {
    
    /**
     * Send an info message to chat
     * @param message Message to display
     */
    fun info(message: String) {
        ClientUtils.displayChatMessage("§7[Southside] §f$message")
    }
    
    /**
     * Send a warning message to chat
     * @param message Message to display
     */
    fun warn(message: String) {
        ClientUtils.displayChatMessage("§7[Southside] §e$message")
    }
    
    /**
     * Send an error message to chat
     * @param message Message to display
     */
    fun error(message: String) {
        ClientUtils.displayChatMessage("§7[Southside] §c$message")
    }
}
