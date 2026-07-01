/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 */
package net.ccbluex.liquidbounce.utils.hypixel

import net.minecraft.client.Minecraft
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.scoreboard.Scoreboard

/**
 * Detects whether the player is currently on Hypixel, using both server IP
 * and scoreboard content. This works even when connecting through acceleration/VPN IPs.
 */
object HypixelServerDetector {

    private val HYPIXEL_IP_PATTERNS = listOf(
        "hypixel.net"
    )

    private val SCOREBOARD_TITLE_PATTERNS = listOf("hypixel", "bed wars", "bedwars", "sky wars", "skywars")
    private val SCOREBOARD_LINE_PATTERNS = listOf("hypixel.net", "www.hypixel.net", "hypixel")

    /**
     * Returns true if the current server is Hypixel.
     * Checks server IP first; if it does not match (accelerator/VPN), falls back to scoreboard.
     */
    @JvmStatic
    fun isHypixel(): Boolean {
        return isHypixelIp() || isHypixelScoreboard()
    }

    @JvmStatic
    fun isHypixelIp(): Boolean {
        val serverData = Minecraft.getMinecraft().currentServerData ?: return false
        val ip = serverData.serverIP.lowercase()
        return HYPIXEL_IP_PATTERNS.any { ip.contains(it) }
    }

    @JvmStatic
    fun isHypixelScoreboard(): Boolean {
        val mc = Minecraft.getMinecraft()
        val world = mc.theWorld ?: return false
        val scoreboard = world.scoreboard ?: return false
        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return false

        val title = stripFormatting(objective.displayName).lowercase()
        if (SCOREBOARD_TITLE_PATTERNS.any { title.contains(it) }) {
            return true
        }

        return getSidebarLines(scoreboard, objective).any { line ->
            SCOREBOARD_LINE_PATTERNS.any { line.contains(it) }
        }
    }

    @JvmStatic
    fun isBedwars(): Boolean {
        val title = getSidebarTitle()?.lowercase() ?: return false
        return title.contains("bed wars") || title.contains("bedwars")
    }

    @JvmStatic
    fun isSkywars(): Boolean {
        val title = getSidebarTitle()?.lowercase() ?: return false
        return title.contains("sky wars") || title.contains("skywars")
    }

    private fun getSidebarTitle(): String? {
        val mc = Minecraft.getMinecraft()
        val world = mc.theWorld ?: return null
        val scoreboard = world.scoreboard ?: return null
        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return null
        return stripFormatting(objective.displayName)
    }

    private fun getSidebarLines(scoreboard: Scoreboard, objective: ScoreObjective): List<String> {
        val scores = scoreboard.getSortedScores(objective)
            .filter { it: Score -> it.objective == objective }
            .sortedBy { it: Score -> it.scorePoints }

        return scores.map { score: Score ->
            val team = scoreboard.getPlayersTeam(score.playerName)
            stripFormatting(ScorePlayerTeam.formatPlayerName(team, score.playerName))
        }
    }

    private fun stripFormatting(text: String): String {
        return net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(text) ?: text
    }
}
