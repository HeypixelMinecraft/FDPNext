/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.server.S02PacketChat
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import java.math.BigInteger
import java.security.MessageDigest
import java.util.regex.Pattern

class ChatTranslator : Module(name = "ChatTranslator", category = ModuleCategory.MISC) {

    private val languageValue = ListValue("Language", arrayOf("Chinese", "English"), "Chinese")
    private val apiValue = ListValue("API", arrayOf("Google", "YouDao", "Bing"), "Google")

    private val client = HttpClients.createDefault()
    private val cache = HashMap<String, String>()

    // Bing auth cache
    private var bingToken: String? = null
    private var bingIG: String? = null
    private var bingKeyTTL: Long = 0
    private var bingIID: String = "translator.5023"

    companion object {
        // YouDao Translate API (from YouDubPlusPlus)
        private const val YOUDAO_URL = "https://dict.youdao.com/jsonapi_s?doctype=json&jsonversion=4"
        private const val YOUDAO_CLIENT = "webmain"
        private const val YOUDAO_KEYFROM = "webfanyi.webmain"
        private const val YOUDAO_SECRET = "t2he2k4m2g6QKRigK0KAmSpXKgAezywG"

        // Bing Translate API
        private const val BING_TRANSLATOR_URL = "https://www.bing.com/translator"
        private const val BING_TRANSLATE_URL = "https://www.bing.com/ttranslatev3"
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S02PacketChat) {
            val msg = event.packet.chatComponent.formattedText
            if (!cache.contains(msg)) {
                doTranslate(msg)
            } else {
                ClientUtils.displayChatMessage(if (cache.containsKey(msg)) { msg } else { cache[msg]!! })
            }

            event.cancelEvent()
        }
    }

    private fun getTargetLang(): String {
        return if (languageValue.get().equals("chinese", ignoreCase = true)) "zh-Hans" else "en"
    }

    private fun getBingFromLang(): String {
        return "auto-detect"
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }

    // ======================== YouDao ========================

    private fun getYoudaoSign(q: String): Pair<String, String> {
        val nowMs = System.currentTimeMillis()
        val suffix = ((q + YOUDAO_KEYFROM).length % 10).toString()
        val timestamp = "$nowMs$suffix"
        val queryHash = md5(q + YOUDAO_KEYFROM)
        val raw = "$YOUDAO_CLIENT$q$timestamp$YOUDAO_SECRET$queryHash"
        val sign = md5(raw)
        return Pair(sign, timestamp)
    }

    private fun getYoudaoResult(data: String): String {
        val json = JsonParser().parse(data).asJsonObject

        val webTrans = json.getAsJsonObject("web_trans")
        if (webTrans != null) {
            val webTranslations = webTrans.getAsJsonArray("web-translation")
            if (webTranslations != null && webTranslations.size() > 0) {
                val firstItem = webTranslations.get(0).asJsonObject
                val trans = firstItem.getAsJsonArray("trans")
                if (trans != null && trans.size() > 0) {
                    val value = trans.get(0).asJsonObject.get("value")
                    if (value != null && !value.asString.isNullOrBlank()) {
                        return value.asString
                    }
                }
            }
        }

        val ec = json.getAsJsonObject("ec")
        if (ec != null) {
            val word = ec.getAsJsonArray("word")
            if (word != null && word.size() > 0) {
                val wordObj = word.get(0).asJsonObject
                val trs = wordObj.getAsJsonArray("trs")
                if (trs != null && trs.size() > 0) {
                    val values = mutableListOf<String>()
                    for (tr in trs) {
                        val tran = tr.asJsonObject.get("tran")
                        if (tran != null && !tran.asString.isNullOrBlank()) {
                            values.add(tran.asString)
                        }
                    }
                    if (values.isNotEmpty()) {
                        return values.joinToString("; ")
                    }
                }
            }
        }

        throw IllegalStateException("Cannot extract translation from YouDao response")
    }

    // ======================== Google ========================

    private fun getGoogleLink(msg: String): String {
        val message = msg.replace(" ", "%20")
        // Google uses zh-CN / en (Bing uses zh-Hans)
        val tl = if (languageValue.get().equals("chinese", ignoreCase = true)) "zh-CN" else "en"
        return "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&dj=1&ie=UTF-8&sl=auto&tl=$tl&q=$message"
    }

    private fun getGoogleResult(data: String): String {
        val json = JsonParser().parse(data).asJsonObject
        return json.get("sentences").asJsonArray.get(0).asJsonObject.get("trans").asString
    }

    // ======================== Bing ========================

    private fun refreshBingAuth() {
        try {
            val request = HttpGet(BING_TRANSLATOR_URL)
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
            val response = client.execute(request)
            val html = EntityUtils.toString(response.entity)

            // Extract IG from HTML: look for query string patterns like "IG=..." in script content or HTML
            val igPattern = Pattern.compile("""IG=([A-F0-9]+)""")
            val igMatcher = igPattern.matcher(html)
            if (igMatcher.find()) {
                bingIG = igMatcher.group(1)
            }

            // Extract token from params_AbusePreventionHelper
            val tokenPattern = Pattern.compile("""params_AbusePreventionHelper\s*=\s*\[(\d+),\s*"([A-Za-z0-9+/=]+)"\s*,\s*(\d+)\]""")
            val tokenMatcher = tokenPattern.matcher(html)
            if (tokenMatcher.find()) {
                bingToken = tokenMatcher.group(2)
                bingKeyTTL = System.currentTimeMillis() + tokenMatcher.group(3).toLong()
            }

            // Extract IID from page
            val iidPattern = Pattern.compile("""IID=([a-zA-Z0-9.]+)""")
            val iidMatcher = iidPattern.matcher(html)
            if (iidMatcher.find()) {
                bingIID = iidMatcher.group(1)
            }

            ClientUtils.logInfo("[ChatTranslator] Bing auth refreshed: IG=$bingIG, token=${bingToken?.take(8)}..., IID=$bingIID")
        } catch (e: Exception) {
            e.printStackTrace()
            ClientUtils.logError("[ChatTranslator] Failed to refresh Bing auth")
        }
    }

    private fun ensureBingAuth() {
        if (bingToken == null || bingIG == null || System.currentTimeMillis() > bingKeyTTL) {
            refreshBingAuth()
        }
    }

    private fun doBingTranslate(msg: String) {
        ensureBingAuth()

        if (bingToken == null || bingIG == null) {
            throw IllegalStateException("Failed to obtain Bing auth token")
        }

        val key = System.currentTimeMillis()
        val fromLang = getBingFromLang()
        val to = getTargetLang() // zh-Hans or en

        // URL with IG
        val url = "$BING_TRANSLATE_URL?isVertical=1&&IG=${bingIG}&IID=$bingIID&SFX=1"

        val params = mutableListOf<NameValuePair>()
        params.add(BasicNameValuePair("fromLang", fromLang))
        params.add(BasicNameValuePair("to", to))
        params.add(BasicNameValuePair("tone", "Casual"))
        params.add(BasicNameValuePair("text", msg))
        params.add(BasicNameValuePair("token", bingToken!!))
        params.add(BasicNameValuePair("key", key.toString()))

        val post = HttpPost(url)
        post.entity = UrlEncodedFormEntity(params, "UTF-8")
        post.setHeader("Accept", "*/*")
        post.setHeader("Content-Type", "application/x-www-form-urlencoded")
        post.setHeader("Origin", "https://www.bing.com")
        post.setHeader("Referer", "https://www.bing.com/translator")
        post.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")

        val response = client.execute(post)
        if (response.statusLine.statusCode != 200) {
            throw IllegalStateException("Bing resp code: " + response.statusLine.statusCode + " != 200")
        }

        val jsonStr = EntityUtils.toString(response.entity)
        val json = JsonParser().parse(jsonStr).asJsonArray
        val translation = json.get(0).asJsonObject
            .get("translations").asJsonArray
            .get(0).asJsonObject
            .get("text").asString

        cache[msg] = translation
        ClientUtils.displayChatMessage(translation)
    }

    // ======================== Main dispatch ========================

    private fun doTranslate(msg: String) {
        Thread {
            try {
                when (apiValue.get().lowercase()) {
                    "google" -> {
                        val request = HttpGet(getGoogleLink(msg))
                        val response = client.execute(request)
                        if (response.statusLine.statusCode != 200) {
                            throw IllegalStateException("Google resp code: " + response.statusLine.statusCode + " != 200")
                        }
                        val result = getGoogleResult(EntityUtils.toString(response.entity))
                        cache[msg] = result
                        ClientUtils.displayChatMessage(result)
                    }
                    "youdao" -> {
                        val (sign, timestamp) = getYoudaoSign(msg)

                        val params = mutableListOf<NameValuePair>()
                        params.add(BasicNameValuePair("q", msg))
                        params.add(BasicNameValuePair("needTranslate", "true"))
                        params.add(BasicNameValuePair("dicts", """{"count":"1","dicts":["fanyi"]}"""))
                        params.add(BasicNameValuePair("sign", sign))
                        params.add(BasicNameValuePair("t", timestamp))
                        params.add(BasicNameValuePair("client", YOUDAO_CLIENT))
                        params.add(BasicNameValuePair("keyfrom", YOUDAO_KEYFROM))

                        val post = HttpPost(YOUDAO_URL)
                        post.entity = UrlEncodedFormEntity(params, "UTF-8")
                        post.setHeader("Accept", "application/json, text/plain, */*")
                        post.setHeader("Content-Type", "application/x-www-form-urlencoded")
                        post.setHeader("Origin", "https://fanyi.youdao.com")
                        post.setHeader("Referer", "https://fanyi.youdao.com/")
                        post.setHeader("User-Agent", "Mozilla/5.0")

                        val response = client.execute(post)
                        if (response.statusLine.statusCode != 200) {
                            throw IllegalStateException("YouDao resp code: " + response.statusLine.statusCode + " != 200")
                        }

                        val result = getYoudaoResult(EntityUtils.toString(response.entity))
                        cache[msg] = result
                        ClientUtils.displayChatMessage(result)
                    }
                    "bing" -> {
                        doBingTranslate(msg)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ClientUtils.displayChatMessage(msg)
            }
        }.start()
    }
}