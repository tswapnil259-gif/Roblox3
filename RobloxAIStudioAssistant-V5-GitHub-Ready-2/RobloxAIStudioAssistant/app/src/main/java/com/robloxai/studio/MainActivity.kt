package com.robloxai.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.robloxai.studio.ui.MainViewModel
import com.robloxai.studio.ui.components.ProviderModelBar
import com.robloxai.studio.ui.screens.*
import com.robloxai.studio.ui.theme.RobloxAiTheme
import com.robloxai.studio.ui.theme.RobloxBlue
import com.robloxai.studio.ui.theme.SlateSurface
import com.robloxai.studio.ui.theme.TextMuted

private sealed class NavTab(val route: String, val label: String, val icon: ImageVector) {
    data object Projects : NavTab("projects", "Projects", Icons.Default.Folder)
    data object Chat : NavTab("chat", "AI Chat", Icons.Default.AutoAwesome)
    data object Editor : NavTab("editor", "Editor", Icons.Default.Code)
    data object Fixer : NavTab("fixer", "Fixer", Icons.Default.Build)
    data object Model3D : NavTab("model3d", "3D", Icons.Default.ViewInAr)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RobloxAiTheme {
                RobloxAiStudioApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobloxAiStudioApp(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val projects by vm.projects.collectAsState()
    val activeProject by vm.activeProject.collectAsState()
    val scripts by vm.projectScripts.collectAsState()
    val chat by vm.projectChat.collectAsState()
    val concepts3D by vm.projectConcepts3D.collectAsState()
    val selectedProvider by vm.selectedProvider.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val availableModels by vm.availableModels.collectAsState()
    val providerStatuses by vm.providerStatuses.collectAsState()
    val isBusy by vm.isBusy.collectAsState()
    val banner by vm.bannerMessage.collectAsState()
    val editorTitle by vm.editorTitle.collectAsState()
    val editorType by vm.editorScriptType.collectAsState()
    val editorCode by vm.editorCode.collectAsState()
    val editorNotes by vm.editorNotes.collectAsState()
    val errorAnalysis by vm.errorAnalysisResult.collectAsState()
    val errorFixedCode by vm.errorFixedCode.collectAsState()
    val latest3D by vm.latest3DOutput.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(banner) {
        banner?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearBanner()
        }
    }

    val tabs = listOf(NavTab.Projects, NavTab.Chat, NavTab.Editor, NavTab.Fixer, NavTab.Model3D)
    val isSettings = currentRoute == "settings"
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface),
                    title = {
                        Column {
                            Text("Roblox AI Studio Assistant", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(activeProject?.name ?: "No Project", fontSize = 11.sp, color = RobloxBlue)
                        }
                    },
                    navigationIcon = {
                        if (isSettings) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    },
                    actions = {
                        if (!isSettings) {
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(Icons.Default.Settings, "Settings")
                            }
                        }
                    }
                )
                if (!isSettings) {
                    ProviderModelBar(
                        selectedProvider = selectedProvider,
                        selectedModel = selectedModel,
                        availableModels = availableModels,
                        statuses = providerStatuses,
                        onSelectProvider = vm::selectProvider,
                        onSelectModel = vm::selectModel,
                        onRefreshOpenRouterFree = vm::refreshOpenRouterFreeModels,
                        onOpenSettings = { navController.navigate("settings") }
                    )
                }
            }
        },
        bottomBar = {
            if (!isSettings) {
                NavigationBar(containerColor = SlateSurface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(NavTab.Projects.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RobloxBlue,
                                selectedTextColor = RobloxBlue,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = NavTab.Projects.route, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(NavTab.Projects.route) {
                ProjectsScreen(
                    projects = projects,
                    activeProject = activeProject,
                    projectScripts = scripts,
                    onSelectProject = vm::selectProject,
                    onCreateProject = vm::createProject,
                    onUpdateNotes = vm::updateProjectNotes,
                    onDeleteProject = vm::deleteProject,
                    onOpenScriptInEditor = { sc ->
                        vm.loadScriptIntoEditor(sc)
                        navController.navigate(NavTab.Editor.route)
                    }
                )
            }
            composable(NavTab.Chat.route) {
                ChatScreen(
                    messages = chat,
                    isBusy = isBusy,
                    onSendMessage = vm::sendChatMessage,
                    onClearChat = vm::clearProjectChat,
                    onSaveCodeToEditor = { title, code ->
                        vm.saveCodeSnippetToEditor(title, code)
                        navController.navigate(NavTab.Editor.route)
                    }
                )
            }
            composable(NavTab.Editor.route) {
                EditorScreen(
                    scripts = scripts,
                    title = editorTitle,
                    scriptType = editorType,
                    code = editorCode,
                    notes = editorNotes,
                    isBusy = isBusy,
                    onFieldsChange = vm::setEditorFields,
                    onGenerateScript = vm::generateScriptToEditor,
                    onSaveScript = vm::saveCurrentScriptToProject,
                    onLoadScript = vm::loadScriptIntoEditor,
                    onDeleteScript = vm::deleteScript,
                    onBuildShareIntent = { t, type, c -> vm.createLuaShareIntent(context, t, type, c) }
                )
            }
            composable(NavTab.Fixer.route) {
                ErrorFixerScreen(
                    analysisResult = errorAnalysis,
                    fixedCode = errorFixedCode,
                    isBusy = isBusy,
                    onAnalyzeError = vm::analyzeAndFixError,
                    onSendFixedCodeToEditor = { title, code ->
                        vm.saveCodeSnippetToEditor(title, code)
                        navController.navigate(NavTab.Editor.route)
                    }
                )
            }
            composable(NavTab.Model3D.route) {
                Model3DScreen(
                    concepts = concepts3D,
                    latestOutput = latest3D,
                    isBusy = isBusy,
                    onGenerate3D = vm::generate3DConcept,
                    onDeleteConcept = vm::deleteConcept3D
                )
            }
            composable("settings") {
                SettingsScreen(
                    statuses = providerStatuses,
                    isRelayEnabled = vm.isRelayEnabled(),
                    relayUrl = vm.getRelayUrl(),
                    relayToken = vm.getRelayToken(),
                    getApiKey = vm::getStoredApiKey,
                    onSaveApiKey = vm::saveCredentialsAndVerify,
                    onVerifyProvider = vm::verifyProvider,
                    onSaveRelaySettings = vm::saveRelaySettings,
                    onRefreshOpenRouterFree = vm::refreshOpenRouterFreeModels
                )
            }
        }
    }
}
