package com.robloxai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.local.ChatMessageEntity
import com.robloxai.studio.domain.RobloxPrompts
import com.robloxai.studio.ui.theme.*

@Composable
fun ChatScreen(
    messages: List<ChatMessageEntity>,
    isBusy: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onSaveCodeToEditor: (String, String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = SlateSurface) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Game Builder Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonMint)
                    TextButton(onClick = onClearChat, contentPadding = PaddingValues(4.dp)) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", fontSize = 11.sp)
                    }
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RobloxPrompts.gameBuilderTemplates) { tpl ->
                        AssistChip(onClick = { if (!isBusy) onSendMessage(tpl.prompt) }, label = { Text(tpl.title, fontSize = 12.sp) })
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (messages.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = ElevatedCard)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Unified Roblox AI Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Tap a preset or ask any Luau / Roblox design question.", fontSize = 13.sp, color = TextMuted)
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                val extracted = remember(msg.content) { if (!isUser) RobloxPrompts.extractFirstLuaBlock(msg.content) else null }
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isUser) RobloxBlue.copy(alpha = 0.16f) else ElevatedCard),
                    border = BorderStroke(1.dp, if (isUser) RobloxBlue.copy(alpha = 0.4f) else BorderSubtle),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isUser) "You" else "${msg.providerName} • ${msg.modelId}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isUser) RobloxBlue else NeonMint)
                        Text(msg.content, fontSize = 13.sp)
                        if (!isUser) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(msg.content)) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp)
                                }
                                if (extracted != null) {
                                    Button(onClick = { onSaveCodeToEditor("ChatGeneratedScript", extracted) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                        Icon(Icons.Default.Code, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("To Editor", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, placeholder = { Text("Ask AI to build a Roblox system…") }, maxLines = 4, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            FilledIconButton(enabled = !isBusy && input.isNotBlank(), onClick = { val t = input; input = ""; onSendMessage(t) }) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send")
            }
        }
    }
}
