package com.example.bashrunner

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.bashrunner.data.model.AppTheme
import com.example.bashrunner.data.model.ScriptItem
import com.example.bashrunner.ui.MainViewModel
import com.example.bashrunner.ui.screens.ExecutionDialog
import com.example.bashrunner.ui.screens.FindScriptsDialog
import com.example.bashrunner.ui.screens.HomeScreen
import com.example.bashrunner.ui.screens.ScriptDetailsDialog
import com.example.bashrunner.ui.screens.SettingsScreen
import com.example.bashrunner.ui.theme.BashScriptProMaxTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val useDarkTheme = when (appTheme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            BashScriptProMaxTheme(darkTheme = useDarkTheme) {
                BashRunnerApp(
                    viewModel = viewModel,
                    onOpenDocument = { launcher ->
                        launcher.launch(arrayOf("*/*", "text/x-sh", "application/x-sh", "text/plain"))
                    }
                )
            }
        }
    }
}

@Composable
fun BashRunnerApp(
    viewModel: MainViewModel,
    onOpenDocument: (androidx.activity.result.ActivityResultLauncher<Array<String>>) -> Unit
) {
    val savedScripts by viewModel.savedScripts.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val executionMode by viewModel.executionMode.collectAsState()
    val runtimeDiagnostics by viewModel.runtimeDiagnostics.collectAsState()
    val discoveryState by viewModel.discoveryState.collectAsState()
    val executionUiState by viewModel.executionUiState.collectAsState()

    var currentScreen by remember { mutableStateOf("HOME") }
    var detailsScript by remember { mutableStateOf<ScriptItem?>(null) }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val context = viewModel.getApplication<android.app.Application>()
            var fileName = "script.sh"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: "script.sh"
                    }
                }
            }

            val added = viewModel.addScriptFromUri(uri, fileName)
            if (!added) {
                Toast.makeText(context, "Script already exists in list", Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (currentScreen) {
        "HOME" -> {
            HomeScreen(
                savedScripts = savedScripts,
                onSelectScript = { onOpenDocument(safLauncher) },
                onStartDiscovery = { viewModel.startDiscovery() },
                onRunScript = { script -> viewModel.runScript(script) },
                onToggleBookmark = { id -> viewModel.toggleBookmark(id) },
                onRemoveScript = { id -> viewModel.removeScript(id) },
                onViewDetails = { script -> detailsScript = script },
                onOpenSettings = { currentScreen = "SETTINGS" }
            )
        }

        "SETTINGS" -> {
            SettingsScreen(
                theme = appTheme,
                onThemeSelected = { viewModel.setTheme(it) },
                executionMode = executionMode,
                onExecutionModeSelected = { viewModel.setExecutionMode(it) },
                diagnostics = runtimeDiagnostics,
                onRefreshDiagnostics = { viewModel.refreshRuntimeDiagnostics() },
                onBack = { currentScreen = "HOME" }
            )
        }
    }

    // Details Dialog
    detailsScript?.let { script ->
        ScriptDetailsDialog(
            script = script,
            onDismiss = { detailsScript = null },
            onRun = { viewModel.runScript(script) }
        )
    }

    // Discovery Dialog
    if (discoveryState.isSearching || discoveryState.foundScripts.isNotEmpty() || discoveryState.errorMessage != null) {
        FindScriptsDialog(
            state = discoveryState,
            onCancel = { viewModel.cancelDiscovery() },
            onAddScripts = { scripts -> viewModel.addDiscoveredScripts(scripts) },
            onDismiss = { viewModel.dismissDiscoveryDialog() }
        )
    }

    // Execution Terminal Dialog
    if (executionUiState.isRunning || executionUiState.logs.isNotEmpty() || executionUiState.exitCode != null) {
        ExecutionDialog(
            state = executionUiState,
            onStop = { viewModel.stopExecution() },
            onDismiss = { viewModel.dismissExecutionDialog() }
        )
    }
}
