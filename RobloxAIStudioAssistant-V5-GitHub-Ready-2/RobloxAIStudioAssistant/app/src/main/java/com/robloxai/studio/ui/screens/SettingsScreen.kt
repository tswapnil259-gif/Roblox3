package com.robloxai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.model.AiProvider
import com.robloxai.studio.data.model.ConnectionStatus
import com.robloxai.studio.data.model.ProviderStatusInfo
import com.robloxai.studio.ui.theme.*

@Composable
fun SettingsScreen(
    statuses: Map<AiProvider, ProviderStatusInfo>,
    isRelayEnabled: Boolean,
    relayUrl: String,
    relayToken: String,
    getApiKey: (AiProvider) -> String,
    onSaveApiKey: (AiProvider, String) -> Unit,
    onVerifyProvider: (AiProvider) -> Unit,
    onSaveRelaySettings: (Boolean, String, String) -> Unit,
    onRefreshOpenRouterFree: () -> Unit
) {
    var relayEnabledDraft by remember { mutableStateOf(isRelayEnabled) }
    var relayUrlDraft by remember { mutableStateOf(relayUrl) }
    var relayTokenDraft by remember { mutableStateOf(relayToken) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = NeonMint)
            Spacer(Modifier.width(8.dp))
            Text("API Credentials & Relay", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text("Keys stored with Android Keystore AES-256. Never hardcoded.", fontSize = 12.sp, color = TextMuted)

        Card(colors = CardDefaults.cardColors(containerColor = ElevatedCard), border = BorderStroke(1.dp, BorderSubtle)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Backend Relay Mode", fontWeight = FontWeight.Bold)
                        Text("Route calls through your HTTPS Node.js relay", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(checked = relayEnabledDraft, onCheckedChange = { relayEnabledDraft = it })
                }
                if (relayEnabledDraft) {
                    OutlinedTextField(value = relayUrlDraft, onValueChange = { relayUrlDraft = it }, label = { Text("Relay Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = relayTokenDraft, onValueChange = { relayTokenDraft = it }, label = { Text("Bearer Token (optional)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                }
                Button(onClick = { onSaveRelaySettings(relayEnabledDraft, relayUrlDraft, relayTokenDraft) }, modifier = Modifier.align(Alignment.End)) { Text("Apply Relay") }
            }
        }

        AiProvider.entries.forEach { provider ->
            var keyDraft by remember(provider) { mutableStateOf(getApiKey(provider)) }
            var showKey by remember { mutableStateOf(false) }
            val statusInfo = statuses[provider] ?: ProviderStatusInfo()
            val statusColor = when (statusInfo.status) {
                ConnectionStatus.CONNECTED -> NeonMint
                ConnectionStatus.CHECKING, ConnectionStatus.RATE_LIMITED -> AmberWarn
                ConnectionStatus.AUTH_FAILED, ConnectionStatus.ERROR -> CrimsonError
                ConnectionStatus.UNCONFIGURED -> TextMuted
            }
            Card(colors = CardDefaults.cardColors(containerColor = SlateSurface), border = BorderStroke(1.dp, BorderSubtle)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(provider.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${statusInfo.status.name}: ${statusInfo.message}", fontSize = 12.sp, color = statusColor)
                    OutlinedTextField(
                        value = keyDraft, onValueChange = { keyDraft = it },
                        label = { Text("${provider.name} API Key") }, singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSaveApiKey(provider, keyDraft) }) { Text("Save & Verify") }
                        OutlinedButton(onClick = { onVerifyProvider(provider) }) { Text("Test") }
                        if (provider == AiProvider.OPENROUTER) {
                            OutlinedButton(onClick = onRefreshOpenRouterFree) { Text("Free Models") }
                        }
                    }
                }
            }
        }
    }
}
