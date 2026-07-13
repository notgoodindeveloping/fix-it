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

private val DarkColorScheme =
  darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = ElegantBackground,
    surface = ElegantSurface,
    onBackground = ElegantTextPrimary,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantSurface,
    onSurfaceVariant = ElegantTextSecondary,
    outline = ElegantOutline
  )

private val LightColorScheme =
  darkColorScheme( // Map light to dark too, or keep dark as default for "Elegant Dark" app theme!
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = ElegantBackground,
    surface = ElegantSurface,
    onBackground = ElegantTextPrimary,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantSurface,
    onSurfaceVariant = ElegantTextSecondary,
    outline = ElegantOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for Elegant Dark
  dynamicColor: Boolean = false, // Set to false to use our custom Elegant Dark palette consistently
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
