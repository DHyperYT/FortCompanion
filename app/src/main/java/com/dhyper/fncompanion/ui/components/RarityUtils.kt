package com.dhyper.fncompanion.ui.components

import androidx.compose.ui.graphics.Color
import com.dhyper.fncompanion.ui.theme.CommonColor
import com.dhyper.fncompanion.ui.theme.EpicColor
import com.dhyper.fncompanion.ui.theme.ExoticColor
import com.dhyper.fncompanion.ui.theme.LegendaryColor
import com.dhyper.fncompanion.ui.theme.MythicColor
import com.dhyper.fncompanion.ui.theme.RareColor
import com.dhyper.fncompanion.ui.theme.UncommonColor

fun getRarityColor(rarityName: String?): Color {
    val r = rarityName?.lowercase() ?: ""
    return when {
        r.contains("mythic") || r.contains("ur") -> MythicColor
        r.contains("legendary") || r.contains("sr") -> LegendaryColor
        r.contains("epic") || r.contains("vr") -> EpicColor
        r.contains("rare") || r.contains("r") -> RareColor
        r.contains("uncommon") || r.contains("uc") -> UncommonColor
        r.contains("exotic") -> ExoticColor
        else -> CommonColor
    }
}
