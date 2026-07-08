package net.ccbluex.liquidbounce.ui.client.gui.music

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.util.Collections
import java.util.LinkedHashMap

/**
 * LRU 封面纹理管理器
 * 限制同时加载的封面纹理数量，避免内存溢出
 */
object CoverTextureManager {
    private const val MAX_COVERS = 20
    
    // LRU 缓存：最近最少使用的封面会被自动移除
    private val coverCache = object : LinkedHashMap<String, ResourceLocation>(MAX_COVERS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ResourceLocation>): Boolean {
            if (size > MAX_COVERS) {
                // 移除最久未使用的纹理
                val removedLocation = eldest.value
                try {
                    Minecraft.getMinecraft().textureManager.deleteTexture(removedLocation)
                } catch (_: Exception) {}
                return true
            }
            return false
        }
    }
    
    // 线程安全的缓存访问
    private val lock = Any()
    
    /**
     * 获取或加载封面纹理
     * @param key 封面唯一标识（通常是歌曲ID + URL的哈希）
     * @param loader 加载器函数，返回 BufferedImage
     * @return ResourceLocation 或 null
     */
    fun getOrLoad(key: String, loader: () -> BufferedImage?): ResourceLocation? {
        synchronized(lock) {
            // 检查缓存
            coverCache[key]?.let { return it }
            
            // 加载新纹理
            val image = loader() ?: return null
            val resized = net.ccbluex.liquidbounce.utils.render.ImageUtils.resizeImage(image, 48, 48)
            val texture = DynamicTexture(resized)
            val location = ResourceLocation("fdpnext", "cover_$key")
            
            try {
                Minecraft.getMinecraft().textureManager.loadTexture(location, texture)
                coverCache[key] = location
                return location
            } catch (e: Exception) {
                return null
            }
        }
    }
    
    /**
     * 检查纹理是否已缓存
     */
    fun contains(key: String): Boolean {
        synchronized(lock) {
            return coverCache.containsKey(key)
        }
    }
    
    /**
     * 获取缓存的纹理（如果存在）
     */
    fun get(key: String): ResourceLocation? {
        synchronized(lock) {
            return coverCache[key]
        }
    }
    
    /**
     * 清空所有缓存的纹理
     */
    fun clear() {
        synchronized(lock) {
            coverCache.values.forEach { location ->
                try {
                    Minecraft.getMinecraft().textureManager.deleteTexture(location)
                } catch (_: Exception) {}
            }
            coverCache.clear()
        }
    }
    
    /**
     * 获取当前缓存大小
     */
    fun size(): Int {
        synchronized(lock) {
            return coverCache.size
        }
    }
}
