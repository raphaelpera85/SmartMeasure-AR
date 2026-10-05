package com.smartmeasure.ar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E69),
    secondary = Color(0xFF4B635F),
    tertiary = Color(0xFF49617A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF58D7CD),
    secondary = Color(0xFFB2CCC7),
    tertiary = Color(0xFFB0C9E7),
)

@Composable
fun SmartMeasureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

