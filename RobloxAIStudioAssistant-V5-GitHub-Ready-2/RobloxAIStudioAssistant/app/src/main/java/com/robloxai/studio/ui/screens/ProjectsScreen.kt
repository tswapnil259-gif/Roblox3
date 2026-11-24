package com.robloxai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.local.ProjectEntity
import com.robloxai.studio.data.local.ScriptEntity
import com.robloxai.studio.ui.theme.*

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    activeProject: ProjectEntity?,
    projectScripts: List<ScriptEntity>,
    onSelectProject: (Long) -> Unit,
    onCreateProject: (String, String, String) -> Unit,
    onUpdateNotes: (ProjectEntity, String, String) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit,
    onOpenScriptInEditor: (ScriptEntity) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var instructions by remember(activeProject?.id) { mutableStateOf(activeProject?.instructions.orEmpty()) }
    var progress by remember(activeProject?.id) { mutableStateOf(activeProject?.progressNotes.orEmpty()) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ElevatedCard), border = BorderStroke(1.dp, RobloxBlue.copy(alpha = 0.4f)), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = RobloxBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Roblox Studio runs on Windows/macOS. Generate Luau here and export .lua files into Studio.", fontSize = 12.sp, color = TextMuted)
                }
            }
        }
        item {
            Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Roblox Projects", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { showCreate = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }
        items(projects, key = { it.id }) { proj ->
            val selected = proj.id == activeProject?.id
            Card(onClick = { onSelectProject(proj.id) }, colors = CardDefaults.cardColors(containerColor = if (selected) ElevatedCard else SlateSurface), border = if (selected) BorderStroke(1.5.dp, RobloxBlue) else BorderStroke(1.dp, BorderSubtle)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier = Modifier.weight(1f)) {
                        Text(proj.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Genre: ${proj.genre}", fontSize = 12.sp, color = RobloxBlue)
                    }
                    if (projects.size > 1) {
                        IconButton(onClick = { onDeleteProject(proj) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = CrimsonError)
                        }
                    }
                }
            }
        }
        activeProject?.let { active ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SlateSurface), border = BorderStroke(1.dp, BorderSubtle)) {
                    Column(Modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Active: ${active.name}", fontWeight = FontWeight.Bold, color = NeonMint)
                        OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("AI Instructions") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = progress, onValueChange = { progress = it }, label = { Text("Progress Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { onUpdateNotes(active, instructions, progress) }, modifier = Modifier.align(Alignment.End)) { Text("Save Notes") }
                    }
                }
            }
            item { Text("Scripts (${projectScripts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(projectScripts, key = { it.id }) { sc ->
                Card(onClick = { onOpenScriptInEditor(sc) }, colors = CardDefaults.cardColors(containerColor = ElevatedCard), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(sc.title, fontWeight = FontWeight.SemiBold)
                        Text("${sc.scriptType} • ${sc.studioLocation}", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var genre by remember { mutableStateOf("Simulator / RPG") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New Roblox Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") })
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Instructions") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = { onCreateProject(name, genre, notes); showCreate = false }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}
