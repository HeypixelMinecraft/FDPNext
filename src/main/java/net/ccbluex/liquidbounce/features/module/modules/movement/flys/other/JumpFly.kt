package net.ccbluex.liquidbounce.features.module.modules.movement.flys.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flys.FlyMode
import net.minecraft.block.BlockLadder
import net.minecraft.block.material.Material
import net.minecraft.util.AxisAlignedBB

class JumpFly : FlyMode("Jump") {

    private var jumpY = 0.0

    override fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null) return
        if (mc.thePlayer.onGround && !mc.thePlayer.isJumping)
            mc.thePlayer.jump()
        if ((mc.gameSettings.keyBindJump.isKeyDown && !mc.gameSettings.keyBindSneak.isKeyDown) || mc.thePlayer.onGround)
            jumpY = mc.thePlayer.posY
    }

    override fun onBlockBB(event: BlockBBEvent) {
        val jumpYCondition =
            if (!mc.gameSettings.keyBindJump.isKeyDown && mc.gameSettings.keyBindSneak.isKeyDown) event.y.toDouble() < jumpY else event.y.toDouble() <= jumpY
        if ((!event.block.material.blocksMovement() && event.block.material != Material.carpet && event.block.material != Material.vine && event.block.material != Material.snow && event.block !is BlockLadder) && jumpYCondition) {
            event.boundingBox = AxisAlignedBB.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x.toDouble() + 1,
                1.0,
                event.z.toDouble() + 1
            )
        }
    }
}
