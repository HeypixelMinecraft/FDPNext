/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * MyauKillAura - Adapted from LeaderClient KillAura
 * https://github.com/Mornly/LeaderClient/blob/main/src/main/java/unfair/module/modules/combat/KillAura.java
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySilverfish
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class MyauKillAura : Module(name = "MyauKillAura", category = ModuleCategory.COMBAT) {

    // ─ Mode & Sort ──
    private val modeValue = ListValue("Mode", arrayOf("Single", "Switch"), "Single")
    private val sortValue = ListValue("Sort", arrayOf("Distance", "Health", "HurtTime", "FOV"), "Distance")

    // ── AutoBlock ──
    private val autoBlockValue = ListValue(
        "AutoBlock",
        arrayOf("None", "Vanilla", "Hypixel", "Legit", "Fake", "HypixelTest", "HypixelCustom"),
        "None"
    )
    private val autoBlockRequirePress = BoolValue("AutoBlockRequirePress", false)
    private val autoBlockCPS = IntegerValue("AutoBlockCPS", 10, 1, 20)
    private val autoBlockRange = FloatValue("AutoBlockRange", 6f, 3f, 8f)

    // ── Ranges ──
    private val swingRange = FloatValue("SwingRange", 3.5f, 3f, 6f)
    private val attackRange = FloatValue("AttackRange", 3f, 3f, 6f)

    // ── CPS ──
    private val minCPS = IntegerValue("MinCPS", 14, 1, 20)
    private val maxCPS = IntegerValue("MaxCPS", 14, 1, 20)
    private val switchDelay = IntegerValue("SwitchDelay", 150, 0, 1000)

    // ── Rotations ──
    private val rotationsValue = ListValue("Rotations", arrayOf("None", "Legit", "Silent", "LockView"), "Silent")
    private val moveFixValue = ListValue("MoveFix", arrayOf("None", "Silent", "Strict"), "Silent")
    private val smoothing = FloatValue("Smoothing", 0f, 0f, 100f)
    private val angleStep = IntegerValue("AngleStep", 90, 30, 180)

    // ── Misc ──
    private val throughWalls = BoolValue("ThroughWalls", true)
    private val requirePress = BoolValue("RequirePress", false)
    private val allowMining = BoolValue("AllowMining", false)
    private val weaponsOnly = BoolValue("WeaponsOnly", false)
    private val allowTools = BoolValue("AllowTools", false)
    private val inventoryCheck = BoolValue("InventoryCheck", true)
    private val lowTimerCheck = BoolValue("LowTimerCheck", true)
    private val botCheck = BoolValue("BotCheck", true)
    private val fov = IntegerValue("FOV", 360, 30, 360)

    // ── Targets ──
    private val players = BoolValue("Players", true)
    private val bosses = BoolValue("Bosses", false)
    private val mobs = BoolValue("Mobs", false)
    private val animals = BoolValue("Animals", false)
    private val golems = BoolValue("Golems", false)
    private val silverfish = BoolValue("Silverfish", false)
    private val teams = BoolValue("Teams", true)

    // ─ Hypixel Custom AutoBlock ticks ──
    private val maxTick = IntegerValue("MaxTick", 3, 1, 5)
    private val startBlinkTick = IntegerValue("StartBlinkTick", 0, 1, 5)
    private val stopBlinkTick = IntegerValue("StopBlinkTick", 2, 1, 5)
    private val swapTick = IntegerValue("SwapTick", 2, 1, 5)
    private val switchBackTick = IntegerValue("SwitchBackTick", 2, 1, 5)
    private val stopBlockTick = IntegerValue("StopBlockTick", 2, 1, 5)
    private val attackTick = IntegerValue("AttackTick", 0, 1, 5)
    private val startBlockTick = IntegerValue("StartBlockTick", 0, 1, 5)
    private val postStartBlock = BoolValue("PostBlock", false)
    private val noSwap = BoolValue("NoSwap", true)

    // ── Internal state ──
    private var target: AttackData? = null
    private var switchTickCounter = 0
    private var hitRegistered = false
    private var blockingState = false
    private var isBlocking = false
    private var fakeBlockState = false
    private var attackDelayMS = 0L
    private var blockTick = 0
    private var swapped = false
    private var postBlock = false
    private var postSwap = false

    private val switchTimer = MSTimer()

    // ── AttackData ──
    private class AttackData(val entity: EntityLivingBase) {
        val box: AxisAlignedBB = entity.hitBox.expand(
            entity.collisionBorderSize.toDouble(),
            entity.collisionBorderSize.toDouble(),
            entity.collisionBorderSize.toDouble()
        )
        val x = entity.posX
        val y = entity.posY
        val z = entity.posZ
    }

    // ── Helpers ──
    private fun getAttackDelay(): Long {
        return if (isBlocking) (1000f / autoBlockCPS.get()).toLong()
        else {
            val min = minCPS.get().toLong()
            val max = maxCPS.get().toLong()
            val cps = if (max <= min) min else min + (Random().nextLong() % (max - min + 1))
            1000L / cps
        }
    }

    private fun isPlayerBlocking(): Boolean {
        return (mc.thePlayer.isUsingItem || blockingState) && mc.thePlayer.heldItem?.item is ItemSword
    }

    private fun canAutoBlock(): Boolean {
        if (mc.thePlayer.heldItem?.item !is ItemSword) return false
        return !autoBlockRequirePress.get() || mc.thePlayer.isUsingItem
    }

    private fun isBoxInAttackRange(aabb: AxisAlignedBB): Boolean {
        return distanceToBox(aabb) <= attackRange.get()
    }

    private fun isBoxInSwingRange(aabb: AxisAlignedBB): Boolean {
        return distanceToBox(aabb) <= swingRange.get()
    }

    private fun isInBlockRange(entity: EntityLivingBase): Boolean {
        return mc.thePlayer.getDistanceToEntityBox(entity) <= autoBlockRange.get()
    }

    private fun isInSwingRange(entity: EntityLivingBase): Boolean {
        return mc.thePlayer.getDistanceToEntityBox(entity) <= swingRange.get()
    }

    private fun isInAttackRange(entity: EntityLivingBase): Boolean {
        return mc.thePlayer.getDistanceToEntityBox(entity) <= attackRange.get()
    }

    private fun isInRange(entity: EntityLivingBase): Boolean {
        return isInBlockRange(entity) || isInSwingRange(entity) || isInAttackRange(entity)
    }

    private fun distanceToBox(aabb: AxisAlignedBB): Double {
        val eyes = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.posY + mc.thePlayer.getEyeHeight().toDouble(),
            mc.thePlayer.posZ
        )
        return aabb.calculateIntercept(
            eyes,
            Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight().toDouble(),
                mc.thePlayer.posZ
            )
        )?.let {
            eyes.distanceTo(it.hitVec)
        } ?: Double.MAX_VALUE
    }

    private fun angleToEntity(entity: EntityLivingBase): Float {
        val dx = entity.posX - mc.thePlayer.posX
        val dz = entity.posZ - mc.thePlayer.posZ
        val yaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f
        var diff = abs(net.minecraft.util.MathHelper.wrapAngleTo180_float(yaw) - net.minecraft.util.MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw))
        if (diff > 180f) diff = 360f - diff
        return diff
    }

    private fun hasVisiblePoint(aabb: AxisAlignedBB): Boolean {
        val eyesPos = Vec3(
            mc.thePlayer.posX,
            mc.thePlayer.posY + mc.thePlayer.getEyeHeight().toDouble(),
            mc.thePlayer.posZ
        )
        return mc.theWorld.rayTraceBlocks(eyesPos, Vec3(aabb.minX + (aabb.maxX - aabb.minX) * 0.5, aabb.minY + (aabb.maxY - aabb.minY) * 0.5, aabb.minZ + (aabb.maxZ - aabb.minZ) * 0.5)) == null
    }

    private fun isPlayerTarget(entity: EntityLivingBase): Boolean {
        return entity is EntityPlayer && EntityUtils.isSelected(entity, false)
    }

    private fun isTeamMate(entity: EntityLivingBase): Boolean {
        val teams = FDPNext.moduleManager.getModule(Teams::class.java)
        return teams?.state == true && teams.isInYourTeam(entity)
    }

    private fun isValidTarget(entity: EntityLivingBase): Boolean {
        if (!mc.theWorld.loadedEntityList.contains(entity)) return false
        if (entity === mc.thePlayer || entity === mc.thePlayer.ridingEntity) return false
        if (entity === mc.renderViewEntity || entity === mc.renderViewEntity?.ridingEntity) return false
        if (entity.deathTime > 0) return false
        if (angleToEntity(entity) > fov.get().toFloat()) return false
        if (!throughWalls.get() && !hasVisiblePoint(
                entity.entityBoundingBox.expand(
                    entity.collisionBorderSize.toDouble(),
                    entity.collisionBorderSize.toDouble(),
                    entity.collisionBorderSize.toDouble()
                )
            )
        ) return false

        return when (entity) {
            is EntityOtherPlayerMP -> {
                if (!players.get()) return false
                if (EntityUtils.isFriend(entity)) return false
                (!teams.get() || !isTeamMate(entity)) &&
                        (!botCheck.get() || !AntiBot.isBot(entity))
            }
            is EntityDragon, is EntityWither -> bosses.get()
            is EntityMob, is EntitySlime -> {
                if (entity is EntitySilverfish) silverfish.get() && (!teams.get() || !isTeamMate(entity))
                else mobs.get()
            }
            is EntityAnimal, is EntityBat, is EntitySquid, is EntityVillager -> animals.get()
            is EntityIronGolem -> golems.get() && (!teams.get() || !isTeamMate(entity))
            else -> false
        }
    }

    private fun hasValidTarget(): Boolean {
        return mc.theWorld.loadedEntityList.any { entity ->
            entity is EntityLivingBase && isValidTarget(entity) && isInBlockRange(entity)
        }
    }

    private fun canAttack(): Boolean {
        if (inventoryCheck.get() && mc.currentScreen is GuiContainer) return false
        if (lowTimerCheck.get() && mc.timer.timerSpeed < 1f) return false
        if (weaponsOnly.get() && mc.thePlayer.heldItem?.item !is ItemSword && !(allowTools.get() && isHoldingTool())) return false
        if (mc.playerController.isHittingBlock) return false
        if ((isEating() || isUsingBow()) && mc.thePlayer.isUsingItem) return false
        if (requirePress.get() && !mc.gameSettings.keyBindAttack.isKeyDown) return false
        if (!allowMining.get() && mc.objectMouseOver?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.gameSettings.keyBindAttack.isKeyDown) return false
        return true
    }

    private fun isHoldingTool(): Boolean {
        val item = mc.thePlayer.heldItem?.item ?: return false
        val name = item.javaClass.simpleName.lowercase()
        return name.contains("pickaxe") || name.contains("axe") || name.contains("shovel")
    }

    private fun isEating(): Boolean {
        val item = mc.thePlayer.heldItem?.item ?: return false
        val name = item.javaClass.simpleName.lowercase()
        return name.contains("food") || name.contains("apple") || name.contains("potion") || name.contains("milk")
    }

    private fun isUsingBow(): Boolean {
        return mc.thePlayer.heldItem?.item?.javaClass?.simpleName?.lowercase()?.contains("bow") == true
    }

    private fun performAttack(yaw: Float, pitch: Float): Boolean {
        if (isPlayerBlocking() && autoBlockValue.get() != "Vanilla") return false
        if (attackDelayMS > 0L) return false

        attackDelayMS += getAttackDelay()
        mc.thePlayer.swingItem()

        if ((rotationsValue.get() != "None" || !isBoxInAttackRange(target!!.box))
            && rayTraceBox(target!!.box, yaw, pitch, attackRange.get().toDouble()) == null
        ) return false

        mc.netHandler.addToSendQueue(C02PacketUseEntity(target!!.entity, C02PacketUseEntity.Action.ATTACK))
        if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
            mc.thePlayer.attackTargetEntityWithCurrentItem(target!!.entity)
        }
        hitRegistered = true
        return true
    }

    private fun rayTraceBox(aabb: AxisAlignedBB, yaw: Float, pitch: Float, range: Double): MovingObjectPosition? {
        val eyes = Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight().toDouble(), mc.thePlayer.posZ)
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val dirX = -Math.sin(yawRad) * Math.cos(pitchRad)
        val dirY = -Math.sin(pitchRad)
        val dirZ = Math.cos(yawRad) * Math.cos(pitchRad)
        val end = Vec3(eyes.xCoord + dirX * range, eyes.yCoord + dirY * range, eyes.zCoord + dirZ * range)
        return aabb.calculateIntercept(eyes, end)
    }

    private fun sendUseItem() {
        startBlock(mc.thePlayer.heldItem)
    }

    private fun startBlock(itemStack: ItemStack?) {
        if (itemStack == null) return
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(itemStack))
        mc.thePlayer.setItemInUse(itemStack, itemStack.maxItemUseDuration)
        blockingState = true
    }

    private fun stopBlock() {
        mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
        mc.thePlayer.stopUsingItem()
        blockingState = false
    }

    private fun interactAttack(yaw: Float, pitch: Float) {
        if (target == null) return
        val mop = rayTraceBox(target!!.box, yaw, pitch, 8.0) ?: return
        mc.netHandler.addToSendQueue(
            C02PacketUseEntity(
                target!!.entity,
                Vec3(mop.hitVec.xCoord - target!!.x, mop.hitVec.yCoord - target!!.y, mop.hitVec.zCoord - target!!.z)
            )
        )
        mc.netHandler.addToSendQueue(C02PacketUseEntity(target!!.entity, C02PacketUseEntity.Action.INTERACT))
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        mc.thePlayer.setItemInUse(mc.thePlayer.heldItem, mc.thePlayer.heldItem.maxItemUseDuration)
        blockingState = true
    }

    private fun randomSlotChange() {
        var randomSlot = Random().nextInt(9)
        while (randomSlot == mc.thePlayer.inventory.currentItem) {
            randomSlot = Random().nextInt(9)
        }
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(randomSlot))
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
    }

    // ── Target Selection ──
    private fun updateTarget() {
        if (target == null || !isValidTarget(target!!.entity) || !isBoxInAttackRange(target!!.box) || !isBoxInSwingRange(target!!.box) || switchTimer.hasTimePassed(switchDelay.get().toLong())) {
            switchTimer.reset()
            val targets = mutableListOf<EntityLivingBase>()
            for (entity in mc.theWorld.loadedEntityList) {
                if (entity is EntityLivingBase && isValidTarget(entity) && isInRange(entity)) {
                    targets.add(entity)
                }
            }
            if (targets.isEmpty()) {
                target = null
            } else {
                // Filter by swing range first
                if (targets.any { isInSwingRange(it) }) {
                    targets.removeAll { !isInSwingRange(it) }
                }
                // Filter by attack range
                if (targets.any { isInAttackRange(it) }) {
                    targets.removeAll { !isInAttackRange(it) }
                }
                // Filter by player target
                if (targets.any { isPlayerTarget(it) }) {
                    targets.removeAll { !isPlayerTarget(it) }
                }
                // Sort
                targets.sortWith(Comparator { a, b ->
                    val sortBase = when (sortValue.get()) {
                        "Health" -> java.lang.Float.compare(a.health, b.health)
                        "HurtTime" -> Integer.compare(a.hurtResistantTime, b.hurtResistantTime)
                        "FOV" -> java.lang.Float.compare(angleToEntity(a), angleToEntity(b))
                        else -> java.lang.Double.compare(
                            mc.thePlayer.getDistanceToEntityBox(a),
                            mc.thePlayer.getDistanceToEntityBox(b)
                        )
                    }
                    if (sortBase != 0) sortBase
                    else java.lang.Double.compare(
                        mc.thePlayer.getDistanceToEntityBox(a),
                        mc.thePlayer.getDistanceToEntityBox(b)
                    )
                })
                if (modeValue.get() == "Switch" && hitRegistered) {
                    hitRegistered = false
                    switchTickCounter++
                }
                if (modeValue.get() == "Single" || switchTickCounter >= targets.size) {
                    switchTickCounter = 0
                }
                target = AttackData(targets[switchTickCounter])
            }
        }
        if (target != null) {
            target = AttackData(target!!.entity)
        }
    }

    // ── Events ──
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (attackDelayMS > 0L) {
            attackDelayMS -= 50L
        }

        var attack = target != null && canAttack()
        var block = attack && canAutoBlock()

        if (!block) {
            isBlocking = false
            fakeBlockState = false
            blockTick = 0
        }

        if (attack) {
            var swap = false
            var blocked = false

            when (autoBlockValue.get()) {
                "None" -> {
                    if (mc.thePlayer.isUsingItem) {
                        isBlocking = true
                        if (!isPlayerBlocking()) swap = true
                    } else {
                        isBlocking = false
                        if (isPlayerBlocking()) stopBlock()
                    }
                    fakeBlockState = false
                }
                "Vanilla" -> {
                    if (hasValidTarget()) {
                        if (!isPlayerBlocking()) swap = true
                        isBlocking = true
                        fakeBlockState = false
                    } else {
                        isBlocking = false
                        fakeBlockState = false
                    }
                }
                "Hypixel" -> {
                    if (hasValidTarget()) {
                        when (blockTick) {
                            0 -> {
                                if (!isPlayerBlocking()) swap = true
                                blocked = true
                                blockTick = 1
                            }
                            1 -> {
                                attack = false
                                blockTick = 2
                            }
                            2 -> {
                                if (isPlayerBlocking()) {
                                    if (!noSwap.get()) randomSlotChange()
                                    stopBlock()
                                }
                                attack = false
                                if (attackDelayMS <= 50L) blockTick = 0
                            }
                            else -> blockTick = 0
                        }
                        isBlocking = true
                        fakeBlockState = true
                    } else {
                        randomSlotChange()
                        stopBlock()
                        isBlocking = false
                        fakeBlockState = false
                    }
                }
                "Legit" -> {
                    if (hasValidTarget()) {
                        when (blockTick) {
                            0 -> {
                                if (!isPlayerBlocking()) swap = true
                                blockTick = 1
                            }
                            1 -> {
                                if (isPlayerBlocking()) {
                                    stopBlock()
                                    attack = false
                                }
                                if (attackDelayMS <= 50L) blockTick = 0
                            }
                            else -> blockTick = 0
                        }
                        isBlocking = true
                        fakeBlockState = false
                    } else {
                        isBlocking = false
                        fakeBlockState = false
                    }
                }
                "Fake" -> {
                    isBlocking = false
                    fakeBlockState = hasValidTarget()
                    if (mc.thePlayer.isUsingItem && !isPlayerBlocking()) swap = true
                }
                "HypixelTest" -> {
                    if (hasValidTarget()) {
                        when (blockTick) {
                            0 -> {
                                blocked = true
                                if (!isPlayerBlocking()) swap = true
                                blockTick = 1
                            }
                            1 -> {
                                if (isPlayerBlocking()) randomSlotChange()
                                attack = false
                                blockTick = 2
                            }
                            2 -> {
                                attack = false
                                stopBlock()
                                if (attackDelayMS <= 50L) blockTick = 0
                            }
                            else -> blockTick = 0
                        }
                        isBlocking = true
                        fakeBlockState = true
                    } else {
                        randomSlotChange()
                        isBlocking = false
                        fakeBlockState = false
                    }
                }
                "HypixelCustom" -> {
                    if (hasValidTarget()) {
                        if (blockTick + 1 == startBlinkTick.get()) blocked = true
                        if (blockTick + 1 != attackTick.get()) attack = false
                        if (blockTick + 1 == startBlockTick.get()) {
                            if (!isPlayerBlocking()) {
                                swap = true
                                if (postStartBlock.get()) postBlock = true
                            }
                        }
                        if (blockTick + 1 == swapTick.get()) {
                            randomSlotChange()
                            swapped = true
                        }
                        if (blockTick + 1 == switchBackTick.get()) {
                            if (swapped) {
                                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                                swapped = false
                            }
                        }
                        if (blockTick + 1 == stopBlockTick.get()) {
                            if (isPlayerBlocking()) stopBlock()
                        }
                        blockTick++
                        if (blockTick >= maxTick.get() - 1) blockTick = 0
                        isBlocking = true
                        fakeBlockState = true
                    } else {
                        if (swapped) {
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                            swapped = false
                        }
                        isBlocking = false
                        fakeBlockState = false
                    }
                }
            }

            // Perform attack
            var attacked = false
            if (target != null && isBoxInSwingRange(target!!.box)) {
                if (rotationsValue.get() == "Silent" || rotationsValue.get() == "LockView") {
                    val targetRotations = RotationUtils.getRotations(
                        target!!.entity.posX,
                        target!!.entity.posY + target!!.entity.getEyeHeight().toDouble(),
                        target!!.entity.posZ
                    )
                    val smoothedYaw = mc.thePlayer.rotationYaw + (targetRotations.yaw - mc.thePlayer.rotationYaw) * (smoothing.get() / 100f)
                    val smoothedPitch = mc.thePlayer.rotationPitch + (targetRotations.pitch - mc.thePlayer.rotationPitch) * (smoothing.get() / 100f)

                    if (rotationsValue.get() == "Silent") {
                        RotationUtils.setTargetRotation(Rotation(smoothedYaw, smoothedPitch), 0)
                    }
                }
                if (attack) {
                    attacked = performAttack(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
                }
            }

            if (swap) {
                if (attacked) interactAttack(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
                else if (!postBlock) sendUseItem()
            }
            if (blocked) {
                // Blink placeholder - FDPNext doesn't have blink manager in same way
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            updateTarget()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C07PacketPlayerDigging && packet.status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
            blockingState = false
        }
        if (packet is C09PacketHeldItemChange) {
            blockingState = false
            if (isBlocking) mc.thePlayer.stopUsingItem()
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (postSwap) {
            randomSlotChange()
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            stopBlock()
            postSwap = false
        }
        if (postBlock) {
            sendUseItem()
            postBlock = false
        }
        // Sync blocking state
        if (isPlayerBlocking() && !mc.thePlayer.isBlocking) {
            mc.thePlayer.setItemInUse(mc.thePlayer.heldItem, mc.thePlayer.heldItem.maxItemUseDuration)
        }
    }

    override fun onEnable() {
        target = null
        switchTickCounter = 0
        hitRegistered = false
        attackDelayMS = 0L
        blockTick = 0
        blockingState = false
        isBlocking = false
        fakeBlockState = false
        swapped = false
        postBlock = false
        postSwap = false
    }

    override fun onDisable() {
        if (autoBlockValue.get() == "Hypixel" || autoBlockValue.get() == "HypixelTest" || autoBlockValue.get() == "HypixelCustom") {
            randomSlotChange()
            stopBlock()
        }
        blockingState = false
        isBlocking = false
        fakeBlockState = false
    }
}
