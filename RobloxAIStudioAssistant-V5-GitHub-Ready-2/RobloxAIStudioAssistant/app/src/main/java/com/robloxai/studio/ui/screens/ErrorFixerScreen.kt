package com.robloxai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.ui.theme.*

@Composable
fun ErrorFixerScreen(
    analysisResult: String,
    fixedCode: String,
    isBusy: Boolean,
    onAnalyzeError: (String, String) -> Unit,
    onSendFixedCodeToEditor: (String, String) -> Unit
) {
    var studioError by remember { mutableStateOf("") }
    var brokenCode by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Roblox Studio Error Fixer", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Paste the red/orange error from Studio Output and optionally the broken script.", fontSize = 12.sp, color = TextMuted)
        OutlinedTextField(value = studioError, onValueChange = { studioError = it }, label = { Text("Studio Output Error") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = brokenCode, onValueChange = { brokenCode = it }, label = { Text("Broken Luau (Optional)") }, minLines = 5, modifier = Modifier.fillMaxWidth())
        Button(enabled = !isBusy && studioError.isNotBlank(), onClick = { onAnalyzeError(studioError, brokenCode) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isBusy) "Analyzing…" else "Analyze & Fix")
        }
        if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (analysisResult.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = ElevatedCard), border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Diagnosis & Solution", fontWeight = FontWeight.Bold, color = NeonMint)
                    Text(analysisResult, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(analysisResult)) }) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy")
                        }
                        if (fixedCode.isNotBlank()) {
                            Button(onClick = { onSendFixedCodeToEditor("FixedScript", fixedCode) }) {
                                Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Open Fixed")
                            }
                        }
                    }
                }
            }
        }
    }
}
