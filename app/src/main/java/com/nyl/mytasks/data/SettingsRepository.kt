package com.nyl.mytasks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val dailyHour: Int = 8,
    val dailyMinute: Int = 0,
    val dueEveEnabled: Boolean = true,
    val weeklyEnabled: Boolean = true,
    val weeklyHour: Int = 18,
    val weeklyMinute: Int = 0,
    /** Multiplicateur de la taille de police (accessibilité). */
    val fontScale: Float = 1f,
    /** Quota annuel de points d'effort proposé pour les années sans valeur propre. */
    val defaultYearQuota: Int = 45,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val enabled = booleanPreferencesKey("notifications_enabled")
        val dailyHour = intPreferencesKey("daily_hour")
        val dailyMinute = intPreferencesKey("daily_minute")
        val dueEve = booleanPreferencesKey("due_eve")
        val weeklyEnabled = booleanPreferencesKey("weekly_enabled")
        val weeklyHour = intPreferencesKey("weekly_hour")
        val weeklyMinute = intPreferencesKey("weekly_minute")
        val fontScale = floatPreferencesKey("font_scale")
        val defaultYearQuota = intPreferencesKey("default_year_quota")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            notificationsEnabled = p[Keys.enabled] ?: d.notificationsEnabled,
            dailyHour = p[Keys.dailyHour] ?: d.dailyHour,
            dailyMinute = p[Keys.dailyMinute] ?: d.dailyMinute,
            dueEveEnabled = p[Keys.dueEve] ?: d.dueEveEnabled,
            weeklyEnabled = p[Keys.weeklyEnabled] ?: d.weeklyEnabled,
            weeklyHour = p[Keys.weeklyHour] ?: d.weeklyHour,
            weeklyMinute = p[Keys.weeklyMinute] ?: d.weeklyMinute,
            fontScale = p[Keys.fontScale] ?: d.fontScale,
            defaultYearQuota = p[Keys.defaultYearQuota] ?: d.defaultYearQuota,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val cur = AppSettings(
                notificationsEnabled = p[Keys.enabled] ?: true,
                dailyHour = p[Keys.dailyHour] ?: 8,
                dailyMinute = p[Keys.dailyMinute] ?: 0,
                dueEveEnabled = p[Keys.dueEve] ?: true,
                weeklyEnabled = p[Keys.weeklyEnabled] ?: true,
                weeklyHour = p[Keys.weeklyHour] ?: 18,
                weeklyMinute = p[Keys.weeklyMinute] ?: 0,
                fontScale = p[Keys.fontScale] ?: 1f,
                defaultYearQuota = p[Keys.defaultYearQuota] ?: 45,
            )
            val n = transform(cur)
            p[Keys.enabled] = n.notificationsEnabled
            p[Keys.dailyHour] = n.dailyHour
            p[Keys.dailyMinute] = n.dailyMinute
            p[Keys.dueEve] = n.dueEveEnabled
            p[Keys.weeklyEnabled] = n.weeklyEnabled
            p[Keys.weeklyHour] = n.weeklyHour
            p[Keys.weeklyMinute] = n.weeklyMinute
            p[Keys.fontScale] = n.fontScale
            p[Keys.defaultYearQuota] = n.defaultYearQuota
        }
    }
}
