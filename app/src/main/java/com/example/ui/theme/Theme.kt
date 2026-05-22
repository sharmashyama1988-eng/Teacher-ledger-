package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryTeal,
    secondary = DarkSecondaryTeal,
    tertiary = DarkAccentOrange,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = OnDarkPrimary,
    onBackground = OnDarkBackground,
    onSurface = OnDarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryTeal,
    tertiary = BrightAccentOrange,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = OnLightPrimary,
    onBackground = OnLightBackground,
    onSurface = OnLightBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Fallback to our elegant brand colors as default for strong visual identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
