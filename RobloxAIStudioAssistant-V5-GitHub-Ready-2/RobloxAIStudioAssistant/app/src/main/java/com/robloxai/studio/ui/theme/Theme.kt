package com.robloxai.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF0A0E17)
val SlateSurface = Color(0xFF131A26)
val ElevatedCard = Color(0xFF1B2434)
val RobloxBlue = Color(0xFF00A2FF)
val NeonMint = Color(0xFF10B981)
val AmberWarn = Color(0xFFF59E0B)
val CrimsonError = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF1F5F9)
val TextMuted = Color(0xFF94A3B8)
val BorderSubtle = Color(0xFF263248)

private val GamingDarkColors = darkColorScheme(
    primary = RobloxBlue,
    onPrimary = Color(0xFF001424),
    secondary = NeonMint,
    onSecondary = Color(0xFF002116),
    tertiary = AmberWarn,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = TextMuted,
    error = CrimsonError,
    outline = BorderSubtle
)

@Composable
fun RobloxAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GamingDarkColors,
        content = content
    )
}
