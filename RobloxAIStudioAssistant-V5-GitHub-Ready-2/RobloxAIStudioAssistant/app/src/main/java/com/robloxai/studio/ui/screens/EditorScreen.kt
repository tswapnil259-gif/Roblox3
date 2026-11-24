package com.robloxai.studio.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.local.ScriptEntity
import com.robloxai.studio.data.model.RobloxScriptType
import com.robloxai.studio.ui.components.LuauSyntaxHighlighter
import com.robloxai.studio.ui.theme.*

@Composable
fun EditorScreen(
    scripts: List<ScriptEntity>,
    title: String,
    scriptType: RobloxScriptType,
    code: String,
    notes: String,
    isBusy: Boolean,
    onFieldsChange: (String, RobloxScriptType, String) -> Unit,
    onGenerateScript: (String, RobloxScriptType, String, Boolean) -> Unit,
    onSaveScript: () -> Unit,
    onLoadScript: (ScriptEntity) -> Unit,
    onDeleteScript: (ScriptEntity) -> Unit,
    onBuildShareIntent: (String, RobloxScriptType, String) -> Intent
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showGen by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) context.contentResolver.openOutputStream(uri)?.use { it.write(code.toByteArray()) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (scripts.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scripts, key = { it.id }) { sc ->
                    InputChip(
                        selected = sc.title == title && sc.code == code,
                        onClick = { onLoadScript(sc) },
                        label = { Text(sc.title, fontSize = 12.sp) },
                        trailingIcon = {
                            IconButton(onClick = { onDeleteScript(sc) }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, "Delete", modifier = Modifier.size(14.dp))
                            }
                        }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { onFieldsChange(it, scriptType, code) }, label = { Text("Script Name") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { showGen = true }, enabled = !isBusy) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("AI Generate")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RobloxScriptType.entries.forEach { t ->
                FilterChip(selected = scriptType == t, onClick = { onFieldsChange(title, t, code) }, label = { Text(t.name.replace("_", " "), fontSize = 11.sp) })
            }
        }
        Text("Studio Target: ${scriptType.recommendedParent} (${scriptType.extension})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NeonMint)
        if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Surface(color = ObsidianBg, border = BorderStroke(1.dp, BorderSubtle), shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f).fillMaxWidth()) {
            val vScroll = rememberScrollState()
            val hScroll = rememberScrollState()
            BasicTextField(
                value = code,
                onValueChange = { onFieldsChange(title, scriptType, it) },
                textStyle = TextStyle(color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp),
                cursorBrush = SolidColor(RobloxBlue),
                visualTransformation = LuauSyntaxHighlighter.visualTransformation,
                modifier = Modifier.fillMaxSize().verticalScroll(vScroll).horizontalScroll(hScroll).padding(12.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSaveScript, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
            }
            OutlinedButton(onClick = {
                val safe = title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "Script" }
                exportLauncher.launch("$safe${scriptType.extension}")
            }) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
            }
            OutlinedButton(onClick = {
                context.startActivity(Intent.createChooser(onBuildShareIntent(title, scriptType, code), "Share Luau"))
            }) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
            }
            if (notes.isNotBlank()) {
                OutlinedButton(onClick = { showNotes = true }) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showGen) {
        var genTitle by remember { mutableStateOf(title.ifBlank { "CombatSystem" }) }
        var genType by remember { mutableStateOf(scriptType) }
        var spec by remember { mutableStateOf("") }
        var comments by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showGen = false },
            title = { Text("AI Luau Generator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = genTitle, onValueChange = { genTitle = it }, label = { Text("Name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RobloxScriptType.entries.forEach { t ->
                            FilterChip(selected = genType == t, onClick = { genType = t }, label = { Text(t.name.take(6), fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Describe system") }, minLines = 3)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = comments, onCheckedChange = { comments = it })
                        Text("Include comments", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(enabled = spec.isNotBlank(), onClick = { showGen = false; onGenerateScript(genTitle, genType, spec, comments) }) { Text("Generate") }
            },
            dismissButton = { TextButton(onClick = { showGen = false }) { Text("Cancel") } }
        )
    }
    if (showNotes) {
        AlertDialog(onDismissRequest = { showNotes = false }, title = { Text("Placement Notes") }, text = { Text(notes, fontSize = 13.sp) }, confirmButton = { Button(onClick = { showNotes = false }) { Text("Close") } })
    }
}
