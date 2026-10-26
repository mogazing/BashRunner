package com.example.bashrunner.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.bashrunner.data.model.AppTheme
import com.example.bashrunner.data.model.ExecutionMode

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bash_runner_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_EXECUTION_MODE = "execution_mode"
    }

    fun getTheme(): AppTheme {
        val name = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun getExecutionMode(): ExecutionMode {
        val name = prefs.getString(KEY_EXECUTION_MODE, ExecutionMode.USER.name) ?: ExecutionMode.USER.name
        return try {
            ExecutionMode.valueOf(name)
        } catch (e: Exception) {
            ExecutionMode.USER
        }
    }

    fun setExecutionMode(mode: ExecutionMode) {
        prefs.edit().putString(KEY_EXECUTION_MODE, mode.name).apply()
    }
}
