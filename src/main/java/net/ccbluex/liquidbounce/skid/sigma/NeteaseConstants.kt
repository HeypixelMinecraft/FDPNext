package net.ccbluex.liquidbounce.skid.sigma

object NeteaseConstants {
    const val PC_APP_VER = "3.1.28.205001"
    const val API_DOMAIN = "https://interface.music.163.com"
    const val WEB_DOMAIN = "https://music.163.com"
    const val SIMPLE_SONG_URL_PREFIX = "https://music.163.com/song/media/outer/url?id="

    const val UA_PC_BROWSER =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 Edg/127.0.0.0"

    const val EAPI_KEY = "e82ckenh8dichen8"
    const val EAPI_SEPARATOR = "-36cd479b6b5-"
    const val LINUX_API_KEY = "rFgB&h#%2?^eDg:Q"

    const val PLAYLIST_HOT_SONGS = 3778678L
    const val PLAYLIST_NEW_SONGS = 3779629L
    const val PLAYLIST_SURGE = 19723756L
    const val PLAYLIST_ORIGINAL = 2884035L

    private const val PRESET_KEY = "0CoJUm6Qyw8W8jud"
    private const val IV = "0102030405060708"
    private const val PUBLIC_KEY = "010001"
    private const val MODULUS =
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7" +
                "b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280" +
                "104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932" +
                "575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b" +
                "3ece0462db0a22b8e7"
    private const val CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun getPresetKey(): String = PRESET_KEY
    fun getIv(): String = IV
    fun getPublicKey(): String = PUBLIC_KEY
    fun getModulus(): String = MODULUS
    fun getCharSet(): String = CHARSET
}
