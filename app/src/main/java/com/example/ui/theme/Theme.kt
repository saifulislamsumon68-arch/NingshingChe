package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightPortalScheme = lightColorScheme(
    primary = PortalSaffron,
    onPrimary = Color.White,
    primaryContainer = PortalCream1,
    onPrimaryContainer = PortalMaroon,
    secondary = PortalMaroon,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E0D6),
    onSecondaryContainer = PortalMaroon,
    tertiary = PortalGold,
    onTertiary = PortalDeepBrown,
    background = PortalCream2,
    onBackground = PortalDeepBrown,
    surface = PortalWhite,
    onSurface = PortalDeepBrown,
    surfaceVariant = PortalCream1,
    onSurfaceVariant = Color(0xFF6B534C),
    outline = Color(0xFFE4D5C6),
    outlineVariant = Color(0xFFF0E6DA),
    inverseSurface = PortalDarkSurface,
    inverseOnSurface = PortalDarkText,
    inversePrimary = PortalSaffron,
    error = Color(0xFFB42318)
)

private val DarkPortalScheme = darkColorScheme(
    primary = PortalSaffron,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2414),
    onPrimaryContainer = Color(0xFFFFE7C2),
    secondary = Color(0xFFF3D1C4),
    onSecondary = PortalDarkBg,
    secondaryContainer = Color(0xFF2A1A16),
    onSecondaryContainer = Color(0xFFF6E7DF),
    tertiary = PortalGold,
    onTertiary = PortalDarkBg,
    background = PortalDarkBg,
    onBackground = PortalDarkText,
    surface = PortalDarkSurface,
    onSurface = PortalDarkText,
    surfaceVariant = PortalDarkVariant,
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = PortalDarkBorder,
    outlineVariant = Color(0xFF374151),
    inverseSurface = PortalCream2,
    inverseOnSurface = PortalDeepBrown,
    inversePrimary = PortalMaroon,
    error = Color(0xFFF97066)
)

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 320)
    @Composable
    fun c(color: Color) = animateColorAsState(color, spec, label = "theme").value
    return target.copy(
        primary = c(target.primary),
        onPrimary = c(target.onPrimary),
        primaryContainer = c(target.primaryContainer),
        onPrimaryContainer = c(target.onPrimaryContainer),
        secondary = c(target.secondary),
        onSecondary = c(target.onSecondary),
        secondaryContainer = c(target.secondaryContainer),
        onSecondaryContainer = c(target.onSecondaryContainer),
        tertiary = c(target.tertiary),
        onTertiary = c(target.onTertiary),
        background = c(target.background),
        onBackground = c(target.onBackground),
        surface = c(target.surface),
        onSurface = c(target.onSurface),
        surfaceVariant = c(target.surfaceVariant),
        onSurfaceVariant = c(target.onSurfaceVariant),
        outline = c(target.outline),
        outlineVariant = c(target.outlineVariant)
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val target = if (darkTheme) DarkPortalScheme else LightPortalScheme
    val colorScheme = animateScheme(target)

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
            ?: (view.context as? android.content.ContextWrapper)?.let {
                var ctx: android.content.Context? = it
                while (ctx is android.content.ContextWrapper) {
                    if (ctx is Activity) break
                    ctx = ctx.baseContext
                }
                ctx as? Activity
            }
        activity?.window?.let { window ->
            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EditorialTypography,
        content = content
    )
}
