/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.player.InvManager
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.Stealer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.autoplayrobot.*
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.ItemBow
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import kotlin.math.sqrt

class AutoPlayRobot : Module(
    name = "AutoPlayRobot",
    category = ModuleCategory.MISC,
    description = "Baritone-driven Hypixel mini-game robot controller."
) {
    private val modeValue = ListValue("Mode", arrayOf("HypixelMurder", "HypixelBedWars", "HypixelSkyWars"), "HypixelMurder")
    private val autoQueueValue = BoolValue("AutoQueue", true)
    private val useBaritoneValue = BoolValue("UseBaritone", true)
    private val debugOverlayValue = BoolValue("DebugOverlay", true)
    private val safetyStopValue = BoolValue("SafetyStop", true)
    private val maxFightRangeValue = FloatValue("MaxFightRange", 4.2F, 2F, 8F)
    private val stuckTimeoutValue = IntegerValue("StuckTimeout", 5, 2, 20, "s")
    private val decisionDelayValue = IntegerValue("DecisionDelay", 250, 50, 1000, "ms")

    private val context = AutoPlayRobotContext()
    private val managedModules = linkedMapOf<Module, Boolean>()
    private var queuedAt = 0L

    override val tag: String
        get() = "${modeValue.get()} ${context.task}"

    override fun onEnable() {
        context.detail = "Starting"
        queuedAt = 0L

        if (useBaritoneValue.get() && !BaritoneBridge.isAvailable()) {
            alert("AutoPlayRobot: Baritone unavailable (${BaritoneBridge.getFailureReason()})")
            FDPNext.hud.addNotification(
                Notification(name, "Baritone unavailable. Module disabled.", NotifyType.ERROR, 3500)
            )
            state = false
            return
        }

        if (modeValue.equals("HypixelMurder")) {
            ensureModule(MurderDetector, true)
        }
    }

    override fun onDisable() {
        BaritoneBridge.stop()
        stopUsingBow()
        restoreManagedModules()
        context.task = AutoPlayRobotTask.STOPPED
        context.detail = "Disabled"
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        BaritoneBridge.stop()
        stopUsingBow()
        context.targetEntity = null
        context.targetPos = null
        context.stuckSince = 0L
        context.detail = "World changed"
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S02PacketChat) {
            val text = packet.chatComponent.unformattedText
            context.lastChatHint = text
            if (autoQueueValue.get() && isGameEndChat(text)) {
                context.task = AutoPlayRobotTask.AUTO_QUEUE
                queuedAt = System.currentTimeMillis()
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        if (mc.theWorld == null) return

        val now = System.currentTimeMillis()
        if (now - context.lastDecisionAt < decisionDelayValue.get()) {
            updateStuckState(now)
            return
        }

        context.lastDecisionAt = now
        context.game = expectedGame()
        context.phase = HypixelGameDetector.phase(context.lastChatHint)

        if (safetyStopValue.get() && !isExpectedEnvironment()) {
            stopRobot("Not in selected Hypixel game")
            return
        }

        if (context.phase == AutoPlayRobotPhase.DEAD || context.phase == AutoPlayRobotPhase.ENDED) {
            handleAutoQueue(now)
            return
        }

        updateStuckState(now)

        when (modeValue.get()) {
            "HypixelMurder" -> tickMurder(player, now)
            "HypixelBedWars" -> tickBedWars()
            "HypixelSkyWars" -> tickSkyWars()
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!debugOverlayValue.get()) {
            return
        }

        val font = mc.fontRendererObj
        val lines = listOf(
            "AutoPlayRobot: ${modeValue.get()}",
            "Game: ${context.game} Phase: ${context.phase}",
            "Task: ${context.task}",
            "Target: ${context.targetEntity?.name ?: context.targetPos ?: "None"}",
            "Baritone: ${if (BaritoneBridge.isAvailable()) "OK" else BaritoneBridge.getFailureReason()} Pathing=${BaritoneBridge.isPathing()}",
            "Detail: ${context.detail}"
        )

        var y = 92
        for (line in lines) {
            font.drawStringWithShadow(line, 6F, y.toFloat(), 0xFFFFFF)
            y += 10
        }
    }

    private fun tickMurder(player: EntityPlayer, now: Long) {
        ensureModule(MurderDetector, true)

        val murderers = MurderDetector.getMurderers()
        val nearestMurderer = murderers.minByOrNull { player.getDistanceToEntity(it) }
        if (nearestMurderer != null && player.getDistanceToEntity(nearestMurderer) <= 9F) {
            evade(player, nearestMurderer)
            return
        }

        if (nearestMurderer != null && hasBowAndArrow() && player.canEntityBeSeen(nearestMurderer)) {
            shootMurderer(nearestMurderer)
            return
        }

        stopUsingBow()
        val gold = findGold(murderers)
        if (gold != null) {
            context.task = AutoPlayRobotTask.COLLECT_GOLD
            context.targetEntity = gold
            context.targetPos = BlockPos(gold.posX, gold.posY, gold.posZ)
            context.detail = "Collecting gold"
            goto(context.targetPos!!, now)
        } else {
            context.task = AutoPlayRobotTask.WAITING_FOR_GAME
            context.targetEntity = null
            context.targetPos = null
            context.detail = "No safe gold target"
        }
    }

    private fun tickBedWars() {
        ensureModule(FDPNext.moduleManager[Scaffold::class.java], false)
        context.task = AutoPlayRobotTask.BW_COLLECT_RESOURCES
        context.detail = "BedWars strategy scaffold ready; resource/shop/bed tasks pending"
    }

    private fun tickSkyWars() {
        ensureModule(FDPNext.moduleManager[Stealer::class.java], true)
        ensureModule(FDPNext.moduleManager[InvManager::class.java], true)
        context.task = AutoPlayRobotTask.SW_LOOT_CHESTS
        context.detail = "SkyWars looting modules enabled; center/fight tasks pending"
    }

    private fun evade(player: EntityPlayer, murderer: EntityPlayer) {
        stopUsingBow()
        val dx = player.posX - murderer.posX
        val dz = player.posZ - murderer.posZ
        val length = sqrt(dx * dx + dz * dz).coerceAtLeast(0.1)
        val target = BlockPos(player.posX + dx / length * 14.0, player.posY, player.posZ + dz / length * 14.0)

        context.task = AutoPlayRobotTask.EVADE_MURDERER
        context.targetEntity = murderer
        context.targetPos = target
        context.detail = "Evading ${murderer.name}"
        goto(target, System.currentTimeMillis())
    }

    private fun shootMurderer(target: EntityPlayer) {
        context.task = AutoPlayRobotTask.SHOOT_MURDERER
        context.targetEntity = target
        context.targetPos = BlockPos(target.posX, target.posY, target.posZ)
        context.detail = "Shooting ${target.name}"
        BaritoneBridge.stop()

        val bowSlot = findBowSlot() ?: return
        mc.thePlayer.inventory.currentItem = bowSlot
        val heldItem = mc.thePlayer.heldItem ?: return
        if (heldItem.item !is ItemBow) {
            return
        }

        RotationUtils.faceBow(target, true, true, 2F)
        mc.gameSettings.keyBindUseItem.pressed = true
        if (!mc.thePlayer.isUsingItem) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, heldItem)
        }

        if (mc.thePlayer.isUsingItem && mc.thePlayer.itemInUseDuration >= 20) {
            mc.gameSettings.keyBindUseItem.pressed = false
            mc.thePlayer.stopUsingItem()
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN)
            )
        }
    }

    private fun goto(pos: BlockPos, now: Long) {
        if (!useBaritoneValue.get()) {
            return
        }

        if (context.targetPos != pos || now - context.lastPathChangeAt > 2_000L || !BaritoneBridge.isPathing()) {
            if (BaritoneBridge.goto(pos)) {
                context.lastPathChangeAt = now
            } else {
                context.detail = "Baritone goto failed: ${BaritoneBridge.getFailureReason()}"
            }
        }
    }

    private fun findGold(murderers: List<EntityPlayer>): EntityItem? {
        val player = mc.thePlayer ?: return null
        return mc.theWorld.loadedEntityList
            .asSequence()
            .filterIsInstance<EntityItem>()
            .filter { it.entityItem?.item == Items.gold_ingot }
            .filter { gold -> murderers.none { it.getDistanceToEntity(gold) < 8F } }
            .minByOrNull { player.getDistanceToEntity(it) }
    }

    private fun updateStuckState(now: Long) {
        val player = mc.thePlayer ?: return
        val moved = player.getDistance(context.lastPlayerX, context.lastPlayerY, context.lastPlayerZ) > 0.45
        if (moved) {
            context.stuckSince = 0L
            context.lastPlayerX = player.posX
            context.lastPlayerY = player.posY
            context.lastPlayerZ = player.posZ
            return
        }

        if (BaritoneBridge.isPathing()) {
            if (context.stuckSince == 0L) {
                context.stuckSince = now
            } else if (now - context.stuckSince > stuckTimeoutValue.get() * 1000L) {
                BaritoneBridge.stop()
                context.stuckSince = 0L
                context.detail = "Stuck timeout, path reset"
            }
        }
    }

    private fun handleAutoQueue(now: Long) {
        context.task = AutoPlayRobotTask.AUTO_QUEUE
        BaritoneBridge.stop()
        stopUsingBow()

        if (!autoQueueValue.get()) {
            context.detail = "Game ended"
            return
        }

        if (queuedAt == 0L) {
            queuedAt = now
        }

        if (now - queuedAt >= 2_500L) {
            mc.thePlayer?.sendChatMessage(queueCommand())
            queuedAt = now
            context.detail = "Queued ${queueCommand()}"
        }
    }

    private fun stopRobot(reason: String) {
        BaritoneBridge.stop()
        stopUsingBow()
        context.task = AutoPlayRobotTask.IDLE
        context.detail = reason
    }

    private fun expectedGame(): AutoPlayRobotGame {
        return when (modeValue.get()) {
            "HypixelMurder" -> AutoPlayRobotGame.HYPIXEL_MURDER
            "HypixelBedWars" -> AutoPlayRobotGame.HYPIXEL_BEDWARS
            "HypixelSkyWars" -> AutoPlayRobotGame.HYPIXEL_SKYWARS
            else -> AutoPlayRobotGame.NONE
        }
    }

    private fun isExpectedEnvironment(): Boolean {
        val detected = HypixelGameDetector.currentGame()
        return detected == AutoPlayRobotGame.NONE || detected == expectedGame()
    }

    private fun queueCommand(): String {
        return when (modeValue.get()) {
            "HypixelMurder" -> "/play murder_classic"
            "HypixelBedWars" -> "/play bedwars_eight_one"
            "HypixelSkyWars" -> "/play solo_normal"
            else -> "/lobby"
        }
    }

    private fun isGameEndChat(text: String): Boolean {
        return text.contains("play again", true) ||
            text.contains("you died", true) ||
            text.contains("winner", true) ||
            text.contains("game over", true)
    }

    private fun hasBowAndArrow(): Boolean {
        return findBowSlot() != null && mc.thePlayer.inventory.mainInventory.any { it?.item == Items.arrow }
    }

    private fun findBowSlot(): Int? {
        for (slot in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(slot) ?: continue
            if (stack.item is ItemBow) {
                return slot
            }
        }
        return null
    }

    private fun stopUsingBow() {
        mc.gameSettings.keyBindUseItem.pressed = false
        if (mc.thePlayer?.heldItem?.item is ItemBow && mc.thePlayer.isUsingItem) {
            mc.thePlayer.stopUsingItem()
        }
    }

    private fun ensureModule(module: Module?, enabled: Boolean) {
        if (module == null) {
            return
        }

        if (!managedModules.containsKey(module)) {
            managedModules[module] = module.state
        }

        if (module.state != enabled) {
            module.state = enabled
        }
    }

    private fun restoreManagedModules() {
        managedModules.forEach { (module, previousState) ->
            if (module.state != previousState) {
                module.state = previousState
            }
        }
        managedModules.clear()
    }
}
