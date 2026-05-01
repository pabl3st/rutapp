package com.pabl3st.rutapp.core.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.themeDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "rutasapp_theme")

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { prefs ->
            when (prefs[KEY_THEME]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK"  -> ThemeMode.DARK
                else    -> ThemeMode.SYSTEM
            }
        }

    suspend fun setTheme(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME] = mode.name
        }
    }
}
