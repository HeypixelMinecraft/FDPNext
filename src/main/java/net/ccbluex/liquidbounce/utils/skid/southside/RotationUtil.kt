/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.skid.southside

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.*

object RotationUtil : MinecraftInstance() {
    
    /**
     * Set target rotation for silent aim
     */
    fun setTargetRotation(rotation: Rotation, ticks: Int = 0) {
        RotationUtils.setTargetRotation(rotation, ticks)
    }
    
    /**
     * Get rotation to a block position with specific mode
     * @param pos Block position to look at
     * @param mode 0F for normal calculation, 1F for alternative calculation
     * @return Rotation object with yaw and pitch
     */
    fun getRotationBlock(pos: BlockPos, mode: Float): Rotation {
        val player = mc.thePlayer ?: return Rotation(0f, 0f)
        
        val eyesPos = Vec3(
            player.posX,
            player.posY + player.getEyeHeight(),
            player.posZ
        )
        
        // Calculate center of the block
        val blockCenter = Vec3(
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5
        )
        
        val diffX = blockCenter.xCoord - eyesPos.xCoord
        val diffY = blockCenter.yCoord - eyesPos.yCoord
        val diffZ = blockCenter.zCoord - eyesPos.zCoord
        
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
        
        val yaw = MathHelper.wrapAngleTo180_float(
            (atan2(diffZ, diffX) * 180.0 / PI).toFloat() - 90f
        )
        val pitch = MathHelper.wrapAngleTo180_float(
            (-atan2(diffY, diffXZ) * 180.0 / PI).toFloat()
        )
        
        // Adjust based on mode
        if (mode == 1F) {
            // Alternative calculation for different aiming behavior
            return Rotation(yaw, pitch.coerceIn(-90f, 90f))
        }
        
        return Rotation(yaw, pitch)
    }
}
