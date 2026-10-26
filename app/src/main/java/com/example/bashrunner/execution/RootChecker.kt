package com.example.bashrunner.execution

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootChecker {

    private val SU_PATHS = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    fun isSuBinaryPresent(): Boolean {
        for (path in SU_PATHS) {
            if (File(path).exists()) return true
        }
        val pathEnv = System.getenv("PATH") ?: ""
        for (dir in pathEnv.split(":")) {
            if (File(dir, "su").exists()) return true
        }
        return false
    }

    fun checkRootAccess(): Pair<Boolean, String> {
        if (!isSuBinaryPresent()) {
            return Pair(
                false,
                "Root binary ('su') not found on system PATH. Root execution requires a rooted device (e.g., Magisk / KernelSU / APatch)."
            )
        }

        return try {
            val process = ProcessBuilder("su", "-c", "id").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            val exitCode = process.waitFor()

            if (exitCode == 0 && output.contains("uid=0")) {
                Pair(true, "Root access granted (uid=0). Output: $output")
            } else if (exitCode == 0) {
                Pair(true, "Root shell executed successfully. Output: $output")
            } else {
                Pair(
                    false,
                    "Root request denied or failed with exit code $exitCode. Make sure root permissions are allowed for Bash Runner in your root manager."
                )
            }
        } catch (e: Exception) {
            Pair(
                false,
                "Failed to execute 'su' process: ${e.message}. Device may not be rooted or 'su' access was blocked."
            )
        }
    }
}
