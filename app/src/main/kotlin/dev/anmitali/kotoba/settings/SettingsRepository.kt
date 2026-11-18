package dev.anmitali.kotoba.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val KeyCornerRadiusKey = intPreferencesKey("key_corner_radius_dp")
private val NumberRowKey = booleanPreferencesKey("number_row")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<KeyboardSettings> = store.data.map { prefs ->
        KeyboardSettings(
            keyCornerRadiusDp = (prefs[KeyCornerRadiusKey] ?: KeyboardSettings.DefaultKeyCornerRadiusDp)
                .coerceIn(KeyboardSettings.MinKeyCornerRadiusDp, KeyboardSettings.MaxKeyCornerRadiusDp),
            numberRow = prefs[NumberRowKey] ?: false,
        )
    }

    suspend fun setKeyCornerRadius(dp: Int) {
        store.edit { it[KeyCornerRadiusKey] = dp }
    }

    suspend fun setNumberRow(enabled: Boolean) {
        store.edit { it[NumberRowKey] = enabled }
    }
}
