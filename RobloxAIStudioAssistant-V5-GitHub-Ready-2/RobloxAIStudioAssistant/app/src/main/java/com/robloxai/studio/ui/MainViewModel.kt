package com.robloxai.studio.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robloxai.studio.data.local.*
import com.robloxai.studio.data.model.*
import com.robloxai.studio.data.remote.AiApiService
import com.robloxai.studio.data.remote.AiResult
import com.robloxai.studio.data.security.ApiKeyVault
import com.robloxai.studio.domain.RobloxPrompts
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = ApiKeyVault(app)
    private val dao = AppDatabase.getInstance(app).studioDao()
    private val api = AiApiService()

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val projects: StateFlow<List<ProjectEntity>> = dao.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProject: StateFlow<ProjectEntity?> = combine(projects, _activeProjectId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val projectScripts: StateFlow<List<ScriptEntity>> = _activeProjectId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeScripts(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectChat: StateFlow<List<ChatMessageEntity>> = _activeProjectId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeChat(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectConcepts3D: StateFlow<List<Concept3DEntity>> = _activeProjectId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeConcepts3D(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProvider = MutableStateFlow(vault.getSelectedProvider())
    val selectedProvider: StateFlow<AiProvider> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow(vault.getSelectedModel(vault.getSelectedProvider()))
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow(_selectedProvider.value.defaultModels)
    val availableModels: StateFlow<List<ModelOption>> = _availableModels.asStateFlow()

    private val _providerStatuses = MutableStateFlow<Map<AiProvider, ProviderStatusInfo>>(emptyMap())
    val providerStatuses: StateFlow<Map<AiProvider, ProviderStatusInfo>> = _providerStatuses.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _banner = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _banner.asStateFlow()

    private val _editorTitle = MutableStateFlow("NewScript")
    val editorTitle: StateFlow<String> = _editorTitle.asStateFlow()
    private val _editorType = MutableStateFlow(RobloxScriptType.SCRIPT)
    val editorScriptType: StateFlow<RobloxScriptType> = _editorType.asStateFlow()
    private val _editorCode = MutableStateFlow("-- New Luau script\n")
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()
    private val _editorNotes = MutableStateFlow("")
    val editorNotes: StateFlow<String> = _editorNotes.asStateFlow()

    private val _errorAnalysis = MutableStateFlow("")
    val errorAnalysisResult: StateFlow<String> = _errorAnalysis.asStateFlow()
    private val _errorFixed = MutableStateFlow("")
    val errorFixedCode: StateFlow<String> = _errorFixed.asStateFlow()
    private val _latest3D = MutableStateFlow("")
    val latest3DOutput: StateFlow<String> = _latest3D.asStateFlow()

    init {
        viewModelScope.launch {
            // Ensure at least one starter project
            val current = dao.observeProjects().first()
            if (current.isEmpty()) {
                val id = dao.insertProject(
                    ProjectEntity(
                        name = "My First Obby / RPG",
                        genre = "Obby / RPG",
                        instructions = "Starter project – generate systems with AI Chat or Script Generator.",
                        progressNotes = "Created on first launch."
                    )
                )
                _activeProjectId.value = id
            } else {
                _activeProjectId.value = current.first().id
            }
            // Refresh statuses
            AiProvider.entries.forEach { verifyProvider(it) }
        }
    }

    fun clearBanner() { _banner.value = null }

    fun selectProject(id: Long) { _activeProjectId.value = id }

    fun createProject(name: String, genre: String, notes: String) = viewModelScope.launch {
        val id = dao.insertProject(ProjectEntity(name = name, genre = genre, instructions = notes, progressNotes = ""))
        _activeProjectId.value = id
        _banner.value = "Project \"$name\" created"
    }

    fun updateProjectNotes(project: ProjectEntity, instructions: String, progress: String) = viewModelScope.launch {
        dao.updateProject(project.copy(instructions = instructions, progressNotes = progress, updatedAt = System.currentTimeMillis()))
        _banner.value = "Project notes saved"
    }

    fun deleteProject(project: ProjectEntity) = viewModelScope.launch {
        dao.deleteProject(project)
        _banner.value = "Project deleted"
    }

    fun selectProvider(p: AiProvider) {
        _selectedProvider.value = p
        vault.saveSelectedProvider(p)
        _availableModels.value = p.defaultModels
        _selectedModel.value = vault.getSelectedModel(p)
    }

    fun selectModel(id: String) {
        _selectedModel.value = id
        vault.saveSelectedModel(_selectedProvider.value, id)
    }

    fun getStoredApiKey(p: AiProvider) = vault.getApiKey(p)
    fun isRelayEnabled() = vault.isRelayEnabled()
    fun getRelayUrl() = vault.getRelayUrl()
    fun getRelayToken() = vault.getRelayToken()

    fun saveRelaySettings(enabled: Boolean, url: String, token: String) {
        vault.setRelayEnabled(enabled)
        vault.saveRelayUrl(url)
        vault.saveRelayToken(token)
        _banner.value = "Relay settings saved"
    }

    fun saveCredentialsAndVerify(provider: AiProvider, key: String) = viewModelScope.launch {
        vault.saveApiKey(provider, key)
        verifyProvider(provider)
    }

    fun verifyProvider(provider: AiProvider) = viewModelScope.launch {
        val statuses = _providerStatuses.value.toMutableMap()
        statuses[provider] = ProviderStatusInfo(ConnectionStatus.CHECKING, "Checking…")
        _providerStatuses.value = statuses
        val info = api.verifyConnection(
            provider,
            vault.getApiKey(provider),
            vault.isRelayEnabled(),
            vault.getRelayUrl(),
            vault.getRelayToken()
        )
        statuses[provider] = info
        _providerStatuses.value = statuses
    }

    fun refreshOpenRouterFreeModels() = viewModelScope.launch {
        _isBusy.value = true
        when (val res = api.fetchOpenRouterFreeModels(vault.getApiKey(AiProvider.OPENROUTER))) {
            is AiResult.Success -> {
                _availableModels.value = res.data
                _banner.value = "Loaded ${res.data.size} free models"
            }
            is AiResult.Failure -> _banner.value = res.errorMessage
        }
        _isBusy.value = false
    }

    fun sendChatMessage(text: String) = viewModelScope.launch {
        val project = activeProject.value ?: return@launch
        _isBusy.value = true
        dao.insertChat(
            ChatMessageEntity(
                projectId = project.id,
                role = "user",
                content = text,
                providerName = _selectedProvider.value.displayName,
                modelId = _selectedModel.value
            )
        )
        val history = projectChat.value.map {
            ChatTurn(role = it.role, content = it.content, providerName = it.providerName, modelId = it.modelId)
        } + ChatTurn(role = "user", content = text, providerName = _selectedProvider.value.displayName, modelId = _selectedModel.value)

        val system = RobloxPrompts.unifiedAssistantSystem(project.name, project.instructions)
        val result = api.sendChat(
            provider = _selectedProvider.value,
            modelId = _selectedModel.value,
            apiKey = vault.getApiKey(_selectedProvider.value),
            systemPrompt = system,
            history = history,
            relayEnabled = vault.isRelayEnabled(),
            relayUrl = vault.getRelayUrl(),
            relayToken = vault.getRelayToken()
        )
        when (result) {
            is AiResult.Success -> {
                dao.insertChat(
                    ChatMessageEntity(
                        projectId = project.id,
                        role = "assistant",
                        content = result.data,
                        providerName = _selectedProvider.value.displayName,
                        modelId = _selectedModel.value
                    )
                )
            }
            is AiResult.Failure -> _banner.value = result.errorMessage
        }
        _isBusy.value = false
    }

    fun clearProjectChat() = viewModelScope.launch {
        activeProject.value?.let { dao.clearChat(it.id) }
    }

    fun setEditorFields(title: String, type: RobloxScriptType, code: String) {
        _editorTitle.value = title
        _editorType.value = type
        _editorCode.value = code
    }

    fun loadScriptIntoEditor(sc: ScriptEntity) {
        _editorTitle.value = sc.title
        _editorType.value = RobloxScriptType.entries.firstOrNull { it.name == sc.scriptType } ?: RobloxScriptType.SCRIPT
        _editorCode.value = sc.code
        _editorNotes.value = sc.notes
    }

    fun saveCodeSnippetToEditor(title: String, code: String) {
        _editorTitle.value = title
        _editorCode.value = code
        _editorType.value = RobloxScriptType.SCRIPT
        _editorNotes.value = "Imported from AI Chat / Error Fixer"
    }

    fun generateScriptToEditor(title: String, type: RobloxScriptType, spec: String, comments: Boolean) = viewModelScope.launch {
        _isBusy.value = true
        val prompt = RobloxPrompts.scriptGeneratorPrompt(title, type, spec, comments)
        val result = api.sendChat(
            provider = _selectedProvider.value,
            modelId = _selectedModel.value,
            apiKey = vault.getApiKey(_selectedProvider.value),
            systemPrompt = RobloxPrompts.unifiedAssistantSystem(activeProject.value?.name ?: "Project", activeProject.value?.instructions ?: ""),
            history = listOf(ChatTurn(role = "user", content = prompt, providerName = "", modelId = "")),
            relayEnabled = vault.isRelayEnabled(),
            relayUrl = vault.getRelayUrl(),
            relayToken = vault.getRelayToken()
        )
        when (result) {
            is AiResult.Success -> {
                val lua = RobloxPrompts.extractFirstLuaBlock(result.data) ?: result.data
                _editorTitle.value = title
                _editorType.value = type
                _editorCode.value = lua
                _editorNotes.value = result.data
                _banner.value = "Script generated"
            }
            is AiResult.Failure -> _banner.value = result.errorMessage
        }
        _isBusy.value = false
    }

    fun saveCurrentScriptToProject() = viewModelScope.launch {
        val project = activeProject.value ?: return@launch
        dao.upsertScript(
            ScriptEntity(
                projectId = project.id,
                title = _editorTitle.value,
                scriptType = _editorType.value.name,
                studioLocation = _editorType.value.recommendedParent,
                code = _editorCode.value,
                notes = _editorNotes.value
            )
        )
        _banner.value = "Script saved"
    }

    fun deleteScript(sc: ScriptEntity) = viewModelScope.launch {
        dao.deleteScript(sc)
    }

    fun createLuaShareIntent(context: Context, title: String, type: RobloxScriptType, code: String): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "Script" }
        val file = File(dir, "$safe${type.extension}")
        file.writeText(code)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun analyzeAndFixError(error: String, broken: String) = viewModelScope.launch {
        _isBusy.value = true
        val prompt = RobloxPrompts.errorFixerPrompt(error, broken)
        val result = api.sendChat(
            provider = _selectedProvider.value,
            modelId = _selectedModel.value,
            apiKey = vault.getApiKey(_selectedProvider.value),
            systemPrompt = "You are a Roblox Studio error diagnosis expert.",
            history = listOf(ChatTurn(role = "user", content = prompt, providerName = "", modelId = "")),
            relayEnabled = vault.isRelayEnabled(),
            relayUrl = vault.getRelayUrl(),
            relayToken = vault.getRelayToken()
        )
        when (result) {
            is AiResult.Success -> {
                _errorAnalysis.value = result.data
                _errorFixed.value = RobloxPrompts.extractFirstLuaBlock(result.data) ?: ""
            }
            is AiResult.Failure -> _banner.value = result.errorMessage
        }
        _isBusy.value = false
    }

    fun generate3DConcept(name: String, category: String, style: String, budget: String) = viewModelScope.launch {
        _isBusy.value = true
        val prompt = RobloxPrompts.model3DPrompt(name, category, style, budget)
        val result = api.sendChat(
            provider = _selectedProvider.value,
            modelId = _selectedModel.value,
            apiKey = vault.getApiKey(_selectedProvider.value),
            systemPrompt = "You are a Roblox 3D asset and prompt expert.",
            history = listOf(ChatTurn(role = "user", content = prompt, providerName = "", modelId = "")),
            relayEnabled = vault.isRelayEnabled(),
            relayUrl = vault.getRelayUrl(),
            relayToken = vault.getRelayToken()
        )
        when (result) {
            is AiResult.Success -> {
                _latest3D.value = result.data
                activeProject.value?.let { p ->
                    dao.insertConcept3D(
                        Concept3DEntity(
                            projectId = p.id,
                            title = name,
                            category = category,
                            generatorPrompt = RobloxPrompts.extractPromptBlock(result.data) ?: name,
                            conceptDetails = result.data
                        )
                    )
                }
            }
            is AiResult.Failure -> _banner.value = result.errorMessage
        }
        _isBusy.value = false
    }

    fun deleteConcept3D(c: Concept3DEntity) = viewModelScope.launch {
        dao.deleteConcept3D(c)
    }
}
