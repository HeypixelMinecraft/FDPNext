/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.watut

import net.minecraft.util.ResourceLocation
import java.util.UUID

enum class WatutActivity {
    ACTIVE,
    TYPING,
    IN_GUI,
    IDLE
}

enum class WatutGuiState {
    NONE,
    CHAT,
    INVENTORY,
    CHEST,
    CRAFTING,
    PAUSE,
    SIGN,
    BOOK,
    ANVIL,
    ENCHANTING,
    FURNACE,
    DISPENSER,
    HOPPER,
    BEACON,
    MERCHANT,
    OTHER
}

enum class WatutPacketType {
    STATUS,
    SCREEN_CHUNK,
    SCREEN_CLEAR,
    PARTICLE_EVENT
}

enum class WatutPreviewQuality(val width: Int, val height: Int) {
    Low(96, 54),
    Medium(128, 72),
    High(160, 90);

    companion object {
        fun byName(name: String) = values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Medium
    }
}

data class WatutStatus(
    val uuid: UUID,
    var activity: WatutActivity = WatutActivity.ACTIVE,
    var guiState: WatutGuiState = WatutGuiState.NONE,
    var mouseX: Float = 0.5F,
    var mouseY: Float = 0.5F,
    var mousePressed: Boolean = false,
    var typingAmplifier: Float = 0F,
    var idleTicks: Int = 0,
    var updatedAt: Long = System.currentTimeMillis(),
    var screen: WatutScreenTexture? = null,
    var poseBlend: Float = 0F
) {
    val visible: Boolean
        get() = activity != WatutActivity.ACTIVE || guiState != WatutGuiState.NONE || screen != null
}

data class WatutScreenTexture(
    val frameId: Int,
    val width: Int,
    val height: Int,
    val location: ResourceLocation,
    val updatedAt: Long = System.currentTimeMillis()
)

data class WatutScreenFrame(
    val frameId: Int,
    val width: Int,
    val height: Int,
    val rgb: ByteArray
)
