package com.example.bashrunner.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bashrunner.data.model.AppTheme
import com.example.bashrunner.data.model.ExecutionMode
import com.example.bashrunner.data.model.RuntimeDiagnostics
import com.example.bashrunner.execution.RootChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    executionMode: ExecutionMode,
    onExecutionModeSelected: (ExecutionMode) -> Unit,
    diagnostics: RuntimeDiagnostics?,
    onRefreshDiagnostics: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val isSuAvailable = RootChecker.isSuBinaryPresent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshDiagnostics) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Diagnostics"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AppTheme.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = theme == option,
                                onClick = { onThemeSelected(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (option) {
                                    AppTheme.SYSTEM -> "System Default"
                                    AppTheme.LIGHT -> "Light Theme"
                                    AppTheme.DARK -> "Dark Theme"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Execution Mode Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Script Execution Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExecutionMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = executionMode == mode,
                                onClick = {
                                    if (mode == ExecutionMode.ROOT && !isSuAvailable) {
                                        // Still allow selection or warn
                                        onExecutionModeSelected(mode)
                                    } else {
                                        onExecutionModeSelected(mode)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (mode) {
                                        ExecutionMode.USER -> "USER Mode (Default)"
                                        ExecutionMode.ROOT -> "ROOT Mode (su)"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (mode) {
                                        ExecutionMode.USER -> "Runs scripts under app UID with private working environment."
                                        ExecutionMode.ROOT -> if (isSuAvailable) "Runs scripts with elevated privileges via 'su'." else "Root binary ('su') not detected on this device."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (mode == ExecutionMode.ROOT && !isSuAvailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Runtime Diagnostics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Bundled Runtime Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (diagnostics != null) {
                        DiagnosticItem(
                            label = "Device Architecture (ABI):",
                            value = diagnostics.abi,
                            isSuccess = true
                        )

                        DiagnosticItem(
                            label = "Bash Interpreter:",
                            value = diagnostics.bashVersion ?: "Failed to execute",
                            isSuccess = diagnostics.bashVersion != null
                        )

                        DiagnosticItem(
                            label = "BusyBox Environment:",
                            value = diagnostics.busyBoxVersion ?: "Failed to execute",
                            isSuccess = diagnostics.busyBoxVersion != null
                        )

                        DiagnosticItem(
                            label = "Root ('su') Availability:",
                            value = if (isSuAvailable) "Binary Present" else "Not Present",
                            isSuccess = isSuAvailable
                        )

                        if (diagnostics.initializationError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Init Error: ${diagnostics.initializationError}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            text = "Checking runtime environment...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRefreshDiagnostics,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Re-verify Runtime")
                    }
                }
            }

            // Security Explanation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security & Execution Notes",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Scripts run offline and locally. Your scripts or outputs are never uploaded.\n" +
                                "• USER mode executes inside the app's sandboxed environment using private bundled binaries.\n" +
                                "• ROOT mode requires an existing root manager (e.g., Magisk). If root access is denied, execution stops safely.\n" +
                                "• Scripts are staged securely into private storage prior to execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
