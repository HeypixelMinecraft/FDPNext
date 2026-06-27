package net.ccbluex.liquidbounce.file.config.sections

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.file.config.ConfigSection
import net.ccbluex.liquidbounce.ui.client.hud.Config
import net.ccbluex.liquidbounce.utils.ClientUtils

/**
 * HUD（visual）配置 section。
 *
 * FDP 5.3.5 的 cfg 原本只保存 modules / macros，visual（HUD 布局）需要另外
 * 用 `.theme save` 单独导出。这里把 HUD 一并纳入 cfg，使 `.config save/load`
 * 也能保存与恢复 visual，无需再单独 `.theme save`。
 *
 * 序列化复用 [Config]（与 `.theme` 完全相同的格式），元素数组存放在 "elements" 键下。
 */
class HudSection : ConfigSection("hud") {

    override fun load(json: JsonObject) {
        // 旧配置或空配置不含 HUD：保持当前/默认 HUD，避免清空
        if (!json.has("elements")) return
        val elements = json.getAsJsonArray("elements")
        if (elements.size() == 0) return

        // 运行时替换 HUD 时屏蔽启动期副作用，沿用 ThemeCommand 的做法；
        // 用前值还原，确保启动阶段加载配置不会提前结束 isStarting
        val prevStarting = FDPNext.isStarting
        FDPNext.isStarting = true
        try {
            FDPNext.hud.clearElements()
            FDPNext.hud = Config(elements.toString()).toHUD()
        } catch (e: Exception) {
            ClientUtils.logError("Failed to load HUD from config.", e)
        } finally {
            FDPNext.isStarting = prevStarting
        }
    }

    override fun save(): JsonObject {
        val json = JsonObject()
        json.add("elements", JsonParser().parse(Config(FDPNext.hud).toJson()).asJsonArray)
        return json
    }
}
