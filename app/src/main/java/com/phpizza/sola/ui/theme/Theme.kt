package com.phpizza.sola.ui.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
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
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun SolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast -> lightColorScheme(
            primary = Color(0xFF172B75), onPrimary = Color.White,
            primaryContainer = Color.White, onPrimaryContainer = Color(0xFF172B75),
            secondary = Color(0xFF172B75), onSecondary = Color.White,
            secondaryContainer = Color.White, onSecondaryContainer = Color(0xFF172B75),
            background = Color.White, onBackground = Color(0xFF111111),
            surface = Color.White, onSurface = Color(0xFF111111),
            surfaceVariant = Color.White, onSurfaceVariant = Color(0xFF262626),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
            surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color.White, surfaceTint = Color.Transparent,
            outline = Color(0xFF333333), outlineVariant = Color(0xFF333333),
            error = Color(0xFF990000), errorContainer = Color.White,
            onErrorContainer = Color(0xFF990000)
        )
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalHighContrast provides highContrast) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}