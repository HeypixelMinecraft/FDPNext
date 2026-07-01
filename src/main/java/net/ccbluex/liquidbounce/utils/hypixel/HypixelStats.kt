/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 */
package net.ccbluex.liquidbounce.utils.hypixel

/**
 * Aggregate Hypixel player stats fetched from api.hypixel.net.
 */
class HypixelStats {

    var fetchedAt: Long = 0
        private set

    var general: GeneralStats? = null
    var bedwars: BedwarsStats? = null
    var skywars: SkywarsStats? = null

    fun updateGeneral(stats: GeneralStats) {
        general = stats
        fetchedAt = System.currentTimeMillis()
    }

    fun updateBedwars(stats: BedwarsStats) {
        bedwars = stats
        fetchedAt = System.currentTimeMillis()
    }

    fun updateSkywars(stats: SkywarsStats) {
        skywars = stats
        fetchedAt = System.currentTimeMillis()
    }

    fun isExpired(ttlMinutes: Int): Boolean {
        return System.currentTimeMillis() - fetchedAt > ttlMinutes * 60_000L
    }

    data class GeneralStats(
        val rank: String,
        val firstLogin: Long,
        val lastLogin: Long,
        val lastLogout: Long,
        val lastClaimedReward: Long,
        val karma: Int,
        val achievementPoints: Int
    )

    data class BedwarsStats(
        val level: Int,
        val finalKills: Int,
        val finalDeaths: Int,
        val fkdr: Double,
        val wins: Int,
        val losses: Int,
        val wlr: Double,
        val winstreak: Int,
        val bedsBroken: Int,
        val bedsLost: Int,
        val clutchRatio: Double
    )

    data class SkywarsStats(
        val levelFormatted: String,
        val kills: Int,
        val deaths: Int,
        val kdr: Double,
        val wins: Int,
        val losses: Int,
        val wlr: Double
    )
}
