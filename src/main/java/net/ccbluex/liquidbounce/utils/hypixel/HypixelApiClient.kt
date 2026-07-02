/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 */
package net.ccbluex.liquidbounce.utils.hypixel

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Client for the public Hypixel API (api.hypixel.net).
 */
object HypixelApiClient {

    private const val USER_AGENT = "Mozilla/5.0"
    private const val TIMEOUT_MS = 10_000
    private var lastMissingKeyWarn = 0L

    @JvmStatic
    fun fetchPlayer(name: String, uuid: String, apiKey: String): HypixelStats? {
        val cleanKey = apiKey.replace(" ", "")
        if (cleanKey.isEmpty()) {
            warnMissingKey()
            return null
        }

        var connection: HttpURLConnection? = null
        return try {
            val cleanUuid = uuid.replace("-", "")
            val url = URL("https://api.hypixel.net/v2/player?uuid=$cleanUuid")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("API-Key", cleanKey)
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val code = connection.responseCode
            if (code != 200) {
                logHttpError(code)
                return null
            }

            val response = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val json = JsonParser().parse(response).asJsonObject
            if (!json.has("success") || !json.get("success").asBoolean) {
                val cause = if (json.has("cause")) json.get("cause").asString else "Unknown"
                ClientUtils.displayChatMessage("\u00A7c[HypixelOverlay] API error: $cause")
                return null
            }

            if (!json.has("player") || json.get("player").isJsonNull) {
                ClientUtils.displayChatMessage("\u00A7c[HypixelOverlay] Player not found: $name")
                return null
            }

            parsePlayer(json.getAsJsonObject("player"))
        } catch (e: Exception) {
            ClientUtils.displayChatMessage("\u00A7c[HypixelOverlay] Failed to fetch stats: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parsePlayer(player: JsonObject): HypixelStats {
        val stats = HypixelStats()

        val achievements = getObject(player, "achievements")
        val bw = getObject(getObject(player, "stats"), "Bedwars")
        val sw = getObject(getObject(player, "stats"), "SkyWars")

        val general = HypixelStats.GeneralStats(
            rank = getString(player, "rank", ""),
            firstLogin = getLong(player, "firstLogin", 0L),
            lastLogin = getLong(player, "lastLogin", 0L),
            lastLogout = getLong(player, "lastLogout", 0L),
            lastClaimedReward = getLong(player, "lastClaimedReward", 0L),
            karma = getInt(player, "karma", 0),
            achievementPoints = getInt(player, "achievementPoints", 0)
        )

        val finalKills = getInt(bw, "final_kills_bedwars", 0)
        val finalDeaths = getInt(bw, "final_deaths_bedwars", 1).coerceAtLeast(1)
        val wins = getInt(bw, "wins_bedwars", 0)
        val losses = getInt(bw, "losses_bedwars", 1).coerceAtLeast(1)
        val bedsLost = getInt(bw, "beds_lost_bedwars", 0)

        val bedwars = HypixelStats.BedwarsStats(
            level = getInt(achievements, "bedwars_level", 0),
            finalKills = finalKills,
            finalDeaths = finalDeaths,
            fkdr = finalKills / finalDeaths.toDouble(),
            wins = wins,
            losses = losses,
            wlr = wins / losses.toDouble(),
            winstreak = getInt(bw, "winstreak", 0),
            bedsBroken = getInt(bw, "beds_broken_bedwars", 0),
            bedsLost = bedsLost,
            clutchRatio = if (bedsLost == 0) 0.0 else 1.0 - finalDeaths / bedsLost.toDouble()
        )

        val swKills = getInt(sw, "kills", 0)
        val swDeaths = getInt(sw, "deaths", 1).coerceAtLeast(1)
        val swWins = getInt(sw, "wins", 0)
        val swLosses = getInt(sw, "losses", 1).coerceAtLeast(1)

        val skywars = HypixelStats.SkywarsStats(
            levelFormatted = getString(sw, "levelFormatted", "0"),
            kills = swKills,
            deaths = swDeaths,
            kdr = swKills / swDeaths.toDouble(),
            wins = swWins,
            losses = swLosses,
            wlr = swWins / swLosses.toDouble()
        )

        stats.updateGeneral(general)
        stats.updateBedwars(bedwars)
        stats.updateSkywars(skywars)
        return stats
    }

    private fun logHttpError(code: Int) {
        val message = when (code) {
            400 -> "Bad request (missing data)"
            403 -> "Forbidden (invalid API key?)"
            429 -> "Rate limited"
            else -> "HTTP $code"
        }
        ClientUtils.displayChatMessage("\u00A7c[HypixelOverlay] $message")
    }

    private fun warnMissingKey() {
        val now = System.currentTimeMillis()
        if (now - lastMissingKeyWarn > 10_000L) {
            ClientUtils.displayChatMessage("\u00A7c[HypixelOverlay] API key is missing. Set it in the module settings.")
            lastMissingKeyWarn = now
        }
    }

    private fun getObject(obj: JsonObject?, key: String): JsonObject {
        return obj?.getAsJsonObject(key) ?: JsonObject()
    }

    private fun getString(obj: JsonObject, key: String, fallback: String): String {
        return if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asString else fallback
    }

    private fun getInt(obj: JsonObject, key: String, fallback: Int): Int {
        return if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asInt else fallback
    }

    private fun getLong(obj: JsonObject, key: String, fallback: Long): Long {
        return if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asLong else fallback
    }
}
