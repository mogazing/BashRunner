package com.example.bashrunner.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.example.bashrunner.data.model.ScriptItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ScriptRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bash_runner_scripts_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SCRIPTS_JSON = "saved_scripts_json"

        fun isValidScriptExtension(fileName: String): Boolean {
            val lower = fileName.lowercase()
            return lower.endsWith(".sh") || lower.endsWith(".bash")
        }
    }

    @Synchronized
    fun getSavedScripts(): List<ScriptItem> {
        val jsonString = prefs.getString(KEY_SCRIPTS_JSON, null) ?: return emptyList()
        val list = mutableListOf<ScriptItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ScriptItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Untitled"),
                        uriString = obj.optString("uriString", ""),
                        path = if (obj.has("path") && !obj.isNull("path")) obj.getString("path") else null,
                        isBookmarked = obj.optBoolean("isBookmarked", false),
                        addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                        lastRunTimestamp = if (obj.has("lastRunTimestamp") && !obj.isNull("lastRunTimestamp")) obj.getLong("lastRunTimestamp") else null,
                        lastExitCode = if (obj.has("lastExitCode") && !obj.isNull("lastExitCode")) obj.getInt("lastExitCode") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    private fun saveScripts(scripts: List<ScriptItem>) {
        val jsonArray = JSONArray()
        for (item in scripts) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("uriString", item.uriString)
                put("path", item.path)
                put("isBookmarked", item.isBookmarked)
                put("addedTimestamp", item.addedTimestamp)
                put("lastRunTimestamp", item.lastRunTimestamp)
                put("lastExitCode", item.lastExitCode)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SCRIPTS_JSON, jsonArray.toString()).apply()
    }

    @Synchronized
    fun addScript(script: ScriptItem): Boolean {
        val current = getSavedScripts().toMutableList()
        // Check duplicate by URI or Path
        val duplicate = current.any {
            it.uriString.equals(script.uriString, ignoreCase = true) ||
                    (it.path != null && script.path != null && it.path.equals(script.path, ignoreCase = true))
        }
        if (duplicate) {
            return false
        }

        // Try to take persistable URI permission if it's a content URI
        if (script.uriString.startsWith("content://")) {
            try {
                val uri = Uri.parse(script.uriString)
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission take might fail if URI is not persistable or flags are missing, ignore
            }
        }

        current.add(0, script)
        saveScripts(current)
        return true
    }

    @Synchronized
    fun removeScript(id: String) {
        val current = getSavedScripts().filter { it.id != id }
        saveScripts(current)
    }

    @Synchronized
    fun toggleBookmark(id: String) {
        val current = getSavedScripts().map {
            if (it.id == id) it.copy(isBookmarked = !it.isBookmarked) else it
        }
        saveScripts(current)
    }

    @Synchronized
    fun updateRunResult(id: String, exitCode: Int, timestamp: Long) {
        val current = getSavedScripts().map {
            if (it.id == id) it.copy(lastExitCode = exitCode, lastRunTimestamp = timestamp) else it
        }
        saveScripts(current)
    }
}
