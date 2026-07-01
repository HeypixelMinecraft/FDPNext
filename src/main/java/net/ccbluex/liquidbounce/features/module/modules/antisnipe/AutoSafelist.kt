/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.antisnipe

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import java.util.regex.Pattern

/**
 * Skidded from Meowtils AutoSafelist
 * Automatically safelists players that take a final death
 */
object AutoSafelist : Module(name = "AutoSafelist", category = ModuleCategory.ANTI_SNIPE, description = "自动将死亡玩家加入安全名单") {
    
    private val showMessage = BoolValue("ShowMessage", true)
    
    private val finalKillPattern = Pattern.compile("FINAL KILL!")
    private val playerPattern = Pattern.compile("^([A-Za-z0-9_]+)(?=[\\s'])")
    
    private val safelistedPlayers = HashSet<String>()
    
    override fun onEnable() {
        safelistedPlayers.clear()
    }
    
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        
        val packet = event.packet
        if (packet is S02PacketChat) {
            val message = packet.chatComponent.unformattedText
            
            if (finalKillPattern.matcher(message).find()) {
                val matcher = playerPattern.matcher(message)
                if (matcher.find()) {
                    val playerName = matcher.group(1)
                    
                    if (playerName == mc.thePlayer.name) return
                    if (safelistedPlayers.contains(playerName)) return
                    
                    safelistedPlayers.add(playerName)
                    
                    if (showMessage.get()) {
                        val chatMessage = ChatComponentText(
                            "${EnumChatFormatting.GREEN}Auto-safelisted ${EnumChatFormatting.RESET}$playerName${EnumChatFormatting.GREEN}."
                        )
                        mc.thePlayer.addChatMessage(chatMessage)
                    }
                }
            }
        }
    }
    
    fun isSafelisted(playerName: String): Boolean {
        return safelistedPlayers.contains(playerName)
    }
    
    override fun onDisable() {
        safelistedPlayers.clear()
    }
}
