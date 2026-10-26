package com.example.bashrunner.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bashrunner.data.model.AppTheme
import com.example.bashrunner.data.model.DiscoveryState
import com.example.bashrunner.data.model.ExecutionMode
import com.example.bashrunner.data.model.ExecutionUiState
import com.example.bashrunner.data.model.LogLine
import com.example.bashrunner.data.model.RuntimeDiagnostics
import com.example.bashrunner.data.model.ScriptItem
import com.example.bashrunner.data.repository.ScriptRepository
import com.example.bashrunner.data.repository.SettingsRepository
import com.example.bashrunner.execution.ScriptExecutor
import com.example.bashrunner.execution.ScriptFinder
import com.example.bashrunner.runtime.RuntimeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scriptRepository = ScriptRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val runtimeManager = RuntimeManager(application)
    private val scriptExecutor = ScriptExecutor(application, runtimeManager)
    private val scriptFinder = ScriptFinder(application)

    private val _savedScripts = MutableStateFlow<List<ScriptItem>>(emptyList())
    val savedScripts: StateFlow<List<ScriptItem>> = _savedScripts.asStateFlow()

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _executionMode = MutableStateFlow(ExecutionMode.USER)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    private val _runtimeDiagnostics = MutableStateFlow<RuntimeDiagnostics?>(null)
    val runtimeDiagnostics: StateFlow<RuntimeDiagnostics?> = _runtimeDiagnostics.asStateFlow()

    private val _discoveryState = MutableStateFlow(DiscoveryState())
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _executionUiState = MutableStateFlow(ExecutionUiState())
    val executionUiState: StateFlow<ExecutionUiState> = _executionUiState.asStateFlow()

    private var discoveryJob: Job? = null
    private var executionJob: Job? = null

    init {
        loadSettingsAndScripts()
        refreshRuntimeDiagnostics()
    }

    private fun loadSettingsAndScripts() {
        _savedScripts.value = scriptRepository.getSavedScripts()
        _appTheme.value = settingsRepository.getTheme()
        _executionMode.value = settingsRepository.getExecutionMode()
    }

    fun refreshRuntimeDiagnostics() {
        viewModelScope.launch {
            _runtimeDiagnostics.value = runtimeManager.initializeRuntime()
        }
    }

    fun addScriptFromUri(uri: Uri, name: String): Boolean {
        val script = ScriptItem(
            id = UUID.randomUUID().toString(),
            name = name,
            uriString = uri.toString(),
            path = if (uri.scheme == "file") uri.path else null,
            isBookmarked = false,
            addedTimestamp = System.currentTimeMillis()
        )
        val added = scriptRepository.addScript(script)
        if (added) {
            _savedScripts.value = scriptRepository.getSavedScripts()
        }
        return added
    }

    fun removeScript(id: String) {
        scriptRepository.removeScript(id)
        _savedScripts.value = scriptRepository.getSavedScripts()
    }

    fun toggleBookmark(id: String) {
        scriptRepository.toggleBookmark(id)
        _savedScripts.value = scriptRepository.getSavedScripts()
    }

    fun setTheme(theme: AppTheme) {
        settingsRepository.setTheme(theme)
        _appTheme.value = theme
    }

    fun setExecutionMode(mode: ExecutionMode) {
        settingsRepository.setExecutionMode(mode)
        _executionMode.value = mode
    }

    fun startDiscovery() {
        discoveryJob?.cancel()
        _discoveryState.value = DiscoveryState(isSearching = true)

        discoveryJob = viewModelScope.launch {
            try {
                val found = scriptFinder.searchForScripts { scannedCount, foundScripts ->
                    _discoveryState.update {
                        it.copy(
                            scannedDirsCount = scannedCount,
                            foundScripts = foundScripts
                        )
                    }
                }
                _discoveryState.update {
                    it.copy(
                        isSearching = false,
                        foundScripts = found
                    )
                }
            } catch (e: Exception) {
                _discoveryState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "Search failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _discoveryState.update { it.copy(isSearching = false) }
    }

    fun addDiscoveredScripts(scripts: List<ScriptItem>) {
        var addedAny = false
        scripts.forEach { script ->
            if (scriptRepository.addScript(script)) {
                addedAny = true
            }
        }
        if (addedAny) {
            _savedScripts.value = scriptRepository.getSavedScripts()
        }
        _discoveryState.value = DiscoveryState()
    }

    fun dismissDiscoveryDialog() {
        cancelDiscovery()
        _discoveryState.value = DiscoveryState()
    }

    fun runScript(script: ScriptItem) {
        executionJob?.cancel()
        val currentMode = _executionMode.value

        _executionUiState.value = ExecutionUiState(
            scriptName = script.name,
            mode = currentMode,
            isRunning = true,
            logs = emptyList(),
            exitCode = null,
            durationMs = null,
            errorMessage = null
        )

        executionJob = viewModelScope.launch {
            val result = scriptExecutor.executeScript(script, currentMode) { logLine ->
                _executionUiState.update { state ->
                    state.copy(logs = state.logs + logLine)
                }
            }

            // Update repository with run result
            scriptRepository.updateRunResult(
                id = script.id,
                exitCode = result.exitCode,
                timestamp = System.currentTimeMillis()
            )
            _savedScripts.value = scriptRepository.getSavedScripts()

            _executionUiState.update { state ->
                state.copy(
                    isRunning = false,
                    exitCode = result.exitCode,
                    durationMs = result.durationMs,
                    errorMessage = if (result.exitCode != 0) "Script finished with exit code ${result.exitCode}" else null
                )
            }
        }
    }

    fun stopExecution() {
        scriptExecutor.stopExecution()
        executionJob?.cancel()
        executionJob = null
        _executionUiState.update { state ->
            state.copy(
                isRunning = false,
                logs = state.logs + LogLine("Execution manually stopped by user.", isError = true),
                exitCode = -1,
                errorMessage = "Terminated by user"
            )
        }
    }

    fun dismissExecutionDialog() {
        if (_executionUiState.value.isRunning) {
            stopExecution()
        }
        _executionUiState.value = ExecutionUiState()
    }
}
