package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class UserTier {
    FREE,
    PRO
}

data class UserPreferences(
    val activeBoardId: String?,
    val widgetBoardId: String?,
    val hapticsEnabled: Boolean,
    val themeMode: ThemeMode,
    val isPro: Boolean
) {
    val tier: UserTier
        get() = if (isPro) UserTier.PRO else UserTier.FREE
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferenceKeys {
        val ACTIVE_BOARD_ID = stringPreferencesKey("active_board_id")
        val WIDGET_BOARD_ID = stringPreferencesKey("widget_board_id")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_PRO = booleanPreferencesKey("is_pro")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val activeBoardId = preferences[PreferenceKeys.ACTIVE_BOARD_ID]
        val widgetBoardId = preferences[PreferenceKeys.WIDGET_BOARD_ID]
        val hapticsEnabled = preferences[PreferenceKeys.HAPTICS_ENABLED] ?: true
        val themeModeString = preferences[PreferenceKeys.THEME_MODE] ?: ThemeMode.CONSOLE_DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: Exception) {
            ThemeMode.CONSOLE_DARK
        }
        val isPro = preferences[PreferenceKeys.IS_PRO] ?: false

        UserPreferences(
            activeBoardId = activeBoardId,
            widgetBoardId = widgetBoardId,
            hapticsEnabled = hapticsEnabled,
            themeMode = themeMode,
            isPro = isPro
        )
    }

    suspend fun setActiveBoardId(boardId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.ACTIVE_BOARD_ID] = boardId
        }
    }

    suspend fun setWidgetBoardId(boardId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.WIDGET_BOARD_ID] = boardId
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setProEntitlement(isPro: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_PRO] = isPro
        }
    }
}
