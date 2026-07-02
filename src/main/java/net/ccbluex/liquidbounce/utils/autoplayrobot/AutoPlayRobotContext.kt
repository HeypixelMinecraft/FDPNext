/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.autoplayrobot

import net.minecraft.entity.Entity
import net.minecraft.util.BlockPos

enum class AutoPlayRobotTask {
    IDLE,
    WAITING_FOR_GAME,
    COLLECT_GOLD,
    EVADE_MURDERER,
    SHOOT_MURDERER,
    BW_COLLECT_RESOURCES,
    BW_DEFEND_BED,
    BW_ATTACK_BED,
    SW_LOOT_CHESTS,
    SW_MOVE_TO_CENTER,
    SW_FIGHT,
    AUTO_QUEUE,
    STOPPED
}

data class AutoPlayRobotContext(
    var game: AutoPlayRobotGame = AutoPlayRobotGame.NONE,
    var phase: AutoPlayRobotPhase = AutoPlayRobotPhase.UNKNOWN,
    var task: AutoPlayRobotTask = AutoPlayRobotTask.IDLE,
    var targetEntity: Entity? = null,
    var targetPos: BlockPos? = null,
    var lastChatHint: String? = null,
    var lastDecisionAt: Long = 0L,
    var lastPathChangeAt: Long = 0L,
    var lastPlayerX: Double = 0.0,
    var lastPlayerY: Double = 0.0,
    var lastPlayerZ: Double = 0.0,
    var stuckSince: Long = 0L,
    var detail: String = ""
)
