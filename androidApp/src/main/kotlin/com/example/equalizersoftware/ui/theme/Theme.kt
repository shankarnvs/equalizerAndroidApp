package com.example.equalizersoftware.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun EqualizerTheme(
    volume: Int,
    preset: String = "Custom",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Calculate hue based on volume (0-100) and preset
    // Volume mapped to 0-120 degrees (Red to Green)
    // Preset adds an offset to the hue
    val presetOffset = when (preset) {
        "Rock" -> 0f
        "Pop" -> 90f
        "Flat" -> 180f
        else -> 270f
    }
    
    val hue = (volume * 1.2f + presetOffset) % 360f
    
    val primaryColor = Color.hsv(hue, 0.7f, 0.5f)
    val secondaryColor = Color.hsv((hue + 30f) % 360f, 0.6f, 0.6f)
    val tertiaryColor = Color.hsv((hue + 60f) % 360f, 0.5f, 0.7f)
    val backgroundColor = Color.hsv(hue, 0.1f, if (darkTheme) 0.1f else 0.95f)
    val surfaceColor = Color.hsv(hue, 0.05f, if (darkTheme) 0.15f else 0.98f)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = tertiaryColor,
            background = backgroundColor,
            surface = surfaceColor,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = tertiaryColor,
            background = backgroundColor,
            surface = surfaceColor,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.Black,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
