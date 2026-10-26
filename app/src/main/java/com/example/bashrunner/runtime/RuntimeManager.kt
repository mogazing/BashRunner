package com.example.bashrunner.runtime

import android.content.Context
import android.net.Uri
import android.os.Build
import com.example.bashrunner.data.model.RuntimeDiagnostics
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader

class RuntimeManager(private val context: Context) {

    private val runtimeDir: File
        get() = File(context.filesDir, "runtime/bin").apply { if (!exists()) mkdirs() }

    private val stagedDir: File
        get() = File(context.filesDir, "staged").apply { if (!exists()) mkdirs() }

    val bashBinary: File
        get() = File(runtimeDir, "bash")

    val busyBoxBinary: File
        get() = File(runtimeDir, "busybox")

    /**
     * Determines current ABI and initializes bundled executables.
     * On Android 10+ (API 29+), SELinux blocks executing binaries from app writable data storage.
     * If bundled binaries fail execution validation, falls back gracefully to /system/bin/sh.
     */
    @Synchronized
    fun initializeRuntime(): RuntimeDiagnostics {
        val abi = getSupportedAbi()
        var initError: String? = null

        try {
            if (!bashBinary.exists() || !busyBoxBinary.exists() || bashBinary.length() == 0L || busyBoxBinary.length() == 0L) {
                extractBinariesForAbi(abi)
            }
            setExecutablePermissions(bashBinary)
            setExecutablePermissions(busyBoxBinary)
        } catch (e: Exception) {
            initError = "Extraction failed: ${e.message}"
        }

        val bashVersion = getBinaryVersion(bashBinary, listOf("--version"))
        val busyBoxVersion = getBinaryVersion(busyBoxBinary, listOf("--help"))
        val systemShVersion = getSystemShVersion()

        val isBundledAvailable = bashVersion != null
        val isFallbackNeeded = !isBundledAvailable
        val isReady = isBundledAvailable || systemShVersion != null

        val finalError = when {
            isBundledAvailable -> initError
            systemShVersion != null -> "Android 10+ SELinux restriction active (executing from app storage blocked). Automatically redirected to native system shell (/system/bin/sh)."
            else -> initError ?: "No executable shell available on device"
        }

        val bashVersionDisplay = bashVersion ?: (systemShVersion ?: "Unavailable")
        val busyBoxVersionDisplay = busyBoxVersion ?: "Unavailable (SELinux restricted)"

        return RuntimeDiagnostics(
            abi = abi,
            bashPath = if (isBundledAvailable) bashBinary.absolutePath else "/system/bin/sh",
            bashVersion = bashVersionDisplay,
            busyBoxPath = if (busyBoxVersion != null) busyBoxBinary.absolutePath else "/system/bin",
            busyBoxVersion = busyBoxVersionDisplay,
            isReady = isReady,
            initializationError = finalError,
            isUsingSystemShFallback = isFallbackNeeded
        )
    }

    private fun getSupportedAbi(): String {
        for (abi in Build.SUPPORTED_ABIS) {
            when (abi) {
                "arm64-v8a", "x86_64", "armeabi-v7a" -> return abi
            }
        }
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    private fun extractBinariesForAbi(abi: String) {
        val assetAbi = when (abi) {
            "arm64-v8a" -> "arm64-v8a"
            "x86_64" -> "x86_64"
            "armeabi-v7a", "armeabi" -> "armeabi-v7a"
            else -> "arm64-v8a"
        }

        copyAssetToFile("bin/$assetAbi/bash", bashBinary)
        copyAssetToFile("bin/$assetAbi/busybox", busyBoxBinary)
    }

    private fun copyAssetToFile(assetPath: String, outFile: File) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun setExecutablePermissions(file: File) {
        if (!file.exists()) return
        file.setExecutable(true, false)
        file.setReadable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", file.absolutePath)).waitFor()
        } catch (e: Exception) {
            // ignore chmod exception
        }
    }

    private fun getBinaryVersion(binary: File, args: List<String>): String? {
        if (!binary.exists()) return null
        return try {
            val cmd = mutableListOf(binary.absolutePath)
            cmd.addAll(args)
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            process.destroy()
            line?.trim()
        } catch (e: Exception) {
            null
        }
    }

    fun getSystemShVersion(): String? {
        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", "echo \"Android System Shell (${Build.VERSION.RELEASE})\"; exit 0").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            process.destroy()
            line?.trim() ?: "Android /system/bin/sh"
        } catch (e: Exception) {
            if (File("/system/bin/sh").exists()) "Android /system/bin/sh" else null
        }
    }

    /**
     * Stages script to a clean private file in context.filesDir/staged/
     */
    fun stageScript(uriOrPath: String, scriptName: String): File {
        val sanitizedName = scriptName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val stagedFile = File(stagedDir, "${System.currentTimeMillis()}_$sanitizedName")

        val inputStream: InputStream? = if (uriOrPath.startsWith("content://")) {
            context.contentResolver.openInputStream(Uri.parse(uriOrPath))
        } else {
            val file = File(uriOrPath)
            if (file.exists() && file.canRead()) file.inputStream() else null
        }

        if (inputStream == null) {
            throw IllegalArgumentException("Cannot read source script from $uriOrPath")
        }

        inputStream.use { input ->
            FileOutputStream(stagedFile).use { output ->
                input.copyTo(output)
            }
        }

        setExecutablePermissions(stagedFile)
        return stagedFile
    }

    fun cleanupStagedFiles() {
        try {
            stagedDir.listFiles()?.forEach { file ->
                if (System.currentTimeMillis() - file.lastModified() > 3600_000) { // older than 1 hour
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
