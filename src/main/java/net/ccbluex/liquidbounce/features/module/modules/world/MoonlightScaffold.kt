/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * MoonlightScaffold - Adapted from MoonLight Scaffold
 * https://github.com/randomguy3725/MoonLight/blob/main/src/main/java/wtf/moonlight/features/modules/impl/movement/Scaffold.java
 *
 * Author(s): [Randumbguy & wxdbie & opZywl & MukjepScarlet & lucas & eonian]
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.material.Material
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.potion.Potion
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.*

class MoonlightScaffold : Module(name = "MoonlightScaffold", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_V) {

    // ─ Switch Block ─
    private val switchBlockValue = ListValue("SwitchBlock", arrayOf("Silent", "Switch", "Spoof"), "Spoof")
    private val biggestStackValue = BoolValue("BiggestStack", false)

    // ─ Mode ─
    private val modeValue = ListValue("Mode", arrayOf("Normal", "Telly", "Snap"), "Normal")
    private val minTellyTicksValue = IntegerValue("MinTellyTicks", 2, 1, 5).displayable { modeValue.equals("Telly") }
    private val maxTellyTicksValue = IntegerValue("MaxTellyTicks", 4, 1, 5).displayable { modeValue.equals("Telly") }

    // ─ Rotations ─
    private val rotationsValue = ListValue(
        "Rotations",
        arrayOf("Normal", "Normal2", "Strict", "GodBridge", "Custom", "Hypixel", "Derp"),
        "Normal"
    )
    private val customYawValue = IntegerValue("CustomYaw", 180, 0, 180).displayable { rotationsValue.equals("Custom") }
    private val minPitchValue = FloatValue("MinPitch", 55f, 50f, 90f).displayable { rotationsValue.equals("Custom") || rotationsValue.equals("GodBridge") }
    private val maxPitchValue = FloatValue("MaxPitch", 75f, 50f, 90f).displayable { rotationsValue.equals("Custom") || rotationsValue.equals("GodBridge") }
    private val minRotSpeedValue = IntegerValue("MinRotSpeed", 45, 1, 180)
    private val maxRotSpeedValue = IntegerValue("MaxRotSpeed", 90, 1, 180)
    private val smoothResetValue = BoolValue("SmoothReset", true)

    // ─ Addons ─
    private val sprintValue = BoolValue("Sprint", true)
    private val swingValue = BoolValue("Swing", true)
    private val movementFixValue = BoolValue("MovementFix", true)
    private val rayTraceValue = BoolValue("RayTrace", true)
    private val keepYValue = BoolValue("KeepY", false)
    private val safeWalkValue = BoolValue("SafeWalk", false)
    private val adStrafeValue = BoolValue("ADStrafe", false)
    private val hoverValue = BoolValue("Hover", false)
    private val sneakValue = BoolValue("Sneak", false)
    private val jumpValue = BoolValue("Jump", false)
    private val targetEspValue = BoolValue("TargetBlockESP", false)

    private val blocksToJumpValue = IntegerValue("BlocksToJump", 7, 1, 8).displayable { jumpValue.get() }
    private val blocksToSneakValue = IntegerValue("BlocksToSneak", 7, 1, 8).displayable { sneakValue.get() }
    private val sneakDistanceValue = FloatValue("SneakDistance", 0f, 0f, 0.5f).displayable { sneakValue.get() }

    // ─ Tower ─
    private val towerValue = ListValue("Tower", arrayOf("Jump", "Vanilla", "Watchdog"), "Jump")
    private val towerStopValue = BoolValue("TowerStop", true).displayable { towerValue.equals("Watchdog") }
    private val towerStopTickValue = IntegerValue("TowerStopTick", 7, 4, 20).displayable { towerStopValue.get() }
    private val towerMoveValue = ListValue("TowerMove", arrayOf("Jump", "Vanilla", "Watchdog", "Low"), "Jump")
    private val towerMoveStopValue = BoolValue("TowerMoveStop", true).displayable { towerMoveValue.equals("Watchdog") }
    private val towerMoveStopTickValue = IntegerValue("TowerMoveStopTick", 7, 4, 20).displayable { towerMoveStopValue.get() }

    // ─ Counter ─
    private val counterValue = ListValue("Counter", arrayOf("None", "Simple", "Normal"), "Normal")

    // ─ State ─
    private var oloSlot = -1
    private var onGroundY = 0.0
    private var targetBlock: BlockPos? = null
    private var rotation: FloatArray? = null
    private var previousRotation: FloatArray? = null
    private var canPlace = true
    private var blocksPlaced = 0
    private var placing = false
    private var placed = false
    private var tellyTicks = 0
    private var isOnRightSide = false
    private var flagged = false
    private var derpYaw = 0f
    private var hoverState = HoverState.DONE
    private var spoofingSlot = false
    private var offGroundTicks = 0

    private enum class HoverState { JUMP, FALL, DONE }

    private class PlaceData(var blockPos: BlockPos, var facing: EnumFacing)
    private var data: PlaceData? = null

    companion object {
        private val blacklistedBlocks: Set<Block> = setOf(
            Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.lava, Blocks.flowing_lava,
            Blocks.wooden_slab, Blocks.chest, Blocks.enchanting_table, Blocks.carpet, Blocks.glass_pane,
            Blocks.skull, Blocks.stained_glass_pane, Blocks.iron_bars, Blocks.snow_layer, Blocks.ice,
            Blocks.packed_ice, Blocks.coal_ore, Blocks.diamond_ore, Blocks.emerald_ore, Blocks.trapped_chest,
            Blocks.torch, Blocks.anvil, Blocks.noteblock, Blocks.jukebox, Blocks.tnt, Blocks.gold_ore,
            Blocks.iron_ore, Blocks.lapis_ore, Blocks.lit_redstone_ore, Blocks.quartz_ore, Blocks.redstone_ore,
            Blocks.wooden_pressure_plate, Blocks.stone_pressure_plate, Blocks.light_weighted_pressure_plate,
            Blocks.heavy_weighted_pressure_plate, Blocks.stone_button, Blocks.wooden_button, Blocks.lever,
            Blocks.tallgrass, Blocks.tripwire, Blocks.tripwire_hook, Blocks.rail, Blocks.waterlily,
            Blocks.red_flower, Blocks.red_mushroom, Blocks.brown_mushroom, Blocks.vine, Blocks.trapdoor,
            Blocks.yellow_flower, Blocks.ladder, Blocks.furnace, Blocks.sand, Blocks.cactus, Blocks.dispenser,
            Blocks.dropper, Blocks.crafting_table, Blocks.pumpkin, Blocks.sapling, Blocks.cobblestone_wall,
            Blocks.oak_fence, Blocks.activator_rail, Blocks.detector_rail, Blocks.golden_rail,
            Blocks.redstone_torch, Blocks.acacia_stairs, Blocks.birch_stairs, Blocks.brick_stairs,
            Blocks.dark_oak_stairs, Blocks.jungle_stairs, Blocks.nether_brick_stairs, Blocks.oak_stairs,
            Blocks.quartz_stairs, Blocks.red_sandstone_stairs, Blocks.sandstone_stairs, Blocks.spruce_stairs,
            Blocks.stone_brick_stairs, Blocks.stone_stairs, Blocks.double_wooden_slab, Blocks.stone_slab,
            Blocks.double_stone_slab, Blocks.stone_slab2, Blocks.double_stone_slab2, Blocks.web, Blocks.gravel,
            Blocks.daylight_detector_inverted, Blocks.daylight_detector, Blocks.soul_sand, Blocks.piston,
            Blocks.piston_extension, Blocks.piston_head, Blocks.sticky_piston, Blocks.iron_trapdoor,
            Blocks.ender_chest, Blocks.end_portal, Blocks.end_portal_frame, Blocks.standing_banner,
            Blocks.wall_banner, Blocks.deadbush, Blocks.slime_block, Blocks.acacia_fence_gate,
            Blocks.birch_fence_gate, Blocks.dark_oak_fence_gate, Blocks.jungle_fence_gate,
            Blocks.spruce_fence_gate, Blocks.oak_fence_gate
        )
    }

    // ─ Helpers ─
    private fun isSpeedEnabled(): Boolean = FDPNext.moduleManager[Speed::class.java]?.state == true

    private fun getBlockSlot(): Int {
        if (getBlockCount() == 0) return -1
        var slot = -1
        var size = 0
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
            val item = stack.item
            if (item is ItemBlock && !blacklistedBlocks.contains(item.block)) {
                if (biggestStackValue.get() && stack.stackSize > size || !biggestStackValue.get()) {
                    size = stack.stackSize
                    slot = i
                }
            }
        }
        return slot - 36
    }

    private fun getBlockCount(): Int {
        var count = 0
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
            val item = stack.item
            if (item is ItemBlock && !blacklistedBlocks.contains(item.block)) {
                count += stack.stackSize
            }
        }
        return count
    }

    private fun towering(): Boolean =
        Keyboard.isKeyDown(mc.gameSettings.keyBindJump.keyCode) && !MovementUtils.isMoving()

    private fun towerMoving(): Boolean =
        Keyboard.isKeyDown(mc.gameSettings.keyBindJump.keyCode) && MovementUtils.isMoving()

    private fun isMovingStraight(): Boolean = mc.thePlayer.movementInput.moveStrafe == 0f

    private fun getRawDirection(): Float = MovementUtils.movingYaw

    private fun getSpeedEffect(): Float =
        mc.thePlayer.getActivePotionEffect(Potion.moveSpeed)?.let { (it.amplifier + 1).toFloat() } ?: 0f

    private fun stopXZ() {
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
    }

    private fun canBePlacedOn(blockPos: BlockPos): Boolean {
        val material = mc.theWorld.getBlockState(blockPos).block.material
        return material.blocksMovement() && material.isSolid && mc.theWorld.getBlockState(blockPos).block !is BlockAir
    }

    private fun getPlaceData(pos: BlockPos): PlaceData? {
        val facings = arrayOf(EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP)
        for (facing in facings) {
            val blockPos = pos.add(facing.opposite.directionVec)
            if (canBePlacedOn(blockPos)) return PlaceData(blockPos, facing)
        }
        val posBelow = pos.add(0, -1, 0)
        if (canBePlacedOn(posBelow)) return PlaceData(posBelow, EnumFacing.UP)
        for (facing in facings) {
            val blockPos = pos.add(facing.opposite.directionVec)
            for (facing1 in facings) {
                val blockPos1 = blockPos.add(facing1.opposite.directionVec)
                if (canBePlacedOn(blockPos1)) return PlaceData(blockPos1, facing1)
            }
        }
        return null
    }

    private fun getVec3(placeData: PlaceData): Vec3 {
        val pos = placeData.blockPos
        val face = placeData.facing
        var x = pos.x + 0.5
        var y = pos.y + 0.5
        var z = pos.z + 0.5
        x += face.frontOffsetX / 2.0
        z += face.frontOffsetZ / 2.0
        y += face.frontOffsetY / 2.0
        return Vec3(x, y, z)
    }

    private fun getBestRotation(blockPos: BlockPos, face: EnumFacing): FloatArray {
        var bestRot = RotationUtils.getRotations(
            blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5
        )
        var bestDist = RotationUtils.getRotationDifference(bestRot)

        val step = 0.1f
        var x = 0.1f
        while (x <= 0.9f) {
            var y = 0.1f
            while (y <= 0.9f) {
                var z = 0.1f
                while (z <= 0.9f) {
                    val candidate = Vec3(
                        blockPos.x + x.toDouble(),
                        blockPos.y + y.toDouble(),
                        blockPos.z + z.toDouble()
                    )
                    val rot = RotationUtils.toRotation(candidate, true)
                    val diff = RotationUtils.getRotationDifference(rot)
                    if (diff < bestDist) {
                        bestDist = diff
                        bestRot = rot
                    }
                    z += step
                }
                y += step
            }
            x += step
        }
        return floatArrayOf(bestRot.yaw, bestRot.pitch)
    }

    private fun place(pos: BlockPos, facing: EnumFacing, hitVec: Vec3) {
        if (!canPlace || data == null) return
        val heldItem = mc.thePlayer.heldItem
        if (!rayTraceValue.get()) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, pos, facing, hitVec)) {
                if (swingValue.get()) {
                    mc.thePlayer.swingItem()
                    mc.itemRenderer.resetEquippedProgress()
                } else {
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }
                placing = true
                blocksPlaced++
                placed = true
            }
        } else {
            // RayTrace 必须基于 scaffold 的（静默）目标旋转，而非 mc.objectMouseOver。
            // 旋转通过 setTargetRotation 写入出站封包，不会改动客户端镜头，因此
            // mc.objectMouseOver 反映的是玩家真实准星，静默放置时不命中目标方块。
            val rot = rotation
            val ray = if (rot != null) {
                rayTraceWithRotation(rot[0], rot[1], mc.playerController.blockReachDistance.toDouble())
            } else {
                mc.objectMouseOver
            }
            if (ray != null && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, ray.blockPos, ray.sideHit, ray.hitVec)
            ) {
                if (swingValue.get()) {
                    mc.thePlayer.swingItem()
                    mc.itemRenderer.resetEquippedProgress()
                } else {
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }
                placing = true
                blocksPlaced++
                placed = true
            }
        }
    }

    private fun rayTraceWithRotation(yaw: Float, pitch: Float, reach: Double): MovingObjectPosition? {
        val eyes = Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ)
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val dirX = -sin(yawRad) * cos(pitchRad)
        val dirY = -sin(pitchRad)
        val dirZ = cos(yawRad) * cos(pitchRad)
        val end = Vec3(eyes.xCoord + dirX * reach, eyes.yCoord + dirY * reach, eyes.zCoord + dirZ * reach)
        return mc.theWorld.rayTraceBlocks(eyes, end, false, false, true)
    }

    private fun getRotations() {
        val data = data ?: return
        when (rotationsValue.get()) {
            "Normal" -> {
                val v = getVec3(data)
                rotation = floatArrayOf(
                    RotationUtils.getRotations(v.xCoord, v.yCoord, v.zCoord).yaw,
                    RotationUtils.getRotations(v.xCoord, v.yCoord, v.zCoord).pitch
                )
            }
            "Normal2" -> {
                val r = RotationUtils.getRotations(data.blockPos.x + 0.5, data.blockPos.y + 0.5, data.blockPos.z + 0.5)
                rotation = floatArrayOf(r.yaw, r.pitch)
            }
            "Strict" -> {
                val v = getVec3(data)
                val r = RotationUtils.getRotations(v.xCoord, v.yCoord, v.zCoord)
                rotation = floatArrayOf(r.yaw, r.pitch)
            }
            "GodBridge" -> {
                val movingYaw = getRawDirection() + 180
                if (mc.thePlayer.onGround) {
                    isOnRightSide = floor(mc.thePlayer.posX + cos(Math.toRadians(movingYaw.toDouble())) * 0.5).toInt() != floor(mc.thePlayer.posX).toInt() ||
                        floor(mc.thePlayer.posZ + sin(Math.toRadians(movingYaw.toDouble())) * 0.5).toInt() != floor(mc.thePlayer.posZ).toInt()
                    val posInDirection = mc.thePlayer.position.offset(EnumFacing.fromAngle(movingYaw.toDouble()), 1)
                    val isLeaningOffBlock = mc.theWorld.getBlockState(mc.thePlayer.position.down()).block is BlockAir
                    val nextBlockIsAir = mc.theWorld.getBlockState(posInDirection.down()).block is BlockAir
                    if (isLeaningOffBlock && nextBlockIsAir) isOnRightSide = !isOnRightSide
                }
                var yaw = if (isMovingStraight()) movingYaw + if (isOnRightSide) 45 else -45 else movingYaw
                yaw = round(yaw / 45f) * 45f
                var i = minPitchValue.get()
                while (i < maxPitchValue.get()) {
                    val rot = floatArrayOf(yaw, i)
                    val mop = rayTraceWithRotation(rot[0], rot[1], mc.playerController.blockReachDistance.toDouble())
                    if (mop?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mop.blockPos == data.blockPos) {
                        rotation = rot
                    }
                    i += 0.01f
                }
            }
            "Custom" -> {
                val yaw = getRawDirection() + customYawValue.get()
                var i = minPitchValue.get()
                while (i < maxPitchValue.get()) {
                    val rot = floatArrayOf(yaw, i)
                    val mop = rayTraceWithRotation(rot[0], rot[1], mc.playerController.blockReachDistance.toDouble())
                    if (mop?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mop.blockPos == data.blockPos) {
                        rotation = rot
                    }
                    i += 0.01f
                }
            }
            "Hypixel" -> {
                val target = targetBlock ?: return
                var yaw = getRawDirection()
                val targetRot = RotationUtils.getRotations(target.x + 0.5, target.y + 0.5, target.z + 0.5)
                if (isMovingStraight()) {
                    yaw += if (abs(MathHelper.wrapAngleTo180_double((targetRot.yaw - getRawDirection() - 118).toDouble())) <
                        abs(MathHelper.wrapAngleTo180_double((targetRot.yaw - getRawDirection() + 118).toDouble()))) 118 else -118
                } else {
                    yaw += 132
                }
                if (rotation == null) rotation = floatArrayOf(yaw, previousRotation?.get(1) ?: 82f)
                rotation!![0] = yaw
                rotation!![1] = getBestRotation(data.blockPos, data.facing)[1]
            }
            "Derp" -> {
                derpYaw += 30f
                rotation = floatArrayOf(derpYaw, 85f)
            }
        }
        if (towerValue.equals("Watchdog") && towering()) {
            rotation = getBestRotation(data.blockPos, data.facing)
        }
        previousRotation = rotation
    }

    // ─ Events ─
    override fun onEnable() {
        if (hoverValue.get() && mc.thePlayer.onGround && !isSpeedEnabled()) {
            hoverState = HoverState.JUMP
        } else {
            hoverState = HoverState.DONE
        }
        oloSlot = mc.thePlayer.inventory.currentItem
        onGroundY = mc.thePlayer.entityBoundingBox.minY
        previousRotation = floatArrayOf(mc.thePlayer.rotationYaw + 180, 82f)
        flagged = false
        canPlace = true
    }

    override fun onDisable() {
        when (switchBlockValue.get()) {
            "Silent" -> mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            "Switch", "Spoof" -> mc.thePlayer.inventory.currentItem = oloSlot
        }
        spoofingSlot = false
        previousRotation = null
        rotation = null
        blocksPlaced = 0
        placed = false
        placing = false
        tellyTicks = 0
        data = null
        targetBlock = null
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Track off-ground ticks
        if (mc.thePlayer.onGround) offGroundTicks = 0 else offGroundTicks++

        data = null
        if (getBlockSlot() == -1) return

        if (rotationsValue.equals("Derp")) derpYaw += 30f

        when (switchBlockValue.get()) {
            "Silent" -> mc.netHandler.addToSendQueue(C09PacketHeldItemChange(getBlockSlot()))
            "Switch" -> mc.thePlayer.inventory.currentItem = getBlockSlot()
            "Spoof" -> {
                mc.thePlayer.inventory.currentItem = getBlockSlot()
                spoofingSlot = true
            }
        }

        if (mc.thePlayer.onGround) onGroundY = mc.thePlayer.entityBoundingBox.minY

        var posY = mc.thePlayer.entityBoundingBox.minY
        if ((hoverState != HoverState.DONE || keepYValue.get() && !isSpeedEnabled()) &&
            !mc.gameSettings.keyBindJump.isKeyDown
        ) {
            posY = onGroundY
        }
        if (towerMoving() || towering()) {
            onGroundY = mc.thePlayer.entityBoundingBox.minY
            posY = onGroundY
        }

        if (modeValue.equals("Telly") && mc.thePlayer.onGround) {
            tellyTicks = (minTellyTicksValue.get()..maxTellyTicksValue.get()).random()
        }

        val posX = mc.thePlayer.posX
        val posZ = mc.thePlayer.posZ
        targetBlock = BlockPos(posX, posY, posZ).offset(EnumFacing.DOWN)
        data = getPlaceData(targetBlock!!) ?: return

        placing = false

        if (sprintValue.get()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, true)
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, false)
            mc.thePlayer.isSprinting = false
        }

        if ((!towerValue.equals("Jump") && towering() && !isSpeedEnabled()) ||
            (!towerMoveValue.equals("Jump") && towerMoving())
        ) {
            hoverState = HoverState.JUMP
            blocksPlaced = 0
        }

        when (hoverState) {
            HoverState.JUMP -> {
                if (mc.thePlayer.onGround && !isSpeedEnabled() && !mc.gameSettings.keyBindJump.isKeyDown) {
                    mc.thePlayer.jump()
                }
                hoverState = HoverState.FALL
            }
            HoverState.FALL -> if (mc.thePlayer.onGround) hoverState = HoverState.DONE
            HoverState.DONE -> {}
        }

        if (!placed) rotation = floatArrayOf(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        canPlace = (modeValue.equals("Telly") && offGroundTicks >= tellyTicks ||
            modeValue.equals("Snap") && data != null ||
            (!modeValue.equals("Telly") && !modeValue.equals("Snap")))

        getRotations()

        if (canPlace && rotation != null) {
            RotationUtils.setTargetRotation(Rotation(rotation!![0], rotation!![1]), 0)
        }

        val d = data ?: return
        place(d.blockPos, d.facing, getVec3(d))
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE) return
        if (!towering() && !isSpeedEnabled() && !towerMoving()) {
            if (towerValue.equals("Watchdog")) {
                if (!mc.thePlayer.isPotionActive(Potion.jump) &&
                    (!towerStopValue.get() || offGroundTicks < towerStopTickValue.get())
                ) {
                    if (towering()) {
                        stopXZ()
                        if (mc.thePlayer.posY % 1.0 == 0.5) mc.thePlayer.motionY = 0.42
                        val motionY = mc.thePlayer.motionY
                        val valY = Math.round((motionY % 1.0) * 10000).toInt()
                        when {
                            valY == 0 -> mc.thePlayer.motionY = 0.42
                            valY > 4000 && valY < 4300 -> mc.thePlayer.motionY = 0.33
                            valY > 7000 -> mc.thePlayer.motionY = (1.0 - mc.thePlayer.posY % 1.0)
                        }
                    }
                }
            }
        }
        if (towerMoveValue.equals("Watchdog")) {
            if (MovementUtils.isMoving() && MovementUtils.getSpeed() > 0.1f &&
                !mc.thePlayer.isPotionActive(Potion.jump) &&
                (!towerMoveStopValue.get() || offGroundTicks < towerMoveStopTickValue.get())
            ) {
                if (towerMoving()) {
                    val motionY = mc.thePlayer.motionY
                    val valY = Math.round((motionY % 1.0) * 10000).toInt()
                    when {
                        valY == 0 -> {
                            mc.thePlayer.motionY = 0.42
                            MovementUtils.strafe(0.28f + getSpeedEffect() * 0.04f)
                        }
                        valY > 4000 && valY < 4300 -> {
                            mc.thePlayer.motionY = 0.33
                            MovementUtils.strafe(0.28f + getSpeedEffect() * 0.04f)
                        }
                        valY > 7000 -> mc.thePlayer.motionY = (1.0 - mc.thePlayer.posY % 1.0)
                    }
                }
            }
        }
        if (towerMoveValue.equals("Low") && towerMoving()) {
            when (offGroundTicks) {
                1 -> {
                    mc.thePlayer.motionY += 0.057
                    MovementUtils.strafe(max(MovementUtils.getSpeed(), 0.33f + getSpeedEffect() * 0.075f))
                }
                3 -> mc.thePlayer.motionY -= 0.1309
                4 -> mc.thePlayer.motionY -= 0.2
            }
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (mc.thePlayer.onGround && keepYValue.get() && MovementUtils.isMoving() && !towering() && !towerMoving()) {
            mc.thePlayer.jump()
        }
        if (jumpValue.get()) {
            if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown && MovementUtils.isMoving() &&
                isMovingStraight() && !mc.thePlayer.isSneaking
            ) {
                if (blocksPlaced >= blocksToJumpValue.get()) {
                    mc.thePlayer.jump()
                    blocksPlaced = 0
                }
            } else {
                blocksPlaced = 0
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (towerValue.equals("Vanilla") && !mc.thePlayer.isPotionActive(Potion.jump) && towering()) {
            event.y = 0.42
            mc.thePlayer.motionY = 0.42
        }
        if (towerMoveValue.equals("Vanilla") && MovementUtils.isMoving() && MovementUtils.getSpeed() > 0.1f &&
            !mc.thePlayer.isPotionActive(Potion.jump) && towerMoving()
        ) {
            mc.thePlayer.motionY = 0.42
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            blocksPlaced = 0
            flagged = true
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (targetEspValue.get()) {
            data?.let { RenderUtils.drawBlockBox(it.blockPos, Color(0, 100, 255), false) }
        }
    }

    @EventTarget
    fun onMoveSafeWalk(event: MoveEvent) {
        if (safeWalkValue.get() && mc.thePlayer.onGround) event.isSafeWalk = true
    }
}
