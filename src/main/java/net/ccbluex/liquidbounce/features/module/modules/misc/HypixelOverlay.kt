/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 *
 * Skidded from Meowtils Hypixel overlay.
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.utils.hypixel.HypixelApiClient
import net.ccbluex.liquidbounce.utils.hypixel.HypixelServerDetector
import net.ccbluex.liquidbounce.utils.hypixel.HypixelStats
import net.ccbluex.liquidbounce.utils.hypixel.HypixelStatsCache
import net.ccbluex.liquidbounce.utils.hypixel.NickDetector
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.util.EnumChatFormatting
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Hypixel overlay: tablist BW/SW stats, nick detection and auto daily rewards.
 */
object HypixelOverlay : Module(
    name = "HypixelOverlay",
    category = ModuleCategory.MISC,
    description = "Hypixel tablist BW/SW stats, nick detection and auto tip."
) {

    val apiKey = TextValue("API-Key", "")
    val cacheMinutes = IntegerValue("CacheMinutes", 5, 1, 60)

    val tablist = BoolValue("Tablist", true)
    val nickDetection = BoolValue("NickDetection", true)
    val showRealName = BoolValue("ShowRealName", true)
    val autoTip = BoolValue("AutoTip", false)
    val tipDelay = IntegerValue("TipDelayMinutes", 5, 1, 10).displayable { autoTip.get() }
    val hideTipMessages = BoolValue("HideTipMessages", true).displayable { autoTip.get() }

    val displayMode = ListValue("DisplayMode", arrayOf("Compact", "Full", "Lowercase"), "Compact")

    val bwEnabled = BoolValue("BW-Enabled", true)
    val bwLevel = BoolValue("BW-Level", true)
    val bwFinals = BoolValue("BW-Finals", false)
    val bwFkdr = BoolValue("BW-FKDR", true)
    val bwWlr = BoolValue("BW-WLR", true)
    val bwWs = BoolValue("BW-WS", true)
    val bwCr = BoolValue("BW-CR", false)

    val swEnabled = BoolValue("SW-Enabled", true)
    val swLevel = BoolValue("SW-Level", true)
    val swKills = BoolValue("SW-Kills", false)
    val swKdr = BoolValue("SW-KDR", true)
    val swWins = BoolValue("SW-Wins", false)
    val swWlr = BoolValue("SW-WLR", true)

    private val fetchExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FDPNext-HypixelOverlay-Fetch").apply { isDaemon = true }
    }
    private val pendingFetches = ConcurrentHashMap.newKeySet<String>()

    private var tickCounter = 0
    private var lastTipTime = 0L

    override fun onEnable() {
        tickCounter = 0
        lastTipTime = 0L
    }

    override fun onDisable() {
        pendingFetches.clear()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!state || mc.thePlayer == null || mc.theWorld == null) return
        if (!HypixelServerDetector.isHypixel()) return

        if (autoTip.get() && ++tickCounter >= 200) {
            tickCounter = 0
            val current = System.currentTimeMillis()
            val delayMs = tipDelay.get() * 60_000L
            if (current - lastTipTime >= delayMs) {
                mc.thePlayer.sendChatMessage("/tip all")
                lastTipTime = current
            }
        }
    }

    /**
     * Called from the Tab overlay Mixin. Returns the full formatted player name.
     */
    @JvmStatic
    fun formatTabName(info: NetworkPlayerInfo, original: String): String {
        if (!state || !HypixelServerDetector.isHypixel()) return original

        val profile = info.gameProfile ?: return original
        val uuid = profile.id?.toString() ?: return original
        val name = profile.name

        val prefix = StringBuilder()
        val suffix = StringBuilder()

        if (tablist.get()) {
            val stats = getStats(name, uuid)
            if (stats != null) {
                prefix.append(buildPrefix(stats))
                suffix.append(buildSuffix(stats))
            }
        }

        if (nickDetection.get() && NickDetector.isNicked(profile)) {
            suffix.append(EnumChatFormatting.DARK_PURPLE).append(EnumChatFormatting.BOLD).append(" [NICK]")
            if (showRealName.get()) {
                val realName = NickDetector.getRealName(profile)
                if (!realName.isNullOrBlank()) {
                    suffix.append(EnumChatFormatting.GRAY).append(" (").append(realName).append(")")
                }
            }
        }

        return "$prefix$original$suffix"
    }

    /**
     * Hides AutoTip response messages when enabled.
     */
    @JvmStatic
    fun shouldHideTipMessage(message: String): Boolean {
        if (!state || !autoTip.get() || !hideTipMessages.get()) return false
        val lower = message.lowercase()
        return lower.contains("you tipped") ||
            lower.contains("you already tipped everyone") ||
            lower.contains("no one has a network booster active right now")
    }

    @JvmStatic
    fun getStats(name: String, uuid: String): HypixelStats? {
        if (!state) return null

        val cacheKey = uuid.replace("-", "").lowercase()
        val cached = HypixelStatsCache.getValid(cacheKey, cacheMinutes.get())
        if (cached != null) return cached
        if (!HypixelStatsCache.canRetry(cacheKey)) return null
        if (!pendingFetches.add(cacheKey)) return null

        fetchExecutor.execute {
            try {
                val stats = HypixelApiClient.fetchPlayer(name, uuid, apiKey.get())
                if (stats != null) {
                    HypixelStatsCache.put(cacheKey, stats)
                } else {
                    HypixelStatsCache.markFailed(cacheKey)
                }
            } finally {
                pendingFetches.remove(cacheKey)
            }
        }

        return null
    }

    private fun buildPrefix(stats: HypixelStats): String {
        val builder = StringBuilder()

        if (HypixelServerDetector.isBedwars() && bwEnabled.get() && bwLevel.get()) {
            val level = stats.bedwars?.level ?: 0
            builder.append("\u00A77[").append(formatBwLevel(level)).append("\u00A77] ")
        } else if (HypixelServerDetector.isSkywars() && swEnabled.get() && swLevel.get()) {
            val level = stats.skywars?.levelFormatted ?: ""
            builder.append(level).append(" ")
        }

        return builder.toString()
    }

    private fun buildSuffix(stats: HypixelStats): String {
        val parts = mutableListOf<String>()

        if (HypixelServerDetector.isBedwars() && bwEnabled.get()) {
            val bw = stats.bedwars
            if (bw != null) {
                if (bwFinals.get()) parts.add(format("Finals", bw.finalKills))
                if (bwFkdr.get()) parts.add(format("FKDR", bw.fkdr))
                if (bwWlr.get()) parts.add(format("WLR", bw.wlr))
                if (bwWs.get()) parts.add(format("WS", bw.winstreak))
                if (bwCr.get()) parts.add(format("CR", bw.clutchRatio))
            }
        } else if (HypixelServerDetector.isSkywars() && swEnabled.get()) {
            val sw = stats.skywars
            if (sw != null) {
                if (swKills.get()) parts.add(format("Kills", sw.kills))
                if (swKdr.get()) parts.add(format("KDR", sw.kdr))
                if (swWins.get()) parts.add(format("Wins", sw.wins))
                if (swWlr.get()) parts.add(format("WLR", sw.wlr))
            }
        }

        if (parts.isEmpty()) return ""
        return "\u00A78 \u25B8 " + parts.joinToString("\u00A78 | ")
    }

    private fun format(label: String, value: Any): String {
        val mode = displayMode.get()
        val text = when (value) {
            is Double -> String.format("%.2f", value)
            is Float -> String.format("%.2f", value)
            else -> value.toString()
        }

        return when (mode) {
            "Compact" -> "\u00A7f$text"
            "Full" -> "\u00A7b$label: \u00A7f$text"
            "Lowercase" -> "\u00A7b${label.lowercase()}: \u00A7f$text"
            else -> "\u00A7f$text"
        }
    }

    private fun formatBwLevel(level: Int): String {
        val symbol = when {
            level >= 3200 -> "\u272A"
            level >= 2200 -> "\u2726"
            else -> "\u272B"
        }
        return "\u00A7e$level$symbol"
    }
}
