package com.wwdcqxt.wordmemory.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 用户设置（DataStore 持久化，重启后保留） */
data class UserSettings(
    val dailyGoal: Int = 20,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val lastReminderDay: String = ""
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

/**
 * 设置仓库：每日目标、每日提醒开关与时间。
 * 使用 DataStore（协程 + Flow），满足“至少一种持久化方案”中的第二种。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val LAST_REMINDER_DAY = stringPreferencesKey("last_reminder_day")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            dailyGoal = prefs[Keys.DAILY_GOAL] ?: 20,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 8,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            lastReminderDay = prefs[Keys.LAST_REMINDER_DAY] ?: ""
        )
    }

    suspend fun setDailyGoal(goal: Int) {
        context.settingsDataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(5, 200) }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun markReminderShown(day: String) {
        context.settingsDataStore.edit { it[Keys.LAST_REMINDER_DAY] = day }
    }
}
