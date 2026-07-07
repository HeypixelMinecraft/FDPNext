/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.skid.southside

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import java.awt.Color

object RenderUtil : MinecraftInstance() {
    
    /**
     * Draw outlined bounding box around a block position
     * @param pos Block position
     * @param lineWidth Width of the outline
     * @param color Color of the outline
     */
    fun drawOutlinedBoundingBox(pos: BlockPos, lineWidth: Int, color: Color) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        
        val x = pos.x.toDouble()
        val y = pos.y.toDouble()
        val z = pos.z.toDouble()
        
        val box = AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0)
        
        // Draw outline using block position directly
        RenderUtils.drawBlockBox(pos, color, false)
    }
    
    /**
     * Draw filled bounding ESP box
     * @param box Axis aligned bounding box
     * @param color Fill color
     */
    fun boundingESPBoxFilled(box: AxisAlignedBB, color: Color) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        
        // Convert AxisAlignedBB to BlockPos for drawing
        val minX = box.minX.toInt()
        val minY = box.minY.toInt()
        val minZ = box.minZ.toInt()
        val pos = BlockPos(minX, minY, minZ)
        
        RenderUtils.drawBlockBox(pos, color, true)
    }
}
