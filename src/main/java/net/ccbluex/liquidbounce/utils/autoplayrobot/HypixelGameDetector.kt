/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.autoplayrobot

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.hypixel.HypixelServerDetector
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.EnumChatFormatting

enum class AutoPlayRobotGame {
    NONE,
    HYPIXEL_MURDER,
    HYPIXEL_BEDWARS,
    HYPIXEL_SKYWARS
}

enum class AutoPlayRobotPhase {
    LOBBY,
    WAITING,
    PLAYING,
    DEAD,
    ENDED,
    UNKNOWN
}

object HypixelGameDetector : MinecraftInstance() {
    fun currentGame(): AutoPlayRobotGame {
        if (!HypixelServerDetector.isHypixel()) {
            return AutoPlayRobotGame.NONE
        }

        val title = sidebarTitle().lowercase()
        val lines = sidebarLines().joinToString(" ").lowercase()

        return when {
            title.contains("murder") || lines.contains("murder mystery") || lines.contains("innocents left") || lines.contains("detective") ->
                AutoPlayRobotGame.HYPIXEL_MURDER
            title.contains("bed wars") || title.contains("bedwars") || lines.contains("bed destroyed") || lines.contains("diamond") && lines.contains("emerald") ->
                AutoPlayRobotGame.HYPIXEL_BEDWARS
            title.contains("sky wars") || title.contains("skywars") || lines.contains("refill") || lines.contains("players left") ->
                AutoPlayRobotGame.HYPIXEL_SKYWARS
            else -> AutoPlayRobotGame.NONE
        }
    }

    fun phase(lastChatHint: String? = null): AutoPlayRobotPhase {
        val lines = sidebarLines().map { it.lowercase() }
        val chat = lastChatHint?.lowercase().orEmpty()

        if (chat.contains("you died") || chat.contains("you are now a spectator") || lines.any { it.contains("spectator") }) {
            return AutoPlayRobotPhase.DEAD
        }

        if (chat.contains("play again") || chat.contains("winner") || chat.contains("game over") || lines.any { it.contains("game ended") }) {
            return AutoPlayRobotPhase.ENDED
        }

        if (lines.any { it.contains("starting in") || it.contains("waiting") }) {
            return AutoPlayRobotPhase.WAITING
        }

        if (mc.theWorld != null && mc.thePlayer != null && currentGame() != AutoPlayRobotGame.NONE) {
            return AutoPlayRobotPhase.PLAYING
        }

        return AutoPlayRobotPhase.UNKNOWN
    }

    fun sidebarLines(): List<String> {
        val world = mc.theWorld ?: return emptyList()
        val scoreboard = world.scoreboard ?: return emptyList()
        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return emptyList()

        return scoreboard.getSortedScores(objective)
            .filter { it: Score -> it.objective == objective }
            .sortedBy { it: Score -> it.scorePoints }
            .map { score ->
                val team = scoreboard.getPlayersTeam(score.playerName)
                strip(ScorePlayerTeam.formatPlayerName(team, score.playerName))
            }
    }

    fun sidebarTitle(): String {
        val world = mc.theWorld ?: return ""
        val objective = world.scoreboard?.getObjectiveInDisplaySlot(1) ?: return ""
        return strip(objective.displayName)
    }

    private fun strip(text: String): String {
        return EnumChatFormatting.getTextWithoutFormattingCodes(text) ?: text
    }
}
