package com.example.bashrunner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bashrunner.data.model.ScriptItem
import com.example.bashrunner.data.repository.ScriptRepository
import com.example.bashrunner.runtime.RuntimeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BashRunnerInstrumentationTest {

    @Test
    fun testScriptRepositoryPersistence() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = ScriptRepository(appContext)

        val testScript = ScriptItem(
            id = "test_inst_1",
            name = "instrumentation_test.sh",
            uriString = "file:///sdcard/instrumentation_test.sh",
            path = "/sdcard/instrumentation_test.sh",
            isBookmarked = true
        )

        // Clear existing test item if present
        repo.removeScript("test_inst_1")

        // Add
        val added = repo.addScript(testScript)
        assertTrue("Script should be added successfully", added)

        // Retrieve
        val saved = repo.getSavedScripts()
        val found = saved.find { it.id == "test_inst_1" }
        assertNotNull("Saved script should be retrieved", found)
        assertEquals("instrumentation_test.sh", found?.name)
        assertTrue("Saved script should be bookmarked", found?.isBookmarked == true)

        // Cleanup
        repo.removeScript("test_inst_1")
    }

    @Test
    fun testRuntimeManagerInitialization() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val runtimeManager = RuntimeManager(appContext)

        val diagnostics = runtimeManager.initializeRuntime()
        assertNotNull("Diagnostics should not be null", diagnostics)
        assertNotNull("ABI should be identified", diagnostics.abi)
        assertTrue("Bash binary path should exist", File(diagnostics.bashPath).parentFile?.exists() == true)
    }
}
