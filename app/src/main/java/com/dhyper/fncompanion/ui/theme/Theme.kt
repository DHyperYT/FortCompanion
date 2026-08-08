package com.dhyper.fncompanion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = SleekPrimary,
  secondary = SleekCyan,
  tertiary = SleekEmerald,
  background = SleekBackground,
  surface = SleekSurface,
  surfaceVariant = SleekSurfaceVariant,
  outline = SleekSurfaceBorder,
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onTertiary = Color.Black,
  onBackground = SleekTextPrimary,
  onSurface = SleekTextPrimary,
  onSurfaceVariant = SleekTextSecondary
)

private val LightColorScheme = DarkColorScheme

@Composable
fun FortniteCompanionTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
