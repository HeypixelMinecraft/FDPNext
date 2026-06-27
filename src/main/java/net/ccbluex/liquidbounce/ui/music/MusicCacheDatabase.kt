/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.music

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * 音乐缓存数据库（SQLite）
 *
 * 记录每首曲目（音乐源 + 曲目 ID）对应的本地缓存 mp3 文件。
 * 流地址通常带时效 token 会过期，但下载下来的 mp3 内容是永久有效的，
 * 因此以「曲目身份」为键缓存，可跨会话复用，避免重复下载。
 *
 * 数据库文件：FDPNext-1.8/MusicTemp/cache.db
 */
object MusicCacheDatabase {

    private val lock = Any()
    private var connection: Connection? = null

    /** 缓存目录，所有 mp3 与数据库都放在这里 */
    val cacheDir: File get() = FDPNext.fileManager.musicTempDir

    /**
     * 懒加载数据库连接并建表（失败时返回 null，缓存功能优雅降级）
     */
    private fun conn(): Connection? {
        connection?.let { return it }
        return try {
            if (!cacheDir.exists()) cacheDir.mkdirs()
            // 显式加载驱动，规避 Forge LaunchWrapper 下的 ServiceLoader 问题
            Class.forName("org.sqlite.JDBC")
            val dbFile = File(cacheDir, "cache.db")
            val c = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            c.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS music_cache (
                        source     TEXT NOT NULL,
                        track_id   TEXT NOT NULL,
                        name       TEXT,
                        artist     TEXT,
                        file_name  TEXT NOT NULL,
                        file_size  INTEGER,
                        created_at INTEGER,
                        PRIMARY KEY (source, track_id)
                    )
                    """.trimIndent()
                )
            }
            connection = c
            c
        } catch (e: Throwable) {
            ClientUtils.logWarn("[MusicCache] 初始化数据库失败: ${e.message}")
            null
        }
    }

    /**
     * 查询曲目对应的有效缓存文件。
     * 若记录存在但文件已丢失，则清理该记录并返回 null。
     */
    fun lookup(track: MusicTrack): File? {
        synchronized(lock) {
            val c = conn() ?: return null
            try {
                c.prepareStatement(
                    "SELECT file_name FROM music_cache WHERE source = ? AND track_id = ?"
                ).use { ps ->
                    ps.setString(1, track.source.name)
                    ps.setString(2, track.id)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val file = File(cacheDir, rs.getString("file_name"))
                            if (file.isFile && file.length() > 0) return file
                            // 文件丢失：清理脏记录
                            remove(track)
                        }
                    }
                }
            } catch (e: Throwable) {
                ClientUtils.logWarn("[MusicCache] 查询失败: ${e.message}")
            }
            return null
        }
    }

    /**
     * 登记曲目与缓存文件的映射
     */
    fun store(track: MusicTrack, file: File): Unit = synchronized(lock) {
        val c = conn() ?: return
        try {
            c.prepareStatement(
                """
                INSERT OR REPLACE INTO music_cache
                    (source, track_id, name, artist, file_name, file_size, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, track.source.name)
                ps.setString(2, track.id)
                ps.setString(3, track.name)
                ps.setString(4, track.artist)
                ps.setString(5, file.name)
                ps.setLong(6, file.length())
                ps.setLong(7, System.currentTimeMillis())
                ps.executeUpdate()
            }
        } catch (e: Throwable) {
            ClientUtils.logWarn("[MusicCache] 写入失败: ${e.message}")
        }
    }

    private fun remove(track: MusicTrack) {
        val c = connection ?: return
        try {
            c.prepareStatement(
                "DELETE FROM music_cache WHERE source = ? AND track_id = ?"
            ).use { ps ->
                ps.setString(1, track.source.name)
                ps.setString(2, track.id)
                ps.executeUpdate()
            }
        } catch (_: Throwable) {
        }
    }
}
