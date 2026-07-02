/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.autoplayrobot

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.util.BlockPos
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

object AiDecisionClient {
    private val gson = Gson()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AutoPlayRobot-AI").apply {
            isDaemon = true
        }
    }

    fun requestAsync(request: AiDecisionRequest): Future<AiDecisionResult> {
        return executor.submit(Callable { request(request) })
    }

    private fun request(request: AiDecisionRequest): AiDecisionResult {
        val started = System.currentTimeMillis()
        return runCatching {
            val connection = URL(request.endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = request.timeoutMs
            connection.readTimeout = request.timeoutMs
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${request.apiKey}")

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                it.write(gson.toJson(buildPayload(request)))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) {
                return AiDecisionResult(null, System.currentTimeMillis() - started, request.imageDataUrl != null, "HTTP $code: ${body.take(160)}")
            }

            val root = JsonParser().parse(body).asJsonObject
            val choice = root.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
                ?: return AiDecisionResult(null, System.currentTimeMillis() - started, request.imageDataUrl != null, "No choices")
            val finishReason = choice.get("finish_reason")?.asString
            if (finishReason == "length") {
                return AiDecisionResult(null, System.currentTimeMillis() - started, request.imageDataUrl != null, "Response truncated")
            }

            val content = choice.getAsJsonObject("message")?.get("content")?.asString?.trim().orEmpty()
            if (content.isEmpty()) {
                return AiDecisionResult(null, System.currentTimeMillis() - started, request.imageDataUrl != null, "Empty response")
            }

            AiDecisionResult(parseDecision(content), System.currentTimeMillis() - started, request.imageDataUrl != null)
        }.getOrElse {
            AiDecisionResult(null, System.currentTimeMillis() - started, request.imageDataUrl != null, it.javaClass.simpleName + ": " + (it.message ?: "request failed"))
        }
    }

    private fun buildPayload(request: AiDecisionRequest): JsonObject {
        val payload = JsonObject()
        payload.addProperty("model", request.model)
        payload.addProperty("temperature", 0.1)
        payload.addProperty("max_tokens", 220)
        payload.add("response_format", JsonObject().apply { addProperty("type", "json_object") })

        val messages = JsonArray()
        messages.add(JsonObject().apply {
            addProperty("role", "system")
            addProperty("content", systemPrompt(request.directMode))
        })
        messages.add(JsonObject().apply {
            addProperty("role", "user")
            if (request.imageDataUrl == null) {
                addProperty("content", "Return exactly one JSON decision for this observation:\n${request.observation}")
            } else {
                val content = JsonArray()
                content.add(JsonObject().apply {
                    addProperty("type", "text")
                    addProperty("text", "Return exactly one JSON decision for this observation and screenshot:\n${request.observation}")
                })
                content.add(JsonObject().apply {
                    addProperty("type", "image_url")
                    add("image_url", JsonObject().apply { addProperty("url", request.imageDataUrl) })
                })
                add("content", content)
            }
        })
        payload.add("messages", messages)
        return payload
    }

    private fun systemPrompt(directMode: Boolean): String {
        val actions = if (directMode) {
            "MOVE_TO, LOOK_AT, ATTACK, USE_ITEM, OPEN_CHEST, BREAK_BLOCK, ENABLE_MODULE, DISABLE_MODULE, STOP"
        } else {
            "COLLECT_GOLD, COLLECT_RESOURCE, LOOT_CHEST, MOVE_TO, FIGHT_ENTITY, EVADE_ENTITY, ATTACK_BED, DEFEND_BED, SHOOT_MURDERER, AUTO_QUEUE, HOLD"
        }

        return "You control a Minecraft mini-game bot. Output JSON only, no markdown. " +
            "Allowed actions: $actions. " +
            "Schema: {\"action\":\"MOVE_TO\",\"entityId\":123,\"x\":0,\"y\":64,\"z\":0,\"module\":\"KillAura\",\"reason\":\"short\"}. " +
            "Use only ids and coordinates present in the observation. Prefer safe, simple decisions."
    }

    private fun parseDecision(content: String): AiDecision? {
        val json = JsonParser().parse(content).asJsonObject
        val actionName = json.get("action")?.asString?.uppercase(Locale.ROOT) ?: return null
        val action = AiDecisionAction.values().firstOrNull { it.name == actionName } ?: return null
        val hasPos = json.has("x") && json.has("y") && json.has("z")
        return AiDecision(
            action = action,
            entityId = json.get("entityId")?.takeIf { !it.isJsonNull }?.asInt,
            pos = if (hasPos) BlockPos(json.get("x").asInt, json.get("y").asInt, json.get("z").asInt) else null,
            module = json.get("module")?.takeIf { !it.isJsonNull }?.asString,
            reason = json.get("reason")?.takeIf { !it.isJsonNull }?.asString ?: ""
        )
    }
}

data class AiDecisionRequest(
    val endpoint: String,
    val model: String,
    val apiKey: String,
    val timeoutMs: Int,
    val directMode: Boolean,
    val observation: String,
    val imageDataUrl: String?
)
