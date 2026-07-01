/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.antisnipe

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import java.util.*

/**
 * Skidded from Meowtils AntiCheat
 * Detects suspicious behaviour of players around you
 */
object AntiCheat : Module(name = "AntiCheat", category = ModuleCategory.ANTI_SNIPE, description = "检测周围玩家的可疑行为") {
    
    private val violationLevel = IntegerValue("ViolationLevel", 1, 0, 10)
    private val flagSound = BoolValue("FlagSound", true)
    private val wdrButton = BoolValue("WDRButton", true)
    private val autoBlockCheck = BoolValue("AutoBlock", true)
    private val noSlowCheck = BoolValue("NoSlow", true)
    private val killauraCheck = BoolValue("Killaura", true)
    private val legitScaffoldCheck = BoolValue("LegitScaffold", true)
    
    private val anticheatData = HashMap<UUID, AntiCheatData>()
    private val violationLevels = HashMap<String, HashMap<String, Int>>()
    
    private var tickCounter = 0
    
    override fun onEnable() {
        anticheatData.clear()
        violationLevels.clear()
    }
    
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        
        if (++tickCounter < 20) return
        tickCounter = 0
        
        for (player in mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.name == null) continue
            
            val data = anticheatData.computeIfAbsent(player.uniqueID) { AntiCheatData() }
            
            // 检测 AutoBlock
            if (autoBlockCheck.get() && checkAutoBlock(player, data)) {
                if (incrementViolation(player.name, "AutoBlock")) {
                    sendFlagMessage(player.name, "AutoBlock")
                    data.resetAutoBlock()
                }
            }
            
            // 检测 NoSlow
            if (noSlowCheck.get() && checkNoSlow(player, data)) {
                if (incrementViolation(player.name, "NoSlow")) {
                    sendFlagMessage(player.name, "NoSlow")
                    data.resetNoSlow()
                }
            }
            
            // 检测 LegitScaffold
            if (legitScaffoldCheck.get() && checkLegitScaffold(player, data)) {
                if (incrementViolation(player.name, "LegitScaffold")) {
                    sendFlagMessage(player.name, "LegitScaffold")
                    data.resetLegitScaffold()
                }
            }
            
            // 检测 Killaura
            if (killauraCheck.get() && checkKillaura(player, data)) {
                if (incrementViolation(player.name, "Killaura")) {
                    sendFlagMessage(player.name, "Killaura")
                    data.resetKillaura()
                }
            }
        }
    }
    
    private fun checkAutoBlock(player: EntityPlayer, data: AntiCheatData): Boolean {
        // 检测 AutoBlock 的逻辑
        if (player.isBlocking && player.swingProgressInt > 0) {
            data.autoBlockViolations++
            return data.autoBlockViolations >= 3
        }
        data.autoBlockViolations = 0
        return false
    }
    
    private fun checkNoSlow(player: EntityPlayer, data: AntiCheatData): Boolean {
        // 检测 NoSlow 的逻辑
        if (player.isUsingItem && player.moveForward > 0.2f) {
            val expectedSpeed = if (player.isSprinting) 0.28f else 0.22f
            val actualSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ).toFloat()
            
            if (actualSpeed > expectedSpeed * 1.5f) {
                data.noSlowViolations++
                return data.noSlowViolations >= 3
            }
        }
        data.noSlowViolations = 0
        return false
    }
    
    private fun checkLegitScaffold(player: EntityPlayer, data: AntiCheatData): Boolean {
        // 检测 LegitScaffold 的逻辑
        if (player.isSneaking && player.onGround) {
            val blockBelow = mc.theWorld.getBlockState(
                net.minecraft.util.BlockPos(player.posX, player.posY - 1, player.posZ)
            ).block
            
            if (blockBelow.toString().contains("air", ignoreCase = true)) {
                data.legitScaffoldViolations++
                return data.legitScaffoldViolations >= 5
            }
        }
        data.legitScaffoldViolations = 0
        return false
    }
    
    private fun checkKillaura(player: EntityPlayer, data: AntiCheatData): Boolean {
        // 检测 Killaura 的逻辑
        if (player.swingProgressInt > 0) {
            val nearbyEntities = mc.theWorld.getEntitiesInAABBexcluding(
                player,
                player.entityBoundingBox.expand(4.0, 2.0, 4.0),
                { it is EntityPlayer && it != mc.thePlayer }
            )
            
            if (nearbyEntities.isNotEmpty()) {
                data.killauraViolations++
                return data.killauraViolations >= 10
            }
        }
        data.killauraViolations = 0
        return false
    }
    
    private fun incrementViolation(playerName: String, checkType: String): Boolean {
        val playerViolations = violationLevels.computeIfAbsent(playerName) { HashMap() }
        val newLevel = playerViolations.getOrDefault(checkType, 0) + 1
        playerViolations[checkType] = newLevel
        
        if (newLevel >= violationLevel.get()) {
            playerViolations[checkType] = 0
            return true
        }
        return false
    }
    
    private fun sendFlagMessage(playerName: String, checkType: String) {
        val message = ChatComponentText(
            "${EnumChatFormatting.GRAY}[${EnumChatFormatting.RED}AntiCheat${EnumChatFormatting.GRAY}] " +
            "${EnumChatFormatting.YELLOW}$playerName ${EnumChatFormatting.GRAY}failed " +
            "${EnumChatFormatting.RED}$checkType"
        )
        
        if (wdrButton.get()) {
            val wdrText = ChatComponentText(
                " ${EnumChatFormatting.DARK_GRAY}[${EnumChatFormatting.AQUA}WDR${EnumChatFormatting.DARK_GRAY}]"
            )
            message.appendSibling(wdrText)
        }
        
        mc.thePlayer.addChatMessage(message)
        
        if (flagSound.get()) {
            mc.thePlayer.playSound("random.orb", 1.0f, 1.0f)
        }
    }
    
    override fun onDisable() {
        anticheatData.clear()
        violationLevels.clear()
    }
}

class AntiCheatData {
    var autoBlockViolations = 0
    var noSlowViolations = 0
    var legitScaffoldViolations = 0
    var killauraViolations = 0
    
    fun resetAutoBlock() {
        autoBlockViolations = 0
    }
    
    fun resetNoSlow() {
        noSlowViolations = 0
    }
    
    fun resetLegitScaffold() {
        legitScaffoldViolations = 0
    }
    
    fun resetKillaura() {
        killauraViolations = 0
    }
}
