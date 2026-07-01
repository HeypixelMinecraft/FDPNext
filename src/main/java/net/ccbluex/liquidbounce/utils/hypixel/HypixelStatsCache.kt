/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 */
package net.ccbluex.liquidbounce.utils.hypixel

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cache for Hypixel player statistics.
 */
object HypixelStatsCache {

    private val cache = ConcurrentHashMap<String, HypixelStats>()
    private val failed = ConcurrentHashMap<String, Long>()

    private const val RETRY_DELAY_MS = 5 * 60_000L // 5 minutes

    @JvmStatic
    fun getValid(name: String, ttlMinutes: Int): HypixelStats? {
        val stats = cache[name.lowercase()] ?: return null
        if (stats.isExpired(ttlMinutes)) {
            cache.remove(name.lowercase())
            return null
        }
        return stats
    }

    @JvmStatic
    fun canRetry(name: String): Boolean {
        val last = failed[name.lowercase()] ?: return true
        if (System.currentTimeMillis() - last > RETRY_DELAY_MS) {
            failed.remove(name.lowercase())
            return true
        }
        return false
    }

    @JvmStatic
    fun put(name: String, stats: HypixelStats) {
        cache[name.lowercase()] = stats
    }

    @JvmStatic
    fun markFailed(name: String) {
        failed[name.lowercase()] = System.currentTimeMillis()
    }

    @JvmStatic
    fun clear() {
        cache.clear()
        failed.clear()
    }
}
