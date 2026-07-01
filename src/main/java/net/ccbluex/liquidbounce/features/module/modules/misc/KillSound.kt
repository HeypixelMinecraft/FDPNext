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
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.sound.TipSoundPlayer
import net.minecraft.entity.EntityLivingBase
import java.io.File

/**
 * Plays a custom sound when you kill a player
 * Skidded from LeaderClient
 */
object KillSound : Module(name = "KillSound", category = ModuleCategory.MISC, description = "击杀时播放自定义音效") {
    
    private val soundMode = ListValue(
        "Sound",
        arrayOf("Zako", "ZhangXueFeng", "FAHHHH", "Custom"),
        "Zako"
    )
    
    private var target: EntityLivingBase? = null
    private var played = false
    
    private val soundDir = File("assets/minecraft/FDPNext/sound/kill_snd")
    
    override fun onEnable() {
        reset()
        
        // 创建音效目录
        if (!soundDir.exists()) {
            soundDir.mkdirs()
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
            val soundName = soundMode.get()
            val soundFile = when (soundName) {
                "Zako" -> File(soundDir, "zako.wav")
                "ZhangXueFeng" -> File(soundDir, "zhangxuefeng.wav")
                "FAHHHH" -> File(soundDir, "fahhhh.wav")
                "Custom" -> File(soundDir, "custom.wav")
                else -> File(soundDir, "zako.wav")
            }
            
            if (soundFile.exists()) {
                TipSoundPlayer(soundFile).asyncPlay()
            } else {
                // 如果文件不存在，播放 Minecraft 默认音效
                mc.thePlayer?.playSound("random.orb", 1.0f, 1.0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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