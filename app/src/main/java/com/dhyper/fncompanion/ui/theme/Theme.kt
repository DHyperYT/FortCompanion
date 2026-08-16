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
    accentColor: String = "Cyan",
    content: @Composable () -> Unit,
) {
    val selectedAccent = when (accentColor) {
        "Primary" -> SleekPrimary
        "Emerald" -> SleekEmerald
        "Gold" -> FortniteGold
        "Purple" -> FortnitePurple
        "Orange" -> LegendaryColor
        "Pink" -> SleekAccent
        "Red" -> Color(0xFFEF4444)
        "Blue" -> Color(0xFF3B82F6)
        else -> SleekCyan
    }

    val colorScheme = darkColorScheme(
        primary = selectedAccent,
        secondary = selectedAccent,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
