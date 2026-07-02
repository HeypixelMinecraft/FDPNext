/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.watut

import net.minecraft.client.model.ModelBiped
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.PI
import kotlin.math.sin

object WatutModelPose {
    private const val DEG = (PI / 180.0).toFloat()

    @JvmStatic
    fun apply(player: EntityPlayer, model: ModelBiped) {
        if (!WatutManager.enabled || !WatutManager.armPoseEnabled) {
            return
        }

        val status = WatutManager.poseStatusFor(player.uniqueID) ?: return
        val blend = status.poseBlend.coerceIn(0F, 1F)
        if (blend <= 0F) {
            return
        }

        when (status.activity) {
            WatutActivity.TYPING -> applyTyping(model, status, blend)
            WatutActivity.IN_GUI -> applyGui(model, status, blend)
            WatutActivity.IDLE -> applyIdle(model, blend)
            WatutActivity.ACTIVE -> Unit
        }
    }

    private fun applyTyping(model: ModelBiped, status: WatutStatus, blend: Float) {
        val time = System.currentTimeMillis() / 90.0
        val amp = 0.55F + status.typingAmplifier * 0.45F
        val tap = sin(time).toFloat() * 8F * DEG * amp

        model.bipedRightArm.rotateAngleX = mix(model.bipedRightArm.rotateAngleX, -58F * DEG + tap, blend)
        model.bipedLeftArm.rotateAngleX = mix(model.bipedLeftArm.rotateAngleX, -58F * DEG - tap, blend)
        model.bipedRightArm.rotateAngleY = mix(model.bipedRightArm.rotateAngleY, -14F * DEG, blend)
        model.bipedLeftArm.rotateAngleY = mix(model.bipedLeftArm.rotateAngleY, 14F * DEG, blend)
        model.bipedRightArm.rotateAngleZ = mix(model.bipedRightArm.rotateAngleZ, 8F * DEG, blend)
        model.bipedLeftArm.rotateAngleZ = mix(model.bipedLeftArm.rotateAngleZ, -8F * DEG, blend)
    }

    private fun applyGui(model: ModelBiped, status: WatutStatus, blend: Float) {
        val mouseX = (status.mouseX - 0.5F) * 28F * DEG
        val mouseY = (status.mouseY - 0.5F) * 22F * DEG

        model.bipedRightArm.rotateAngleX = mix(model.bipedRightArm.rotateAngleX, -72F * DEG - mouseY, blend)
        model.bipedLeftArm.rotateAngleX = mix(model.bipedLeftArm.rotateAngleX, -63F * DEG - mouseY * 0.65F, blend)
        model.bipedRightArm.rotateAngleY = mix(model.bipedRightArm.rotateAngleY, -18F * DEG + mouseX, blend)
        model.bipedLeftArm.rotateAngleY = mix(model.bipedLeftArm.rotateAngleY, 18F * DEG + mouseX * 0.4F, blend)
        model.bipedRightArm.rotateAngleZ = mix(model.bipedRightArm.rotateAngleZ, 11F * DEG, blend)
        model.bipedLeftArm.rotateAngleZ = mix(model.bipedLeftArm.rotateAngleZ, -10F * DEG, blend)
        model.bipedHead.rotateAngleX = mix(model.bipedHead.rotateAngleX, model.bipedHead.rotateAngleX + 8F * DEG, blend * 0.35F)
    }

    private fun applyIdle(model: ModelBiped, blend: Float) {
        model.bipedRightArm.rotateAngleX = mix(model.bipedRightArm.rotateAngleX, 12F * DEG, blend * 0.6F)
        model.bipedLeftArm.rotateAngleX = mix(model.bipedLeftArm.rotateAngleX, 12F * DEG, blend * 0.6F)
        model.bipedHead.rotateAngleX = mix(model.bipedHead.rotateAngleX, 16F * DEG, blend * 0.5F)
    }

    private fun mix(current: Float, target: Float, blend: Float): Float {
        return current + (target - current) * blend.coerceIn(0F, 1F)
    }
}
