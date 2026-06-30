/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * Scaffold2 - Skidded from edFDP
 * Original: net.ccbluex.liquidbounce.features.module.modules.world.Scaffold2
 *
 * 模式: Simple / SpeedBridge / Breezily / JitterBridge / TellyBridge
 * SafewalkType: Sneak / Safewalk / None (仅 Simple 模式生效)
 * SimpleDerpBridge: Simple 模式下的 Derp 桥接
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import kotlin.math.roundToInt

class Scaffold2 : Module(name = "Scaffold2", category = ModuleCategory.WORLD, keyBind = 0) {

    private val modeValue = ListValue("Mode", arrayOf("Simple", "SpeedBridge", "Breezily", "JitterBridge", "TellyBridge"), "Simple")
    private val safewalkValue = ListValue("SafewalkType", arrayOf("Sneak", "Safewalk", "None"), "Safewalk")
        .displayable { modeValue.equals("Simple") }
    private val derpValue = BoolValue("SimpleDerpBridge", false)
        .displayable { modeValue.equals("Simple") }

    private var playerRot = Rotation(0f, 0f)
    private var oldPlayerRot = Rotation(0f, 0f)
    private var lockRotation = Rotation(0f, 0f)
    private var camYaw = 0f
    private var camPitch = 0f
    private var prevSlot = 0
    private var fw = false
    private var bw = false
    private var left = false
    private var right = false
    private var breezily = false

    override fun onEnable() {
        prevSlot = mc.thePlayer.inventory.currentItem
    }

    override fun onDisable() {
        mc.thePlayer.inventory.currentItem = prevSlot
        correctControls(0)
        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        mc.gameSettings.keyBindSprint.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSprint)
        mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (modeValue.equals("Simple") && safewalkValue.equals("Safewalk")) {
            event.isSafeWalk = true
        }
    }

    @EventTarget
    fun onUpdate(e: UpdateEvent) {
        val blockSlot = InventoryUtils.findAutoBlockBlock()
        if (blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = blockSlot - 36
            mc.rightClickDelayTimer = 1
            mc.gameSettings.keyBindSprint.pressed = true
        }

        oldPlayerRot = Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        camYaw = mc.thePlayer.rotationYaw
        camPitch = mc.thePlayer.rotationPitch

        val mode = (modeValue.get() as String).lowercase()
        when (mode) {
            "breezily" -> {
                val rpitch = if ((camYaw / 45f).roundToInt() % 2 == 0) 79.6f else 76.3f
                playerRot = Rotation(camYaw + 180f, rpitch)
                lockRotation = RotationUtils.limitAngleChange(oldPlayerRot, playerRot, 60f)
                correctControls(1)
                mc.gameSettings.keyBindRight.pressed = false
                mc.gameSettings.keyBindLeft.pressed = false
                val blockBelow = mc.theWorld.getBlockState(
                    BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
                ).block
                if (blockBelow === Blocks.air && (camYaw / 45f).roundToInt() % 2 == 0) {
                    breezily = !breezily
                    mc.gameSettings.keyBindRight.pressed = breezily
                    mc.gameSettings.keyBindLeft.pressed = !breezily
                }
            }
            "simple" -> {
                val rpitch = if ((camYaw / 45f).roundToInt() % 2 == 0) {
                    if (safewalkValue.equals("None")) 79.0f else 83.2f
                } else {
                    if (safewalkValue.equals("None")) 76.3f else 78.1f
                }
                if (derpValue.get()) {
                    val blockBelow = mc.theWorld.getBlockState(
                        BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
                    ).block
                    playerRot = if (blockBelow === Blocks.air) {
                        Rotation(camYaw + 180f, rpitch)
                    } else if (mc.thePlayer.onGround && mc.gameSettings.keyBindJump.pressed) {
                        Rotation(camYaw + 31f, rpitch)
                    } else {
                        Rotation(camYaw + 45f, rpitch)
                    }
                    lockRotation = RotationUtils.limitAngleChange(oldPlayerRot, playerRot, 180f)
                } else {
                    playerRot = Rotation(camYaw + 180f, rpitch)
                    lockRotation = RotationUtils.limitAngleChange(oldPlayerRot, playerRot, 90f)
                }

                if (derpValue.get()) {
                    val blockBelow = mc.theWorld.getBlockState(
                        BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
                    ).block
                    if (blockBelow === Blocks.air) {
                        correctControls(1)
                    } else {
                        correctControls(2)
                    }
                } else {
                    correctControls(1)
                }

                if (safewalkValue.equals("Sneak")) {
                    val blockBelow = mc.theWorld.getBlockState(
                        BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
                    ).block
                    mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) || blockBelow === Blocks.air
                }
            }
            "speedbridge" -> {
                val rpitch = if ((camYaw / 15f).roundToInt() % 6 == 0) 78.7 else 78.9
                if (rpitch == 78.7) {
                    playerRot = Rotation(camYaw - 135f, rpitch.toFloat())
                    correctControls(3)
                } else {
                    playerRot = Rotation(camYaw - 180f, rpitch.toFloat())
                    correctControls(1)
                }
                lockRotation = RotationUtils.limitAngleChange(oldPlayerRot, playerRot, 90f)
                val blockBelow = mc.theWorld.getBlockState(
                    BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
                ).block
                mc.gameSettings.keyBindSneak.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) || blockBelow === Blocks.air
            }
            "jitterbridge" -> {
                val rpitch = if ((camYaw / 45f).roundToInt() % 2 == 0) 77.4f else 77.1f
                playerRot = Rotation(camYaw + 180f, rpitch)
                lockRotation = RotationUtils.limitAngleChange(oldPlayerRot, playerRot, 80f)
                correctControls(1)
                mc.gameSettings.keyBindJump.pressed = true
            }
            "tellybridge" -> {
                val rpitch = if ((camYaw / 45f).roundToInt() % 2 == 0) 75.1f else 75.5f
                if (mc.thePlayer.onGround) {
                    playerRot = Rotation(camYaw, rpitch)
                    correctControls(0)
                } else {
                    playerRot = Rotation(camYaw + 180f, rpitch)
                    correctControls(1)
                }
                lockRotation = RotationUtils.limitAngleChange(oldPlayerRot, playerRot, 180f)
                mc.gameSettings.keyBindJump.pressed = true
            }
        }

        lockRotation.toPlayer(mc.thePlayer)

        // 放置方块逻辑
        placeBlock()
    }

    private fun placeBlock() {
        val blockPos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)
        val blockState = mc.theWorld.getBlockState(blockPos)

        if (blockState.block === Blocks.air || !blockState.block.isFullBlock) {
            val itemStack = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem)
            if (itemStack != null && itemStack.item is ItemBlock) {
                val itemBlock = itemStack.item as ItemBlock
                if (itemBlock.block != null && itemBlock.block != Blocks.air) {
                    // 尝试从下方放置
                    val belowPos = blockPos.add(0, -1, 0)
                    val belowState = mc.theWorld.getBlockState(belowPos)

                    if (belowState.block.isFullBlock) {
                        // 从下方方块的上方放置
                        val hitVec = Vec3(
                            blockPos.x + 0.5,
                            blockPos.y + 1.0,
                            blockPos.z + 0.5
                        )
                        val blockPlacement = C08PacketPlayerBlockPlacement(
                            belowPos,
                            EnumFacing.UP.index,
                            itemStack,
                            0.5f, 1.0f, 0.5f
                        )
                        mc.netHandler.addToSendQueue(blockPlacement)

                        // 挥手
                        mc.netHandler.addToSendQueue(
                            net.minecraft.network.play.client.C0APacketAnimation()
                        )
                    } else {
                        // 尝试从侧面放置
                        val directions = arrayOf(
                            EnumFacing.NORTH, EnumFacing.SOUTH,
                            EnumFacing.WEST, EnumFacing.EAST
                        )
                        for (face in directions) {
                            val neighborPos = blockPos.add(face.directionVec)
                            val neighborState = mc.theWorld.getBlockState(neighborPos)
                            if (neighborState.block.isFullBlock) {
                                val oppositeFace = face.opposite
                                val hitVec = Vec3(
                                    blockPos.x + 0.5,
                                    blockPos.y + 0.5,
                                    blockPos.z + 0.5
                                )
                                val blockPlacement = C08PacketPlayerBlockPlacement(
                                    neighborPos,
                                    oppositeFace.index,
                                    itemStack,
                                    0.5f, 0.5f, 0.5f
                                )
                                mc.netHandler.addToSendQueue(blockPlacement)

                                // 挥手
                                mc.netHandler.addToSendQueue(
                                    net.minecraft.network.play.client.C0APacketAnimation()
                                )
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    private fun correctControls(type: Int) {
        fw = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
        bw = GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
        right = GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
        left = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
        when (type) {
            0 -> {
                mc.gameSettings.keyBindForward.pressed = fw
                mc.gameSettings.keyBindBack.pressed = bw
                mc.gameSettings.keyBindRight.pressed = right
                mc.gameSettings.keyBindLeft.pressed = left
            }
            1 -> {
                mc.gameSettings.keyBindForward.pressed = bw
                mc.gameSettings.keyBindBack.pressed = fw
                mc.gameSettings.keyBindRight.pressed = left
                mc.gameSettings.keyBindLeft.pressed = right
            }
            2 -> {
                mc.gameSettings.keyBindForward.pressed = fw || right
                mc.gameSettings.keyBindBack.pressed = left || bw
                mc.gameSettings.keyBindRight.pressed = right || bw
                mc.gameSettings.keyBindLeft.pressed = fw || left
            }
            3 -> {
                mc.gameSettings.keyBindForward.pressed = left || bw
                mc.gameSettings.keyBindBack.pressed = fw || right
                mc.gameSettings.keyBindRight.pressed = fw || left
                mc.gameSettings.keyBindLeft.pressed = right || bw
            }
        }
    }
}