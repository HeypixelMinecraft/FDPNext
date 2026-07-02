/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * Skidded from LeaderClient KillSound
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ResourceLocation

/**
 * Plays a bundled resource sound when you kill a player.
 */
object KillSound : Module(
    name = "KillSound",
    category = ModuleCategory.MISC,
    description = "Plays a bundled sound after a kill."
) {

    private val soundMode = ListValue(
        "Sound",
        arrayOf("Zako", "ZhangXueFeng", "FAHHHH", "Custom"),
        "Zako"
    )
    private val volumeValue = FloatValue("Volume", 2.5F, 0.1F, 5F)
    private val testOnEnableValue = BoolValue("TestOnEnable", false)

    private var target: EntityLivingBase? = null
    private var played = false

    override fun onEnable() {
        reset()
        if (testOnEnableValue.get()) {
            playKillSound()
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (!state) return

        val entity = event.targetEntity
        if (entity is EntityLivingBase) {
            target = entity
            played = false
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!state || target == null || played) return

        val targetEntity = target!!
        if (targetEntity.isDead || targetEntity.health <= 0.0f) {
            playKillSound()
            played = true
            target = null
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        reset()
    }

    override fun onDisable() {
        reset()
    }

    private fun playKillSound() {
        try {
            val soundEvent = when (soundMode.get()) {
                "Zako" -> "fdpnext:kill.zako"
                "ZhangXueFeng" -> "fdpnext:kill.zhangxuefeng"
                "FAHHHH" -> "fdpnext:kill.fahhhh"
                "Custom" -> "fdpnext:kill.zako"
                else -> "fdpnext:kill.zako"
            }

            val player = mc.thePlayer
            mc.soundHandler.playSound(
                PositionedSoundRecord(
                    ResourceLocation(soundEvent),
                    volumeValue.get(),
                    1.0F,
                    player?.posX?.toFloat() ?: 0F,
                    player?.posY?.toFloat() ?: 0F,
                    player?.posZ?.toFloat() ?: 0F
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            mc.thePlayer?.playSound("random.orb", 1.0F, 1.0F)
        }
    }

    private fun reset() {
        target = null
        played = false
    }

    override fun handleEvents(): Boolean {
        return state
    }
}
