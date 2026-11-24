package com.robloxai.studio.data.model

enum class AiProvider(
    val displayName: String,
    val baseUrl: String,
    val defaultModels: List<ModelOption>
) {
    OPENROUTER(
        displayName = "OpenRouter (Free & Paid)",
        baseUrl = "https://openrouter.ai/api/v1",
        defaultModels = listOf(
            ModelOption("openrouter/free", "Free Models Router (Auto :free)", isFree = true),
            ModelOption("nvidia/nemotron-3-ultra-550b-a55b:free", "Nemotron 3 Ultra (Free)", isFree = true),
            ModelOption("google/gemma-4-31b-it:free", "Gemma 4 31B IT (Free)", isFree = true),
            ModelOption("qwen/qwen3.8-27b:free", "Qwen 3.8 27B (Free)", isFree = true)
        )
    ),
    OPENAI(
        displayName = "ChatGPT / OpenAI",
        baseUrl = "https://api.openai.com/v1",
        defaultModels = listOf(
            ModelOption("gpt-6.2-sol", "GPT-6.2 Sol"),
            ModelOption("gpt-6.1-sol", "GPT-6.1 Sol"),
            ModelOption("gpt-6.1-terra", "GPT-6.1 Terra"),
            ModelOption("gpt-6-luna", "GPT-6 Luna")
        )
    ),
    ANTHROPIC(
        displayName = "Claude / Anthropic",
        baseUrl = "https://api.anthropic.com/v1",
        defaultModels = listOf(
            ModelOption("claude-sonnet-5-5", "Claude Sonnet 5.5"),
            ModelOption("claude-opus-5-5", "Claude Opus 5.5"),
            ModelOption("claude-haiku-5-5", "Claude Haiku 5.5"),
            ModelOption("claude-fable-5-2", "Claude Fable 5.2")
        )
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com",
        defaultModels = listOf(
            ModelOption("deepseek-flash", "DeepSeek V4.1 Flash"),
            ModelOption("deepseek-v4-pro", "DeepSeek V4 Pro")
        )
    )
}

data class ModelOption(
    val id: String,
    val label: String,
    val isFree: Boolean = false
)

enum class ConnectionStatus {
    UNCONFIGURED,
    CHECKING,
    CONNECTED,
    AUTH_FAILED,
    RATE_LIMITED,
    ERROR
}

data class ProviderStatusInfo(
    val status: ConnectionStatus = ConnectionStatus.UNCONFIGURED,
    val message: String = "No API key configured"
)

enum class RobloxScriptType(
    val label: String,
    val extension: String,
    val recommendedParent: String
) {
    SCRIPT("Script (Server)", ".server.lua", "ServerScriptService"),
    LOCAL_SCRIPT("LocalScript (Client)", ".client.lua", "StarterPlayer > StarterPlayerScripts"),
    MODULE_SCRIPT("ModuleScript (Shared)", ".lua", "ReplicatedStorage > Modules")
}

data class ChatTurn(
    val id: Long = 0,
    val role: String, // "user" or "assistant"
    val content: String,
    val providerName: String,
    val modelId: String,
    val timestamp: Long = System.currentTimeMillis()
)
