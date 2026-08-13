package com.dhyper.fncompanion.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PennyProfileResponse(
    @Json(name = "alternate_account_names") val alternateAccounts: List<PennyAlternateAccount>?,
    @Json(name = "has_stw") val hasStw: Boolean,
    @Json(name = "profile_summary") val profileSummary: PennyProfileSummary?,
    @Json(name = "ventures_data") val venturesData: PennyVenturesData?,
    @Json(name = "resources_summary") val resourcesSummary: PennyResourcesSummary?,
    val survivors: Map<String, PennySurvivor>?,
    @Json(name = "survivor_bonus_overview") val survivorBonusOverview: PennySurvivorBonusOverview?,
    val squads: Map<String, PennySquad>?,
    val heroes: Map<String, PennyHero>?,
    @Json(name = "weekly_supercharger") val weeklySupercharger: Map<String, Any>?,
    val defenders: Map<String, PennyDefender>?,
    val schematics: Map<String, PennySchematic>?,
    val loadouts: PennyLoadoutsContainer?,
    val achievements: Map<String, Map<String, PennyAchievement>>?,
    @Json(name = "active_quests") val activeQuests: Map<String, PennyQuestItem>?,
    @Json(name = "live_ventures_quests") val liveVenturesQuests: Map<String, PennyQuestItem>?,
    @Json(name = "live_weekly_quests") val liveWeeklyQuests: Map<String, PennyQuestItem>?,
    @Json(name = "live_wargames_quests") val liveWargamesQuests: Map<String, PennyQuestItem>?,
    @Json(name = "live_dungeons_quests") val liveDungeonsQuests: Map<String, PennyQuestItem>?,
    @Json(name = "live_stormshield_quests") val liveStormshieldQuests: Map<String, PennyQuestItem>?,
    @Json(name = "live_endurance_daily_quest") val liveEnduranceDailyQuest: Map<String, PennyQuestItem>?,
    @Json(name = "completed_quests") val completedQuests: Map<String, PennyQuestItem>?,
    @Json(name = "expeditions_data") val expeditions: Map<String, PennyExpedition>?,
    @Json(name = "daily_mission_data") val dailyMissionData: Map<String, PennyDailyMission>?,
    @Json(name = "fort_stats") val fortStats: Map<String, PennyFortStat>?
)

@JsonClass(generateAdapter = true)
data class PennyAlternateAccount(
    val id: String?,
    val displayName: String?,
    val externalAuths: Map<String, PennyExternalAuth>?
)

@JsonClass(generateAdapter = true)
data class PennyExternalAuth(
    val accountId: String?,
    val type: String?,
    val externalDisplayName: String?
)

@JsonClass(generateAdapter = true)
data class PennyProfileSummary(
    val id: Int?,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "power_level") val powerLevel: Double?,
    @Json(name = "commander_level") val commanderLevel: Int?,
    @Json(name = "account_stw_level") val accountStwLevel: Int?,
    @Json(name = "stw_collectionbook_level") val collectionBookLevel: Int?,
    @Json(name = "stw_matches_played") val matchesPlayed: Int?,
    @Json(name = "llamas_opened") val llamasOpened: Int?,
    @Json(name = "unslot_cost") val unslotCost: Int?
)

@JsonClass(generateAdapter = true)
data class PennyVenturesData(
    @Json(name = "venture_power_level") val venturePowerLevel: Int?,
    @Json(name = "current_venture_level") val currentVentureLevel: Int?,
    @Json(name = "current_level_progress") val currentLevelProgress: String?,
    @Json(name = "next_reward") val nextReward: String?,
    @Json(name = "xp_needed_for_next_level") val xpNeeded: Long?,
    @Json(name = "fort_stats") val fortStats: Map<String, Int>?,
    val quests: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class PennyResourcesSummary(
    val llamas: Map<String, PennyResource>?,
    val resources: Map<String, PennyResource>?
)

@JsonClass(generateAdapter = true)
data class PennyResource(
    val name: String?,
    val image: String?,
    val quantity: Long?
)

@JsonClass(generateAdapter = true)
data class PennySurvivor(
    val name: String?,
    val templateId: String? = null,
    @Json(name = "template_id") val fullTemplateId: String? = null,
    @Json(name = "power_level_value") val powerLevel: Int?,
    val rarity: String?,
    @Json(name = "image_link") val imageLink: String?,
    @Json(name = "set_bonus") val setBonus: String?,
    @Json(name = "set_bonus_image_link") val setBonusImage: String?,
    val personality: String?,
    @Json(name = "personality_image_link") val personalityImage: String?,
    val attributes: PennyAttributes?
)

@JsonClass(generateAdapter = true)
data class PennySurvivorBonusOverview(
    @Json(name = "active_bonuses") val activeBonuses: List<PennyActiveBonus>?,
    @Json(name = "overall_totals") val overallTotals: Map<String, PennyActiveBonus>?
)

@JsonClass(generateAdapter = true)
data class PennyActiveBonus(
    @Json(name = "bonus_name") val bonusName: String?,
    @Json(name = "survivors_required") val survivorsRequired: Int?,
    @Json(name = "matched_survivors") val matchedSurvivors: Int?,
    @Json(name = "active_bonuses") val activeBonuses: Int?,
    @Json(name = "total_bonus_pct") val totalBonusPct: Int?
)

@JsonClass(generateAdapter = true)
data class PennySquad(
    @Json(name = "squad_id") val squadId: String?,
    @Json(name = "squad_name") val squadName: String?,
    @Json(name = "lead_survivor") val leadSurvivor: PennyLeadSurvivor?,
    @Json(name = "worker_count") val workerCount: Int?,
    @Json(name = "active_bonuses") val activeBonuses: List<PennyActiveBonus>?,
    @Json(name = "bonus_details") val bonusDetails: Map<String, PennyActiveBonus>? = null
)

@JsonClass(generateAdapter = true)
data class PennyLeadSurvivor(
    @Json(name = "survivor_guid") val guid: String?,
    val name: String?,
    val rarity: String?,
    val personality: String?,
    @Json(name = "manager_synergy") val managerSynergy: String? = null
)

@JsonClass(generateAdapter = true)
data class PennyHero(
    val name: String? = null,
    val templateId: String? = null,
    @Json(name = "power_level_value") val powerLevel: Int?,
    val rarity: String?,
    @Json(name = "image_link") val imageLink: String?,
    @Json(name = "hero_class") val heroClass: String?,
    @Json(name = "hero_class_image") val heroClassImage: String?,
    val description: String?,
    @Json(name = "hero_perks") val heroPerks: PennyHeroPerks?,
    val attributes: PennyAttributes?
)

@JsonClass(generateAdapter = true)
data class PennyHeroPerks(
    @Json(name = "commander_perk") val commanderPerk: PennyPerk?,
    @Json(name = "sub_commander_perk") val subCommanderPerk: PennyPerk?,
    val abilities: List<PennyAbility>?
)

@JsonClass(generateAdapter = true)
data class PennyAttributes(
    val level: Int?,
    @Json(name = "squad_id") val squadId: String?,
    @Json(name = "squad_slot_idx") val slotIdx: Int?
)

@JsonClass(generateAdapter = true)
data class PennyPerk(
    val name: String?,
    val description: String?,
    @Json(name = "image_link") val imageLink: String?
)

@JsonClass(generateAdapter = true)
data class PennyAbility(
    val name: String?,
    @Json(name = "image_link") val imageLink: String?
)

@JsonClass(generateAdapter = true)
data class PennyDefender(
    val name: String? = null,
    val templateId: String? = null,
    @Json(name = "power_level_value") val powerLevel: Int?,
    val rarity: String?,
    @Json(name = "image_link") val imageLink: String?,
    @Json(name = "class") val defenderClass: String?,
    @Json(name = "class_type_image_link") val classImage: String?,
    val perks: List<PennyPerkSimple>?,
    val attributes: PennyAttributes?
)

@JsonClass(generateAdapter = true)
data class PennyPerkSimple(
    val name: String?
)

@JsonClass(generateAdapter = true)
data class PennySchematic(
    val name: String? = null,
    val templateId: String? = null,
    @Json(name = "power_level_value") val powerLevel: Int?,
    val rarity: String?,
    @Json(name = "image_link") val imageLink: String?,
    val description: String?,
    val perks: List<PennySchematicPerk>?,
    @Json(name = "class_image") val classImage: String?,
    @Json(name = "ammo_type") val ammoType: String?,
    @Json(name = "crafting_costs") val craftingCosts: List<PennyCraftingCost>?,
    val stats: Map<String, Any>?,
    val attributes: PennyAttributes?
)

@JsonClass(generateAdapter = true)
data class PennySchematicPerk(
    val name: String?,
    val rarity: String?
)

@JsonClass(generateAdapter = true)
data class PennyCraftingCost(
    val name: String?,
    val quantity: Int?,
    val image: String?
)

@JsonClass(generateAdapter = true)
data class PennyLoadoutsContainer(
    val loadouts: List<PennyLoadout>?,
    @Json(name = "current_loadout_guid") val currentLoadoutGuid: String?
)

@JsonClass(generateAdapter = true)
data class PennyLoadout(
    val index: Int?,
    val guid: String?,
    val commander: PennyHeroBasic?,
    @Json(name = "team_perk") val teamPerk: String?,
    @Json(name = "team_perk_image") val teamPerkImage: String?,
    val followers: List<PennyHeroBasic>?,
    @Json(name = "gadget_1") val gadget1: String?,
    @Json(name = "gadget_1_image_link") val gadget1Image: String?,
    @Json(name = "gadget_2") val gadget2: String?,
    @Json(name = "gadget_2_image_link") val gadget2Image: String?
)

@JsonClass(generateAdapter = true)
data class PennyHeroBasic(
    val name: String?,
    @Json(name = "image_link") val imageLink: String?,
    val rarity: String?,
    @Json(name = "power_level_value") val powerLevel: Int?,
    @Json(name = "hero_class") val heroClass: String?
)

@JsonClass(generateAdapter = true)
data class PennyAchievement(
    val name: String? = null,
    @Json(name = "total_required") val totalRequired: Long?,
    @Json(name = "current_value") val currentValue: Long?,
    @Json(name = "image_link") val imageLink: String?,
    val completed: PennyAchievementCompleted?
)

@JsonClass(generateAdapter = true)
data class PennyAchievementCompleted(
    @Json(name = "completed_date") val date: String?
)

@JsonClass(generateAdapter = true)
data class PennyQuestItem(
    val name: String?,
    @Json(name = "active_quest") val activeQuest: Boolean?,
    val description: String? = null,
    @Json(name = "completion_data") val completionData: Map<String, Int>? = null
)

@JsonClass(generateAdapter = true)
data class PennyExpedition(
    val name: String?,
    val quantity: Int?,
    @Json(name = "templateId") val templateId: String?
)

@JsonClass(generateAdapter = true)
data class PennyDailyMission(
    val name: String?,
    val description: String?,
    @Json(name = "total_required") val totalRequired: Int?,
    @Json(name = "current_total") val currentTotal: Int?,
    val reward: Int?,
    @Json(name = "daily_reward") val dailyReward: String?
)

@JsonClass(generateAdapter = true)
data class PennyFortStat(
    val name: String?,
    val quantity: Int?
)
