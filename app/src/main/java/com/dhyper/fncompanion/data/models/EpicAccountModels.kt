package com.dhyper.fncompanion.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- EPIC OAUTH TOKEN MODEL ---
@JsonClass(generateAdapter = true)
data class EpicTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "account_id") val accountId: String,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "error") val error: String?,
    @Json(name = "errorMessage") val errorMessage: String?,
    @Json(name = "numericErrorCode") val numericErrorCode: Int?
)

// --- MCP PROFILE QUERY MODELS ---
@JsonClass(generateAdapter = true)
data class McpQueryResponse(
    val profileRevision: Long?,
    val profileId: String?,
    val profileChanges: List<McpProfileChange>?
)

@JsonClass(generateAdapter = true)
data class McpProfileChange(
    val changeType: String?,
    val profile: McpProfileData?
)

@JsonClass(generateAdapter = true)
data class McpProfileData(
    val accountId: String?,
    val profileId: String?,
    val rvn: Long?,
    val items: Map<String, McpItemData>?,
    val stats: McpStatsData?
)

@JsonClass(generateAdapter = true)
data class McpItemData(
    val templateId: String,
    val quantity: Int = 1,
    val attributes: Map<String, Any?>?
)

@JsonClass(generateAdapter = true)
data class McpStatsData(
    val attributes: Map<String, Any?>?
)

// --- PARSED ATHENA BR LOCKER ITEM ---
data class ParsedLockerItem(
    val id: String,
    val templateId: String,
    val cosmeticId: String = "",
    val category: LockerCategory,
    val name: String,
    val description: String? = null,
    val rarity: String,
    val iconUrl: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val quantity: Int = 1,
    val introduction: CosmeticIntro? = null,
    val set: CosmeticSet? = null,
    val added: String? = null,
    val previewUrl: String? = null,
    val artist: String? = null
)

enum class LockerCategory {
    OUTFIT,
    BACK_BLING,
    PICKAXE,
    GLIDER,
    CONTRAIL,
    EMOTE,
    EMOTICON,
    SPRAY,
    WRAP,
    MUSIC,
    LOADING_SCREEN,
    SIDEKICK,
    JAM_TRACK,
    BANNER,
    KICKS,
    CAR,
    CAR_DECAL,
    WHEELS,
    CAR_TRAIL,
    CAR_BOOST,
    GUITAR,
    BASS,
    DRUMS,
    KEYTAR,
    MIC,
    LEGO_BUILD,
    LEGO_DECOR,
    AURA,
    OTHER
}

data class EquippedPresetSlot(
    val category: LockerCategory,
    val slotLabel: String,
    val item: ParsedLockerItem?
)

data class EquippedPreset(
    val presetId: String = "default_preset",
    val presetName: String = "Active Preset",
    val slots: Map<LockerCategory, ParsedLockerItem?> = emptyMap()
)

// --- ACCOUNT CAREER & PAST SEASONS MODELS ---
data class AccountCareerDetails(
    val accountName: String,
    val accountId: String? = null,
    val firstPlayed: String,
    val lastPlayed: String,
    val lifetimeWins: Int,
    val seasonalWins: Int,
    val accountLevel: Int,
    val seasonalLevel: Int,
    val currentSeasonName: String,
    val currentBattlePassTier: Int,
    val pastSeasons: List<PastSeasonData>
)

@JsonClass(generateAdapter = true)
data class DeviceAuthResponse(
    val deviceId: String,
    val accountId: String,
    val secret: String,
    val userAgent: String?,
    val created: Map<String, Any?>?
)

data class PastSeasonData(
    val seasonNumber: Int,
    val seasonName: String,
    val seasonLevel: Int,
    val battlePassTier: Int,
    val seasonWins: Int,
    val hasBattlePass: Boolean
)

// --- SAVE THE WORLD (STW) MODELS ---
data class StwHeroLoadout(
    val id: String,
    val name: String,
    val commander: StwHero?,
    val teamPerk: String,
    val support: List<StwHero>,
    val isActive: Boolean = false
)

data class StwHero(
    val id: String, // Instance ID
    val templateId: String, // HID_...
    val name: String,
    val rarity: String,
    val level: Int = 1,
    val rating: Int = 0,
    val classType: String = "Soldier",
    val iconUrl: String? = null
)

data class StwMissionAlert(
    val id: String,
    val name: String,
    val zoneName: String,
    val missionType: String,
    val difficulty: Double,
    val rewards: List<StwReward>,
    val bonusRewards: List<StwReward>,
    val biome: String = "",
    val requirements: String = "",
    val modifiers: List<String> = emptyList(),
    val timeRemaining: String = ""
)

data class StwReward(
    val id: String,
    val name: String,
    val quantity: Int,
    val iconUrl: String? = null,
    val rarity: String = "Common"
)


