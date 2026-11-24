package com.robloxai.studio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.model.*
import com.robloxai.studio.ui.theme.*

@Composable
fun ProviderModelBar(
    selectedProvider: AiProvider,
    selectedModel: String,
    availableModels: List<ModelOption>,
    statuses: Map<AiProvider, ProviderStatusInfo>,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    onRefreshOpenRouterFree: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    val statusInfo = statuses[selectedProvider] ?: ProviderStatusInfo()
    val dotColor = when (statusInfo.status) {
        ConnectionStatus.CONNECTED -> NeonMint
        ConnectionStatus.CHECKING, ConnectionStatus.RATE_LIMITED -> AmberWarn
        ConnectionStatus.AUTH_FAILED, ConnectionStatus.ERROR -> CrimsonError
        ConnectionStatus.UNCONFIGURED -> TextMuted
    }

    Surface(color = SlateSurface, tonalElevation = 2.dp) {
        Column(Modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    AssistChip(onClick = { providerExpanded = true }, label = { Text(selectedProvider.displayName, fontSize = 12.sp) })
                    DropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                        AiProvider.entries.forEach { p ->
                            DropdownMenuItem(text = { Text(p.displayName) }, onClick = {
                                onSelectProvider(p); providerExpanded = false
                            })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    AssistChip(onClick = { modelExpanded = true }, label = {
                        Text(availableModels.firstOrNull { it.id == selectedModel }?.label ?: selectedModel, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    })
                    DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        availableModels.forEach { m ->
                            DropdownMenuItem(text = { Text(m.label + if (m.isFree) " (Free)" else "") }, onClick = {
                                onSelectModel(m.id); modelExpanded = false
                            })
                        }
                        if (selectedProvider == AiProvider.OPENROUTER) {
                            DropdownMenuItem(text = { Text("↻ Refresh Free Models") }, onClick = {
                                onRefreshOpenRouterFree(); modelExpanded = false
                            })
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().clickable { onOpenSettings() }, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${statusInfo.status.name}: ${statusInfo.message}",
                    fontSize = 11.sp, color = dotColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (statusInfo.status != ConnectionStatus.CONNECTED) {
                    Text("Configure →", fontSize = 11.sp, color = RobloxBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
