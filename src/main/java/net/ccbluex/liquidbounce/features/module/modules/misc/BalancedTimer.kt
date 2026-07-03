/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color

/**
 * Skidded from SilenceFix BalancedTimer
 * Time management module for storing and releasing ticks
 */
object BalancedTimer : Module("BalancedTimer", category = ModuleCategory.MISC) {

    private val releaseSpeed = IntegerValue("ReleaseSpeed", 5, 1, 20)
    private val storeKey = IntegerValue("StoreKey", Keyboard.KEY_LCONTROL, 0, 256)
    private val releaseKey = IntegerValue("ReleaseKey", Keyboard.KEY_LMENU, 0, 256)

    var balance = 0
    var stage = Stage.IDLE

    private var yAnimation = 0f
    private var xAnimation = 0f

    override fun onEnable() {
        balance = 0
        stage = Stage.IDLE
        mc.timer.timerSpeed = 1f
    }

    override fun onDisable() {
        balance = 0
        stage = Stage.IDLE
        mc.timer.timerSpeed = 1f
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        balance = 0
        stage = Stage.IDLE
        mc.timer.timerSpeed = 1f
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val player = mc.thePlayer
        if (player == null) {
            mc.timer.timerSpeed = 1f
            return
        }

        // Update stage based on key presses
        val isStoring = Keyboard.isKeyDown(storeKey.get()) || Mouse.isButtonDown(3)
        val isReleasing = Keyboard.isKeyDown(releaseKey.get()) || Mouse.isButtonDown(4)

        if (isStoring) {
            stage = Stage.STORE
            mc.timer.timerSpeed = 0.01f // Very slow to "store" ticks
            balance++
        } else if (isReleasing && balance > 0) {
            stage = Stage.RELEASE
            mc.timer.timerSpeed = releaseSpeed.get().toFloat() // Speed up to release stored ticks
            balance--
        } else {
            stage = Stage.IDLE
            mc.timer.timerSpeed = 1f
        }

        // Draw HUD indicator
        drawIndicator(event.partialTicks, event.scaledResolution)
    }

    private fun drawIndicator(partialTicks: Float, sr: ScaledResolution) {
        if (stage == Stage.IDLE && balance == 0) return

        val x = sr.scaledWidth / 2 - 50
        val y = sr.scaledHeight / 2 + 20

        // Animate position
        if (stage != Stage.IDLE || balance > 0) {
            yAnimation = RenderUtils.getAnimationState(yAnimation.toDouble(), 0.0, 0.5).toFloat()
        } else {
            yAnimation = RenderUtils.getAnimationState(yAnimation.toDouble(), -30.0, 0.5).toFloat()
        }

        // Draw background
        val backgroundColor = Color(0, 0, 0, 120)
        RenderUtils.drawRoundedCornerRect(x, y + yAnimation, x + 100, y + yAnimation + 30, 5f, backgroundColor.rgb)

        // Draw stage text
        val stageColor = when (stage) {
            Stage.STORE -> Color(255, 100, 100)
            Stage.RELEASE -> Color(100, 255, 100)
            Stage.IDLE -> Color(200, 200, 200)
        }
        Fonts.fontSFUI35.drawStringWithShadow(stage.display, x + 5, y + yAnimation + 5, stageColor.rgb)

        // Draw balance count
        Fonts.fontSFUI35.drawStringWithShadow("Balance: $balance", x + 5, y + yAnimation + 15, Color.WHITE.rgb)
    }

    enum class Stage(val display: String) {
        STORE("Saving"),
        IDLE("Idle"),
        RELEASE("Releasing")
    }
}