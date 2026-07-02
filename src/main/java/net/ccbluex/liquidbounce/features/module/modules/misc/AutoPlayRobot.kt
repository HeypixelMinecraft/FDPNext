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
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoWeapon
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.InvManager
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.Stealer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.autoplayrobot.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.block.Block
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemPickaxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.util.Locale
import java.util.concurrent.Future
import kotlin.math.sqrt

class AutoPlayRobot : Module(
    name = "AutoPlayRobot",
    category = ModuleCategory.MISC,
    description = "Baritone-driven Hypixel mini-game robot controller."
) {
    private val modeValue = ListValue("Mode", arrayOf("HypixelMurder", "HypixelBedWars", "HypixelSkyWars"), "HypixelMurder")
    private val decisionModeValue = ListValue("DecisionMode", arrayOf("Local", "AIHybrid", "AIDirect"), "Local")
    private val aiProviderValue = ListValue("AIProvider", arrayOf("DeepSeek", "QwenDashScope", "OpenAICompatible", "Custom"), "DeepSeek")
    private val aiEndpointValue = TextValue("AIEndpoint", "")
    private val aiModelValue = TextValue("AIModel", "")
    private val aiApiKeyValue = TextValue("AIApiKey", "")
    private val aiVisionValue = ListValue("AIVision", arrayOf("Off", "Auto", "ForceOn"), "Auto")
    private val visionFpsValue = IntegerValue("VisionFps", 1, 1, 5)
    private val visionWidthValue = IntegerValue("VisionWidth", 320, 160, 640)
    private val aiIntervalValue = IntegerValue("AIInterval", 1500, 500, 10000, "ms")
    private val aiTimeoutValue = IntegerValue("AITimeout", 1800, 500, 10000, "ms")
    private val aiDebugValue = BoolValue("AIDebug", false)
    private val autoQueueValue = BoolValue("AutoQueue", true)
    private val useBaritoneValue = BoolValue("UseBaritone", true)
    private val debugOverlayValue = BoolValue("DebugOverlay", true)
    private val safetyStopValue = BoolValue("SafetyStop", true)
    private val maxFightRangeValue = FloatValue("MaxFightRange", 4.2F, 2F, 8F)
    private val stuckTimeoutValue = IntegerValue("StuckTimeout", 5, 2, 20, "s")
    private val decisionDelayValue = IntegerValue("DecisionDelay", 250, 50, 1000, "ms")
    private val searchRadiusValue = IntegerValue("SearchRadius", 32, 12, 96)
    private val centerRadiusValue = IntegerValue("CenterRadius", 10, 4, 32)

    private val context = AutoPlayRobotContext()
    private val managedModules = linkedMapOf<Module, Boolean>()
    private val openedChests = linkedMapOf<BlockPos, Long>()
    private var queuedAt = 0L
    private var pendingAiDecision: Future<AiDecisionResult>? = null
    private var latestAiDecision: AiDecision? = null
    private var latestAiLatency = 0L
    private var latestAiError = "Idle"
    private var latestAiVision = false
    private var latestAiRequestedAt = 0L
    private var latestVisionAt = 0L
    private var aiFailureCount = 0

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

        if (modeValue.get().equals("HypixelMurder", true)) {
            ensureModule(MurderDetector, true)
        }
    }

    override fun onDisable() {
        BaritoneBridge.stop()
        stopUsingBow()
        restoreManagedModules()
        pendingAiDecision?.cancel(true)
        pendingAiDecision = null
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
        openedChests.clear()
        pendingAiDecision?.cancel(true)
        pendingAiDecision = null
        latestAiDecision = null
        aiFailureCount = 0
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

        if (decisionModeValue.get() != "Local") {
            pollAiDecision()
            scheduleAiDecision(now)
            val decision = latestAiDecision
            if (decision != null && handleAiDecision(player, decision, now)) {
                latestAiDecision = null
                return
            }
        }

        when (modeValue.get()) {
            "HypixelMurder" -> tickMurder(player, now)
            "HypixelBedWars" -> tickBedWars(player, now)
            "HypixelSkyWars" -> tickSkyWars(player, now)
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
            "AI: ${decisionModeValue.get()} ${resolvedProvider()} ${resolvedModel()} ${latestAiLatency}ms Vision=$latestAiVision",
            "AIStatus: ${latestAiDecision?.action ?: latestAiError}",
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

    private fun pollAiDecision() {
        val future = pendingAiDecision ?: return
        if (!future.isDone) {
            return
        }

        pendingAiDecision = null
        val result = runCatching { future.get() }.getOrElse {
            AiDecisionResult(null, 0L, false, it.javaClass.simpleName + ": " + (it.message ?: "AI failed"))
        }

        latestAiLatency = result.latencyMs
        latestAiVision = result.sentVision
        if (result.error != null || result.decision == null) {
            latestAiError = result.error ?: "No decision"
            latestAiDecision = null
            aiFailureCount++
            return
        }

        latestAiDecision = result.decision
        latestAiError = result.decision.action.name
        aiFailureCount = 0
    }

    private fun scheduleAiDecision(now: Long) {
        if (pendingAiDecision != null || now - latestAiRequestedAt < aiIntervalValue.get()) {
            return
        }

        val apiKey = aiApiKeyValue.get().trim()
        if (apiKey.isEmpty()) {
            latestAiError = "Missing AIApiKey"
            return
        }

        val image = captureVisionIfNeeded(now)
        val request = AiDecisionRequest(
            endpoint = resolvedEndpoint(),
            model = resolvedModel(),
            apiKey = apiKey,
            timeoutMs = aiTimeoutValue.get(),
            directMode = decisionModeValue.get() == "AIDirect" && aiFailureCount < 3,
            observation = buildAiObservation().toString(),
            imageDataUrl = image
        )

        latestAiRequestedAt = now
        pendingAiDecision = AiDecisionClient.requestAsync(request)
        if (aiDebugValue.get()) {
            latestAiError = "Requested ${resolvedProvider()} ${request.model}"
        }
    }

    private fun captureVisionIfNeeded(now: Long): String? {
        if (!shouldUseVision()) {
            return null
        }

        val delay = 1000L / visionFpsValue.get().coerceAtLeast(1)
        if (now - latestVisionAt < delay) {
            return null
        }

        latestVisionAt = now
        return AiVisionCapture.captureJpegDataUrl(visionWidthValue.get())
    }

    private fun shouldUseVision(): Boolean {
        return when (aiVisionValue.get()) {
            "Off" -> false
            "ForceOn" -> true
            else -> {
                val model = resolvedModel().lowercase(Locale.ROOT)
                model.contains("vl") ||
                    model.contains("vision") ||
                    model.contains("qwen-vl") ||
                    model.contains("qwen3-vl") ||
                    model.contains("gpt-4o") ||
                    model.contains("gemini")
            }
        }
    }

    private fun buildAiObservation(): JsonObject {
        val player = mc.thePlayer
        val observation = JsonObject()
        observation.addProperty("mode", modeValue.get())
        observation.addProperty("decisionMode", decisionModeValue.get())
        observation.addProperty("game", context.game.name)
        observation.addProperty("phase", context.phase.name)
        observation.addProperty("task", context.task.name)
        observation.addProperty("baritonePathing", BaritoneBridge.isPathing())
        observation.addProperty("lastChatHint", context.lastChatHint?.take(160) ?: "")

        observation.add("player", JsonObject().apply {
            if (player != null) {
                addProperty("health", player.health)
                addProperty("food", player.foodStats.foodLevel)
                addProperty("x", player.posX.toInt())
                addProperty("y", player.posY.toInt())
                addProperty("z", player.posZ.toInt())
                addProperty("hasBowAndArrow", hasBowAndArrow())
                addProperty("blocks", countBlocks())
                addProperty("iron", countItem(Items.iron_ingot))
                addProperty("gold", countItem(Items.gold_ingot))
                addProperty("diamond", countItem(Items.diamond))
                addProperty("emerald", countItem(Items.emerald))
                addProperty("skyWarsGeared", hasBasicSkyWarsGear())
            }
        })

        observation.add("players", JsonArray().apply {
            val self = player ?: return@apply
            mc.theWorld?.playerEntities
                ?.asSequence()
                ?.filterIsInstance<EntityPlayer>()
                ?.filter { it !== self && self.getDistanceToEntity(it) <= 48F }
                ?.sortedBy { self.getDistanceToEntity(it) }
                ?.take(12)
                ?.forEach { target ->
                    add(JsonObject().apply {
                        addProperty("id", target.entityId)
                        addProperty("name", target.name)
                        addProperty("distance", "%.1f".format(Locale.US, self.getDistanceToEntity(target)).toDouble())
                        addProperty("health", target.health)
                        addProperty("visible", self.canEntityBeSeen(target))
                        addProperty("sameTeam", target.isOnSameTeam(self))
                    })
                }
        })

        observation.add("items", JsonArray().apply {
            val self = player ?: return@apply
            mc.theWorld?.loadedEntityList
                ?.asSequence()
                ?.filterIsInstance<EntityItem>()
                ?.filter { self.getDistanceToEntity(it) <= 36F }
                ?.filter { it.entityItem?.item in setOf(Items.gold_ingot, Items.iron_ingot, Items.diamond, Items.emerald, Items.arrow, Items.ender_pearl) }
                ?.sortedBy { self.getDistanceToEntity(it) }
                ?.take(12)
                ?.forEach { item ->
                    add(JsonObject().apply {
                        addProperty("id", item.entityId)
                        addProperty("item", item.entityItem.displayName)
                        addProperty("x", item.posX.toInt())
                        addProperty("y", item.posY.toInt())
                        addProperty("z", item.posZ.toInt())
                        addProperty("distance", "%.1f".format(Locale.US, self.getDistanceToEntity(item)).toDouble())
                    })
                }
        })

        observation.add("nearBlocks", JsonObject().apply {
            if (player != null) {
                nearestUnopenedChest(player)?.let { add("chest", blockJson(it)) }
                nearestBlock(player, setOf(Blocks.bed), searchRadiusValue.get(), includeBelow = true)?.let { add("bed", blockJson(it)) }
                mapCenterTarget(player).let { add("center", blockJson(it)) }
            }
        })

        observation.add("murderers", JsonArray().apply {
            MurderDetector.getMurderers().forEach {
                add(JsonObject().apply {
                    addProperty("id", it.entityId)
                    addProperty("name", it.name)
                    addProperty("x", it.posX.toInt())
                    addProperty("y", it.posY.toInt())
                    addProperty("z", it.posZ.toInt())
                })
            }
        })

        observation.addProperty("allowedActions", if (decisionModeValue.get() == "AIDirect") {
            "MOVE_TO,LOOK_AT,ATTACK,USE_ITEM,OPEN_CHEST,BREAK_BLOCK,ENABLE_MODULE,DISABLE_MODULE,STOP"
        } else {
            "COLLECT_GOLD,COLLECT_RESOURCE,LOOT_CHEST,MOVE_TO,FIGHT_ENTITY,EVADE_ENTITY,ATTACK_BED,DEFEND_BED,SHOOT_MURDERER,AUTO_QUEUE,HOLD"
        })
        return observation
    }

    private fun blockJson(pos: BlockPos): JsonObject {
        return JsonObject().apply {
            addProperty("x", pos.x)
            addProperty("y", pos.y)
            addProperty("z", pos.z)
        }
    }

    private fun handleAiDecision(player: EntityPlayer, decision: AiDecision, now: Long): Boolean {
        if (decisionModeValue.get() == "AIDirect" && aiFailureCount < 3) {
            if (handleDirectAiDecision(player, decision, now)) {
                context.detail = "AI direct ${decision.action}: ${decision.reason}"
                return true
            }
            aiFailureCount++
        }

        return handleHybridAiDecision(player, decision, now)
    }

    private fun handleHybridAiDecision(player: EntityPlayer, decision: AiDecision, now: Long): Boolean {
        val entity = decision.entityId?.let { entityById(it, 96F) }
        val pos = decision.pos?.takeIf { isSafeTargetPos(player, it, 96.0) }

        when (decision.action) {
            AiDecisionAction.COLLECT_GOLD -> {
                val gold = (entity as? EntityItem)?.takeIf { it.entityItem?.item == Items.gold_ingot } ?: findGold(MurderDetector.getMurderers()) ?: return false
                context.task = AutoPlayRobotTask.COLLECT_GOLD
                context.targetEntity = gold
                context.targetPos = BlockPos(gold.posX, gold.posY, gold.posZ)
                context.detail = "AI collecting gold: ${decision.reason}"
                goto(context.targetPos!!, now)
                return true
            }
            AiDecisionAction.COLLECT_RESOURCE -> {
                val resource = (entity as? EntityItem) ?: nearestResourceItem(player, 36F, modeValue.get().equals("HypixelBedWars", true)) ?: return false
                context.task = AutoPlayRobotTask.BW_COLLECT_RESOURCES
                context.targetEntity = resource
                context.targetPos = BlockPos(resource.posX, resource.posY, resource.posZ)
                context.detail = "AI collecting resource: ${decision.reason}"
                goto(context.targetPos!!, now)
                return true
            }
            AiDecisionAction.LOOT_CHEST -> {
                val chest = pos ?: nearestUnopenedChest(player) ?: return false
                context.task = AutoPlayRobotTask.SW_LOOT_CHESTS
                context.targetEntity = null
                context.targetPos = chest
                context.detail = "AI looting chest: ${decision.reason}"
                if (player.getDistanceSq(chest) <= 4.25 * 4.25) {
                    BaritoneBridge.stop()
                    openChest(chest)
                    openedChests[chest] = now
                } else {
                    goto(chest, now)
                }
                return true
            }
            AiDecisionAction.MOVE_TO -> {
                val target = pos ?: return false
                context.task = AutoPlayRobotTask.IDLE
                context.targetEntity = null
                context.targetPos = target
                context.detail = "AI moving: ${decision.reason}"
                enableBridgeIfNeeded(player, target)
                goto(target, now)
                return true
            }
            AiDecisionAction.FIGHT_ENTITY -> {
                val target = entity as? EntityPlayer ?: return false
                fightOrPath(player, target, now, "AI")
                return true
            }
            AiDecisionAction.EVADE_ENTITY -> {
                val target = entity as? EntityPlayer ?: return false
                evade(player, target)
                return true
            }
            AiDecisionAction.ATTACK_BED -> {
                val bed = pos ?: return false
                if (mc.theWorld.getBlockState(bed).block != Blocks.bed) {
                    return false
                }
                context.task = AutoPlayRobotTask.BW_ATTACK_BED
                context.targetEntity = null
                context.targetPos = bed
                context.detail = "AI attacking bed: ${decision.reason}"
                equipBestBreaker()
                if (player.getDistanceSq(bed) <= 4.5 * 4.5) {
                    BaritoneBridge.stop()
                    mc.playerController.clickBlock(bed, EnumFacing.UP)
                    player.swingItem()
                } else {
                    enableBridgeIfNeeded(player, bed)
                    goto(bed, now)
                }
                return true
            }
            AiDecisionAction.DEFEND_BED -> {
                val target = entity as? EntityPlayer ?: return false
                context.task = AutoPlayRobotTask.BW_DEFEND_BED
                context.targetEntity = target
                context.targetPos = BlockPos(target)
                context.detail = "AI defending bed: ${decision.reason}"
                goto(context.targetPos!!, now)
                return true
            }
            AiDecisionAction.SHOOT_MURDERER -> {
                val target = entity as? EntityPlayer ?: return false
                if (!hasBowAndArrow() || !player.canEntityBeSeen(target)) {
                    return false
                }
                shootMurderer(target)
                return true
            }
            AiDecisionAction.AUTO_QUEUE -> {
                handleAutoQueue(now)
                return true
            }
            AiDecisionAction.HOLD, AiDecisionAction.STOP -> {
                stopRobot("AI hold: ${decision.reason}")
                return true
            }
            else -> return false
        }
    }

    private fun handleDirectAiDecision(player: EntityPlayer, decision: AiDecision, now: Long): Boolean {
        val entity = decision.entityId?.let { entityById(it, 64F) }
        val pos = decision.pos?.takeIf { isSafeTargetPos(player, it, 64.0) }

        when (decision.action) {
            AiDecisionAction.MOVE_TO -> {
                goto(pos ?: return false, now)
                return true
            }
            AiDecisionAction.LOOK_AT -> {
                val target = pos ?: entity?.let { BlockPos(it.posX, it.posY + it.eyeHeight, it.posZ) } ?: return false
                RotationUtils.faceBlock(target)?.let {
                    RotationUtils.setTargetRotation(it.rotation)
                    return true
                }
                return false
            }
            AiDecisionAction.ATTACK -> {
                val target = entity ?: return false
                if (player.getDistanceToEntity(target) > maxFightRangeValue.get() + 1F) {
                    return false
                }
                mc.playerController.attackEntity(player, target)
                player.swingItem()
                return true
            }
            AiDecisionAction.USE_ITEM -> {
                val stack = player.heldItem ?: return false
                mc.playerController.sendUseItem(player, mc.theWorld, stack)
                player.swingItem()
                return true
            }
            AiDecisionAction.OPEN_CHEST -> {
                val chest = pos ?: return false
                if (player.getDistanceSq(chest) > 5.5 * 5.5 || mc.theWorld.getBlockState(chest).block !in setOf(Blocks.chest, Blocks.trapped_chest)) {
                    return false
                }
                openChest(chest)
                openedChests[chest] = now
                return true
            }
            AiDecisionAction.BREAK_BLOCK -> {
                val block = pos ?: return false
                if (player.getDistanceSq(block) > 5.5 * 5.5) {
                    return false
                }
                mc.playerController.clickBlock(block, EnumFacing.UP)
                player.swingItem()
                return true
            }
            AiDecisionAction.ENABLE_MODULE, AiDecisionAction.DISABLE_MODULE -> {
                val module = aiControllableModule(decision.module ?: return false) ?: return false
                ensureModule(module, decision.action == AiDecisionAction.ENABLE_MODULE)
                return true
            }
            AiDecisionAction.STOP -> {
                stopRobot("AI direct stop: ${decision.reason}")
                return true
            }
            else -> return false
        }
    }

    private fun tickBedWars(player: EntityPlayer, now: Long) {
        ensureCombatModules(true)
        ensureModule(FDPNext.moduleManager[Stealer::class.java], false)
        ensureModule(FDPNext.moduleManager[InvManager::class.java], true)

        val nearestEnemy = nearestEnemyPlayer(player, 18F)
        if (nearestEnemy != null) {
            fightOrPath(player, nearestEnemy, now, "BedWars")
            return
        }

        val ownBed = nearestBlock(player, setOf(Blocks.bed), 18, includeBelow = true)
        val bedThreat = ownBed?.let { bed ->
            nearestEnemyPlayer(player, 32F) { it.getDistanceSq(bed) <= 18.0 * 18.0 }
        }
        if (ownBed != null && bedThreat != null) {
            context.task = AutoPlayRobotTask.BW_DEFEND_BED
            context.targetEntity = bedThreat
            context.targetPos = BlockPos(bedThreat)
            context.detail = "Defending bed from ${bedThreat.name}"
            goto(context.targetPos!!, now)
            return
        }

        val resource = nearestResourceItem(player, 28F, bedWars = true)
        if (resource != null && !hasEnoughBedWarsResources()) {
            ensureModule(FDPNext.moduleManager[Scaffold::class.java], false)
            context.task = AutoPlayRobotTask.BW_COLLECT_RESOURCES
            context.targetEntity = resource
            context.targetPos = BlockPos(resource.posX, resource.posY, resource.posZ)
            context.detail = "Collecting ${resource.entityItem.displayName}"
            goto(context.targetPos!!, now)
            return
        }

        val targetBed = nearestBlock(player, setOf(Blocks.bed), searchRadiusValue.get(), includeBelow = true) {
            ownBed == null || it.distanceSq(ownBed) > 10.0 * 10.0
        }
        if (targetBed != null) {
            context.task = AutoPlayRobotTask.BW_ATTACK_BED
            context.targetEntity = null
            context.targetPos = targetBed
            context.detail = "Attacking bed at ${targetBed.x}, ${targetBed.y}, ${targetBed.z}"
            equipBestBreaker()
            if (player.getDistanceSq(targetBed) <= 4.5 * 4.5) {
                BaritoneBridge.stop()
                mc.playerController.clickBlock(targetBed, EnumFacing.UP)
                mc.thePlayer.swingItem()
            } else {
                enableBridgeIfNeeded(player, targetBed)
                goto(targetBed, now)
            }
            return
        }

        val enemy = nearestEnemyPlayer(player, searchRadiusValue.get().toFloat())
        if (enemy != null) {
            fightOrPath(player, enemy, now, "BedWars hunt")
        } else {
            context.task = AutoPlayRobotTask.BW_COLLECT_RESOURCES
            context.targetEntity = null
            context.targetPos = null
            context.detail = "No bed/enemy target; holding resources"
            BaritoneBridge.stop()
        }
    }

    private fun tickSkyWars(player: EntityPlayer, now: Long) {
        ensureModule(FDPNext.moduleManager[Stealer::class.java], true)
        ensureModule(FDPNext.moduleManager[InvManager::class.java], true)
        ensureCombatModules(true)

        if (mc.currentScreen is GuiChest) {
            context.task = AutoPlayRobotTask.SW_LOOT_CHESTS
            context.detail = "Stealing chest contents"
            return
        }

        val nearestEnemy = nearestEnemyPlayer(player, 18F)
        if (nearestEnemy != null && hasBasicSkyWarsGear()) {
            fightOrPath(player, nearestEnemy, now, "SkyWars")
            return
        }

        val chest = nearestUnopenedChest(player)
        if (chest != null && shouldKeepLootingSkyWars()) {
            context.task = AutoPlayRobotTask.SW_LOOT_CHESTS
            context.targetEntity = null
            context.targetPos = chest
            context.detail = "Looting chest at ${chest.x}, ${chest.y}, ${chest.z}"
            if (player.getDistanceSq(chest) <= 4.25 * 4.25) {
                BaritoneBridge.stop()
                openChest(chest)
                openedChests[chest] = System.currentTimeMillis()
            } else {
                goto(chest, now)
            }
            return
        }

        val enemy = nearestEnemyPlayer(player, searchRadiusValue.get().toFloat())
        if (enemy != null) {
            fightOrPath(player, enemy, now, "SkyWars hunt")
            return
        }

        val center = mapCenterTarget(player)
        context.task = AutoPlayRobotTask.SW_MOVE_TO_CENTER
        context.targetEntity = null
        context.targetPos = center
        context.detail = "Moving to center"
        enableBridgeIfNeeded(player, center)
        goto(center, now)
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

    private fun fightOrPath(player: EntityPlayer, target: EntityPlayer, now: Long, label: String) {
        context.task = if (modeValue.get().equals("HypixelSkyWars", true)) AutoPlayRobotTask.SW_FIGHT else AutoPlayRobotTask.BW_ATTACK_BED
        context.targetEntity = target
        context.targetPos = BlockPos(target)
        context.detail = "$label fighting ${target.name}"

        if (player.getDistanceToEntity(target) <= maxFightRangeValue.get()) {
            ensureModule(FDPNext.moduleManager[Scaffold::class.java], false)
            BaritoneBridge.stop()
        } else {
            goto(context.targetPos!!, now)
        }
    }

    private fun ensureCombatModules(enabled: Boolean) {
        ensureModule(FDPNext.moduleManager[KillAura::class.java], enabled)
        ensureModule(FDPNext.moduleManager[AutoWeapon::class.java], enabled)
    }

    private fun nearestEnemyPlayer(
        player: EntityPlayer,
        radius: Float,
        filter: (EntityPlayer) -> Boolean = { true }
    ): EntityPlayer? {
        val playerTeam = player.team
        return mc.theWorld.playerEntities
            .asSequence()
            .filterIsInstance<EntityPlayer>()
            .filter { it !== player && !it.isDead && it.health > 0F }
            .filter { it.team == null || playerTeam == null || !it.isOnSameTeam(player) }
            .filter { player.getDistanceToEntity(it) <= radius }
            .filter(filter)
            .minByOrNull { player.getDistanceToEntity(it) }
    }

    private fun nearestResourceItem(player: EntityPlayer, radius: Float, bedWars: Boolean): EntityItem? {
        val allowed = if (bedWars) {
            setOf(Items.iron_ingot, Items.gold_ingot, Items.diamond, Items.emerald)
        } else {
            setOf(Items.ender_pearl, Items.arrow, Items.diamond_sword, Items.iron_sword)
        }

        return mc.theWorld.loadedEntityList
            .asSequence()
            .filterIsInstance<EntityItem>()
            .filter { it.entityItem?.item in allowed }
            .filter { player.getDistanceToEntity(it) <= radius }
            .minByOrNull { player.getDistanceToEntity(it) }
    }

    private fun nearestUnopenedChest(player: EntityPlayer): BlockPos? {
        openedChests.entries.removeIf { System.currentTimeMillis() - it.value > 60_000L }
        return nearestBlock(player, setOf(Blocks.chest, Blocks.trapped_chest), searchRadiusValue.get(), includeBelow = false) {
            it !in openedChests
        }
    }

    private fun nearestBlock(
        player: EntityPlayer,
        blocks: Set<Block>,
        radius: Int,
        includeBelow: Boolean,
        filter: (BlockPos) -> Boolean = { true }
    ): BlockPos? {
        val world = mc.theWorld as? WorldClient ?: return null
        val origin = BlockPos(player)
        val yRange = if (includeBelow) -8..8 else -3..5
        var best: BlockPos? = null
        var bestDistance = Double.MAX_VALUE

        for (x in -radius..radius) {
            for (y in yRange) {
                for (z in -radius..radius) {
                    val pos = origin.add(x, y, z)
                    val block = world.getBlockState(pos).block
                    if (block !in blocks || !filter(pos)) {
                        continue
                    }

                    val distance = player.getDistanceSq(pos)
                    if (distance < bestDistance) {
                        best = pos
                        bestDistance = distance
                    }
                }
            }
        }

        return best
    }

    private fun openChest(pos: BlockPos) {
        mc.playerController.onPlayerRightClick(
            mc.thePlayer,
            mc.theWorld,
            mc.thePlayer.heldItem,
            pos,
            EnumFacing.UP,
            Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        )
        mc.thePlayer.swingItem()
    }

    private fun mapCenterTarget(player: EntityPlayer): BlockPos {
        val y = player.posY.toInt().coerceAtLeast(1)
        val center = BlockPos(0, y, 0)
        if (player.getDistanceSq(center) <= centerRadiusValue.get() * centerRadiusValue.get()) {
            return BlockPos(player.posX, player.posY, player.posZ)
        }
        return center
    }

    private fun enableBridgeIfNeeded(player: EntityPlayer, target: BlockPos) {
        val horizontal = sqrt(player.getDistanceSq(target))
        ensureModule(FDPNext.moduleManager[Scaffold::class.java], horizontal > 8.0 && target.y >= player.posY - 2)
    }

    private fun hasEnoughBedWarsResources(): Boolean {
        return countItem(Items.iron_ingot) >= 24 || countItem(Items.gold_ingot) >= 8 || countItem(Items.diamond) > 0 || countItem(Items.emerald) > 0
    }

    private fun shouldKeepLootingSkyWars(): Boolean {
        return !hasBasicSkyWarsGear() || countBlocks() < 24 || countItem(Items.ender_pearl) == 0
    }

    private fun hasBasicSkyWarsGear(): Boolean {
        return mc.thePlayer.inventory.mainInventory.any { stack ->
            stack?.item is ItemSword || stack?.item is ItemBow
        } && mc.thePlayer.inventory.armorInventory.any { it != null }
    }

    private fun countBlocks(): Int {
        return mc.thePlayer.inventory.mainInventory.sumOf { stack ->
            if (stack?.item is net.minecraft.item.ItemBlock) stack.stackSize else 0
        }
    }

    private fun countItem(item: net.minecraft.item.Item): Int {
        return mc.thePlayer.inventory.mainInventory.sumOf { stack ->
            if (stack?.item == item) stack.stackSize else 0
        }
    }

    private fun equipBestBreaker() {
        val inventory = mc.thePlayer.inventory
        val slot = (0..8).firstOrNull { index ->
            val item = inventory.getStackInSlot(index)?.item
            item is ItemPickaxe || item is ItemAxe
        } ?: (0..8).firstOrNull { inventory.getStackInSlot(it)?.item is ItemSword } ?: return

        inventory.currentItem = slot
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

    private fun resolvedProvider(): String {
        return aiProviderValue.get()
    }

    private fun resolvedEndpoint(): String {
        val custom = aiEndpointValue.get().trim()
        if (custom.isNotEmpty()) {
            return custom
        }

        return when (aiProviderValue.get()) {
            "DeepSeek" -> "https://api.deepseek.com/chat/completions"
            "QwenDashScope" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    private fun resolvedModel(): String {
        val custom = aiModelValue.get().trim()
        if (custom.isNotEmpty()) {
            return custom
        }

        return when (aiProviderValue.get()) {
            "DeepSeek" -> "deepseek-v4-flash"
            "QwenDashScope" -> "qwen-vl-plus"
            else -> "gpt-4o-mini"
        }
    }

    private fun entityById(entityId: Int, maxDistance: Float): Entity? {
        val player = mc.thePlayer ?: return null
        return mc.theWorld?.loadedEntityList
            ?.asSequence()
            ?.filterIsInstance<Entity>()
            ?.firstOrNull { it.entityId == entityId && !it.isDead && player.getDistanceToEntity(it) <= maxDistance }
    }

    private fun isSafeTargetPos(player: EntityPlayer, pos: BlockPos, maxDistance: Double): Boolean {
        if (pos.y < 0 || pos.y > 256) {
            return false
        }
        return player.getDistanceSq(pos) <= maxDistance * maxDistance
    }

    private fun aiControllableModule(name: String): Module? {
        return when (name.lowercase(Locale.ROOT)) {
            "killaura" -> FDPNext.moduleManager[KillAura::class.java]
            "autoweapon" -> FDPNext.moduleManager[AutoWeapon::class.java]
            "scaffold" -> FDPNext.moduleManager[Scaffold::class.java]
            "stealer" -> FDPNext.moduleManager[Stealer::class.java]
            "invmanager" -> FDPNext.moduleManager[InvManager::class.java]
            "murderdetector" -> MurderDetector
            else -> null
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
