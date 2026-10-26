package com.example.bashrunner.data.model

data class ScriptItem(
    val id: String,
    val name: String,
    val uriString: String,
    val path: String? = null,
    val isBookmarked: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val lastRunTimestamp: Long? = null,
    val lastExitCode: Int? = null
)

enum class ExecutionMode {
    USER,
    ROOT
}

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

data class LogLine(
    val text: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScriptExecutionResult(
    val exitCode: Int,
    val durationMs: Long,
    val stdoutText: String,
    val stderrText: String
)

data class RuntimeDiagnostics(
    val abi: String,
    val bashPath: String,
    val bashVersion: String?,
    val busyBoxPath: String,
    val busyBoxVersion: String?,
    val isReady: Boolean,
    val initializationError: String? = null,
    val isUsingSystemShFallback: Boolean = false
)

data class DiscoveryState(
    val isSearching: Boolean = false,
    val scannedDirsCount: Int = 0,
    val foundScripts: List<ScriptItem> = emptyList(),
    val errorMessage: String? = null
)

data class ExecutionUiState(
    val scriptName: String = "",
    val mode: ExecutionMode = ExecutionMode.USER,
    val isRunning: Boolean = false,
    val logs: List<LogLine> = emptyList(),
    val exitCode: Int? = null,
    val durationMs: Long? = null,
    val errorMessage: String? = null
)
