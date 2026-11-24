package com.robloxai.studio

import com.robloxai.studio.data.model.AiProvider
import com.robloxai.studio.data.model.ChatTurn
import com.robloxai.studio.data.model.ConnectionStatus
import com.robloxai.studio.data.remote.AiApiService
import com.robloxai.studio.data.remote.AiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AiApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: AiApiService

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val base = server.url("/").toString().trimEnd('/')
        service = AiApiService(
            openAiBaseUrl = base,
            anthropicBaseUrl = base,
            deepSeekBaseUrl = base,
            openRouterBaseUrl = base
        )
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `never reports connected when api key is blank`() = runTest {
        val status = service.verifyConnection(AiProvider.OPENAI, "")
        assertEquals(ConnectionStatus.UNCONFIGURED, status.status)
    }

    @Test
    fun `reports AUTH_FAILED on HTTP 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"Invalid key"}}"""))
        val status = service.verifyConnection(AiProvider.ANTHROPIC, "bad-key")
        assertEquals(ConnectionStatus.AUTH_FAILED, status.status)
        assertTrue(status.message.contains("401"))
    }

    @Test
    fun `parses OpenAI chat completion`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[{"message":{"content":"print('Hello')"}}]}"""))
        val res = service.sendChat(
            provider = AiProvider.OPENAI,
            modelId = "gpt-test",
            apiKey = "sk-test",
            systemPrompt = "sys",
            history = listOf(ChatTurn(role = "user", content = "hi", providerName = "OpenAI", modelId = "gpt-test"))
        )
        assertTrue(res is AiResult.Success)
        assertEquals("print('Hello')", (res as AiResult.Success).data)
    }
}
