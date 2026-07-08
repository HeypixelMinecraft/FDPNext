package net.ccbluex.liquidbounce.ui.client.gui.music

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.ResourceLocation
import java.awt.Font
import java.io.InputStream

/**
 * Sigma 音乐播放器资源加载器
 * 加载从 Sigma 客户端抓取的图标和字体资源
 */
object SigmaMusicResources {
    // 图标资源路径
    private const val MUSIC_DIR = "fdpnext/music/"
    private const val FONT_DIR = "fdpnext/font/"

    val playIcon: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}play.png")
    val pauseIcon: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}pause.png")
    val forwardsIcon: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}forwards.png")
    val backwardsIcon: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}backwards.png")
    val bgImage: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}bg.png")
    val artworkImage: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}artwork.png")
    val particleImage: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}particle.png")
    val repeatIcon: ResourceLocation = ResourceLocation("fdpnext", "${MUSIC_DIR}repeat.png")

    // 字体资源 - 缓存延迟加载的 Font 对象
    private val fontCache = mutableMapOf<String, Font>()

    /**
     * 加载 TTF 字体文件
     */
    fun loadFont(filename: String, size: Float): Font {
        val key = "${filename}_${size}"
        fontCache[key]?.let { return it }

        return try {
            val fontStream: InputStream? = Minecraft::class.java.classLoader
                .getResourceAsStream("assets/minecraft/$FONT_DIR$filename")
            val font = if (fontStream != null) {
                val baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream)
                baseFont.deriveFont(Font.PLAIN, size)
            } else {
                // 加载失败时使用系统字体作为后备
                Font("Arial", Font.PLAIN, size.toInt())
            }
            fontCache[key] = font
            font
        } catch (e: Exception) {
            Font("Arial", Font.PLAIN, size.toInt()).also { fontCache[key] = it }
        }
    }

    // Helvetica Light 字体（多个尺寸）
    fun helveticaLight14(): Font = loadFont("helvetica-neue-light.ttf", 14f)
    fun helveticaLight18(): Font = loadFont("helvetica-neue-light.ttf", 18f)
    fun helveticaLight20(): Font = loadFont("helvetica-neue-light.ttf", 20f)
    fun helveticaLight25(): Font = loadFont("helvetica-neue-light.ttf", 25f)
    fun helveticaLight40(): Font = loadFont("helvetica-neue-light.ttf", 40f)

    // Helvetica Medium 字体（多个尺寸）
    fun helveticaMedium14(): Font = loadFont("helvetica-neue-medium.ttf", 14f)
    fun helveticaMedium20(): Font = loadFont("helvetica-neue-medium.ttf", 20f)
    fun helveticaMedium25(): Font = loadFont("helvetica-neue-medium.ttf", 25f)
    fun helveticaMedium40(): Font = loadFont("helvetica-neue-medium.ttf", 40f)

    // Regular 字体
    fun regularFont20(): Font = loadFont("regular.ttf", 20f)
    fun regularFont40(): Font = loadFont("regular.ttf", 40f)

    // Sigma 主题颜色（来自 ClientColors）
    const val DEEP_TEAL = 0xFF131313.toInt()              // 主背景色
    const val LIGHT_GREYISH_BLUE = 0xFFEEEEEE.toInt()      // 主文本色
    const val SEMI_TRANSPARENT_BLACK = 0x40000000          // 50% 黑色遮罩
    const val MEDIUM_TRANSPARENT = 0x60000000              // 40% 遮罩
    const val TAB_HOVER = 0x302080FF                       // 蓝色 hover
    const val TAB_SELECTED = 0x402080FF                    // 蓝色选中
    const val TEXT_SECONDARY = 0xFF999999.toInt()          // 次要文本
    const val ACCENT_OVERLAY = 0x99000000.toInt()          // 强调遮罩
}
