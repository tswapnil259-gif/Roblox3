package com.robloxai.studio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robloxai.studio.data.local.Concept3DEntity
import com.robloxai.studio.ui.theme.*

@Composable
fun Model3DScreen(
    concepts: List<Concept3DEntity>,
    latestOutput: String,
    isBusy: Boolean,
    onGenerate3D: (String, String, String, String) -> Unit,
    onDeleteConcept: (Concept3DEntity) -> Unit
) {
    val categories = listOf("Building", "Island", "Tree / Foliage", "House", "Environment Prop", "Weapon / Tool")
    val styles = listOf("Low-Poly Stylized", "Blocky Roblox", "Semi-Realistic PBR", "Voxel / Cartoon")
    val budgets = listOf("Ultra Low-Poly (<2k tris)", "Optimized Prop (<5k tris)", "Hero Mesh (<12k tris)")
    var assetName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var selectedStyle by remember { mutableStateOf(styles.first()) }
    var selectedBudget by remember { mutableStateOf(budgets[1]) }
    val clipboard = LocalClipboardManager.current

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text("3D Model Concept & Prompt Builder", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Generate low-poly concepts and copy-ready prompts for Meshy / Tripo / Rodin + Studio import guide.", fontSize = 12.sp, color = TextMuted)
        }
        item {
            OutlinedTextField(value = assetName, onValueChange = { assetName = it }, label = { Text("Asset Name") }, placeholder = { Text("Floating Sky Island Tavern") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RobloxBlue)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { c -> FilterChip(selected = selectedCategory == c, onClick = { selectedCategory = c }, label = { Text(c, fontSize = 11.sp) }) }
            }
        }
        item {
            Text("Art Style", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RobloxBlue)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(styles) { s -> FilterChip(selected = selectedStyle == s, onClick = { selectedStyle = s }, label = { Text(s, fontSize = 11.sp) }) }
            }
        }
        item {
            Text("Budget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RobloxBlue)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(budgets) { b -> FilterChip(selected = selectedBudget == b, onClick = { selectedBudget = b }, label = { Text(b, fontSize = 11.sp) }) }
            }
        }
        item {
            Button(enabled = !isBusy && assetName.isNotBlank(), onClick = { onGenerate3D(assetName, selectedCategory, selectedStyle, selectedBudget) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ViewInAr, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isBusy) "Generating…" else "Generate 3D Concept")
            }
            if (isBusy) { Spacer(Modifier.height(6.dp)); LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        if (latestOutput.isNotBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ElevatedCard), border = BorderStroke(1.dp, RobloxBlue.copy(alpha = 0.4f))) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Latest Concept", fontWeight = FontWeight.Bold, color = NeonMint)
                        Text(latestOutput, fontSize = 13.sp)
                    }
                }
            }
        }
        item { Text("Saved (${concepts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(concepts, key = { it.id }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = SlateSurface), border = BorderStroke(1.dp, BorderSubtle)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text(item.category, fontSize = 11.sp, color = RobloxBlue)
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(item.generatorPrompt)) }) {
                            Icon(Icons.Default.ContentCopy, "Copy", tint = NeonMint)
                        }
                        IconButton(onClick = { onDeleteConcept(item) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = CrimsonError)
                        }
                    }
                    Text(item.generatorPrompt, fontSize = 12.sp, color = TextMuted)
                }
            }
        }
    }
}
