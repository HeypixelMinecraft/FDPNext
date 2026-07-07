/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.skid.southside.*
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C00PacketKeepAlive
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

class SouthsideScaffold : Module(
    name = "SouthsideScaffold",
    description = "Advanced scaffold with multiple modes from Southside",
    category = ModuleCategory.WORLD,
    keyBind = Keyboard.KEY_V,
    defaultOn = false
) {
    
    // Configuration values
    private val fullSprint = BoolValue("Full Sprint", false)
    private val keepFov = BoolValue("Keep fov", false)
    private val switchBack = BoolValue("Switch Back", true)
    private val fovValue = FloatValue("Fov", 1.2f, 0.8f, 1.5f)
    private val bw = BoolValue("Bed Wars", false)
    private val dbgV = BoolValue("Debug", false)
    private val renderTargetPos = BoolValue("Render Target Pos", true)
    private val renderClickPos = BoolValue("Render Click Pos", false)
    
    companion object {
        val invalidBlocks: List<Block> = listOf(
            Blocks.enchanting_table, Blocks.chest, Blocks.ender_chest,
            Blocks.trapped_chest, Blocks.anvil, Blocks.sand, Blocks.web, Blocks.torch,
            Blocks.crafting_table, Blocks.furnace, Blocks.waterlily, Blocks.dispenser,
            Blocks.stone_pressure_plate, Blocks.wooden_pressure_plate, Blocks.noteblock,
            Blocks.dropper, Blocks.tnt, Blocks.standing_banner, Blocks.wall_banner,
            Blocks.redstone_torch, Blocks.crafting_table
        )
        
        fun isValid(item: Item): Boolean {
            return item is ItemBlock && !invalidBlocks.contains((item as ItemBlock).block)
        }
    }
    
    // State variables
    var baseY = -1
    private var slot = 0
    private var canPlace = true
    var bigVelocityTick = 0
    
    private var blockPos: BlockPos? = null
    private var lastBlockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null
    private var lastEnumFacing: EnumFacing? = null
    private var rotateCount = 0
    
    override fun onEnable() {
        if (mc.thePlayer == null) return
        
        lastBlockPos = null
        blockPos = null
        this.slot = mc.thePlayer.inventory.currentItem
        baseY = -1
        canPlace = true
        bigVelocityTick = 0
    }
    
    override fun onDisable() {
        if (mc.thePlayer == null) return
        
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, false)
        if (switchBack.get()) {
            mc.thePlayer.inventory.currentItem = slot
        }
    }
    
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val playerPos = BlockPos(mc.thePlayer)
        val blockState = mc.theWorld.getBlockState(playerPos)
        
        // Check if inside a block (anti-cheat bypass)
        if (blockState.block !== Blocks.air && blockState.block.isPassable(mc.theWorld, playerPos)) return
        
        // Skip first few ticks after joining
        if (mc.thePlayer.ticksExisted <= 5) return
        
        // Handle big velocity tick countdown
        if (bigVelocityTick > 0) {
            bigVelocityTick--
        }
        if (mc.thePlayer.onGround && bigVelocityTick <= 30) {
            bigVelocityTick = 0
        }
        
        val motion = max(mc.thePlayer.motionX, mc.thePlayer.motionZ)
        
        // Place blocks if not in full sprint mode
        if (!fullSprint.get()) {
            place(true)
        }
        
        // Movement checks
        if (!fullSprint.get() && motion <= 0.4) {
            if (abs(mc.thePlayer.motionX) < 0.03 || abs(mc.thePlayer.motionZ) < 0.03) {
                if (!mc.thePlayer.onGround && mc.thePlayer.fallDistance <= 2) return
            } else {
                if (!mc.thePlayer.onGround && mc.thePlayer.fallDistance <= 1) return
            }
        }
        
        // Update base Y position
        if (baseY == -1 || baseY > mc.thePlayer.posY.toInt() - 1 || 
            bigVelocityTick > 0 || mc.thePlayer.onGround || 
            mc.gameSettings.keyBindJump.isKeyDown) {
            baseY = mc.thePlayer.posY.toInt() - 1
        }
        
        // Find block to place
        findBlock()
        
        // Switch to block slot
        if (!InventoryUtil.switchBlock()) return
        
        // Check if can place
        canPlace = !mc.gameSettings.keyBindJump.isKeyDown || mc.thePlayer.fallDistance >= 2
        if (mc.gameSettings.keyBindJump.isKeyDown && !canPlace) {
            return
        }
        
        if (blockPos != null) {
            var reachable = true
            
            // Check if falling and block is above predicted position
            if (mc.thePlayer.motionY < -0.1) {
                val fallingPlayer = FallingPlayer(mc.thePlayer)
                fallingPlayer.calculate(2)
                if (blockPos!!.y > fallingPlayer.getY().toInt()) {
                    reachable = false
                }
            }
            
            // Handle rotation and placement
            if ((!reachable || bigVelocityTick > 0 || fullSprint.get()) && rotateCount <= 8) {
                val rotation = RotationUtil.getRotationBlock(blockPos!!, 0f)
                
                if (dbgV.get()) {
                    ChatUtil.info("working $rotateCount")
                }
                
                mc.thePlayer.fallDistance++
                rotateCount++
                
                mc.netHandler.addToSendQueue(
                    C03PacketPlayer.C05PacketPlayerLook(rotation.yaw, rotation.pitch, mc.thePlayer.onGround)
                )
                
                place(false)
                onUpdate(event) // Recursive call for continuous placement
            } else {
                val rotation = RotationUtil.getRotationBlock(blockPos!!, 1f)
                rotateCount = 0
                RotationUtil.setTargetRotation(rotation, 0)
            }
        }
        
        // Auto-disable in spectator mode
        if (mc.thePlayer.capabilities.isCreativeMode && mc.thePlayer.capabilities.allowFlying) {
            this.state = false
        }
    }
    
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST && switchBack.get()) {
            if (mc.thePlayer.inventory.currentItem != slot) {
                mc.netHandler.addToSendQueue(C00PacketKeepAlive())
                mc.thePlayer.inventory.currentItem = slot
            }
        }
    }
    
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity && mc.thePlayer != null && 
            packet.entityID == mc.thePlayer.entityId) {
            
            val strength = Vec3(
                packet.motionX / 8000.0,
                0.0,
                packet.motionZ / 8000.0
            ).lengthVector()
            
            if (strength >= 1.5) {
                ChatUtil.info("你也是要飞了: $strength")
                bigVelocityTick = 60
            }
        }
    }
    
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (blockPos != null || lastBlockPos != null) {
            if (renderTargetPos.get()) {
                val targetPos = if (lastBlockPos == null) {
                    blockPos?.offset(enumFacing)
                } else if (lastEnumFacing != null) {
                    lastBlockPos?.offset(lastEnumFacing)
                } else {
                    null
                }
                
                if (targetPos != null) {
                    if (mc.theWorld.getBlockState(targetPos).block is BlockAir) {
                        RenderUtil.drawOutlinedBoundingBox(targetPos, 2, Color(255, 0, 0, 120))
                    } else {
                        val box = mc.theWorld.getBlockState(targetPos)
                            .block.getSelectedBoundingBox(mc.theWorld, targetPos)
                        RenderUtil.boundingESPBoxFilled(box, Color(0, 255, 0, 120))
                    }
                }
            }
            
            if (renderClickPos.get() && blockPos != null) {
                val box = mc.theWorld.getBlockState(blockPos!!)
                    .block.getSelectedBoundingBox(mc.theWorld, blockPos!!)
                RenderUtil.boundingESPBoxFilled(box, Color(255, 10, 10, 120))
            }
        }
    }
    
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val sr = ScaledResolution(mc)
        val count = getBlockCount()
        
        val colorCode = when {
            count > 64 -> "§a"  // Green
            count > 0 -> "§e"   // Yellow
            else -> "§c"        // Red
        }
        
        val text = "§fBlocks: $colorCode$count"
        
        mc.fontRendererObj.drawStringWithShadow(
            text,
            (sr.scaledWidth / 2 - mc.fontRendererObj.getStringWidth(text) / 2).toFloat(),
            (sr.scaledHeight / 2 - 30).toFloat(),
            -1
        )
    }
    
    /**
     * Find block position to place
     */
    private fun findBlock() {
        val baseVec = mc.thePlayer.getPositionEyes(2.0f)
        val base = BlockPos(baseVec.xCoord.toInt(), baseY, baseVec.zCoord.toInt())
        val baseX = base.x
        val baseZ = base.z
        
        // Check if standing on solid block
        if (mc.theWorld.getBlockState(base).block.isFullCube) return
        
        // Check base position
        if (checkBlock(baseVec, base)) {
            return
        }
        
        // Spiral search pattern
        for (d in 1..6) {
            if (checkBlock(baseVec, BlockPos(baseX, baseY - d, baseZ))) {
                return
            }
            
            for (x in 1..d) {
                for (z in 0..d - x) {
                    val y = d - x - z
                    
                    for (rev1 in 0..1) {
                        for (rev2 in 0..1) {
                            if (checkBlock(
                                    baseVec,
                                    BlockPos(
                                        baseX + if (rev1 == 0) x else -x,
                                        baseY - y,
                                        baseZ + if (rev2 == 0) z else -z
                                    )
                                )) {
                                return
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if block can be placed at position
     */
    private fun checkBlock(baseVec: Vec3, pos: BlockPos): Boolean {
        if (mc.theWorld.getBlockState(pos).block !is BlockAir) return false
        
        val center = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        
        for (face in EnumFacing.values()) {
            val dirVec = Vec3(face.directionVec)
            val hit = center.addVector(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5)
            val baseBlock = pos.offset(face)
            
            if (!mc.theWorld.getBlockState(baseBlock).block.isFullCube) continue
            
            val relevant = hit.subtract(baseVec)
            
            if (relevant.lengthVector() <= 4.5 * 4.5 && 
                relevant.dotProduct(Vec3(face.directionVec)) >= 0.0) {
                blockPos = baseBlock
                enumFacing = face.opposite
                return true
            }
        }
        
        return false
    }
    
    /**
     * Get vector for block placement with randomization
     */
    private fun getVec3(pos: BlockPos, face: EnumFacing): Vec3 {
        var x = pos.x + 0.5
        var y = pos.y + 0.5
        var z = pos.z + 0.5
        
        if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
            x += Math.random() * 0.6 - 0.3
            z += Math.random() * 0.6 - 0.3
        } else {
            y += Math.random() * 0.6 - 0.3
        }
        
        if (face == EnumFacing.WEST || face == EnumFacing.EAST) {
            z += Math.random() * 0.6 - 0.3
        }
        
        if (face == EnumFacing.SOUTH || face == EnumFacing.NORTH) {
            x += Math.random() * 0.6 - 0.3
        }
        
        return Vec3(x, y, z)
    }
    
    /**
     * Place block at target position
     */
    private fun place(rotate: Boolean) {
        if (!canPlace) return
        if (!InventoryUtil.switchBlock()) return
        
        if (blockPos != null && enumFacing != null) {
            if (mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                mc.thePlayer.heldItem,
                blockPos!!,
                enumFacing!!,
                getVec3(blockPos!!, enumFacing!!)
            )) {
                mc.thePlayer.swingItem()
            }
            
            // Save last placement info
            lastBlockPos = blockPos
            lastEnumFacing = enumFacing
            blockPos = null
            
            if (rotate) {
                RotationUtil.setTargetRotation(
                    net.ccbluex.liquidbounce.utils.Rotation(
                        mc.thePlayer.rotationYaw,
                        mc.thePlayer.rotationPitch
                    ),
                    0
                )
            }
        }
    }
    
    /**
     * Get block count in inventory
     */
    private fun getBlockCount(): Int {
        var blockCount = 0
        
        for (i in 9..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
            
            if (stack.item is ItemBlock) {
                val block = (stack.item as ItemBlock).block
                if (!invalidBlocks.contains(block)) {
                    blockCount += stack.stackSize
                }
            }
        }
        
        return blockCount
    }
}
