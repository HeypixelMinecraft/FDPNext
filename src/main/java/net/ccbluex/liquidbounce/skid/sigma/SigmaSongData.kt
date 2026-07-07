package net.ccbluex.liquidbounce.skid.sigma

import java.io.File

data class SongInfo(
    val id: Long = 0,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0,
    val coverUrl: String = "",
    val localFile: File? = null,
    val neteaseSongId: Long = 0
) {
    val displayTitle: String
        get() = if (artist.isNotEmpty()) "$artist - $title" else title

    val isLocal: Boolean get() = localFile != null
    val isNetease: Boolean get() = neteaseSongId > 0

    companion object {
        fun fromLocalFile(file: File): SongInfo {
            val name = file.nameWithoutExtension
            val parts = name.split(" - ", limit = 2)
            return if (parts.size == 2) {
                SongInfo(title = parts[1].trim(), artist = parts[0].trim(), localFile = file)
            } else {
                SongInfo(title = name, localFile = file)
            }
        }

        fun fromNeteaseTrack(track: NeteaseApiSearch.NeteaseTrack): SongInfo {
            return SongInfo(
                id = track.id,
                title = track.name,
                artist = track.artist,
                album = track.album,
                durationMs = track.duration,
                coverUrl = track.coverUrl,
                neteaseSongId = track.id
            )
        }
    }
}

data class Playlist(
    val name: String,
    val tracks: MutableList<SongInfo> = mutableListOf()
) {
    val size: Int get() = tracks.size
    fun addTrack(track: SongInfo) { tracks.add(track) }
    fun clear() { tracks.clear() }
}
