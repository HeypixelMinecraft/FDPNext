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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import java.util.*

/**
 * Skidded from Meowtils SniperWarning
 * Warns you of certain players that may be snipers
 */
object SniperWarning : Module(name = "SniperWarning", category = ModuleCategory.ANTI_SNIPE, description = "警告你可能被狙击的玩家") {
    
    private val checkGear = BoolValue("CheckGear", true)
    private val checkName = BoolValue("CheckName", true)
    private val sound = BoolValue("Sound", true)
    
    private val sniperNames = arrayOf(
        "mcalt_", "mcalts_", "hassalt_", "dogalt_", "mal_", "bym_", "jy6_", "lf_", "wg_", 
        "ggnekito", "dahai_", "tzi", "nicegen", "opalalts", "msmc", "myau", "vape", "snipe", 
        "nicealts", "rave", "alt", "client", "hack", "hax", "fernan", "watchdog", "anticheat"
    )
    
    private val alertedPlayers = HashSet<String>()
    private var tickCounter = 0
    
    override fun onEnable() {
        alertedPlayers.clear()
    }
    
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        
        if (++tickCounter < 20) return
        tickCounter = 0
        
        for (player in mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.name == null) continue
            if (alertedPlayers.contains(player.name)) continue
            
            // 检查装备
            if (checkGear.get() && isSniperGear(player)) {
                alert(player.name, "Iron Sword + Chainmail Armor")
                alertedPlayers.add(player.name)
                continue
            }
            
            // 检查名字
            if (checkName.get() && isSniperName(player)) {
                alert(player.name, "Suspicious Name")
                alertedPlayers.add(player.name)
            }
        }
    }
    
    private fun isSniperGear(player: EntityPlayer): Boolean {
        var hasChain = false
        var hasIronSword = false
        
        // 检查护甲
        for (armor in player.inventory.armorInventory) {
            if (armor != null && armor.item is net.minecraft.item.ItemArmor) {
                val itemArmor = armor.item as net.minecraft.item.ItemArmor
                if (itemArmor.material == net.minecraft.item.ItemArmor.ArmorMaterial.CHAIN) {
                    hasChain = true
                    break
                }
            }
        }
        
        // 检查主手武器
        val heldItem = player.heldItem
        if (heldItem != null && heldItem.item is net.minecraft.item.ItemSword) {
            val sword = heldItem.item as net.minecraft.item.ItemSword
            if (sword.toolMaterialName.equals("IRON", ignoreCase = true)) {
                hasIronSword = true
            }
        }
        
        return hasChain && hasIronSword
    }
    
    private fun isSniperName(player: EntityPlayer): Boolean {
        val name = player.name.toLowerCase()
        return sniperNames.any { name.contains(it.toLowerCase()) }
    }
    
    private fun alert(name: String, reason: String) {
        val message = ChatComponentText(
            "${EnumChatFormatting.RED}Warning: ${EnumChatFormatting.RESET}$name " +
            "${EnumChatFormatting.GRAY}might be a sniper! ${EnumChatFormatting.DARK_GRAY}($reason)"
        )
        mc.thePlayer.addChatMessage(message)
        
        if (sound.get()) {
            mc.thePlayer.playSound("random.orb", 1.0f, 0.5f)
        }
    }
    
    override fun onDisable() {
        alertedPlayers.clear()
    }
}
