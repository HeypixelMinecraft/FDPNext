/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.antisnipe

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/**
 * Skidded from Meowtils PartyDetector
 * Detects when a party joins your lobby
 */
object PartyDetector : Module(name = "PartyDetector", category = ModuleCategory.ANTI_SNIPE, description = "检测是否有队伍加入你的大厅") {
    
    private val playerThreshold = IntegerValue("PlayerThreshold", 3, 2, 8)
    private val timeWindow = IntegerValue("TimeWindow", 1000, 500, 3000)
    private val sound = BoolValue("Sound", true)
    private val showMissed = BoolValue("ShowMissed", true)
    
    private var playerCounter = 0
    private var lastJoinTime = 0L
    private var countingPlayers = false
    private var missedCounter = 0
    private var alertedMissed = false
    private var tickCounter = 0
    private var gameStarted = false
    
    override fun onEnable() {
        reset()
    }
    
    @EventTarget
    fun onWorld(event: WorldEvent) {
        reset()
    }
    
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        
        // 检测玩家加入
        for (player in mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue
            
            val now = System.currentTimeMillis()
            if (!countingPlayers) {
                lastJoinTime = now
            }
            
            if (now - lastJoinTime <= timeWindow.get()) {
                playerCounter++
                countingPlayers = true
            } else {
                countingPlayers = false
                lastJoinTime = 0L
                playerCounter = 0
            }
            
            if (playerCounter >= playerThreshold.get()) {
                sendPartyWarning(playerCounter)
                playerCounter = 0
                lastJoinTime = 0L
                countingPlayers = false
                break
            }
        }
        
        // 显示错过的玩家
        if (showMissed.get() && !alertedMissed) {
            if (++tickCounter >= 10) {
                tickCounter = 0
                for (player in mc.theWorld.playerEntities) {
                    if (player == null || player == mc.thePlayer) continue
                    missedCounter++
                }
                
                if (missedCounter > 0) {
                    val message = ChatComponentText(
                        "${EnumChatFormatting.GRAY}Missed players: ${EnumChatFormatting.YELLOW}$missedCounter"
                    )
                    mc.thePlayer.addChatMessage(message)
                }
                alertedMissed = true
            }
        }
    }
    
    private fun sendPartyWarning(count: Int) {
        val message = ChatComponentText(
            "${EnumChatFormatting.RED}Warning: ${EnumChatFormatting.YELLOW}$count " +
            "${EnumChatFormatting.WHITE}players joined! ${EnumChatFormatting.DARK_GRAY}(${EnumChatFormatting.BLUE}Party${EnumChatFormatting.DARK_GRAY})"
        )
        mc.thePlayer.addChatMessage(message)
        
        if (sound.get()) {
            mc.thePlayer.playSound("random.orb", 1.0f, 0.5f)
        }
    }
    
    private fun reset() {
        playerCounter = 0
        lastJoinTime = 0L
        countingPlayers = false
        alertedMissed = false
        missedCounter = 0
        tickCounter = 0
        gameStarted = false
    }
    
    override fun onDisable() {
        reset()
    }
}
