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

/**
 * Skidded from Meowtils AutoBlacklist
 * Automatically adds players to the blacklist for selected events
 */
object AutoBlacklist : Module(name = "AutoBlacklist", category = ModuleCategory.ANTI_SNIPE, description = "自动将可疑玩家加入黑名单") {
    
    private val showNotifications = BoolValue("ShowNotifications", true)
    private val forFlags = BoolValue("ForFlags", true)
    private val flagAutoblock = BoolValue("FlagAutoBlock", true)
    private val flagNoslow = BoolValue("FlagNoSlow", true)
    private val flagKillaura = BoolValue("FlagKillaura", true)
    private val flagLegitScaffold = BoolValue("FlagLegitScaffold", true)
    private val forReports = BoolValue("ForReports", true)
    private val whenReportCommand = BoolValue("WhenReportCommand", true)
    private val whenWdrCommand = BoolValue("WhenWdrCommand", true)
    
    private val blacklistedPlayers = HashMap<String, MutableList<String>>()
    
    override fun onEnable() {
        blacklistedPlayers.clear()
    }
    
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        
        val packet = event.packet
        if (packet is S02PacketChat) {
            val message = packet.chatComponent.unformattedText
            
            // 检测 /report 命令
            if (forReports.get() && whenReportCommand.get() && message.contains("/report")) {
                extractAndBlacklist(message, "Report command")
            }
            
            // 检测 /wdr 命令
            if (forReports.get() && whenWdrCommand.get() && message.contains("/wdr")) {
                extractAndBlacklist(message, "WDR command")
            }
        }
    }
    
    private fun extractAndBlacklist(message: String, reason: String) {
        // 简单的名字提取逻辑
        val words = message.split(" ")
        for (word in words) {
            if (word.matches(Regex("^[A-Za-z0-9_]{3,16}$"))) {
                if (word != mc.thePlayer.name && !word.startsWith("/")) {
                    blacklistPlayer(word, reason)
                    break
                }
            }
        }
    }
    
    fun blacklistPlayer(playerName: String, reason: String) {
        if (!forFlags.get()) return
        
        val reasons = blacklistedPlayers.computeIfAbsent(playerName) { mutableListOf() }
        if (!reasons.contains(reason)) {
            reasons.add(reason)
        }
        
        if (showNotifications.get()) {
            val message = ChatComponentText(
                "${EnumChatFormatting.RED}Auto-blacklisted ${EnumChatFormatting.RESET}$playerName${EnumChatFormatting.RED}."
            )
            mc.thePlayer.addChatMessage(message)
        }
    }
    
    fun isBlacklisted(playerName: String): Boolean {
        return blacklistedPlayers.containsKey(playerName)
    }
    
    fun getReasons(playerName: String): List<String> {
        return blacklistedPlayers[playerName] ?: emptyList()
    }
    
    override fun onDisable() {
        blacklistedPlayers.clear()
    }
}
