package com.example.bashrunner.execution

import android.content.Context
import com.example.bashrunner.data.model.ExecutionMode
import com.example.bashrunner.data.model.LogLine
import com.example.bashrunner.data.model.ScriptExecutionResult
import com.example.bashrunner.data.model.ScriptItem
import com.example.bashrunner.runtime.RuntimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ScriptExecutor(
    private val context: Context,
    private val runtimeManager: RuntimeManager
) {

    private var activeProcess: Process? = null

    suspend fun executeScript(
        script: ScriptItem,
        mode: ExecutionMode,
        onLogLine: (LogLine) -> Unit
    ): ScriptExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        // Initialize runtime
        val diagnostics = runtimeManager.initializeRuntime()
        if (!diagnostics.isReady) {
            val errMsg = "Runtime initialization failed: ${diagnostics.initializationError ?: "No shell available"}"
            onLogLine(LogLine(errMsg, isError = true))
            return@withContext ScriptExecutionResult(
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime,
                stdoutText = "",
                stderrText = errMsg
            )
        }

        // Stage script to private file
        val stagedScript: File = try {
            runtimeManager.stageScript(script.uriString, script.name)
        } catch (e: Exception) {
            val errMsg = "Failed to stage script: ${e.message}"
            onLogLine(LogLine(errMsg, isError = true))
            return@withContext ScriptExecutionResult(
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime,
                stdoutText = "",
                stderrText = errMsg
            )
        }

        val bashBin = runtimeManager.bashBinary
        val busyBoxBin = runtimeManager.busyBoxBinary
        val runtimeBinDir = bashBin.parentFile ?: context.filesDir

        // Set file permissions
        stagedScript.setReadable(true, false)
        stagedScript.setExecutable(true, false)
        bashBin.setReadable(true, false)
        bashBin.setExecutable(true, false)
        busyBoxBin.setReadable(true, false)
        busyBoxBin.setExecutable(true, false)

        var process: Process
        val isRoot = mode == ExecutionMode.ROOT

        if (isRoot) {
            val (hasRoot, rootCheckMsg) = RootChecker.checkRootAccess()
            if (!hasRoot) {
                onLogLine(LogLine("ROOT MODE ERROR: $rootCheckMsg", isError = true))
                stagedScript.delete()
                return@withContext ScriptExecutionResult(
                    exitCode = -1,
                    durationMs = System.currentTimeMillis() - startTime,
                    stdoutText = "",
                    stderrText = rootCheckMsg
                )
            }
            onLogLine(LogLine("Starting script in ROOT mode...", isError = false))

            try {
                process = ProcessBuilder("su").start()
                activeProcess = process

                // Pass script execution commands securely via stdin to su
                OutputStreamWriter(process.outputStream).use { writer ->
                    writer.write("export PATH=\"${runtimeBinDir.absolutePath}:\$PATH\"\n")
                    writer.write("export HOME=\"${context.filesDir.absolutePath}\"\n")
                    writer.write("export TMPDIR=\"${context.cacheDir.absolutePath}\"\n")
                    writer.write("export TERM=\"xterm-256color\"\n")
                    writer.write("export BUSYBOX=\"${busyBoxBin.absolutePath}\"\n")
                    writer.write("cd \"${context.filesDir.absolutePath}\"\n")
                    writer.write("if [ -x \"${bashBin.absolutePath}\" ]; then\n")
                    writer.write("    exec \"${bashBin.absolutePath}\" \"${stagedScript.absolutePath}\"\n")
                    writer.write("else\n")
                    writer.write("    exec /system/bin/sh \"${stagedScript.absolutePath}\"\n")
                    writer.write("fi\n")
                    writer.flush()
                }
            } catch (e: Exception) {
                val errMsg = "Failed to launch root process (su): ${e.message}"
                onLogLine(LogLine(errMsg, isError = true))
                stagedScript.delete()
                return@withContext ScriptExecutionResult(
                    exitCode = -1,
                    durationMs = System.currentTimeMillis() - startTime,
                    stdoutText = "",
                    stderrText = errMsg
                )
            }
        } else {
            val useSystemSh = diagnostics.isUsingSystemShFallback || !canExecuteBinaryDirectly(bashBin)
            val primaryCommand = if (useSystemSh) {
                onLogLine(LogLine("Android 10+ SELinux restriction active. Running script with native system shell (/system/bin/sh)...", isError = false))
                listOf("/system/bin/sh", stagedScript.absolutePath)
            } else {
                onLogLine(LogLine("Starting script in USER mode (bundled Bash)...", isError = false))
                listOf(bashBin.absolutePath, stagedScript.absolutePath)
            }

            try {
                val pb = ProcessBuilder(primaryCommand)
                pb.directory(context.filesDir)

                val env = pb.environment()
                val currentPath = env["PATH"] ?: "/system/bin:/system/xbin"
                env["PATH"] = "${runtimeBinDir.absolutePath}:$currentPath"
                env["HOME"] = context.filesDir.absolutePath
                env["TMPDIR"] = context.cacheDir.absolutePath
                env["TERM"] = "xterm-256color"
                env["BUSYBOX"] = busyBoxBin.absolutePath

                process = pb.start()
                activeProcess = process
            } catch (e: Exception) {
                // If bundled bash fails with permission denied on launch, retry automatically with /system/bin/sh
                if (primaryCommand.first() != "/system/bin/sh") {
                    onLogLine(LogLine("Bundled binary execution blocked by SELinux. Automatically redirecting to /system/bin/sh...", isError = true))
                    try {
                        val fallbackPb = ProcessBuilder("/system/bin/sh", stagedScript.absolutePath)
                        fallbackPb.directory(context.filesDir)

                        val env = fallbackPb.environment()
                        val currentPath = env["PATH"] ?: "/system/bin:/system/xbin"
                        env["PATH"] = "${runtimeBinDir.absolutePath}:$currentPath"
                        env["HOME"] = context.filesDir.absolutePath
                        env["TMPDIR"] = context.cacheDir.absolutePath
                        env["TERM"] = "xterm-256color"
                        env["BUSYBOX"] = busyBoxBin.absolutePath

                        process = fallbackPb.start()
                        activeProcess = process
                    } catch (fallbackEx: Exception) {
                        val errMsg = "Failed to launch script process: ${fallbackEx.message}"
                        onLogLine(LogLine(errMsg, isError = true))
                        stagedScript.delete()
                        return@withContext ScriptExecutionResult(
                            exitCode = -1,
                            durationMs = System.currentTimeMillis() - startTime,
                            stdoutText = "",
                            stderrText = errMsg
                        )
                    }
                } else {
                    val errMsg = "Failed to launch script process: ${e.message}"
                    onLogLine(LogLine(errMsg, isError = true))
                    stagedScript.delete()
                    return@withContext ScriptExecutionResult(
                        exitCode = -1,
                        durationMs = System.currentTimeMillis() - startTime,
                        stdoutText = "",
                        stderrText = errMsg
                    )
                }
            }
        }

        // Concurrently read stdout and stderr
        coroutineScope {
            launch(Dispatchers.IO) {
                try {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        stdoutBuilder.append(currentLine).append("\n")
                        onLogLine(LogLine(currentLine, isError = false))
                    }
                } catch (e: Exception) {
                    // process closed or interrupted
                }
            }

            launch(Dispatchers.IO) {
                try {
                    val reader = BufferedReader(InputStreamReader(process.errorStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        stderrBuilder.append(currentLine).append("\n")
                        onLogLine(LogLine(currentLine, isError = true))
                    }
                } catch (e: Exception) {
                    // process closed or interrupted
                }
            }
        }

        val exitCode = try {
            process.waitFor()
        } catch (e: Exception) {
            -1
        } finally {
            activeProcess = null
            stagedScript.delete()
        }

        val durationMs = System.currentTimeMillis() - startTime
        onLogLine(
            LogLine(
                "Process finished with exit code $exitCode (Duration: ${durationMs}ms)",
                isError = exitCode != 0
            )
        )

        ScriptExecutionResult(
            exitCode = exitCode,
            durationMs = durationMs,
            stdoutText = stdoutBuilder.toString(),
            stderrText = stderrBuilder.toString()
        )
    }

    private fun canExecuteBinaryDirectly(file: File): Boolean {
        if (!file.exists()) return false
        return try {
            val process = ProcessBuilder(file.absolutePath, "--version").redirectErrorStream(true).start()
            val exitCode = process.waitFor()
            process.destroy()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun stopExecution() {
        try {
            activeProcess?.destroyForcibly()
            activeProcess = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
