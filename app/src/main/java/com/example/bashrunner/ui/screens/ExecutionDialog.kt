package com.example.bashrunner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bashrunner.data.model.ExecutionMode
import com.example.bashrunner.data.model.ExecutionUiState

@Composable
fun ExecutionDialog(
    state: ExecutionUiState,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom as new logs arrive
    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!state.isRunning) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.isRunning,
            dismissOnClickOutside = !state.isRunning
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = state.scriptName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (state.mode == ExecutionMode.ROOT)
                                            MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        text = state.mode.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (state.isRunning) {
                                    Text(
                                        text = "Running...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (state.exitCode != null) {
                                    Text(
                                        text = if (state.exitCode == 0) "Completed" else "Failed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state.exitCode == 0) Color(0xFF00E676) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    if (state.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Terminal Output Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0D0D0D), shape = MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    if (state.logs.isEmpty() && state.isRunning) {
                        Text(
                            text = "Initializing process environment...",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        items(state.logs) { log ->
                            Text(
                                text = log.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (log.isError) Color(0xFFFF5252) else Color(0xFF00E676),
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Status & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        if (state.durationMs != null) {
                            Text(
                                text = "Elapsed: ${String.format("%.2f", state.durationMs / 1000.0)}s",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (state.exitCode != null) {
                            Text(
                                text = "Exit Code: ${state.exitCode}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.exitCode == 0) Color(0xFF00E676) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (state.isRunning) {
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Process")
                        }
                    } else {
                        Button(onClick = onDismiss) {
                            Text("Close Log")
                        }
                    }
                }
            }
        }
    }
}
