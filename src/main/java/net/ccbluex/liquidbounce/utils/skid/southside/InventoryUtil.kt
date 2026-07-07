/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.skid.southside

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.features.module.modules.world.SouthsideScaffold
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack

object InventoryUtil : MinecraftInstance() {
    
    /**
     * Automatically switch to a block slot in hotbar
     * @return true if successfully switched or already holding a block
     */
    fun switchBlock(): Boolean {
        val player = mc.thePlayer ?: return false
        
        // Check if current slot has a valid block
        val currentStack = player.inventory.mainInventory[player.inventory.currentItem]
        if (currentStack != null && currentStack.item is ItemBlock) {
            val itemBlock = currentStack.item as ItemBlock
            if (SouthsideScaffold.isValid(itemBlock)) {
                return true
            }
        }
        
        // Search for a valid block in hotbar (slots 0-8, which are 36-44 in inventory)
        for (i in 36..44) {
            val stack = player.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item is ItemBlock) {
                val itemBlock = stack.item as ItemBlock
                if (SouthsideScaffold.isValid(itemBlock)) {
                    // Switch to this slot
                    val hotbarSlot = i - 36
                    if (hotbarSlot != player.inventory.currentItem) {
                        swap(i, hotbarSlot)
                    }
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Swap items between two slots
     * @param fromSlot Source slot index (0-44)
     * @param toSlot Destination slot index (0-44)
     */
    fun swap(fromSlot: Int, toSlot: Int) {
        val player = mc.thePlayer ?: return
        mc.playerController.windowClick(
            player.inventoryContainer.windowId,
            fromSlot,
            toSlot,
            2,
            player
        )
    }
}
