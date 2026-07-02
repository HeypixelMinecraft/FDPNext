/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.autoplayrobot

import net.minecraft.util.BlockPos

enum class AiDecisionAction {
    COLLECT_GOLD,
    COLLECT_RESOURCE,
    LOOT_CHEST,
    MOVE_TO,
    FIGHT_ENTITY,
    EVADE_ENTITY,
    ATTACK_BED,
    DEFEND_BED,
    SHOOT_MURDERER,
    AUTO_QUEUE,
    HOLD,
    LOOK_AT,
    ATTACK,
    USE_ITEM,
    OPEN_CHEST,
    BREAK_BLOCK,
    ENABLE_MODULE,
    DISABLE_MODULE,
    STOP
}

data class AiDecision(
    val action: AiDecisionAction,
    val entityId: Int? = null,
    val pos: BlockPos? = null,
    val module: String? = null,
    val reason: String = ""
)

data class AiDecisionResult(
    val decision: AiDecision?,
    val latencyMs: Long,
    val sentVision: Boolean,
    val error: String? = null
)
