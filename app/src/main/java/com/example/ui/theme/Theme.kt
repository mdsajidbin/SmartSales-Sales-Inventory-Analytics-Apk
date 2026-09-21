package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = SmartIndigo,
  onPrimary = Color.White,
  primaryContainer = SmartIndigoLight,
  onPrimaryContainer = SmartIndigoDark,
  secondary = SmartCyan,
  onSecondary = Color.White,
  secondaryContainer = SmartCyanLight,
  onSecondaryContainer = SmartCyanDark,
  tertiary = SmartCyan,
  onTertiary = Color.White,
  background = SmartBgLight,
  onBackground = SmartTextPrimary,
  surface = SmartSurfaceLight,
  onSurface = SmartTextPrimary,
  surfaceVariant = SmartBgLight,
  onSurfaceVariant = SmartTextSecondary,
  outline = SmartCardBorder,
  error = StatusRed,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = SmartIndigo,
  onPrimary = Color.White,
  primaryContainer = SmartIndigoDark,
  onPrimaryContainer = SmartIndigoLight,
  secondary = SmartCyan,
  onSecondary = Color.White,
  secondaryContainer = SmartCyanDark,
  onSecondaryContainer = SmartCyanLight,
  background = Color(0xFF0F172A),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF1E293B),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF1E293B),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF334155),
  error = StatusRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use intentional brand theme tokens
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

