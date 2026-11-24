package com.robloxai.studio.data.remote

import com.robloxai.studio.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class AiResult<out T> {
    data class Success<T>(val data: T) : AiResult<T>()
    data class Failure(
        val status: ConnectionStatus,
        val errorMessage: String,
        val httpCode: Int? = null
    ) : AiResult<Nothing>()
}

class AiApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val openAiBaseUrl: String = AiProvider.OPENAI.baseUrl,
    private val anthropicBaseUrl: String = AiProvider.ANTHROPIC.baseUrl,
    private val deepSeekBaseUrl: String = AiProvider.DEEPSEEK.baseUrl,
    private val openRouterBaseUrl: String = AiProvider.OPENROUTER.baseUrl
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mediaJson = "application/json; charset=utf-8".toMediaType()

    private fun baseUrlFor(provider: AiProvider): String = when (provider) {
        AiProvider.OPENAI -> openAiBaseUrl
        AiProvider.ANTHROPIC -> anthropicBaseUrl
        AiProvider.DEEPSEEK -> deepSeekBaseUrl
        AiProvider.OPENROUTER -> openRouterBaseUrl
    }.trimEnd('/')

    suspend fun verifyConnection(
        provider: AiProvider,
        apiKey: String,
        relayEnabled: Boolean = false,
        relayUrl: String = "",
        relayToken: String = ""
    ): ProviderStatusInfo = withContext(Dispatchers.IO) {
        if (relayEnabled) {
            if (relayUrl.isBlank()) {
                return@withContext ProviderStatusInfo(ConnectionStatus.UNCONFIGURED, "Relay URL is empty")
            }
            return@withContext verifyRelay(provider, relayUrl, relayToken)
        }

        if (apiKey.isBlank()) {
            return@withContext ProviderStatusInfo(ConnectionStatus.UNCONFIGURED, "API key is not set")
        }

        // For OpenRouter, /auth/key verifies the API key explicitly; /models is public.
        val url = when (provider) {
            AiProvider.OPENROUTER -> "${baseUrlFor(provider)}/auth/key"
            AiProvider.OPENAI, AiProvider.DEEPSEEK, AiProvider.ANTHROPIC -> "${baseUrlFor(provider)}/models"
        }

        val builder = Request.Builder().url(url).get()
        if (provider == AiProvider.ANTHROPIC) {
            builder.addHeader("x-api-key", apiKey)
            builder.addHeader("anthropic-version", "2023-06-01")
        } else {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }

        try {
            client.newCall(builder.build()).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                when {
                    resp.isSuccessful -> ProviderStatusInfo(
                        ConnectionStatus.CONNECTED,
                        "Authenticated with ${provider.displayName}"
                    )
                    resp.code == 401 || resp.code == 403 -> ProviderStatusInfo(
                        ConnectionStatus.AUTH_FAILED,
                        "Authentication failed (HTTP ${resp.code}). Check your API key."
                    )
                    resp.code == 429 -> ProviderStatusInfo(
                        ConnectionStatus.RATE_LIMITED,
                        "Rate limited (HTTP 429). Try again shortly."
                    )
                    else -> ProviderStatusInfo(
                        ConnectionStatus.ERROR,
                        "HTTP ${resp.code}: ${extractErrorMessage(bodyStr)}"
                    )
                }
            }
        } catch (e: IOException) {
            ProviderStatusInfo(ConnectionStatus.ERROR, "Network error: ${e.message ?: "Unable to reach host"}")
        }
    }

    private fun verifyRelay(provider: AiProvider, relayUrl: String, relayToken: String): ProviderStatusInfo {
        val reqBuilder = Request.Builder().url("${relayUrl.trimEnd('/')}/api/health").get()
        if (relayToken.isNotBlank()) reqBuilder.addHeader("Authorization", "Bearer $relayToken")
        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return if (resp.code == 401 || resp.code == 403) {
                        ProviderStatusInfo(ConnectionStatus.AUTH_FAILED, "Relay authentication failed (HTTP ${resp.code})")
                    } else {
                        ProviderStatusInfo(ConnectionStatus.ERROR, "Relay error HTTP ${resp.code}")
                    }
                }
                val root = json.parseToJsonElement(body).jsonObject
                val configured = root["configuredProviders"]?.jsonObject?.get(provider.name)?.jsonPrimitive?.booleanOrNull == true
                if (configured) {
                    ProviderStatusInfo(ConnectionStatus.CONNECTED, "Connected via Backend Relay")
                } else {
                    ProviderStatusInfo(ConnectionStatus.AUTH_FAILED, "${provider.name} key not configured on relay server")
                }
            }
        } catch (e: Exception) {
            ProviderStatusInfo(ConnectionStatus.ERROR, "Relay unreachable: ${e.message}")
        }
    }

    suspend fun fetchOpenRouterFreeModels(apiKey: String): AiResult<List<ModelOption>> = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder()
            .url("${baseUrlFor(AiProvider.OPENROUTER)}/models")
            .get()
        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext mapHttpFailure(resp.code, body, resp.header("Retry-After"))
                }
                val root = json.parseToJsonElement(body).jsonObject
                val dataArr = root["data"]?.jsonArray ?: JsonArray(emptyList())
                val parsed = mutableListOf(
                    ModelOption("openrouter/free", "Free Models Router (Auto :free)", isFree = true)
                )
                for (item in dataArr) {
                    val obj = item.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id
                    val pricing = obj["pricing"]?.jsonObject
                    val promptPrice = pricing?.get("prompt")?.jsonPrimitive?.contentOrNull
                    val completionPrice = pricing?.get("completion")?.jsonPrimitive?.contentOrNull
                    val isZeroCost = id.endsWith(":free") || (promptPrice == "0" && completionPrice == "0")
                    if (isZeroCost && id != "openrouter/free") {
                        parsed.add(ModelOption(id = id, label = "$name (Free)", isFree = true))
                    }
                }
                AiResult.Success(parsed.distinctBy { it.id })
            }
        } catch (e: Exception) {
            AiResult.Failure(ConnectionStatus.ERROR, "Failed to load OpenRouter models: ${e.message}")
        }
    }

    suspend fun sendChat(
        provider: AiProvider,
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        history: List<ChatTurn>,
        relayEnabled: Boolean = false,
        relayUrl: String = "",
        relayToken: String = ""
    ): AiResult<String> = withContext(Dispatchers.IO) {
        if (relayEnabled) {
            if (relayUrl.isBlank()) {
                return@withContext AiResult.Failure(ConnectionStatus.UNCONFIGURED, "Backend relay URL is not configured.")
            }
            return@withContext sendViaRelay(provider, modelId, systemPrompt, history, relayUrl, relayToken)
        }

        if (apiKey.isBlank()) {
            return@withContext AiResult.Failure(
                ConnectionStatus.UNCONFIGURED,
                "No API key saved for ${provider.displayName}. Configure it in Settings."
            )
        }

        return@withContext when (provider) {
            AiProvider.ANTHROPIC -> sendAnthropic(modelId, apiKey, systemPrompt, history)
            AiProvider.OPENAI, AiProvider.DEEPSEEK, AiProvider.OPENROUTER ->
                sendOpenAiCompatible(provider, modelId, apiKey, systemPrompt, history)
        }
    }

    private fun sendOpenAiCompatible(
        provider: AiProvider,
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        history: List<ChatTurn>
    ): AiResult<String> {
        val url = "${baseUrlFor(provider)}/chat/completions"
        val messagesJson = buildJsonArray {
            if (systemPrompt.isNotBlank()) {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            history.forEach { turn ->
                add(buildJsonObject {
                    put("role", if (turn.role == "assistant") "assistant" else "user")
                    put("content", turn.content)
                })
            }
        }

        val payload = buildJsonObject {
            put("model", modelId)
            put("messages", messagesJson)
        }.toString()

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(mediaJson))

        if (provider == AiProvider.OPENROUTER) {
            reqBuilder.addHeader("X-Title", "Roblox AI Studio Assistant")
        }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return mapHttpFailure(resp.code, body, resp.header("Retry-After"))
                }
                val root = json.parseToJsonElement(body).jsonObject
                val text = root["choices"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")
                    ?.jsonObject?.get("content")
                    ?.jsonPrimitive?.contentOrNull
                if (text.isNullOrBlank()) {
                    AiResult.Failure(ConnectionStatus.ERROR, "Provider returned an empty response.")
                } else {
                    AiResult.Success(text)
                }
            }
        } catch (e: IOException) {
            AiResult.Failure(ConnectionStatus.ERROR, "Network failure: ${e.message}")
        } catch (e: Exception) {
            AiResult.Failure(ConnectionStatus.ERROR, "Parse error: ${e.message}")
        }
    }

    private fun sendAnthropic(
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        history: List<ChatTurn>
    ): AiResult<String> {
        val url = "${baseUrlFor(AiProvider.ANTHROPIC)}/messages"

        // Normalize turns: Anthropic requires the first turn to be "user" and alternating roles
        val normalized = mutableListOf<Pair<String, String>>()
        for (turn in history) {
            val role = if (turn.role == "assistant") "assistant" else "user"
            if (normalized.isEmpty() && role != "user") continue
            if (normalized.isNotEmpty() && normalized.last().first == role) {
                val prev = normalized.removeAt(normalized.lastIndex)
                normalized.add(role to "${prev.second}\n\n${turn.content}")
            } else {
                normalized.add(role to turn.content)
            }
        }

        val messagesJson = buildJsonArray {
            normalized.forEach { (role, content) ->
                add(buildJsonObject {
                    put("role", role)
                    put("content", content)
                })
            }
        }

        val payload = buildJsonObject {
            put("model", modelId)
            put("max_tokens", 4096)
            if (systemPrompt.isNotBlank()) put("system", systemPrompt)
            put("messages", messagesJson)
        }.toString()

        val req = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toRequestBody(mediaJson))
            .build()

        return try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return mapHttpFailure(resp.code, body, resp.header("Retry-After"))
                }
                val root = json.parseToJsonElement(body).jsonObject
                val text = root["content"]?.jsonArray
                    ?.mapNotNull { block ->
                        val obj = block.jsonObject
                        if (obj["type"]?.jsonPrimitive?.contentOrNull == "text") {
                            obj["text"]?.jsonPrimitive?.contentOrNull
                        } else null
                    }?.joinToString("\n")
                if (text.isNullOrBlank()) {
                    AiResult.Failure(ConnectionStatus.ERROR, "Anthropic returned no text content.")
                } else {
                    AiResult.Success(text)
                }
            }
        } catch (e: IOException) {
            AiResult.Failure(ConnectionStatus.ERROR, "Network failure: ${e.message}")
        } catch (e: Exception) {
            AiResult.Failure(ConnectionStatus.ERROR, "Parse error: ${e.message}")
        }
    }

    private fun sendViaRelay(
        provider: AiProvider,
        modelId: String,
        systemPrompt: String,
        history: List<ChatTurn>,
        relayUrl: String,
        relayToken: String
    ): AiResult<String> {
        val payload = buildJsonObject {
            put("provider", provider.name)
            put("model", modelId)
            put("systemPrompt", systemPrompt)
            put("messages", buildJsonArray {
                history.forEach {
                    add(buildJsonObject {
                        put("role", it.role)
                        put("content", it.content)
                    })
                }
            })
        }.toString()

        val builder = Request.Builder()
            .url("${relayUrl.trimEnd('/')}/api/chat")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(mediaJson))
        if (relayToken.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer $relayToken")
        }

        return try {
            client.newCall(builder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return mapHttpFailure(resp.code, body, resp.header("Retry-After"))
                }
                val root = json.parseToJsonElement(body).jsonObject
                val content = root["content"]?.jsonPrimitive?.contentOrNull
                if (content.isNullOrBlank()) {
                    AiResult.Failure(ConnectionStatus.ERROR, "Relay returned empty response.")
                } else {
                    AiResult.Success(content)
                }
            }
        } catch (e: Exception) {
            AiResult.Failure(ConnectionStatus.ERROR, "Relay request failed: ${e.message}")
        }
    }

    private fun mapHttpFailure(code: Int, body: String, retryAfter: String?): AiResult.Failure {
        val detail = extractErrorMessage(body)
        return when (code) {
            401, 403 -> AiResult.Failure(
                ConnectionStatus.AUTH_FAILED,
                "Authentication failed (HTTP $code): $detail",
                code
            )
            402 -> AiResult.Failure(
                ConnectionStatus.ERROR,
                "Insufficient credits / billing required (HTTP 402): $detail",
                code
            )
            429 -> {
                val waitHint = retryAfter?.let { " Retry after ${it}s." } ?: ""
                AiResult.Failure(
                    ConnectionStatus.RATE_LIMITED,
                    "Rate limit exceeded (HTTP 429).$waitHint $detail",
                    code
                )
            }
            else -> AiResult.Failure(
                ConnectionStatus.ERROR,
                "API Error (HTTP $code): $detail",
                code
            )
        }
    }

    private fun extractErrorMessage(body: String): String {
        if (body.isBlank()) return "Empty error response"
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            root["error"]?.let { err ->
                when (err) {
                    is JsonObject -> err["message"]?.jsonPrimitive?.contentOrNull
                    is JsonPrimitive -> err.contentOrNull
                    else -> null
                }
            } ?: root["message"]?.jsonPrimitive?.contentOrNull ?: body.take(240)
        } catch (_: Exception) {
            body.take(240)
        }
    }
}
