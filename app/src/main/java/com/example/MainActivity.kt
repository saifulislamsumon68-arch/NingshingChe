package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppThemeMode
import com.example.data.model.ReaderPreferences
import com.example.ui.editorial.EditorialTheme
import com.example.ui.reader.EditorialReaderApp
import kotlinx.coroutines.launch

/**
 * Host activity for the public reader.
 *
 * All navigation lives in [EditorialReaderApp]; this class owns the theme
 * (system / light / dark, read from DataStore) and the edge-to-edge window.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = (application as? NinghsingCheApp) ?: NinghsingCheApp.instance

        setContent {
            val preferences by app.preferencesRepository.readerPreferences
                .collectAsStateWithLifecycle(initialValue = ReaderPreferences())
            val coroutineScope = rememberCoroutineScope()

            val darkTheme = when (preferences.appThemeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            EditorialTheme(darkTheme = darkTheme) {
                EditorialReaderApp(
                    app = app,
                    isDark = darkTheme,
                    onToggleTheme = {
                        coroutineScope.launch {
                            val nextMode = if (darkTheme) AppThemeMode.LIGHT else AppThemeMode.DARK
                            app.preferencesRepository.updateAppThemeMode(nextMode)
                        }
                    },
                    modifier = Modifier
                )
            }
        }
    }
}
