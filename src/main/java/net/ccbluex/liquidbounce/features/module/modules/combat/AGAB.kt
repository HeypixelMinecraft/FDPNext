package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.GLUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.item.ItemAppleGold
import net.minecraft.item.ItemAxe
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

object AGAB : Module("AGAB", category = ModuleCategory.COMBAT, defaultOn = false) {

    private val c03CountValue = IntegerValue("C03PacketPlayer", 32, 32, 40, "")
    private val triggerHealthValue = FloatValue("TriggerHealth", 12f, 1f, 18f)
    private val autoEatValue = BoolValue("AutoGapple", true)
    private val armorBreakerValue = BoolValue("ArmorBreaker", true)

    private val progressBarWidthValue = IntegerValue("ProgressBar Width", 180, 100, 300, "")
    private val progressBarHeightValue = IntegerValue("ProgressBar Height", 8, 4, 20, "")
    private val startColorRedValue = IntegerValue("Start Color Red", 255, 0, 255, "")
    private val startColorGreenValue = IntegerValue("Start Color Green", 80, 0, 255, "")
    private val startColorBlueValue = IntegerValue("Start Color Blue", 30, 0, 255, "")
    private val endColorRedValue = IntegerValue("End Color Red", 255, 0, 255, "")
    private val endColorGreenValue = IntegerValue("End Color Green", 255, 0, 255, "")
    private val endColorBlueValue = IntegerValue("End Color Blue", 50, 0, 255, "")
    private val showPercentageValue = BoolValue("Show Percentage", true)
    private val randomGradientValue = BoolValue("Random Gradient", true)
    private val gradientSpeedValue = IntegerValue("Gradient Speed", 3, 1, 10, "")

    private var gappleX = 0.0
    private var gappleY = 0.0
    private var gappleZ = 0.0
    private var gappleCancelMove = false
    private var gappleR = false
    private var gappleTicks = 0
    private var gapplePauseTicks = 0
    private var gappleYaw = 0f
    private var gapplePitch = 0f
    private var gappleShouldEat = false
    private var gappleLastHealth = 19.5f
    private var gappleHealthBeforeEat = 0.0
    private var gappleGappleCount = 0
    private var gappleLastGappleCheck = 0
    private var gappleWaitingForEffect = false
    private var gappleLastEatTick = 0

    private var armorS = false
    private var armorTicks = 0
    private var armorPress = false
    private var armorBreakerWork = true
    private var armorAxeSlot = -1

    private var progressBarVisible = false
    private var progressBarProgress = 0.0
    private var progressBarTargetProgress = 0.0
    private var progressBarText = "Eating..."

    private var gradientTime = 0
    private val currentGradientColors = ArrayList<IntArray>()
    private val targetGradientColors = ArrayList<IntArray>()

    override val tag: String
        get() = "P:${gappleTicks}/${c03CountValue.get()} | G:${if (gappleShouldEat) "ON" else "OFF"} | B:${if (armorBreakerWork) "ON" else "OFF"} | H:${mc.thePlayer?.health?.toInt() ?: 0}"

    private fun generateRandomColor(): IntArray {
        val hue = Random.nextFloat()
        val saturation = 0.7f + Random.nextFloat() * 0.3f
        val brightness = 0.8f + Random.nextFloat() * 0.2f
        return hslToRgb(hue, saturation, brightness)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
        if (s == 0f) {
            val v = (l * 255).toInt()
            return intArrayOf(v, v, v)
        }
        val hue2rgb: (Float, Float, Float) -> Float = { p, q, t ->
            var tt = t
            if (tt < 0f) tt += 1f
            if (tt > 1f) tt -= 1f
            when {
                tt < 1f / 6f -> p + (q - p) * 6f * tt
                tt < 1f / 2f -> q
                tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
                else -> p
            }
        }
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        val r = hue2rgb(p, q, h + 1f / 3f)
        val g = hue2rgb(p, q, h)
        val b = hue2rgb(p, q, h - 1f / 3f)
        return intArrayOf((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    private fun initGradientColors() {
        currentGradientColors.clear()
        targetGradientColors.clear()
        for (i in 0 until 6) {
            currentGradientColors.add(generateRandomColor())
            targetGradientColors.add(generateRandomColor())
        }
    }

    private fun updateGradientColors() {
        gradientTime++
        if (gradientTime % gradientSpeedValue.get() != 0) return
        for (i in currentGradientColors.indices) {
            for (j in 0..2) {
                currentGradientColors[i][j] += ((targetGradientColors[i][j] - currentGradientColors[i][j]) * 0.1).toInt()
                if (abs((currentGradientColors[i][j] - targetGradientColors[i][j]).toDouble()) < 1.0) {
                    targetGradientColors[i] = generateRandomColor()
                }
            }
        }
    }

    private fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        val r = (r1 + (r2 - r1) * ratio).toInt()
        val g = (g1 + (g2 - g1) * ratio).toInt()
        val b = (b1 + (b2 - b1) * ratio).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun getColorFromSettings(isStart: Boolean): Int {
        val r = if (isStart) startColorRedValue.get() else endColorRedValue.get()
        val g = if (isStart) startColorGreenValue.get() else endColorGreenValue.get()
        val b = if (isStart) startColorBlueValue.get() else endColorBlueValue.get()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun drawRectGradient(x: Float, y: Float, width: Int, height: Int, startColor: Int, endColor: Int) {
        for (i in 0 until width) {
            val ratio = i.toFloat() / (width - 1).coerceAtLeast(1)
            val color = interpolateColor(startColor, endColor, ratio)
            GLUtils.drawRect(x + i, y, x + i + 1f, y + height, color)
        }
    }

    private fun drawRectRandomGradient(x: Float, y: Float, width: Int, height: Int) {
        if (width <= 0) return
        val segmentCount = currentGradientColors.size
        val segmentWidth = width.toFloat() / (segmentCount - 1)
        for (seg in 0 until segmentCount - 1) {
            var startX = x + seg * segmentWidth
            var endX = x + (seg + 1) * segmentWidth
            if (startX >= x + width) break
            if (endX > x + width) endX = x + width
            val segWidth = (endX - startX).toInt()
            for (i in 0 until segWidth) {
                val pixelX = startX + i
                val ratio = i.toFloat() / segWidth.coerceAtLeast(1)
                val c1 = currentGradientColors[seg]
                val c2 = currentGradientColors[seg + 1]
                val r = (c1[0] + (c2[0] - c1[0]) * ratio).toInt()
                val g = (c1[1] + (c2[1] - c1[1]) * ratio).toInt()
                val b = (c1[2] + (c2[2] - c1[2]) * ratio).toInt()
                val color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                GLUtils.drawRect(pixelX, y, pixelX + 1f, y + height, color)
            }
        }
    }

    private fun gappleStuck() {
        if (!gappleR) {
            gappleX = mc.thePlayer!!.motionX
            gappleY = mc.thePlayer!!.motionY
            gappleZ = mc.thePlayer!!.motionZ
            gappleR = true
        }
        gappleCancelMove = true
    }

    private fun gappleStopstuck() {
        gappleCancelMove = false
        if (gappleR) {
            mc.thePlayer!!.motionX = gappleX
            mc.thePlayer!!.motionY = gappleY
            mc.thePlayer!!.motionZ = gappleZ
            gappleR = false
        }
    }

    private fun gappleRelease() {
        mc.netHandler!!.addToSendQueue(C03PacketPlayer.C05PacketPlayerLook(gappleYaw, gapplePitch, mc.thePlayer!!.onGround))
        for (i in 1 until gappleTicks) {
            mc.netHandler!!.addToSendQueue(C03PacketPlayer(mc.thePlayer!!.onGround))
        }
    }

    private fun gappleGetGApple(): Int {
        for (i in 0 until 9) {
            val stack = mc.thePlayer!!.inventory.getStackInSlot(i)
            if (stack == null) continue
            if (stack.item is ItemAppleGold) return i
        }
        return -1
    }

    private fun gappleGetTotalGapples(): Int {
        var count = 0
        try {
            for (i in 0 until 36) {
                val stack = mc.thePlayer!!.inventory.getStackInSlot(i)
                if (stack != null && stack.item is ItemAppleGold) {
                    count += stack.stackSize
                }
            }
        } catch (_: Exception) {}
        return count
    }

    private fun gappleForceUpdateGappleCount(): Boolean {
        val newCount = gappleGetTotalGapples()
        return if (newCount != gappleGappleCount) {
            gappleGappleCount = newCount
            true
        } else false
    }

    private fun gappleCheckHealthCondition(): Boolean {
        if (!autoEatValue.get()) {
            gappleShouldEat = false
            progressBarVisible = false
            armorBreakerWork = armorBreakerValue.get()
            return false
        }
        val currentHealth = mc.thePlayer!!.health
        val maxHealth = mc.thePlayer!!.maxHealth
        if (currentHealth <= triggerHealthValue.get()) {
            if (!gappleShouldEat) {
                ClientUtils.displayChatMessage("§aHealth below ${triggerHealthValue.get()}, eating...")
                gappleHealthBeforeEat = currentHealth.toDouble()
            }
            gappleShouldEat = true
        }
        if (currentHealth >= maxHealth && gappleShouldEat) {
            ClientUtils.displayChatMessage("§aHealth full, stopping")
            gappleShouldEat = false
            gappleWaitingForEffect = false
            progressBarVisible = false
            armorBreakerWork = armorBreakerValue.get()
        }
        gappleLastHealth = currentHealth
        return gappleShouldEat
    }

    private fun gappleShowEatComplete() {
        gappleForceUpdateGappleCount()
        val currentHealth = mc.thePlayer!!.health
        gappleHealthBeforeEat = currentHealth.toDouble()
        gappleWaitingForEffect = false
        progressBarVisible = false
        if (currentHealth <= triggerHealthValue.get()) {
            ClientUtils.displayChatMessage("§6Health still low, continuing...")
            gappleTicks = 0
            gapplePauseTicks = 0
            armorBreakerWork = armorBreakerValue.get()
        }
    }

    private fun armorGetAxe(): Int {
        for (i in 0 until 9) {
            val stack = mc.thePlayer!!.inventory.getStackInSlot(i)
            if (stack == null) continue
            if (stack.item is ItemAxe) return i
        }
        return -1
    }

    private fun renderProgressBar() {
        if (!progressBarVisible) return

        val sr = ScaledResolution(mc)
        val width = sr.scaledWidth
        val height = sr.scaledHeight

        val barWidth = progressBarWidthValue.get()
        val barHeight = progressBarHeightValue.get()
        val textHeight = 10
        val spacing = 8

        val x = (width - barWidth) / 2
        val y = (height - (textHeight + spacing + barHeight)) / 2

        progressBarProgress += (progressBarTargetProgress - progressBarProgress) * 0.15
        if (abs(progressBarProgress - progressBarTargetProgress) < 0.005) {
            progressBarProgress = progressBarTargetProgress
        }

        val displayText = progressBarText
        val textWidth = mc.fontRendererObj!!.getStringWidth(displayText)
        val textX = x + (barWidth - textWidth) / 2f
        val textY = y.toFloat()

        mc.fontRendererObj!!.drawString(displayText, textX + 1f, textY + 1f, Int.MIN_VALUE, false)
        mc.fontRendererObj!!.drawString(displayText, textX, textY, -1, false)

        val barY = y + textHeight + spacing
        GLUtils.drawRect(x.toFloat(), barY.toFloat(), (x + barWidth).toFloat(), (barY + barHeight).toFloat(), 0x80222222.toInt())

        val currentProgressWidth = floor(barWidth * progressBarProgress).toInt()
        if (currentProgressWidth > 0) {
            if (randomGradientValue.get()) {
                drawRectRandomGradient(x.toFloat(), barY.toFloat(), currentProgressWidth, barHeight)
            } else {
                val startColor = getColorFromSettings(true)
                val endColor = getColorFromSettings(false)
                drawRectGradient(x.toFloat(), barY.toFloat(), currentProgressWidth, barHeight, startColor, endColor)
            }
        }

        if (showPercentageValue.get()) {
            val percentText = floor(progressBarProgress * 100).toInt().toString() + "%"
            val percentWidth = mc.fontRendererObj!!.getStringWidth(percentText)
            val percentX = x + (barWidth - percentWidth) / 2f
            val percentY = barY + barHeight + 4
            mc.fontRendererObj!!.drawString(percentText, percentX, percentY.toFloat(), 0xCCFFFFFF.toInt(), false)
        }

        updateGradientColors()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && gappleCancelMove && gappleTicks < c03CountValue.get()) {
            if (packet is C03PacketPlayer.C05PacketPlayerLook) {
                gappleYaw = packet.yaw
                gapplePitch = packet.pitch
            }
            gappleTicks++
            progressBarTargetProgress = gappleTicks.toDouble() / c03CountValue.get()
            if (gappleTicks >= c03CountValue.get() - 1 && gappleShouldEat) {
                armorBreakerWork = false
            }
            event.cancelEvent()
        }

        if (armorBreakerWork && armorBreakerValue.get()) {
            if (packet is C02PacketUseEntity && packet.action == C02PacketUseEntity.Action.ATTACK) {
                armorAxeSlot = armorGetAxe()
                if (armorAxeSlot != -1) {
                    mc.netHandler!!.addToSendQueue(C09PacketHeldItemChange(armorAxeSlot))
                    armorS = true
                }
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (gappleCancelMove) event.cancelEvent()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return

        if (mc.thePlayer!!.ticksExisted - gappleLastGappleCheck > 60) {
            gappleForceUpdateGappleCount()
            gappleLastGappleCheck = mc.thePlayer!!.ticksExisted
        }

        val shouldContinueEating = gappleCheckHealthCondition()

        if (gappleWaitingForEffect) {
            val currentHealth = mc.thePlayer!!.health
            if (currentHealth > gappleHealthBeforeEat + 1.0 || mc.thePlayer!!.ticksExisted - gappleLastEatTick > 0) {
                gappleShowEatComplete()
            }
        }

        if (shouldContinueEating) {
            val slot = gappleGetGApple()

            if (slot >= 0) {
                if (gapplePauseTicks == 0 && !gappleWaitingForEffect) {
                    gappleStuck()
                    if (gappleTicks == 0) {
                        ClientUtils.displayChatMessage("§6§lEating...")
                        gappleForceUpdateGappleCount()
                        progressBarVisible = true
                        progressBarProgress = 0.0
                        progressBarTargetProgress = 0.0
                        initGradientColors()
                    }
                } else {
                    if (gapplePauseTicks > 0) {
                        gappleStopstuck()
                        gapplePauseTicks--
                    }
                }

                if (gappleTicks >= c03CountValue.get() && !gappleWaitingForEffect) {
                    gappleHealthBeforeEat = mc.thePlayer!!.health.toDouble()
                    gappleLastEatTick = mc.thePlayer!!.ticksExisted

                    mc.netHandler!!.addToSendQueue(C09PacketHeldItemChange(slot))
                    mc.netHandler!!.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer!!.inventory.getStackInSlot(slot)))
                    gappleRelease()
                    mc.netHandler!!.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
                    mc.netHandler!!.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer!!.inventory.getStackInSlot(mc.thePlayer!!.inventory.currentItem)))

                    gappleWaitingForEffect = true
                    gapplePauseTicks = 1
                    gappleTicks = 0
                    progressBarTargetProgress = 0.0
                    gappleForceUpdateGappleCount()
                }
            } else {
                if (mc.thePlayer!!.ticksExisted % 40 == 0) {
                    gappleForceUpdateGappleCount()
                    ClientUtils.displayChatMessage("§cWarning: No golden apples found! Count: $gappleGappleCount")
                }
            }
        } else {
            gappleStopstuck()
            gappleTicks = 0
            gapplePauseTicks = 0
            progressBarTargetProgress = 0.0
            progressBarVisible = false
        }

        if (armorTicks > 0) {
            armorTicks--
        } else {
            if (armorPress) {
                mc.gameSettings.keyBindUseItem.pressed = false
                armorPress = false
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE && armorS) {
            if (armorBreakerWork && armorBreakerValue.get()) {
                mc.netHandler!!.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
            }
            armorS = false
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        renderProgressBar()
    }

    override fun onEnable() {
        gappleShouldEat = false
        armorBreakerWork = armorBreakerValue.get()
        gappleTicks = 0
        gapplePauseTicks = 0
        gappleWaitingForEffect = false
        progressBarVisible = false
        progressBarProgress = 0.0
        progressBarTargetProgress = 0.0
        gappleStopstuck()
        gappleForceUpdateGappleCount()
        ClientUtils.displayChatMessage("§aAGAB ON")
        ClientUtils.displayChatMessage("§e- AutoGapple: ${if (autoEatValue.get()) "ON" else "OFF"}")
        ClientUtils.displayChatMessage("§e- ArmorBreaker: ${if (armorBreakerValue.get()) "ON" else "OFF"}")
        ClientUtils.displayChatMessage("§e- ProgressBar: Width=${progressBarWidthValue.get()} Height=${progressBarHeightValue.get()}")
        ClientUtils.displayChatMessage("§e- Random Gradient: ${if (randomGradientValue.get()) "ON" else "OFF"}")
    }

    override fun onDisable() {
        gappleTicks = 0
        gapplePauseTicks = 0
        gappleShouldEat = false
        armorBreakerWork = false
        gappleWaitingForEffect = false
        progressBarVisible = false
        progressBarProgress = 0.0
        progressBarTargetProgress = 0.0
        gappleStopstuck()
        ClientUtils.displayChatMessage("§cAGAB OFF")
    }
}
