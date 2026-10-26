package com.example.bashrunner.execution

import android.content.Context
import android.os.Environment
import com.example.bashrunner.data.model.ScriptItem
import com.example.bashrunner.data.repository.ScriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

class ScriptFinder(private val context: Context) {

    suspend fun searchForScripts(
        onProgress: (scannedDirsCount: Int, foundScripts: List<ScriptItem>) -> Unit
    ): List<ScriptItem> = withContext(Dispatchers.IO) {
        val foundList = mutableListOf<ScriptItem>()
        val visitedPaths = mutableSetOf<String>()
        var scannedCount = 0

        val rootsToSearch = mutableListOf<File>()

        // Add standard accessible roots
        try {
            val primaryStorage = Environment.getExternalStorageDirectory()
            if (primaryStorage != null && primaryStorage.exists() && primaryStorage.canRead()) {
                rootsToSearch.add(primaryStorage)
            }
        } catch (e: Exception) {
            // ignore
        }

        try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloads != null && downloads.exists() && downloads.canRead()) {
                rootsToSearch.add(downloads)
            }
        } catch (e: Exception) {
            // ignore
        }

        try {
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docs != null && docs.exists() && docs.canRead()) {
                rootsToSearch.add(docs)
            }
        } catch (e: Exception) {
            // ignore
        }

        context.getExternalFilesDirs(null).forEach { dir ->
            if (dir != null && dir.exists() && dir.canRead()) {
                rootsToSearch.add(dir)
            }
        }

        for (root in rootsToSearch.distinctBy { it.canonicalPath }) {
            coroutineContext.ensureActive()
            scanDirectoryRecursive(
                dir = root,
                depth = 0,
                maxDepth = 5,
                visitedPaths = visitedPaths,
                foundList = foundList,
                onProgress = {
                    scannedCount++
                    if (scannedCount % 5 == 0 || foundList.isNotEmpty()) {
                        onProgress(scannedCount, foundList.toList())
                    }
                }
            )
        }

        onProgress(scannedCount, foundList.toList())
        foundList
    }

    private suspend fun scanDirectoryRecursive(
        dir: File,
        depth: Int,
        maxDepth: Int,
        visitedPaths: MutableSet<String>,
        foundList: MutableList<ScriptItem>,
        onProgress: () -> Unit
    ) {
        coroutineContext.ensureActive()
        if (depth > maxDepth || !dir.exists() || !dir.canRead() || !dir.isDirectory) return

        val canonicalPath = try {
            dir.canonicalPath
        } catch (e: Exception) {
            dir.absolutePath
        }

        if (visitedPaths.contains(canonicalPath)) return
        visitedPaths.add(canonicalPath)
        onProgress()

        val files = dir.listFiles() ?: return
        for (file in files) {
            coroutineContext.ensureActive()

            // Skip hidden directories like .git or .cache
            if (file.name.startsWith(".")) continue

            if (file.isDirectory) {
                scanDirectoryRecursive(
                    dir = file,
                    depth = depth + 1,
                    maxDepth = maxDepth,
                    visitedPaths = visitedPaths,
                    foundList = foundList,
                    onProgress = onProgress
                )
            } else if (file.isFile && ScriptRepository.isValidScriptExtension(file.name)) {
                val filePath = file.absolutePath
                val fileUri = "file://$filePath"

                // Check duplicate
                if (foundList.none { it.path == filePath || it.uriString == fileUri }) {
                    foundList.add(
                        ScriptItem(
                            id = UUID.randomUUID().toString(),
                            name = file.name,
                            uriString = fileUri,
                            path = filePath,
                            isBookmarked = false,
                            addedTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}
