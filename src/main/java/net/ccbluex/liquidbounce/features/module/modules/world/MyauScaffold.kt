/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.minecraft.block.Block
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.potion.Potion
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import kotlin.math.*

class MyauScaffold : Module(name = "MyauScaffold", category = ModuleCategory.WORLD) {

    private val modeValue = ListValue("Mode", arrayOf("Normal", "Telly", "Snap"), "Telly")
    private val rotationModeValue = ListValue("RotateMode", arrayOf("None", "Vanilla", "Backwards", "Prediction"), "Vanilla")
    private val towerModeValue = ListValue("TowerMode", arrayOf("None", "Vanilla", "Motion", "Jump"), "Vanilla")
    private val moveFixValue = ListValue("MoveFix", arrayOf("None", "Silent"), "None")
    private val jumpDelayValue = IntegerValue("JumpDelay", 2, 0, 5).displayable { modeValue.equals("Telly") }
    private val placeDelayValue = IntegerValue("PlaceDelay", 1, 0, 5)
    private val startRotSpeedValue = FloatValue("StartRotSpeed", 180f, 1f, 180f)
    private val normalRotSpeedValue = FloatValue("NormalRotSpeed", 180f, 1f, 180f)
    private val swingValue = BoolValue("Swing", true)
    private val clutchValue = BoolValue("Clutch", true)
    private val onlyVoidValue = BoolValue("OnlyVoid", false).displayable { clutchValue.get() }
    private val forwardTicksValue = IntegerValue("ForwardTicks", 5, 1, 20).displayable { modeValue.equals("Snap") }
    private val backTicksValue = IntegerValue("BackTicks", 5, 1, 20).displayable { modeValue.equals("Snap") }

    private var rotationTick = 0
    private var lastSlot = -1
    private var blockCount = -1
    private var yaw = -180f
    private var pitch = 0f
    private var canRotate = false
    private var tellyJumpDelayTimer = 0
    private var jumpDelayOverride = -1
    private var wasInAir = false
    private var stage = 0
    private var startY = 256
    private var shouldKeepY = false
    private var towering = false
    private var targetFacing: EnumFacing? = null
    private var placeDelayCounter = 0
    private var snapTickCounter = 0
    private var snapForward = true

    private var clutchActive = false
    private var clutchTickCounter = 0
    private var savedMotionX = 0.0
    private var savedMotionY = 0.0
    private var savedMotionZ = 0.0
    private var sa = false

    private val placeOffsets = doubleArrayOf(
        0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375, 0.40625, 0.46875,
        0.53125, 0.59375, 0.65625, 0.71875, 0.78125, 0.84375, 0.90625, 0.96875
    )

    private data class BlockData(val blockPos: BlockPos, val facing: EnumFacing)

    override fun onEnable() {
        lastSlot = mc.thePlayer?.inventory?.currentItem ?: -1
        blockCount = -1
        rotationTick = 3
        yaw = -180f
        pitch = 0f
        canRotate = false
        towering = false
        placeDelayCounter = 0
        snapTickCounter = 0
        snapForward = true
        clutchActive = false
        clutchTickCounter = 0
    }

    override fun onDisable() {
        clutchReset()
        if (mc.thePlayer != null && lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = lastSlot
        }
        snapTickCounter = 0
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (!event.isPre() || mc.thePlayer == null) return

        val tellyMode = modeValue.equals("Telly")
        val snapMode = modeValue.equals("Snap")

        if (rotationTick > 0) rotationTick--

        if (mc.thePlayer.onGround) {
            if (stage > 0) stage--
            if (stage < 0) stage++
            if (!shouldKeepY) startY = MathHelper.floor_double(mc.thePlayer.posY)
            shouldKeepY = false
            towering = false
            if (wasInAir) {
                tellyJumpDelayTimer = if (jumpDelayOverride >= 0) jumpDelayOverride else jumpDelayValue.get()
                wasInAir = false
            }
            if (tellyJumpDelayTimer > 0) tellyJumpDelayTimer--
        } else {
            wasInAir = true
        }

        if (tellyMode && mc.thePlayer.onGround && isForwardPressed() && !mc.gameSettings.keyBindJump.isKeyDown && stage == 0) {
            stage = 1
        }

        if (tellyMode) {
            jumpDelayOverride = if (mc.gameSettings.keyBindJump.isKeyDown) 2 else -1
        } else {
            jumpDelayOverride = -1
            tellyJumpDelayTimer = 0
        }

        updateClutch()

        if (snapMode) {
            if (mc.thePlayer.onGround && !isOnEdge()) {
                val forward = forwardTicksValue.get()
                val back = backTicksValue.get()
                val cycle = forward + back
                if (cycle > 0) {
                    snapTickCounter++
                    if (snapTickCounter >= cycle) snapTickCounter = 0
                    snapForward = snapTickCounter < forward
                }
            } else {
                snapForward = false
            }
        }

        if (!canPlace()) return

        updateBlockCount()

        if (snapMode) {
            if (snapForward) {
                yaw = quantizeAngle(getCurrentYaw())
                pitch = 80f
            } else {
                yaw = quantizeAngle(getCurrentYaw() + 180f)
                pitch = 85f
            }
            canRotate = true
        }

        val currentYaw = getCurrentYaw()
        val yawDiffTo180 = wrapAngleDiff(currentYaw - 180f, mc.thePlayer.rotationYaw)
        val diagonalYaw = if (isDiagonal(currentYaw))
            yawDiffTo180
        else
            wrapAngleDiff(currentYaw - 135f * if (((currentYaw + 180f) % 90f) < 45f) 1f else -1f, mc.thePlayer.rotationYaw)

        if (!canRotate) {
            when (rotationModeValue.get()) {
                "Vanilla" -> {
                    if (yaw == -180f && pitch == 0f) {
                        yaw = quantizeAngle(diagonalYaw)
                        pitch = quantizeAngle(85f)
                    } else {
                        yaw = quantizeAngle(diagonalYaw)
                    }
                }
                "Backwards" -> {
                    if (yaw == -180f && pitch == 0f) {
                        yaw = quantizeAngle(yawDiffTo180)
                        pitch = quantizeAngle(85f)
                    } else {
                        yaw = quantizeAngle(yawDiffTo180)
                    }
                }
                "Prediction" -> {
                    if (yaw == -180f && pitch == 0f) {
                        yaw = quantizeAngle(diagonalYaw)
                        pitch = quantizeAngle(85f)
                    }
                }
            }
        }

        val blockData = getBlockData(snapMode && snapForward)
        var hitVec: Vec3? = null

        if (blockData != null) {
            when (rotationModeValue.get()) {
                "Prediction" -> {
                    val offsets = doubleArrayOf(0.1, 0.3, 0.5, 0.7, 0.9)
                    var xOffsets = offsets
                    var yOffsets = offsets
                    var zOffsets = offsets
                    when (blockData.facing) {
                        EnumFacing.NORTH -> zOffsets = doubleArrayOf(0.02)
                        EnumFacing.EAST -> xOffsets = doubleArrayOf(0.98)
                        EnumFacing.SOUTH -> zOffsets = doubleArrayOf(0.98)
                        EnumFacing.WEST -> xOffsets = doubleArrayOf(0.02)
                        EnumFacing.DOWN -> yOffsets = doubleArrayOf(0.02)
                        EnumFacing.UP -> yOffsets = doubleArrayOf(0.98)
                    }

                    var bestYaw = -180f
                    var bestPitch = 0f
                    var bestDist = Double.MAX_VALUE

                    for (dx in xOffsets) {
                        for (dy in yOffsets) {
                            for (dz in zOffsets) {
                                val targetX = blockData.blockPos.x + dx
                                val targetY = blockData.blockPos.y + dy
                                val targetZ = blockData.blockPos.z + dz
                                val rot = RotationUtils.getRotations(targetX, targetY, targetZ)
                                val mop = rayTrace(rot.yaw, rot.pitch, mc.playerController.getBlockReachDistance().toDouble())
                                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                                    && mop.blockPos == blockData.blockPos && mop.sideHit == blockData.facing) {
                                    val yawDiff = abs(MathHelper.wrapAngleTo180_float(rot.yaw - yaw).toDouble())
                                    val pitchDiff = abs((rot.pitch - pitch).toDouble())
                                    val dist = sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff)
                                    if (dist < bestDist) {
                                        bestDist = dist
                                        bestYaw = rot.yaw
                                        bestPitch = rot.pitch
                                        hitVec = mop.hitVec
                                    }
                                }
                            }
                        }
                    }

                    if (bestYaw != -180f || bestPitch != 0f) {
                        bestYaw += (Math.random().toFloat() - 0.5f) * 1f
                        bestPitch += (Math.random().toFloat() - 0.5f) * 0.6f
                        yaw = bestYaw
                        pitch = bestPitch
                        canRotate = true
                    }
                }
                else -> {
                    var xOffsets = placeOffsets
                    var yOffsets = placeOffsets
                    var zOffsets = placeOffsets
                    when (blockData.facing) {
                        EnumFacing.NORTH -> zOffsets = doubleArrayOf(0.0)
                        EnumFacing.EAST -> xOffsets = doubleArrayOf(1.0)
                        EnumFacing.SOUTH -> zOffsets = doubleArrayOf(1.0)
                        EnumFacing.WEST -> xOffsets = doubleArrayOf(0.0)
                        EnumFacing.DOWN -> yOffsets = doubleArrayOf(0.0)
                        EnumFacing.UP -> yOffsets = doubleArrayOf(1.0)
                    }
                    var bestYaw = -180f
                    var bestPitch = 0f
                    var bestDiff = 0f
                    for (dx in xOffsets) {
                        for (dy in yOffsets) {
                            for (dz in zOffsets) {
                                val relX = blockData.blockPos.x + dx - mc.thePlayer.posX
                                val relY = blockData.blockPos.y + dy - mc.thePlayer.posY - mc.thePlayer.getEyeHeight().toDouble()
                                val relZ = blockData.blockPos.z + dz - mc.thePlayer.posZ
                                val baseYaw = wrapAngleDiff(yaw, mc.thePlayer.rotationYaw)
                                val basePitch = pitch
                                val rotations = getRotationsTo(relX, relY, relZ, baseYaw, basePitch)
                                val mop = rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance().toDouble())
                                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                                    && mop.blockPos == blockData.blockPos && mop.sideHit == blockData.facing) {
                                    val totalDiff = abs(rotations[0] - baseYaw) + abs(rotations[1] - basePitch)
                                    if ((bestYaw == -180f && bestPitch == 0f) || totalDiff < bestDiff) {
                                        bestYaw = rotations[0]
                                        bestPitch = rotations[1]
                                        bestDiff = totalDiff
                                        hitVec = mop.hitVec
                                    }
                                }
                            }
                        }
                    }
                    if (bestYaw != -180f || bestPitch != 0f) {
                        yaw = bestYaw
                        pitch = bestPitch
                        canRotate = true
                    }
                }
            }
        }

        if (canRotate && isForwardPressed() && abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - yaw)) < 90f) {
            if (rotationModeValue.equals("Backwards")) {
                yaw = quantizeAngle(yawDiffTo180)
            }
        }

        // Apply rotation
        if (rotationModeValue.get() != "None" && !snapMode) {
            var targetYaw = yaw
            var targetPitch = pitch
            if (towering && (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (startY + 1).toDouble())) {
                val yawDiff = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw)
                val tolerance = if (rotationTick >= 2) startRotSpeedValue.get() else normalRotSpeedValue.get()
                if (abs(yawDiff) > tolerance) {
                    val clampedYaw = clampAngle(yawDiff, tolerance)
                    targetYaw = quantizeAngle(mc.thePlayer.rotationYaw + clampedYaw)
                    rotationTick = max(rotationTick, 1)
                }
            }
            if (tellyMode && isTowering() && tellyJumpDelayTimer <= 0) {
                val yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - mc.thePlayer.rotationYaw)
                targetYaw = quantizeAngle(mc.thePlayer.rotationYaw + yawDelta * (0.98f + Math.random().toFloat() * 0.01f))
                targetPitch = quantizeAngle(30f + Math.random().toFloat() * 50f)
                rotationTick = 3
                towering = true
            } else if (tellyMode && tellyJumpDelayTimer > 0) {
                targetYaw = if (yaw != -180f) yaw else quantizeAngle(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - mc.thePlayer.rotationYaw) + mc.thePlayer.rotationYaw)
                targetPitch = if (pitch > 10f || pitch < -10f) pitch else 60f
            }
            RotationUtils.setTargetRotation(Rotation(targetYaw, targetPitch), 3)
        } else if (snapMode && rotationModeValue.get() != "None") {
            var targetYaw = yaw
            var targetPitch = pitch
            val yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw)
            val tolerance = if (rotationTick >= 2) startRotSpeedValue.get() else normalRotSpeedValue.get()
            if (abs(yawDiff) > tolerance) {
                val clampedYaw = clampAngle(yawDiff, tolerance)
                targetYaw = quantizeAngle(mc.thePlayer.rotationYaw + clampedYaw)
                rotationTick = max(rotationTick, 1)
            }
            RotationUtils.setTargetRotation(Rotation(targetYaw, targetPitch), 3)
        }

        // Place block
        if (blockData != null && rotationTick <= 0) {
            if (placeDelayCounter > 0) {
                placeDelayCounter--
            } else {
                val finalCheck = rayTrace(yaw, pitch, mc.playerController.getBlockReachDistance().toDouble())
                if (finalCheck != null && finalCheck.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && finalCheck.blockPos == blockData.blockPos && finalCheck.sideHit == blockData.facing) {
                    place(blockData.blockPos, blockData.facing, finalCheck.hitVec)
                    placeDelayCounter = placeDelayValue.get()
                } else if (canRotate && hitVec != null) {
                    place(blockData.blockPos, blockData.facing, hitVec)
                    placeDelayCounter = placeDelayValue.get()
                }
            }
        }

        if (targetFacing != null && rotationTick <= 0) {
            val belowPlayer = BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY) - 1, MathHelper.floor_double(mc.thePlayer.posZ))
            val hit = getBlockHitVec(belowPlayer, targetFacing!!, yaw, pitch)
            place(belowPlayer, targetFacing!!, hit)
            targetFacing = null
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (modeValue.equals("Snap")) return

        if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.hurtTime <= 5
            && !mc.thePlayer.isPotionActive(Potion.jump)
            && mc.gameSettings.keyBindJump.isKeyDown
            && isHoldingBlock()) {
            if (mc.thePlayer.onGround && tellyJumpDelayTimer <= 0 && isAirBelow()) {
                handleTower(event)
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (modeValue.equals("Telly") && mc.thePlayer.onGround && stage > 0 && isForwardPressed() && tellyJumpDelayTimer <= 0) {
            mc.thePlayer.movementInput.jump = true
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (shouldStopSprint()) {
            mc.thePlayer.setSprinting(false)
        }
    }

    private fun shouldStopSprint(): Boolean {
        if (isTowering()) return false
        return stage <= 0 && !modeValue.equals("Snap")
    }

    private fun canPlace(): Boolean {
        return true
    }

    private fun isDiagonal(forwardYaw: Float): Boolean {
        val absYaw = abs(forwardYaw % 90f)
        return absYaw > 20f && absYaw < 70f
    }

    private fun isTowering(): Boolean {
        if (!isForwardPressed()) return false
        if (isAirAbove()) return false
        if (mc.thePlayer.onGround) {
            if (stage > 0 || mc.gameSettings.keyBindJump.isKeyDown) return true
        }
        return tellyJumpDelayTimer > 0
    }

    private fun getCurrentYaw(): Float {
        return adjustYaw(mc.thePlayer.rotationYaw, getForwardValue(), getLeftValue())
    }

    private fun getForwardValue(): Float {
        return mc.thePlayer.movementInput.moveForward
    }

    private fun getLeftValue(): Float {
        return mc.thePlayer.movementInput.moveStrafe
    }

    private fun isForwardPressed(): Boolean {
        return mc.gameSettings.keyBindForward.isKeyDown
    }

    private fun isHoldingBlock(): Boolean {
        val held = mc.thePlayer.heldItem
        if (held == null || held.item !is ItemBlock) return false
        val itemBlock = held.item as ItemBlock
        return !InventoryUtils.isBlockListBlock(itemBlock) && itemBlock.getBlock().isFullCube
    }

    private fun isAirAbove(): Boolean {
        val headPos = BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.ceiling_double_int(mc.thePlayer.posY) + 1, MathHelper.floor_double(mc.thePlayer.posZ))
        return isReplaceable(headPos)
    }

    private fun isAirBelow(): Boolean {
        val belowPos = BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY) - 2, MathHelper.floor_double(mc.thePlayer.posZ))
        return isReplaceable(belowPos)
    }

    private fun isOnEdge(): Boolean {
        if (!mc.thePlayer.onGround) return true
        val below = BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY) - 1, MathHelper.floor_double(mc.thePlayer.posZ))
        if (isReplaceable(below)) return true
        val edgeThreshold = 0.15
        val xOff = mc.thePlayer.posX - MathHelper.floor_double(mc.thePlayer.posX)
        val zOff = mc.thePlayer.posZ - MathHelper.floor_double(mc.thePlayer.posZ)
        if (xOff < edgeThreshold || xOff > 1.0 - edgeThreshold || zOff < edgeThreshold || zOff > 1.0 - edgeThreshold) {
            val checkX = MathHelper.floor_double(mc.thePlayer.posX) + if (xOff < edgeThreshold) -1 else if (xOff > 1.0 - edgeThreshold) 1 else 0
            val checkZ = MathHelper.floor_double(mc.thePlayer.posZ) + if (zOff < edgeThreshold) -1 else if (zOff > 1.0 - edgeThreshold) 1 else 0
            if (checkX != MathHelper.floor_double(mc.thePlayer.posX) || checkZ != MathHelper.floor_double(mc.thePlayer.posZ)) {
                val adjacentBelow = BlockPos(checkX, MathHelper.floor_double(mc.thePlayer.posY) - 1, checkZ)
                if (isReplaceable(adjacentBelow)) return true
            }
        }
        return false
    }

    private fun isFallingIntoVoid(): Boolean {
        if (mc.thePlayer == null) return false
        for (i in 0..128) {
            val checkPos = BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY) - i, MathHelper.floor_double(mc.thePlayer.posZ))
            if (mc.theWorld.getBlockState(checkPos).block.material.isSolid()) return false
        }
        return true
    }

    private fun bbUnC(): Boolean {
        if (mc.thePlayer == null) return false
        val playerY = MathHelper.floor_double(mc.thePlayer.posY)
        for (i in 1..2) {
            val checkPos = BlockPos(MathHelper.floor_double(mc.thePlayer.posX), playerY - i, MathHelper.floor_double(mc.thePlayer.posZ))
            if (mc.theWorld.getBlockState(checkPos).block.material.isSolid()) return true
        }
        return false
    }

    private fun updateClutch() {
        if (!clutchValue.get()) {
            if (clutchActive) clutchReset()
            return
        }
        if (mc.thePlayer.onGround) {
            if (clutchActive) clutchReset()
            return
        }
        if (bbUnC()) {
            if (clutchActive) clutchReset()
            return
        }
        val fallDistance = mc.thePlayer.fallDistance
        val shouldClutch = fallDistance > 2 && !isAirAbove()
                && !mc.thePlayer.isCollidedHorizontally
                && (!onlyVoidValue.get() || isFallingIntoVoid())
        if (shouldClutch && !clutchActive) {
            clutchActive = true
            savedMotionX = mc.thePlayer.motionX
            savedMotionY = mc.thePlayer.motionY
            savedMotionZ = mc.thePlayer.motionZ
            clutchTickCounter = 0
        }
        if (clutchActive) {
            clutchTickCounter++
            if (clutchTickCounter >= 30) {
                clutchReset()
                clutchActive = false
            }
        }
    }

    private fun clutchReset() {
        clutchActive = false
        clutchTickCounter = 0
    }

    private fun updateBlockCount() {
        val stack = mc.thePlayer.heldItem
        val count = if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        blockCount = minOf(blockCount, count)
        if (blockCount <= 0) {
            var slot = mc.thePlayer.inventory.currentItem
            if (blockCount == 0) slot--
            for (i in slot downTo slot - 8) {
                val hotbarSlot = (i % 9 + 9) % 9
                val candidate = mc.thePlayer.inventory.getStackInSlot(hotbarSlot)
                if (candidate != null && candidate.item is ItemBlock && !InventoryUtils.isBlockListBlock(candidate.item as ItemBlock)) {
                    mc.thePlayer.inventory.currentItem = hotbarSlot
                    blockCount = candidate.stackSize
                    break
                }
            }
        }
    }

    private fun getBlockData(skip: Boolean = false): BlockData? {
        if (skip) return null
        val startY = MathHelper.floor_double(mc.thePlayer.posY)
        val targetPos = BlockPos(
            MathHelper.floor_double(mc.thePlayer.posX),
            if (stage != 0 && !shouldKeepY) minOf(startY, this.startY) else startY - 1,
            MathHelper.floor_double(mc.thePlayer.posZ)
        )
        if (isReplaceable(targetPos)) {
            val positions = ArrayList<BlockPos>()
            val reach = mc.playerController.getBlockReachDistance().toDouble()
            for (x in -4..4) {
                for (y in -4..0) {
                    for (z in -4..4) {
                        val pos = targetPos.add(x, y, z)
                        if (!isReplaceable(pos) && canBeClicked(pos)
                            && mc.thePlayer.getDistance(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= reach
                            && (stage == 0 || shouldKeepY || pos.y < this.startY)) {
                            for (facing in EnumFacing.VALUES) {
                                if (facing != EnumFacing.DOWN) {
                                    val neighbor = pos.offset(facing)
                                    if (isReplaceable(neighbor)) {
                                        positions.add(pos)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (positions.isEmpty()) return null
            positions.sortWith(Comparator.comparingDouble { o: BlockPos ->
                o.distanceSqToCenter(targetPos.x + 0.5, targetPos.y + 0.5, targetPos.z + 0.5)
            })
            val blockPos = positions[0]
            val facing = getBestFacing(blockPos, targetPos) ?: return null
            return BlockData(blockPos, facing)
        }
        return null
    }

    private fun getBestFacing(blockPos: BlockPos, targetPos: BlockPos): EnumFacing? {
        var bestFacing: EnumFacing? = null
        var bestDist = Double.MAX_VALUE
        for (facing in EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                val offset = blockPos.offset(facing)
                if (offset.y <= targetPos.y) {
                    val dist = offset.distanceSqToCenter(targetPos.x + 0.5, targetPos.y + 0.5, targetPos.z + 0.5)
                    if (bestFacing == null || dist < bestDist || (dist == bestDist && facing == EnumFacing.UP)) {
                        bestDist = dist
                        bestFacing = facing
                    }
                }
            }
        }
        return bestFacing
    }

    private fun place(blockPos: BlockPos, enumFacing: EnumFacing, vec3: Vec3) {
        if (!isHoldingBlock() || blockCount <= 0) return
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem, blockPos, enumFacing, vec3)) {
            if (mc.playerController.currentGameType != WorldSettings.GameType.CREATIVE) blockCount--
            if (swingValue.get()) mc.thePlayer.swingItem()
            else mc.netHandler.addToSendQueue(C0APacketAnimation())
        }
    }

    private fun handleTower(event: StrafeEvent) {
        when (towerModeValue.get()) {
            "Vanilla" -> {
                mc.thePlayer.motionY = 0.42
                if (!isForwardPressed()) {
                    mc.thePlayer.motionX = 0.0
                    mc.thePlayer.motionZ = 0.0
                    event.cancelEvent()
                } else {
                    val speed = MovementUtils.getSpeed()
                    val yaw = Math.toRadians(MovementUtils.direction * 180.0 / Math.PI)
                    mc.thePlayer.motionX = -sin(yaw) * speed
                    mc.thePlayer.motionZ = cos(yaw) * speed
                }
            }
            "Motion" -> {
                if (!isForwardPressed() && MovementUtils.getSpeed() < 0.01f) {
                    mc.thePlayer.motionX = 0.0
                    mc.thePlayer.motionZ = 0.0
                }
            }
            "Jump" -> {
                if (!isForwardPressed()) {
                    val microSpeed = 0.005
                    val yaw = Math.toRadians(MovementUtils.direction * 180.0 / Math.PI)
                    mc.thePlayer.motionX = -sin(yaw) * microSpeed
                    mc.thePlayer.motionZ = cos(yaw) * microSpeed
                } else {
                    val speed = MovementUtils.getSpeed()
                    val yaw = Math.toRadians(MovementUtils.direction * 180.0 / Math.PI)
                    mc.thePlayer.motionX = -sin(yaw) * speed
                    mc.thePlayer.motionZ = cos(yaw) * speed
                }
            }
        }
    }

    // Rotation helpers
    private fun quantizeAngle(angle: Float): Float {
        return MathHelper.wrapAngleTo180_float((angle / 45f).roundToInt() * 45f)
    }

    private fun wrapAngleDiff(from: Float, to: Float): Float {
        return MathHelper.wrapAngleTo180_float(from - to)
    }

    private fun clampAngle(angleDiff: Float, maxAngle: Float): Float {
        return if (abs(angleDiff) > maxAngle) maxAngle * sign(angleDiff) else angleDiff
    }

    private fun adjustYaw(currentYaw: Float, forward: Float, strafe: Float): Float {
        if (forward == 0f && strafe == 0f) return currentYaw
        var yaw = currentYaw
        var f = forward
        var s = strafe
        if (f != 0f) {
            s *= if (f > 0f) 1f else -1f
        }
        yaw += if (s > 0f) -45f else if (s < 0f) 45f else 0f
        if (f < 0f) yaw += 180f
        return MathHelper.wrapAngleTo180_float(yaw)
    }

    private fun getRotationsTo(relX: Double, relY: Double, relZ: Double, baseYaw: Float, basePitch: Float): FloatArray {
        val yaw = Math.toDegrees(atan2(relZ, relX)) - 90.0
        val pitch = -Math.toDegrees(atan2(relY, sqrt(relX * relX + relZ * relZ)))
        return floatArrayOf(
            MathHelper.wrapAngleTo180_float((yaw - baseYaw).toFloat()) + baseYaw,
            MathHelper.wrapAngleTo180_float(pitch.toFloat())
        )
    }

    private fun rayTrace(yaw: Float, pitch: Float, distance: Double): MovingObjectPosition? {
        val pos = mc.thePlayer.getPositionEyes(1f)
        val look = RotationUtils.getVectorForRotation(Rotation(yaw, pitch))
        val end = pos.addVector(look.xCoord * distance, look.yCoord * distance, look.zCoord * distance)
        return mc.theWorld.rayTraceBlocks(pos, end, false, false, true)
    }

    private fun getBlockHitVec(blockPos: BlockPos, facing: EnumFacing, yaw: Float, pitch: Float): Vec3 {
        return Vec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
    }

    override val tag: String
        get() = modeValue.get()
}
