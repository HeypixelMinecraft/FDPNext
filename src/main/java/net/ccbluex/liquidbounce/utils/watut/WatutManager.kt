/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.watut

import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.*
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.C17PacketCustomPayload
import net.minecraft.network.play.server.S3FPacketCustomPayload
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlin.math.abs
import kotlin.math.min

object WatutManager : MinecraftInstance() {
    const val CHANNEL = "MC|FDPWATUT"
    private const val PROTOCOL = "WATUT2"
    private const val REMOTE_EXPIRE_MS = 10_000L
    private const val SCREEN_EXPIRE_MS = 15_000L
    private const val SCREEN_CHUNK_SIZE = 12_000

    private val remoteStatuses = linkedMapOf<UUID, WatutStatus>()
    private val chunkBuffers = hashMapOf<UUID, WatutChunkAccumulator>()
    private val outgoingChunks = ArrayDeque<PacketBuffer>()

    var enabled = false
    var syncEnabled = true
    var screenPreviewEnabled = true
    var particlesEnabled = true
    var armPoseEnabled = true
    var showOwnStatus = false
    var idleSeconds = 20
    var maxDistance = 64F
    var previewQuality = WatutPreviewQuality.Medium
    var previewFps = 2

    var selfStatus: WatutStatus? = null
        private set

    private var lastStatusSignature = ""
    private var lastStatusSend = 0L
    private var lastScreenCapture = 0L
    private var frameCounter = 1
    private var lastActiveAt = System.currentTimeMillis()
    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0
    private var lastYaw = 0F
    private var lastPitch = 0F
    private var lastScreenClass: Class<out GuiScreen>? = null
    private var lastChatText = ""
    private var lastChatTypeAt = 0L

    fun reset() {
        clearTextures()
        remoteStatuses.clear()
        chunkBuffers.clear()
        outgoingChunks.clear()
        selfStatus = null
        lastStatusSignature = ""
        lastStatusSend = 0L
        lastScreenCapture = 0L
        frameCounter = 1
        lastActiveAt = System.currentTimeMillis()
        lastScreenClass = null
        lastChatText = ""
        lastChatTypeAt = 0L
    }

    fun updateLocal() {
        val player = mc.thePlayer ?: return
        val now = System.currentTimeMillis()
        val screen = mc.currentScreen

        if (hasActivity(screen)) {
            lastActiveAt = now
        }

        val guiState = guiState(screen)
        val idleTicks = ((now - lastActiveAt) / 50L).toInt().coerceAtLeast(0)
        val activity = when {
            screen is GuiChat -> WatutActivity.TYPING
            screen != null -> WatutActivity.IN_GUI
            idleTicks >= idleSeconds * 20 -> WatutActivity.IDLE
            else -> WatutActivity.ACTIVE
        }

        val status = selfStatus ?: WatutStatus(player.uniqueID).also { selfStatus = it }
        status.activity = activity
        status.guiState = guiState
        status.mouseX = mousePercentX()
        status.mouseY = mousePercentY()
        status.mousePressed = Mouse.isButtonDown(0)
        status.idleTicks = idleTicks
        status.typingAmplifier = typingAmplifier(screen, now)
        status.updatedAt = now
        status.poseBlend = approach(status.poseBlend, if (activity == WatutActivity.ACTIVE) 0F else 1F, 0.15F)

        expireRemote(now)
        flushOneScreenChunk()

        if (syncEnabled && mc.netHandler != null) {
            val signature = statusSignature(status)
            if (signature != lastStatusSignature || now - lastStatusSend >= 2_000L) {
                sendStatus(status)
                lastStatusSignature = signature
                lastStatusSend = now
            }
        }
    }

    fun onGuiDraw(mouseX: Int, mouseY: Int, width: Int, height: Int) {
        if (!enabled || !screenPreviewEnabled || !syncEnabled || mc.thePlayer == null || mc.netHandler == null) {
            return
        }

        val screen = mc.currentScreen ?: return
        if (!canCapture(screen)) {
            return
        }

        val now = System.currentTimeMillis()
        val delay = (1000L / previewFps.coerceIn(1, 10))
        if (now - lastScreenCapture < delay) {
            return
        }
        lastScreenCapture = now

        runCatching {
            val frame = captureFrame(previewQuality)
            queueScreenFrame(frame)
        }
    }

    fun handlePayload(packet: S3FPacketCustomPayload) {
        if (packet.channelName != CHANNEL) {
            return
        }

        val buffer = packet.bufferData ?: return
        val copy = PacketBuffer(buffer.copy())
        try {
            if (copy.readStringFromBuffer(16) != PROTOCOL) {
                return
            }

            when (WatutPacketType.values().getOrNull(copy.readInt())) {
                WatutPacketType.STATUS -> readStatus(copy)
                WatutPacketType.SCREEN_CHUNK -> readScreenChunk(copy)
                WatutPacketType.SCREEN_CLEAR -> readScreenClear(copy)
                WatutPacketType.PARTICLE_EVENT -> Unit
                null -> Unit
            }
        } finally {
            copy.release()
        }
    }

    fun statusesForRender(): Collection<WatutStatus> {
        val list = ArrayList<WatutStatus>(remoteStatuses.values)
        if (showOwnStatus) {
            selfStatus?.let { list += it }
        }
        return list
    }

    fun statusFor(uuid: UUID): WatutStatus? {
        val self = selfStatus
        if (showOwnStatus && self?.uuid == uuid) {
            return self
        }
        return remoteStatuses[uuid]
    }

    fun poseStatusFor(uuid: UUID): WatutStatus? {
        val self = selfStatus
        if (self?.uuid == uuid) {
            return self
        }
        return remoteStatuses[uuid]
    }

    private fun hasActivity(screen: GuiScreen?): Boolean {
        val player = mc.thePlayer ?: return false
        val moved = abs(player.posX - lastPosX) > 0.003 ||
            abs(player.posY - lastPosY) > 0.003 ||
            abs(player.posZ - lastPosZ) > 0.003 ||
            abs(player.rotationYaw - lastYaw) > 0.05F ||
            abs(player.rotationPitch - lastPitch) > 0.05F ||
            screen?.javaClass != lastScreenClass ||
            Mouse.isButtonDown(0) ||
            Mouse.isButtonDown(1)

        lastPosX = player.posX
        lastPosY = player.posY
        lastPosZ = player.posZ
        lastYaw = player.rotationYaw
        lastPitch = player.rotationPitch
        lastScreenClass = screen?.javaClass

        return moved
    }

    private fun guiState(screen: GuiScreen?): WatutGuiState = when {
        screen == null -> WatutGuiState.NONE
        screen is GuiChat -> WatutGuiState.CHAT
        screen is GuiInventory -> WatutGuiState.INVENTORY
        screen is GuiChest -> WatutGuiState.CHEST
        screen is GuiCrafting -> WatutGuiState.CRAFTING
        screen is GuiIngameMenu -> WatutGuiState.PAUSE
        screen is GuiEditSign -> WatutGuiState.SIGN
        screen.javaClass.name.endsWith("GuiScreenBook") -> WatutGuiState.BOOK
        screen.javaClass.name.endsWith("GuiRepair") -> WatutGuiState.ANVIL
        screen.javaClass.name.endsWith("GuiEnchantment") -> WatutGuiState.ENCHANTING
        screen is GuiFurnace -> WatutGuiState.FURNACE
        screen is GuiDispenser -> WatutGuiState.DISPENSER
        screen.javaClass.name.endsWith("GuiHopper") -> WatutGuiState.HOPPER
        screen is GuiBeacon -> WatutGuiState.BEACON
        screen.javaClass.name.endsWith("GuiMerchant") -> WatutGuiState.MERCHANT
        else -> WatutGuiState.OTHER
    }

    private fun canCapture(screen: GuiScreen): Boolean {
        val state = guiState(screen)
        return state != WatutGuiState.NONE &&
            state != WatutGuiState.CHAT &&
            state != WatutGuiState.PAUSE &&
            state != WatutGuiState.OTHER
    }

    private fun typingAmplifier(screen: GuiScreen?, now: Long): Float {
        if (screen !is GuiChat) {
            return 0F
        }

        val text = readChatText(screen)
        if (text != lastChatText) {
            lastChatText = text
            lastChatTypeAt = now
        }

        return (1F - ((now - lastChatTypeAt).coerceAtLeast(0L) / 900F)).coerceIn(0F, 1F)
    }

    private fun mousePercentX(): Float {
        val width = mc.displayWidth.coerceAtLeast(1)
        return (Mouse.getX().toFloat() / width.toFloat()).coerceIn(0F, 1F)
    }

    private fun mousePercentY(): Float {
        val height = mc.displayHeight.coerceAtLeast(1)
        return (1F - Mouse.getY().toFloat() / height.toFloat()).coerceIn(0F, 1F)
    }

    private fun statusSignature(status: WatutStatus): String {
        return "${status.activity}:${status.guiState}:${(status.mouseX * 100).toInt()}:${(status.mouseY * 100).toInt()}:${status.mousePressed}:${status.idleTicks / 20}"
    }

    private fun readChatText(screen: GuiChat): String {
        return runCatching {
            val field = GuiChat::class.java.getDeclaredField("inputField")
            field.isAccessible = true
            val input = field.get(screen)
            val getText = input.javaClass.getMethod("getText")
            getText.invoke(input) as? String ?: ""
        }.getOrDefault("")
    }

    private fun sendStatus(status: WatutStatus) {
        val out = baseBuffer(WatutPacketType.STATUS)
        writeUuid(out, status.uuid)
        out.writeInt(status.activity.ordinal)
        out.writeInt(status.guiState.ordinal)
        out.writeFloat(status.mouseX)
        out.writeFloat(status.mouseY)
        out.writeBoolean(status.mousePressed)
        out.writeFloat(status.typingAmplifier)
        out.writeInt(status.idleTicks)
        out.writeLong(status.updatedAt)
        send(out)
    }

    private fun readStatus(buffer: PacketBuffer) {
        val uuid = readUuid(buffer)
        if (uuid == mc.thePlayer?.uniqueID && !showOwnStatus) {
            return
        }

        val status = remoteStatuses.getOrPut(uuid) { WatutStatus(uuid) }
        status.activity = WatutActivity.values().getOrElse(buffer.readInt()) { WatutActivity.ACTIVE }
        status.guiState = WatutGuiState.values().getOrElse(buffer.readInt()) { WatutGuiState.NONE }
        status.mouseX = buffer.readFloat().coerceIn(0F, 1F)
        status.mouseY = buffer.readFloat().coerceIn(0F, 1F)
        status.mousePressed = buffer.readBoolean()
        status.typingAmplifier = buffer.readFloat().coerceIn(0F, 1F)
        status.idleTicks = buffer.readInt().coerceAtLeast(0)
        status.updatedAt = System.currentTimeMillis()
        status.poseBlend = 1F
    }

    private fun queueScreenFrame(frame: WatutScreenFrame) {
        val compressed = compress(frame.rgb)
        val total = (compressed.size + SCREEN_CHUNK_SIZE - 1) / SCREEN_CHUNK_SIZE
        val uuid = mc.thePlayer?.uniqueID ?: return

        for (index in 0 until total) {
            val start = index * SCREEN_CHUNK_SIZE
            val end = min(start + SCREEN_CHUNK_SIZE, compressed.size)
            val out = baseBuffer(WatutPacketType.SCREEN_CHUNK)
            writeUuid(out, uuid)
            out.writeInt(frame.frameId)
            out.writeInt(frame.width)
            out.writeInt(frame.height)
            out.writeInt(total)
            out.writeInt(index)
            out.writeInt(compressed.size)
            out.writeByteArray(compressed.copyOfRange(start, end))
            outgoingChunks += out
        }
    }

    private fun flushOneScreenChunk() {
        if (!syncEnabled || mc.netHandler == null || outgoingChunks.isEmpty()) {
            return
        }
        send(outgoingChunks.removeFirst())
    }

    private fun readScreenChunk(buffer: PacketBuffer) {
        val uuid = readUuid(buffer)
        val frameId = buffer.readInt()
        val width = buffer.readInt()
        val height = buffer.readInt()
        val total = buffer.readInt().coerceIn(1, 128)
        val index = buffer.readInt().coerceIn(0, total - 1)
        val compressedSize = buffer.readInt().coerceAtLeast(0)
        val chunk = buffer.readByteArray()

        val accumulator = chunkBuffers.getOrPut(uuid) {
            WatutChunkAccumulator(frameId, width, height, total, compressedSize)
        }

        if (accumulator.frameId != frameId || accumulator.total != total || accumulator.compressedSize != compressedSize) {
            chunkBuffers[uuid] = WatutChunkAccumulator(frameId, width, height, total, compressedSize).also {
                it.chunks[index] = chunk
            }
            return
        }

        accumulator.chunks[index] = chunk
        if (!accumulator.complete) {
            return
        }

        val compressed = accumulator.join()
        chunkBuffers.remove(uuid)
        val rgb = decompress(compressed, width * height * 3)
        applyScreenTexture(uuid, frameId, width, height, rgb)
    }

    private fun readScreenClear(buffer: PacketBuffer) {
        val uuid = readUuid(buffer)
        remoteStatuses[uuid]?.screen = null
        chunkBuffers.remove(uuid)
    }

    private fun applyScreenTexture(uuid: UUID, frameId: Int, width: Int, height: Int, rgb: ByteArray) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        var offset = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = rgb[offset++].toInt() and 0xFF
                val g = rgb[offset++].toInt() and 0xFF
                val b = rgb[offset++].toInt() and 0xFF
                image.setRGB(x, y, -0x1000000 or (r shl 16) or (g shl 8) or b)
            }
        }

        val texture = DynamicTexture(image)
        val location = mc.textureManager.getDynamicTextureLocation("fdpwatut/$uuid/$frameId", texture)
        val status = remoteStatuses.getOrPut(uuid) { WatutStatus(uuid) }
        status.screen = WatutScreenTexture(frameId, width, height, location)
        status.updatedAt = System.currentTimeMillis()
    }

    private fun captureFrame(quality: WatutPreviewQuality): WatutScreenFrame {
        val displayWidth = mc.displayWidth
        val displayHeight = mc.displayHeight
        val raw = ByteBuffer.allocateDirect(displayWidth * displayHeight * 3)
        if (OpenGlHelper.isFramebufferEnabled()) {
            GlStateManager.bindTexture(mc.framebuffer.framebufferTexture)
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, raw)
        } else {
            GL11.glReadBuffer(GL11.GL_FRONT)
            GL11.glReadPixels(0, 0, displayWidth, displayHeight, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, raw)
        }

        val width = quality.width
        val height = quality.height
        val rgb = ByteArray(width * height * 3)
        var out = 0
        for (y in 0 until height) {
            val srcY = displayHeight - 1 - (y * displayHeight / height)
            for (x in 0 until width) {
                val srcX = x * displayWidth / width
                val src = (srcY * displayWidth + srcX) * 3
                rgb[out++] = raw.get(src)
                rgb[out++] = raw.get(src + 1)
                rgb[out++] = raw.get(src + 2)
            }
        }

        return WatutScreenFrame(frameCounter++, width, height, rgb)
    }

    private fun expireRemote(now: Long) {
        val iterator = remoteStatuses.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val status = entry.value
            if (now - status.updatedAt > REMOTE_EXPIRE_MS) {
                iterator.remove()
                chunkBuffers.remove(entry.key)
            } else if (status.screen != null && now - status.screen!!.updatedAt > SCREEN_EXPIRE_MS) {
                status.screen = null
            }
            status.poseBlend = approach(status.poseBlend, if (status.visible) 1F else 0F, 0.08F)
        }
    }

    private fun clearTextures() {
        remoteStatuses.values.forEach { it.screen = null }
    }

    private fun baseBuffer(type: WatutPacketType): PacketBuffer {
        return PacketBuffer(Unpooled.buffer()).apply {
            writeString(PROTOCOL)
            writeInt(type.ordinal)
        }
    }

    private fun send(buffer: PacketBuffer) {
        mc.netHandler?.addToSendQueue(C17PacketCustomPayload(CHANNEL, buffer))
    }

    private fun writeUuid(buffer: PacketBuffer, uuid: UUID) {
        buffer.writeString(uuid.toString().replace("-", ""))
    }

    private fun readUuid(buffer: PacketBuffer): UUID {
        val raw = buffer.readStringFromBuffer(32)
        return UUID.fromString(raw.replaceFirst(
            Regex("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})"),
            "$1-$2-$3-$4-$5"
        ))
    }

    private fun compress(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    private fun decompress(bytes: ByteArray, expectedSize: Int): ByteArray {
        val result = ByteArrayOutputStream(expectedSize)
        InflaterInputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(4096)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) {
                    break
                }
                result.write(buffer, 0, read)
            }
        }
        val data = result.toByteArray()
        return if (data.size == expectedSize) data else data.copyOf(expectedSize)
    }

    private fun approach(current: Float, target: Float, step: Float): Float {
        return when {
            current < target -> (current + step).coerceAtMost(target)
            current > target -> (current - step).coerceAtLeast(target)
            else -> current
        }
    }

    private class WatutChunkAccumulator(
        val frameId: Int,
        val width: Int,
        val height: Int,
        val total: Int,
        val compressedSize: Int
    ) {
        val chunks = arrayOfNulls<ByteArray>(total)
        val complete: Boolean
            get() = chunks.all { it != null }

        fun join(): ByteArray {
            val out = ByteArrayOutputStream(compressedSize)
            chunks.forEach { out.write(it) }
            return out.toByteArray()
        }
    }
}
