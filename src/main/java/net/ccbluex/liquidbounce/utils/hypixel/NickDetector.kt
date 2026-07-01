/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 */
package net.ccbluex.liquidbounce.utils.hypixel

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetworkPlayerInfo
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Detects Hypixel nicked players via UUID version and skin texture profileName.
 */
object NickDetector {

    /**
     * Returns true if the given profile belongs to a nicked player.
     * Hypixel nick UUIDs are typically version 1.
     */
    @JvmStatic
    fun isNicked(profile: GameProfile?): Boolean {
        if (profile == null) return false
        val uuid = profile.id ?: return false
        return uuid.version() == 1
    }

    /**
     * Tries to reveal the real name behind a nick by decoding the skin texture payload.
     * Returns null if not available.
     */
    @JvmStatic
    fun getRealName(profile: GameProfile?): String? {
        if (profile == null) return null
        val textures = profile.properties.get("textures") ?: return null
        val property = textures.firstOrNull() ?: return null

        return try {
            val decoded = String(Base64.getDecoder().decode(property.value), StandardCharsets.UTF_8)
            val json = JsonParser().parse(decoded).asJsonObject
            if (json.has("profileName")) json.get("profileName").asString else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Looks up a player's GameProfile by display name from the tab list.
     */
    @JvmStatic
    fun getProfile(name: String): GameProfile? {
        val playerInfoMap = Minecraft.getMinecraft().netHandler?.playerInfoMap ?: return null
        return playerInfoMap
            .firstOrNull { it.gameProfile.name.equals(name, ignoreCase = true) }
            ?.gameProfile
    }
}
