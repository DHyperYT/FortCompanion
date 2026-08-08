package com.dhyper.fncompanion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dhyper.fncompanion.R

// --- FORTNITE FONT SYSTEM ---
val FortniteFont = try {
    FontFamily(
        Font(R.font.fortnite, FontWeight.Normal),
        Font(R.font.fortnite, FontWeight.Bold),
        Font(R.font.fortnite, FontWeight.Black)
    )
} catch (e: Exception) {
    FontFamily.SansSerif
}

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FortniteFont,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FortniteFont,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = 1.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FortniteFont,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FortniteFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FortniteFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
