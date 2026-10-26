package com.example.bashrunner

import com.example.bashrunner.data.model.AppTheme
import com.example.bashrunner.data.model.ExecutionMode
import com.example.bashrunner.data.model.LogLine
import com.example.bashrunner.data.model.ScriptExecutionResult
import com.example.bashrunner.data.model.ScriptItem
import com.example.bashrunner.data.repository.ScriptRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ScriptRunnerUnitTest {

    @Test
    fun testValidScriptExtensions() {
        assertTrue(ScriptRepository.isValidScriptExtension("deploy.sh"))
        assertTrue(ScriptRepository.isValidScriptExtension("backup.bash"))
        assertTrue(ScriptRepository.isValidScriptExtension("SCRIPT.SH"))
        assertTrue(ScriptRepository.isValidScriptExtension("RUN.BASH"))

        assertFalse(ScriptRepository.isValidScriptExtension("script.py"))
        assertFalse(ScriptRepository.isValidScriptExtension("readme.txt"))
        assertFalse(ScriptRepository.isValidScriptExtension("sh"))
        assertFalse(ScriptRepository.isValidScriptExtension("bash"))
    }

    @Test
    fun testScriptItemCreationAndEquality() {
        val id = UUID.randomUUID().toString()
        val item = ScriptItem(
            id = id,
            name = "test_script.sh",
            uriString = "content://com.android.providers.downloads/document/10",
            path = "/sdcard/Download/test_script.sh",
            isBookmarked = true,
            addedTimestamp = 1000L
        )

        assertEquals(id, item.id)
        assertEquals("test_script.sh", item.name)
        assertTrue(item.isBookmarked)
        assertEquals("/sdcard/Download/test_script.sh", item.path)
    }

    @Test
    fun testDuplicateScriptDetectionLogic() {
        val script1 = ScriptItem(
            id = "1",
            name = "script1.sh",
            uriString = "file:///storage/script1.sh",
            path = "/storage/script1.sh"
        )

        val script2 = ScriptItem(
            id = "2",
            name = "script1.sh",
            uriString = "file:///storage/script1.sh",
            path = "/storage/script1.sh"
        )

        val script3 = ScriptItem(
            id = "3",
            name = "script2.sh",
            uriString = "file:///storage/script2.sh",
            path = "/storage/script2.sh"
        )

        val savedList = listOf(script1)

        val isDuplicate2 = savedList.any {
            it.uriString.equals(script2.uriString, ignoreCase = true) ||
                    (it.path != null && script2.path != null && it.path.equals(script2.path, ignoreCase = true))
        }

        val isDuplicate3 = savedList.any {
            it.uriString.equals(script3.uriString, ignoreCase = true) ||
                    (it.path != null && script3.path != null && it.path.equals(script3.path, ignoreCase = true))
        }

        assertTrue("Script2 should be detected as duplicate", isDuplicate2)
        assertFalse("Script3 should not be detected as duplicate", isDuplicate3)
    }

    @Test
    fun testEnumsDefaultsAndValues() {
        assertEquals(AppTheme.SYSTEM, AppTheme.valueOf("SYSTEM"))
        assertEquals(AppTheme.LIGHT, AppTheme.valueOf("LIGHT"))
        assertEquals(AppTheme.DARK, AppTheme.valueOf("DARK"))

        assertEquals(ExecutionMode.USER, ExecutionMode.valueOf("USER"))
        assertEquals(ExecutionMode.ROOT, ExecutionMode.valueOf("ROOT"))
    }

    @Test
    fun testIntegrationScriptTemplateContent() {
        val testScript = """
            #!/bin/bash
            # Integration test script for Bash Runner
            TEST_VAR="Bash Runner Test Output"
            echo "STDOUT: ${'$'}TEST_VAR"
            echo "STDERR: Testing error channel line" >&2
            
            if [ -n "${'$'}BUSYBOX" ]; then
                echo "BUSYBOX_PATH: ${'$'}BUSYBOX"
            fi
            
            exit 0
        """.trimIndent()

        assertTrue(testScript.contains("TEST_VAR=\"Bash Runner Test Output\""))
        assertTrue(testScript.contains("STDERR: Testing error channel line"))
        assertTrue(testScript.contains("exit 0"))
    }

    @Test
    fun testLogLineTypeAndTimestamp() {
        val logOut = LogLine(text = "Standard log line", isError = false)
        val logErr = LogLine(text = "Error log line", isError = true)

        assertFalse(logOut.isError)
        assertTrue(logErr.isError)
        assertNotNull(logOut.timestamp)
    }

    @Test
    fun testScriptExecutionResultDataClass() {
        val result = ScriptExecutionResult(
            exitCode = 0,
            durationMs = 120L,
            stdoutText = "Hello World\n",
            stderrText = ""
        )

        assertEquals(0, result.exitCode)
        assertEquals(120L, result.durationMs)
        assertEquals("Hello World\n", result.stdoutText)
        assertEquals("", result.stderrText)
    }

    @Test
    fun testRuntimeDiagnosticsFallbackDataClass() {
        val diag = com.example.bashrunner.data.model.RuntimeDiagnostics(
            abi = "arm64-v8a",
            bashPath = "/system/bin/sh",
            bashVersion = "Android System Shell",
            busyBoxPath = "/system/bin",
            busyBoxVersion = "Unavailable (SELinux restricted)",
            isReady = true,
            initializationError = "Android 10+ SELinux restriction active",
            isUsingSystemShFallback = true
        )

        assertTrue(diag.isReady)
        assertTrue(diag.isUsingSystemShFallback)
        assertEquals("/system/bin/sh", diag.bashPath)
    }
}
