package com.robloxai.studio.domain

import com.robloxai.studio.data.model.RobloxScriptType

data class GameSystemTemplate(
    val title: String,
    val subtitle: String,
    val prompt: String
)

object RobloxPrompts {

    val gameBuilderTemplates = listOf(
        GameSystemTemplate(
            title = "NPC Dialogue",
            subtitle = "ProximityPrompt + branching chat",
            prompt = "Design a complete NPC Dialogue system with ProximityPrompt, client UI Tweening, and server-verified dialogue trees. Include a ModuleScript for dialogue data, a LocalScript for UI, and a Script for server validation."
        ),
        GameSystemTemplate(
            title = "Quest System",
            subtitle = "Objectives, rewards, DataStore",
            prompt = "Create a full Quest system: quest definitions, progress tracking, server-side completion checks, rewards (coins/XP), and DataStore persistence. Provide ModuleScript + Server Script + client UI LocalScript."
        ),
        GameSystemTemplate(
            title = "Inventory & Weapons",
            subtitle = "Tools, equip, damage",
            prompt = "Build an inventory and weapon system with Tool equipping, server-authoritative damage, cooldowns, and a simple inventory UI. Use RemoteEvents correctly and never trust client damage values."
        ),
        GameSystemTemplate(
            title = "Player Powers",
            subtitle = "Abilities, cooldowns, VFX",
            prompt = "Implement player powers/abilities with server validation, cooldowns, particle VFX hooks, and a hotbar LocalScript. Include ModuleScript for ability definitions."
        ),
        GameSystemTemplate(
            title = "Shop & Currency",
            subtitle = "Coins, purchases, anti-exploit",
            prompt = "Create a shop and currency system with leaderstats, server-side purchase validation, DataStore save/load, and a shop UI. Fully anti-exploit (server never trusts client coin amounts)."
        ),
        GameSystemTemplate(
            title = "Level & XP",
            subtitle = "Progression + leaderstats",
            prompt = "Design a Level/XP system that updates leaderstats, calculates level from XP, persists via DataStore, and fires a RemoteEvent on level-up for client VFX."
        ),
        GameSystemTemplate(
            title = "DataStore Save",
            subtitle = "Player data persistence",
            prompt = "Write a robust DataStore save/load system using UpdateAsync, session locking pattern, retry logic, and BindToClose. Store coins, XP, inventory table, and settings."
        )
    )

    fun unifiedAssistantSystem(projectName: String, projectContext: String): String = """
You are Roblox AI Studio Assistant, an expert Roblox Studio & Luau game engineering assistant.
Active Project: "$projectName"
Project Notes: ${projectContext.ifBlank { "General Roblox experience" }}

Rules:
1. Write modern, strict, idiomatic Luau (--!strict where helpful, task.wait, task.spawn, game:GetService).
2. Clearly state whether each script is a Script (ServerScriptService), LocalScript (StarterPlayerScripts/StarterGui), or ModuleScript (ReplicatedStorage).
3. Enforce server-authoritative validation for RemoteEvents/RemoteFunctions (never trust client currency/damage claims).
4. Never claim that this Android app can run Roblox Studio or edit a live .rbxl file directly; provide instructions for importing/pasting scripts into Roblox Studio.
5. Wrap all Luau code in markdown fences with language tag lua or luau.
6. Prefer ReplicatedStorage for shared modules, ServerScriptService for server logic, StarterPlayerScripts / StarterGui for client.
7. Include brief placement instructions after every script.
""".trimIndent()

    fun scriptGeneratorPrompt(
        title: String,
        scriptType: RobloxScriptType,
        specification: String,
        includeComments: Boolean
    ): String = """
Generate a complete, production-ready ${scriptType.label} named "$title".

Target parent in Explorer: ${scriptType.recommendedParent}
File extension: ${scriptType.extension}

Specification:
$specification

Requirements:
- Idiomatic modern Luau
- ${if (includeComments) "Include clear inline comments and a short Explorer placement note at the top" else "Minimal comments"}
- Server authority for any gameplay-critical values
- Ready to paste into Roblox Studio

Return the full script inside a single ```lua code fence, then a short setup checklist.
""".trimIndent()

    fun errorFixerPrompt(studioError: String, brokenCode: String): String = """
You are an expert Roblox Studio debugger.

Studio Output / Error:
$studioError

${if (brokenCode.isNotBlank()) "Broken script (if provided):\n```lua\n$brokenCode\n```" else "No full script was provided — diagnose from the error text alone."}

Tasks:
1. Explain the root cause in plain language.
2. Provide the fully corrected Luau script inside a ```lua fence.
3. List the exact Explorer location and any RemoteEvent/Module dependencies.
""".trimIndent()

    fun model3DPrompt(
        assetName: String,
        category: String,
        style: String,
        budget: String
    ): String = """
Create a detailed 3D asset concept for Roblox Studio.

Asset Name: $assetName
Category: $category
Art Style: $style
Poly Budget: $budget

Output structure:
1. Short visual description (materials, silhouette, colors).
2. A ready-to-copy prompt for AI 3D generators (Meshy / Tripo / Rodin / etc.) inside a ```prompt fence.
3. Step-by-step import guide for Roblox Studio (.fbx / .obj / .glb) including scale, collision, and texture tips.
4. Suggested Script/LocalScript hooks if the asset is interactive.
""".trimIndent()

    fun extractFirstLuaBlock(markdown: String): String? {
        val patterns = listOf(
            Regex("```(?:lua|luau)\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE),
            Regex("```\\s*\\n([\\s\\S]*?)```")
        )
        for (p in patterns) {
            val m = p.find(markdown)
            if (m != null) {
                val code = m.groupValues[1].trim()
                if (code.isNotBlank()) return code
            }
        }
        return null
    }

    fun extractPromptBlock(markdown: String): String? {
        val p = Regex("```prompt\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        return p.find(markdown)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }
}
