package com.dhyper.fncompanion.ui.utils

object SeasonUtils {
    
    fun getGlobalSeasonNumber(chapter: Int, season: Int): Int {
        val chapterStarts = mapOf(
            1 to 1,
            2 to 11,
            3 to 19,
            4 to 23,
            5 to 28,
            6 to 33,
            7 to 39
        )
        val start = chapterStarts[chapter] ?: return 1
        return start + season - 1
    }

    fun formatSeasonName(seasonNum: Int): String {
        return when {
            seasonNum in 1..10 -> "Chapter 1 Season $seasonNum"
            seasonNum in 11..18 -> "Chapter 2 Season ${seasonNum - 10}"
            seasonNum in 19..22 -> "Chapter 3 Season ${seasonNum - 18}"
            seasonNum in 23..26 -> "Chapter 4 Season ${seasonNum - 22}"
            seasonNum == 27 -> "Chapter 4 Season OG"
            seasonNum in 28..31 -> "Chapter 5 Season ${seasonNum - 27}"
            seasonNum == 32 -> "Chapter 2 Season Remix"
            seasonNum in 33..34 -> "Chapter 6 Season ${seasonNum - 32}"
            seasonNum == 35 -> "Chapter 6 Mini-Season 1"
            seasonNum == 36 -> "Chapter 6 Season 3"
            seasonNum == 37 -> "Chapter 6 Season 4"
            seasonNum == 38 -> "Chapter 6 Mini-Season 2"
            seasonNum in 39..42 -> "Chapter 7 Season ${seasonNum - 38}"
            else -> "Season $seasonNum"
        }
    }
    
    fun getGlobalSeasonNumber(chapterStr: String?, seasonStr: String?): Int {
        if (chapterStr == "2" && seasonStr?.contains("Remix", ignoreCase = true) == true) {
            return 32
        }
        if (chapterStr == "4" && (seasonStr?.contains("OG", ignoreCase = true) == true || seasonStr == "5")) {
            return 27
        }
        val chapter = chapterStr?.toIntOrNull() ?: 1
        val season = when {
            seasonStr?.equals("X", ignoreCase = true) == true -> 10
            seasonStr?.equals("OG", ignoreCase = true) == true -> 5
            else -> seasonStr?.toIntOrNull() ?: 1
        }
        return getGlobalSeasonNumber(chapter, season)
    }

    fun getFormattedIntroduction(chapterStr: String?, seasonStr: String?): String {
        return formatSeasonName(getGlobalSeasonNumber(chapterStr, seasonStr))
    }
}
