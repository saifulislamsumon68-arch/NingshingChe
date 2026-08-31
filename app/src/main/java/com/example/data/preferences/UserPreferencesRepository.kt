package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ningshingche_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val NOTIF_NEW_ARTICLES = booleanPreferencesKey("notif_new_articles")
        val NOTIF_FEATURED = booleanPreferencesKey("notif_featured")
    }

    val readerPreferences: Flow<ReaderPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                emit(emptyPreferences())
            }
        }
        .map { preferences ->
        val fontSize = preferences[Keys.FONT_SIZE] ?: 18f
        val lineSpacing = preferences[Keys.LINE_SPACING] ?: 1.6f
        val themeModeStr = preferences[Keys.THEME_MODE] ?: ReaderThemeMode.PAPER.name
        val themeMode = try {
            ReaderThemeMode.valueOf(themeModeStr)
        } catch (_: Exception) {
            ReaderThemeMode.PAPER
        }
        val appThemeModeStr = preferences[Keys.APP_THEME_MODE] ?: AppThemeMode.LIGHT.name
        val appThemeMode = try {
            AppThemeMode.valueOf(appThemeModeStr)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
        val ttsSpeed = preferences[Keys.TTS_SPEED] ?: 1.0f
        val notifNew = preferences[Keys.NOTIF_NEW_ARTICLES] ?: true
        val notifFeatured = preferences[Keys.NOTIF_FEATURED] ?: true

        ReaderPreferences(
            fontSizeSp = fontSize,
            lineSpacingMultiplier = lineSpacing,
            themeMode = themeMode,
            appThemeMode = appThemeMode,
            ttsSpeed = ttsSpeed,
            notificationNewArticles = notifNew,
            notificationFeatured = notifFeatured
        )
    }

    suspend fun updateFontSize(fontSize: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.FONT_SIZE] = fontSize
        }
    }

    suspend fun updateLineSpacing(spacing: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LINE_SPACING] = spacing
        }
    }

    suspend fun updateThemeMode(mode: ReaderThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateAppThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_THEME_MODE] = mode.name
        }
    }

    suspend fun updateTtsSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TTS_SPEED] = speed
        }
    }

    suspend fun updateNotificationNew(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NOTIF_NEW_ARTICLES] = enabled
        }
    }

    suspend fun updateNotificationFeatured(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NOTIF_FEATURED] = enabled
        }
    }
}
