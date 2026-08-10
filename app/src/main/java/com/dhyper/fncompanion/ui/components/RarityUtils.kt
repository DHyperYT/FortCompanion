package com.dhyper.fncompanion.ui.components

import androidx.compose.ui.graphics.Color
import com.dhyper.fncompanion.ui.theme.*

fun getRarityColor(rarityName: String?): Color {
    val r = rarityName?.lowercase() ?: ""
    return when {
        // Special Series (Official Fortnite Hexes)
        r.contains("marvel") -> Color(0xFFED1D24)
        r.contains("dc") -> Color(0xFF518ECE)
        r.contains("icon") -> Color(0xFF31D2D2)
        r.contains("gaming") -> Color(0xFF5431D6)
        r.contains("star wars") || r.contains("starwars") -> Color(0xFF000000)
        r.contains("lava") -> Color(0xFFFF4500)
        r.contains("frozen") -> Color(0xFFADD8E6)
        r.contains("shadow") -> Color(0xFF363636)
        r.contains("slurp") -> Color(0xFF00FFFF)
        
        // Standard (Linked to Color.kt)
        r.contains("mythic") -> MythicColor
        r.contains("legendary") -> LegendaryColor
        r.contains("exotic") -> ExoticColor
        r.contains("epic") -> EpicColor
        r.contains("rare") -> RareColor
        r.contains("uncommon") -> UncommonColor
        else -> CommonColor
    }
}

private val STANDARD_RARITIES = setOf("common", "uncommon", "rare", "epic", "legendary", "exotic", "mythic", "transcendent")

/**
 * Returns a rank for sorting items by rarity.
 * Higher rank = higher priority (shown first).
 */
fun getRarityRank(rarityName: String?): Int {
    val r = rarityName?.lowercase() ?: ""
    if (r.isEmpty()) return 0
    
    // Check if it's a known standard rarity
    if (STANDARD_RARITIES.any { r == it }) {
        return when (r) {
            "mythic" -> 100
            "exotic" -> 90
            "legendary" -> 80
            "epic" -> 70
            "rare" -> 60
            "uncommon" -> 50
            "common" -> 40
            else -> 0
        }
    }

    // It's a special rarity (Marvel, DC, Icon, etc.)
    // Return a high rank to keep them at the top.
    return 1000
}

/**
 * Returns a high-contrast text color for a given rarity background color.
 */
fun getRarityTextColor(backgroundColor: Color): Color {
    // Luminance formula: 0.299*R + 0.587*G + 0.114*B
    val luminance = 0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue
    return if (luminance > 0.5) Color.Black else Color.White
}
