package com.example.bashrunner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bashrunner.data.model.DiscoveryState
import com.example.bashrunner.data.model.ScriptItem

@Composable
fun FindScriptsDialog(
    state: DiscoveryState,
    onCancel: () -> Unit,
    onAddScripts: (List<ScriptItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedScripts = remember(state.foundScripts) {
        mutableStateListOf<ScriptItem>().apply {
            addAll(state.foundScripts)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (state.isSearching) "Searching for Scripts..." else "Discovered Scripts")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (state.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scanned ${state.scannedDirsCount} directories...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Found ${state.foundScripts.size} script(s)",
                    style = MaterialTheme.typography.titleMedium
                )

                if (state.foundScripts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(state.foundScripts, key = { it.id }) { script ->
                            val isChecked = selectedScripts.contains(script)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (!selectedScripts.contains(script)) selectedScripts.add(script)
                                            } else {
                                                selectedScripts.remove(script)
                                            }
                                        }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = script.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = script.path ?: script.uriString,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                if (state.isSearching) {
                    TextButton(onClick = onCancel) {
                        Text("Stop Search")
                    }
                } else if (state.foundScripts.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onAddScripts(selectedScripts.toList())
                        },
                        enabled = selectedScripts.isNotEmpty()
                    ) {
                        Text("Add Selected (${selectedScripts.size})")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
