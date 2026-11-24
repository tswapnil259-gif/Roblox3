package com.robloxai.studio

import com.robloxai.studio.domain.RobloxPrompts
import com.robloxai.studio.ui.components.LuauSyntaxHighlighter
import org.junit.Assert.*
import org.junit.Test

class LuauUtilsTest {

    @Test
    fun `extracts lua code fence from markdown`() {
        val md = """
            Place this inside ServerScriptService:
            ```lua
            local Players = game:GetService("Players")
            print("Ready")
            ```
        """.trimIndent()
        val code = RobloxPrompts.extractFirstLuaBlock(md)
        assertNotNull(code)
        assertTrue(code!!.contains("game:GetService(\"Players\")"))
    }

    @Test
    fun `extracts luau and prompt fences`() {
        val md = """
            ```luau
            export type Stats = { Coins: number }
            ```
            ```prompt
            Low-poly floating island tavern
            ```
        """.trimIndent()
        assertEquals("export type Stats = { Coins: number }", RobloxPrompts.extractFirstLuaBlock(md))
        assertEquals("Low-poly floating island tavern", RobloxPrompts.extractPromptBlock(md))
    }

    @Test
    fun `highlighter applies spans without crashing`() {
        val code = "-- comment\nlocal x = 42\nprint(\"hello\")"
        val annotated = LuauSyntaxHighlighter.highlight(code)
        assertEquals(code, annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
