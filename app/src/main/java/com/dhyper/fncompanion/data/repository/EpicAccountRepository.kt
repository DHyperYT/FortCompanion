package com.dhyper.fncompanion.data.repository

import com.dhyper.fncompanion.data.api.ApiClient
import com.dhyper.fncompanion.data.models.AccountCareerDetails
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.data.models.LockerCategory
import com.dhyper.fncompanion.data.models.ParsedLockerItem
import com.dhyper.fncompanion.data.models.PastSeasonData
import com.dhyper.fncompanion.data.models.StwHomebaseData
import com.dhyper.fncompanion.data.models.StwResearchStatus
import com.dhyper.fncompanion.data.models.FortStats
import com.dhyper.fncompanion.data.models.StwHero
import com.dhyper.fncompanion.data.models.StwHeroLoadout
import com.dhyper.fncompanion.data.models.StwMissionAlert
import com.dhyper.fncompanion.data.models.StwReward
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class EpicAccountRepository {
    private val api = ApiClient.epicApi
    private val publicApi = ApiClient.publicApi

    private var cosmeticsCacheMap: Map<String, CosmeticItem>? = null

    fun clearCache() {
        cosmeticsCacheMap = null
    }

    private suspend fun getCosmeticsMap(): Map<String, CosmeticItem> {
        cosmeticsCacheMap?.let { return it }
        val result = FortniteRepository().fetchAllCosmetics()
        return result.fold(
            onSuccess = { list ->
                val map = list.associateBy { it.id.lowercase() }
                cosmeticsCacheMap = map
                map
            },
            onFailure = { emptyMap() }
        )
    }

    suspend fun fetchEquippedSkinIcon(accessToken: String, accountId: String): String? {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "athena"
            )
            val profile = response.profileChanges?.firstOrNull()?.profile
            val items = profile?.items ?: emptyMap()
            val stats = profile?.stats?.attributes ?: emptyMap()

            // 1. Find the active loadout
            val loadouts = items.filter { it.value.templateId.startsWith("AthenaCosmeticLoadout:", ignoreCase = true) }
            val activeLoadoutId = stats["active_loadout_id"]?.toString() ?: loadouts.keys.firstOrNull()
            
            val loadout = items[activeLoadoutId]
            val characterId = loadout?.attributes?.get("character_slot")?.toString() ?: ""
            
            val characterItem = items[characterId]
            val templateId = characterItem?.templateId ?: "AthenaCharacter:CID_001_Athena_Character_Default"
            
            val cosmeticId = extractCosmeticId(templateId)
            val apiMap = getCosmeticsMap()
            val apiDetails = cosmeticId?.let { apiMap[it.lowercase()] }
            
            apiDetails?.images?.icon ?: apiDetails?.images?.smallIcon
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchVBucksBalance(accessToken: String, accountId: String): Result<Long> {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "common_core"
            )
            val items = response.profileChanges?.firstOrNull()?.profile?.items
            var totalVbucks = 0L

            items?.values?.forEach { item ->
                if (item.templateId.startsWith("Currency:Mtx", ignoreCase = true)) {
                    totalVbucks += item.quantity
                }
            }
            Result.success(totalVbucks)
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP ${e.code()} fetching V-Bucks: ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error fetching V-Bucks: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error fetching V-Bucks: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchPersonalLockerCosmetics(
        accessToken: String,
        accountId: String
    ): Result<List<ParsedLockerItem>> = coroutineScope {
        return@coroutineScope try {
            // 1. Fetch from all relevant profiles in parallel
            val profiles = listOf("athena", "common_core", "delmar", "spark", "juno")
            val deferredResponses = profiles.map { profileId ->
                async {
                    try {
                        api.queryMcpProfile(
                            bearerToken = "Bearer $accessToken",
                            accountId = accountId,
                            profileId = profileId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val responses = deferredResponses.awaitAll()
            val apiMap = getCosmeticsMap()
            val groupedMap = mutableMapOf<String, ParsedLockerItem>()

            responses.filterNotNull().forEach { response ->
                val itemsMap = response.profileChanges?.firstOrNull()?.profile?.items ?: return@forEach

                itemsMap.forEach { (guid, itemData) ->
                    val category = determineLockerCategory(itemData.templateId)
                    if (category != LockerCategory.OTHER) {
                        val isFav = (itemData.attributes?.get("favorite") as? Boolean) ?: false
                        val isArchived = (itemData.attributes?.get("archived") as? Boolean) ?: false
                        val cosmeticId = extractCosmeticId(itemData.templateId)
                        val apiDetails = cosmeticId?.let { apiMap[it.lowercase()] }

                        val name = apiDetails?.name ?: cleanCosmeticName(itemData.templateId)
                        val description = apiDetails?.description
                        
                        // Normalize Rarity for grouped sorting
                        val rawRarity = apiDetails?.series?.value ?: apiDetails?.rarity?.value ?: determineRarity(itemData.templateId, itemData.attributes)
                        val rarity = normalizeRarity(rawRarity)

                        val iconUrl = when (category) {
                            LockerCategory.LEGO_BUILD, LockerCategory.LEGO_DECOR -> 
                                apiDetails?.images?.legoSmall ?: 
                                apiDetails?.images?.lego?.small ?: 
                                apiDetails?.images?.lego?.large ?: 
                                apiDetails?.images?.lego?.icon ?: 
                                apiDetails?.images?.large ?:
                                apiDetails?.images?.small ?:
                                apiDetails?.images?.icon
                            LockerCategory.JAM_TRACK -> 
                                apiDetails?.images?.coverart ?: 
                                apiDetails?.images?.albumArt ?: 
                                apiDetails?.images?.other?.albumArt ?: 
                                apiDetails?.images?.featured
                            LockerCategory.CAR, LockerCategory.WHEELS, LockerCategory.CAR_TRAIL, LockerCategory.CAR_BOOST, LockerCategory.CAR_DECAL ->
                                apiDetails?.images?.icon ?: 
                                apiDetails?.images?.smallIcon ?: 
                                apiDetails?.images?.featured ?: 
                                apiDetails?.images?.decal ?:
                                apiDetails?.images?.large
                            LockerCategory.GUITAR, LockerCategory.BASS, LockerCategory.DRUMS, LockerCategory.KEYTAR, LockerCategory.MIC ->
                                apiDetails?.images?.large ?:
                                apiDetails?.images?.small ?:
                                apiDetails?.images?.featured ?:
                                apiDetails?.images?.icon
                            else -> 
                                apiDetails?.images?.icon ?: 
                                apiDetails?.images?.smallIcon ?: 
                                apiDetails?.images?.featured
                        } ?: apiDetails?.images?.icon_background ?: apiDetails?.images?.other?.background ?: apiDetails?.images?.background ?: apiDetails?.images?.full_background

                        val largeIconUrl = apiDetails?.images?.large ?: 
                                          apiDetails?.images?.featured ?: 
                                          apiDetails?.images?.icon_background ?: 
                                          apiDetails?.images?.full_background ?: 
                                          iconUrl

                        val backgroundUrl = apiDetails?.images?.other?.background ?: 
                                           apiDetails?.images?.background ?: 
                                           apiDetails?.images?.full_background

                        val legoIconUrl = apiDetails?.images?.lego?.large ?: 
                                         apiDetails?.images?.lego?.small ?: 
                                         apiDetails?.images?.legoLarge ?: 
                                         apiDetails?.images?.legoSmall

                        val beanIconUrl = apiDetails?.images?.bean?.large ?: 
                                         apiDetails?.images?.bean?.small

                    val existing = groupedMap[itemData.templateId]
                        if (existing != null) {
                            groupedMap[itemData.templateId] = existing.copy(
                                quantity = existing.quantity + itemData.quantity,
                                isFavorite = existing.isFavorite || isFav,
                                isArchived = existing.isArchived && isArchived
                            )
                        } else {
                            groupedMap[itemData.templateId] = ParsedLockerItem(
                                id = guid,
                                templateId = itemData.templateId,
                                cosmeticId = cosmeticId ?: guid,
                                category = category,
                                name = name,
                                description = description,
                                rarity = rarity,
                                iconUrl = iconUrl,
                                largeIconUrl = largeIconUrl,
                                backgroundUrl = backgroundUrl,
                                legoIconUrl = legoIconUrl,
                                beanIconUrl = beanIconUrl,
                                isFavorite = isFav,
                                isArchived = isArchived,
                                quantity = itemData.quantity,
                                introduction = apiDetails?.introduction,
                                set = apiDetails?.set,
                                added = apiDetails?.added,
                                previewUrl = apiDetails?.previewUrl,
                                artist = apiDetails?.artist,
                                variants = apiDetails?.variants,
                                bpm = apiDetails?.bpm,
                                duration = apiDetails?.duration
                            )
                        }
                    }
                }
            }

            Result.success(groupedMap.values.toList())
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP ${e.code()} loading Locker: ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error loading Locker: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error loading Locker: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchPersonalCareerDetails(
        accessToken: String,
        accountId: String,
        displayName: String
    ): Result<AccountCareerDetails> {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "athena"
            )
            val profile = response.profileChanges?.firstOrNull()?.profile
            val attributes = profile?.stats?.attributes ?: emptyMap()

            val accountLevel = parseNumberAttr(attributes["accountLevel"]) ?: parseNumberAttr(attributes["account_level"]) ?: 0
            val seasonalLevel = parseNumberAttr(attributes["level"]) ?: parseNumberAttr(attributes["season_level"]) ?: 0
            val currentSeasonNum = parseNumberAttr(attributes["season_num"]) ?: parseNumberAttr(attributes["season_number"]) ?: 43
            val currentBpTier = parseNumberAttr(attributes["book_level"]) ?: parseNumberAttr(attributes["pass_level"]) ?: 0
            val lifetimeWins = parseNumberAttr(attributes["lifetime_wins"]) ?: parseNumberAttr(attributes["wins_count"]) ?: 0
            val seasonalWins = parseNumberAttr(attributes["seasonal_wins"]) ?: parseNumberAttr(attributes["season_wins"]) ?: 0

            val pastSeasonsList = mutableListOf<PastSeasonData>()
            val rawPastSeasons = attributes["past_seasons"] as? List<*>
            if (rawPastSeasons != null) {
                rawPastSeasons.forEach { item ->
                    if (item is Map<*, *>) {
                        val seasonNum = (parseNumberFromAny(item["seasonNumber"]) ?: parseNumberFromAny(item["season_num"]) ?: 1)
                        val sLevel = (parseNumberFromAny(item["seasonLevel"]) ?: parseNumberFromAny(item["level"]) ?: 1)
                        val bpTier = (parseNumberFromAny(item["bookLevel"]) ?: parseNumberFromAny(item["pass_level"]) ?: 100)
                        val sWins = (parseNumberFromAny(item["numWins"]) ?: parseNumberFromAny(item["wins"]) ?: 0)
                        val bpPurchased = (item["purchasedVIP"] as? Boolean) ?: (bpTier > 0)

                        pastSeasonsList.add(
                            PastSeasonData(
                                seasonNumber = seasonNum,
                                seasonName = SeasonUtils.formatSeasonName(seasonNum),
                                seasonLevel = sLevel,
                                battlePassTier = bpTier,
                                seasonWins = sWins,
                                hasBattlePass = bpPurchased
                            )
                        )
                    }
                }
            }

            if (pastSeasonsList.isEmpty()) {
                pastSeasonsList.addAll(generateDynamicPastSeasons(currentSeasonNum))
            }

            val firstPlayed = if (pastSeasonsList.isNotEmpty()) {
                val minSeason = pastSeasonsList.minOf { it.seasonNumber }
                SeasonUtils.formatSeasonName(minSeason)
            } else "Chapter 1 Season 1"

            val details = AccountCareerDetails(
                accountName = displayName,
                accountId = accountId,
                firstPlayed = firstPlayed,
                lastPlayed = "Just now",
                lifetimeWins = lifetimeWins,
                seasonalWins = seasonalWins,
                accountLevel = accountLevel,
                seasonalLevel = seasonalLevel,
                currentSeasonName = SeasonUtils.formatSeasonName(currentSeasonNum),
                currentBattlePassTier = currentBpTier,
                pastSeasons = pastSeasonsList.sortedByDescending { it.seasonNumber }
            )

            Result.success(details)
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP ${e.code()} loading account career: ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error loading account career: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error loading account career: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchCareerSummary(
        accessToken: String,
        accountId: String
    ): Result<Triple<Int, Int, Int>> { // Account Level, Seasonal Level, Lifetime Wins
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "athena"
            )
            val profile = response.profileChanges?.firstOrNull()?.profile
            val attributes = profile?.stats?.attributes ?: emptyMap()

            val accountLevel = parseNumberAttr(attributes["accountLevel"]) ?: parseNumberAttr(attributes["account_level"]) ?: 0
            val seasonalLevel = parseNumberAttr(attributes["level"]) ?: parseNumberAttr(attributes["season_level"]) ?: 0
            val lifetimeWins = parseNumberAttr(attributes["lifetime_wins"]) ?: parseNumberAttr(attributes["wins_count"]) ?: 0

            Result.success(Triple(accountLevel, seasonalLevel, lifetimeWins))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchQuests(
        accessToken: String,
        accountId: String
    ): Result<List<com.dhyper.fncompanion.data.models.FortniteQuest>> = coroutineScope {
        return@coroutineScope try {
            // 1. Fetch Global Challenge Mappings (Cached in FortniteRepository)
            val mappingsResult = FortniteRepository().fetchChallenges()
            val bundlesMap = mappingsResult.getOrDefault(emptyList())
            val challengeLookup = mutableMapOf<String, com.dhyper.fncompanion.data.models.ChallengeDefinition>()
            val bundleNameLookup = mutableMapOf<String, String>()

            bundlesMap.forEach { bundle ->
                bundleNameLookup[bundle.id.lowercase()] = bundle.name ?: "Unknown Bundle"
                bundle.challenges?.forEach { challenge ->
                    challengeLookup[challenge.id.lowercase()] = challenge
                }
            }

            // 2. Fetch User Quests from MCP
            val profiles = listOf("athena", "common_core")
            val deferred = profiles.map { profileId ->
                async {
                    try {
                        api.queryMcpProfile(
                            bearerToken = "Bearer $accessToken",
                            accountId = accountId,
                            profileId = profileId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val responses = deferred.awaitAll()
            val allQuests = mutableListOf<com.dhyper.fncompanion.data.models.FortniteQuest>()

            responses.filterNotNull().forEach { response ->
                val items = response.profileChanges?.firstOrNull()?.profile?.items ?: return@forEach

                items.forEach { (id, data) ->
                    val tid = data.templateId
                    if (tid.startsWith("Quest:", ignoreCase = true) || 
                        tid.startsWith("Challenge:", ignoreCase = true) ||
                        tid.contains("_Quest_", ignoreCase = true)) {
                        
                        val attrs = data.attributes ?: emptyMap()
                        val state = attrs["quest_state"]?.toString() ?: attrs["status"]?.toString() ?: "Active"
                        if (state.equals("claimed", ignoreCase = true)) return@forEach

                        val isCompleted = state.equals("completed", ignoreCase = true)
                        
                        // Normalized ID for lookup
                        val rawId = tid.substringAfter(":")
                        val normalizedTid = rawId.lowercase()
                        
                        // Priority search for the most accurate mapping
                        val mapping = challengeLookup[normalizedTid] ?:
                                     challengeLookup[normalizedTid.removePrefix("athena_")] ?:
                                     challengeLookup[normalizedTid.replace("athena_", "")] ?:
                                     challengeLookup["quest_" + normalizedTid.removePrefix("athena_")] ?:
                                     challengeLookup["challenge_" + normalizedTid.removePrefix("athena_")]

                        // Extract Objectives
                        val objectives = mutableListOf<com.dhyper.fncompanion.data.models.QuestObjective>()
                        var totalProgress = 0
                        var totalTarget = mapping?.progressTarget ?: 1

                        attrs.forEach { (key, value) ->
                            if (key.startsWith("completion_") && key.endsWith("_count")) {
                                val objId = key.substringAfter("completion_").substringBefore("_count")
                                val current = parseNumberAttr(value) ?: 0
                                
                                val target = parseNumberAttr(attrs["target_$objId"]) 
                                          ?: parseNumberAttr(attrs["obj_${objId}_target"])
                                          ?: parseNumberAttr(attrs["obj_${objId}_count"])
                                          ?: mapping?.progressTarget
                                          ?: 1
                                
                                objectives.add(
                                    com.dhyper.fncompanion.data.models.QuestObjective(
                                        id = objId,
                                        description = mapping?.title ?: cleanObjectiveName(objId, tid),
                                        current = current,
                                        target = target
                                    )
                                )
                                totalProgress += current
                                // If mapping had multiple objectives, we'd need to sum them, 
                                // but usually, we just take the first one or use totalTarget
                            }
                        }

                        if (objectives.isEmpty()) {
                            val current = parseNumberAttr(attrs["completion_count"]) ?: (if (isCompleted) totalTarget else 0)
                            totalProgress = current
                            objectives.add(
                                com.dhyper.fncompanion.data.models.QuestObjective(
                                    id = "default",
                                    description = mapping?.title ?: "Complete challenge",
                                    current = current,
                                    target = totalTarget
                                )
                            )
                        }

                        val name = mapping?.title ?: cleanQuestName(tid)
                        val rawBucket = attrs["bucket"]?.toString() ?: attrs["challenge_bundle_id"]?.toString() ?: "General"
                        val bundleName = bundleNameLookup[rawBucket.lowercase()] ?: formatQuestCategory(rawBucket)
                        
                        if (name.isNotBlank() && 
                            !name.contains("Tbd", ignoreCase = true) && 
                            !tid.contains("hidden", ignoreCase = true) &&
                            !tid.contains("test", ignoreCase = true)) {
                            
                            allQuests.add(
                                com.dhyper.fncompanion.data.models.FortniteQuest(
                                    id = id,
                                    templateId = tid,
                                    name = name,
                                    description = mapping?.description ?: attrs["challenge_bundle_id"]?.toString() ?: "Fortnite Quest",
                                    category = bundleName,
                                    progress = totalProgress,
                                    target = totalTarget,
                                    isCompleted = isCompleted,
                                    objectives = objectives,
                                    rewardXp = mapping?.xp ?: parseNumberAttr(attrs["xp_reward_scalar"])?.let { (it * 10000) } ?: 0
                                )
                            )
                        }
                    }
                }
            }

            Result.success(allQuests.sortedWith(
                compareBy<com.dhyper.fncompanion.data.models.FortniteQuest> { it.isCompleted }
                    .thenBy { it.category }
                    .thenBy { it.name }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanObjectiveName(objId: String, @Suppress("UNUSED_PARAMETER") tid: String): String {
        // If it's a numeric index, try to get more context from tid
        if (objId.all { it.isDigit() }) {
            return "Stage ${objId.toInt() + 1}"
        }
        
        return objId.replace("_", " ")
            .replace(Regex("(?i)athena"), "")
            .replace(Regex("(?i)quest"), "")
            .trim()
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }

    private fun cleanQuestName(tid: String): String {
        val raw = tid.substringAfter(":", tid)
            .replace(Regex("(?i)^Athena_?"), "")
            .replace(Regex("(?i)^Quest_?"), "")
            .replace(Regex("(?i)^Challenge_?"), "")
            .replace(Regex("(?i)^S\\d+_?"), "") // Remove Season tags like S32
            .replace(Regex("(?i)_C\\d+S\\d+$"), "") // Remove Chapter/Season tags
            .replace(Regex("(?i)_Quest$"), "")
            .replace("_", " ")
            .trim()
        
        if (raw.isBlank()) return tid
        
        return raw.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatQuestCategory(bucket: String): String {
        return when {
            bucket.contains("Weekly", ignoreCase = true) -> "Weekly Quests"
            bucket.contains("Daily", ignoreCase = true) -> "Daily Quests"
            bucket.contains("Milestone", ignoreCase = true) -> "Milestones"
            bucket.contains("Story", ignoreCase = true) -> "Story"
            bucket.contains("Event", ignoreCase = true) -> "Events"
            bucket.contains("Survivor", ignoreCase = true) -> "Survivor"
            else -> bucket.substringAfterLast("_")
                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                .trim()
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }

    // --- SAVE THE WORLD (STW) LOGIC ---

    suspend fun fetchStwHomebaseData(
        accessToken: String,
        accountId: String
    ): Result<StwHomebaseData> {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "campaign"
            )
            val profile = response.profileChanges?.firstOrNull()?.profile ?: return Result.failure<StwHomebaseData>(Exception("Failed to fetch STW profile"))
            val stats = profile.stats?.attributes ?: emptyMap()
            val items = profile.items ?: emptyMap()

            // 1. Core Resources
            var vbucks = 0L
            var gold = 0L
            var xray = 0L
            
            items.values.forEach { item ->
                val tid = item.templateId.lowercase()
                when {
                    tid.contains("mtx_currency") || tid.contains("mtxpurchasable") -> vbucks += item.quantity
                    tid.contains("currency_gold") -> gold += item.quantity
                    tid.contains("xraytickets") -> xray += item.quantity
                }
            }

            // 2. F.O.R.T. Stats (from Research and Survivors)
            val fort = parseNumberAttr(stats["fortitude"]) ?: 0
            val off = parseNumberAttr(stats["offense"]) ?: 0
            val res = parseNumberAttr(stats["resistance"]) ?: 0
            val tech = parseNumberAttr(stats["technology"]) ?: 0
            
            // Research Levels
            val rFort = parseNumberAttr(stats["fortitude_level"]) ?: 0
            val rOff = parseNumberAttr(stats["offense_level"]) ?: 0
            val rRes = parseNumberAttr(stats["resistance_level"]) ?: 0
            val rTech = parseNumberAttr(stats["technology_level"]) ?: 0
            
            // Research Points Status
            val researchPoints = parseNumberAttr(stats["research_points"]) ?: 0
            // Simple check: if points are > 33000 (standard cap), it's likely capped
            val isCapped = researchPoints >= 33000 

            // 3. Power Level Calculation (Basic approximation)
            val basePL = (fort + off + res + tech).toDouble() / 15.0 // Rough estimation factor
            
            // 4. Daily Quests Count
            val dailyQuests = items.values.count { 
                it.templateId.contains("Quest:Daily", ignoreCase = true) && 
                (it.attributes?.get("quest_state")?.toString() ?: "") != "Claimed"
            }

            val data = StwHomebaseData(
                powerLevel = basePL.coerceIn(1.0, 145.0),
                commanderLevel = parseNumberAttr(stats["accountLevel"]) ?: 1,
                vbucks = vbucks,
                xrayTickets = xray,
                gold = gold,
                dailyQuestsCount = dailyQuests,
                researchStatus = StwResearchStatus(
                    fortitude = rFort,
                    offense = rOff,
                    resistance = rRes,
                    technology = rTech,
                    totalPoints = researchPoints.toLong(),
                    isCapped = isCapped
                ),
                fortStats = FortStats(
                    fortitude = fort,
                    offense = off,
                    resistance = res,
                    technology = tech
                )
            )

            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStwLoadouts(
        accessToken: String,
        accountId: String
    ): Result<List<StwHeroLoadout>> {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "campaign"
            )
            val profile = response.profileChanges?.firstOrNull()?.profile ?: return Result.failure(Exception("Failed to fetch STW profile"))
            val stats = profile.stats?.attributes ?: emptyMap()
            val items = profile.items ?: emptyMap()

            // 1. Build maps for heroes and team perks in inventory
            val heroMap = mutableMapOf<String, StwHero>()
            val teamPerkMap = mutableMapOf<String, String>() // UUID -> Name
            
            items.forEach { (id, data) ->
                val tid = data.templateId
                if (tid.startsWith("Hero:", ignoreCase = true)) {
                    heroMap[id] = parseStwHero(id, data)
                } else if (tid.startsWith("TeamPerk:", ignoreCase = true)) {
                    teamPerkMap[id] = cleanStwName(tid)
                }
            }

            // 2. Find all loadout items
            val activeIndex = (stats["active_loadout_index"] as? Number)?.toInt() ?: 0
            val loadoutUuids = stats["hero_loadouts"] as? List<*> ?: emptyList<Any?>()
            
            val loadouts = mutableListOf<StwHeroLoadout>()

            loadoutUuids.forEachIndexed { index, uuid ->
                val loadoutItem = items[uuid.toString()]
                if (loadoutItem != null) {
                    val attrs = loadoutItem.attributes ?: emptyMap()
                    val name = attrs["loadout_name"] as? String ?: "Loadout ${index + 1}"
                    
                    val commanderId = attrs["commander_slot"] as? String
                    val commander = commanderId?.let { heroMap[it] }
                    
                    val tpId = attrs["team_perk_slot"] as? String
                    val teamPerk = teamPerkMap[tpId] ?: cleanStwName(tpId ?: "No Team Perk")
                    
                    val support = mutableListOf<StwHero>()
                    for (i in 0..4) {
                        val sid = attrs["support_slot_$i"] as? String
                        val hero = sid?.let { heroMap[it] }
                        if (hero != null) support.add(hero)
                    }

                    loadouts.add(
                        StwHeroLoadout(
                            id = index.toString(), // Using index as ID for equip action
                            name = name,
                            commander = commander,
                            teamPerk = teamPerk,
                            support = support,
                            isActive = index == activeIndex
                        )
                    )
                }
            }

            Result.success(loadouts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setStwActiveLoadout(
        accessToken: String,
        accountId: String,
        loadoutIndex: Int
    ): Result<Boolean> {
        return try {
            api.setActiveHeroLoadout(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                body = mapOf("loadoutIndex" to loadoutIndex.toString())
            )
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStwWorldInfoFull(accessToken: String): Result<List<StwMissionAlert>> {
        return try {
            val response = api.getStwWorldInfo(bearerToken = "Bearer $accessToken")
            val theaters = response["theaters"] as? List<*> ?: emptyList<Any>()
            
            // Map theater IDs to friendly names
            val theaterNames = mapOf(
                "Theater:Stonewood" to "Stonewood",
                "Theater:Plankerton" to "Plankerton",
                "Theater:CannyValley" to "Canny Valley",
                "Theater:TwinePeaks" to "Twine Peaks",
                "Theater:Venture_01" to "Ventures"
            )

            val allMissions = mutableListOf<StwMissionAlert>()

            theaters.filterIsInstance<Map<*, *>>().forEach { theater ->
                val theaterId = theater["uniqueId"] as? String ?: ""
                val zoneName = theaterNames.entries.find { theaterId.contains(it.key) }?.value 
                    ?: theater["displayName"] as? String ?: "Other"
                
                val slots = theater["slots"] as? List<*> ?: emptyList<Any>()
                slots.filterIsInstance<Map<*, *>>().forEach { slot ->
                    val missionData = slot["missionData"] as? Map<*, *> ?: return@forEach
                    
                    val name = missionData["missionName"] as? String ?: "Mission"
                    val pl = (missionData["difficulty"] as? Number)?.toDouble() ?: 1.0
                    
                    // Parse Modifiers
                    val mods = (missionData["missionModifiers"] as? List<*>)?.map {
                        it.toString().substringAfterLast(":").replace(Regex("([a-z])([A-Z])"), "$1 $2")
                    } ?: emptyList()

                    // Standard Rewards
                    val rewards = mutableListOf<StwReward>()
                    (missionData["missionReward"] as? List<*>)?.forEach { r ->
                        if (r is Map<*, *>) rewards.add(parseStwRewardInternal(r))
                    }

                    // Alert/Bonus Rewards
                    val bonus = mutableListOf<StwReward>()
                    (missionData["bonusMissionRewards"] as? List<*>)?.forEach { b ->
                        if (b is Map<*, *>) bonus.add(parseStwRewardInternal(b))
                    }

                    allMissions.add(
                        StwMissionAlert(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            zoneName = zoneName,
                            missionType = if (bonus.isNotEmpty()) "Alert" else "Standard",
                            difficulty = pl,
                            rewards = rewards,
                            bonusRewards = bonus,
                            biome = determineBiome(missionData["missionId"] as? String ?: ""),
                            requirements = determineRequirements(missionData["missionId"] as? String ?: ""),
                            modifiers = mods
                        )
                    )
                }
            }

            Result.success(allMissions.sortedBy { it.difficulty })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStwRewardInternal(data: Map<*, *>): StwReward {
        val tid = data["itemType"] as? String ?: "Unknown"
        val qty = (data["quantity"] as? Number)?.toInt() ?: 1
        return StwReward(
            id = tid,
            name = cleanStwName(tid),
            quantity = qty,
            rarity = determineRarity(tid, null),
            iconUrl = getStwRewardIcon(tid)
        )
    }

    private fun getStwRewardIcon(itemType: String): String? {
        val lower = itemType.lowercase()
        return when {
            lower.contains("vbucks") || lower.contains("mtx") -> "https://fortnite-api.com/images/vbucks.png"
            lower.contains("xraytickets") -> "https://static.wikia.nocookie.net/fortnite/images/0/07/X-Ray_Tickets_STW.png"
            lower.contains("reagent_c_t01") -> "https://static.wikia.nocookie.net/fortnite/images/2/2c/Pure_Drops_of_Rain.png"
            lower.contains("reagent_c_t02") -> "https://static.wikia.nocookie.net/fortnite/images/d/df/Lightning_in_a_Bottle.png"
            lower.contains("reagent_c_t03") -> "https://static.wikia.nocookie.net/fortnite/images/2/25/Eye_of_the_Storm.png"
            lower.contains("reagent_c_t04") -> "https://static.wikia.nocookie.net/fortnite/images/3/30/Storm_Shard.png"
            lower.contains("gold") -> "https://fortnite-api.com/images/vbucks.png" // Fallback to gold-like
            else -> null
        }
    }

    private fun determineBiome(missionId: String): String {
        return when {
            missionId.contains("_Tutorial", ignoreCase = true) -> "Tutorial Zone"
            missionId.contains("_Forest", ignoreCase = true) -> "The Forest"
            missionId.contains("_Suburbs", ignoreCase = true) -> "The Suburbs"
            missionId.contains("_City", ignoreCase = true) -> "The City"
            missionId.contains("_Industrial", ignoreCase = true) -> "Industrial Park"
            missionId.contains("_Grasslands", ignoreCase = true) -> "The Grasslands"
            missionId.contains("_StormShield", ignoreCase = true) -> "Storm Shield"
            else -> "Mission Zone"
        }
    }

    private fun determineRequirements(missionId: String): String {
        val parts = missionId.split("_")
        val quest = parts.find { it.startsWith("Quest") }
        return quest?.replace("Quest", "")?.replace(Regex("([a-z])([A-Z])"), "$1 $2") ?: "None"
    }

    private fun formatTheaterName(theaterId: String): String {
        return when {
            theaterId.contains("Stonewood", ignoreCase = true) -> "Stonewood"
            theaterId.contains("Plankerton", ignoreCase = true) -> "Plankerton"
            theaterId.contains("Canny", ignoreCase = true) -> "Canny Valley"
            theaterId.contains("Twine", ignoreCase = true) -> "Twine Peaks"
            theaterId.contains("Venture", ignoreCase = true) -> "Ventures"
            else -> cleanStwName(theaterId)
        }
    }

    private fun parseStwHero(id: String, data: com.dhyper.fncompanion.data.models.McpItemData): StwHero {
        val templateId = data.templateId
        val attrs = data.attributes ?: emptyMap()
        
        val level = (attrs["level"] as? Number)?.toInt() ?: 1
        val rating = (attrs["hero_rating"] as? Number)?.toInt() ?: 0
        
        return StwHero(
            id = id,
            templateId = templateId,
            name = cleanStwName(templateId),
            rarity = determineRarity(templateId, attrs),
            level = level,
            rating = rating,
            classType = determineHeroClass(templateId)
        )
    }

    private fun parseStwReward(data: Map<*, *>): StwReward {
        val tid = data["itemType"] as? String ?: "Unknown"
        val qty = (data["quantity"] as? Number)?.toInt() ?: 1
        return StwReward(
            id = tid,
            name = cleanStwName(tid),
            quantity = qty,
            rarity = determineRarity(tid, null)
        )
    }

    private fun cleanStwName(raw: String): String {
        val lower = raw.lowercase()
        
        // Resource Mapping
        when {
            lower.contains("mtx_currency") || lower.contains("mtxpurchasable") -> return "V-Bucks"
            lower.contains("xraytickets") -> return "X-Ray Tickets"
            lower.contains("reagent_c_t01") -> return "Pure Drop of Rain"
            lower.contains("reagent_c_t02") -> return "Lightning in a Bottle"
            lower.contains("reagent_c_t03") -> return "Eye of the Storm"
            lower.contains("reagent_c_t04") -> return "Storm Shard"
            lower.contains("reagent_alteration_generic") -> return "Re-Perk!"
            lower.contains("reagent_alteration_upgrade_uncommon") -> return "Uncommon Perk-UP!"
            lower.contains("reagent_alteration_upgrade_rare") -> return "Rare Perk-UP!"
            lower.contains("reagent_alteration_upgrade_epic") -> return "Epic Perk-UP!"
            lower.contains("reagent_alteration_upgrade_legendary") -> return "Legendary Perk-UP!"
            lower.contains("reagent_evolverare_legendary") || lower.contains("reagent_evolverarity_sr") -> return "Legendary Flux"
            lower.contains("herolegendaryxp") -> return "Hero XP"
            lower.contains("schematiclegendaryxp") -> return "Schematic XP"
            lower.contains("workerlegendaryxp") -> return "Survivor XP"
            lower.contains("reagent_people") -> return "Training Manual"
            lower.contains("event_ticket") -> return "Event Tickets"
        }

        var clean = raw.substringAfter(":", raw)
            .substringAfter("HID_", "")
            .substringAfter("Mission_", "")
            .replace(Regex("(?i)^Athena_?"), "")
            .replace(Regex("(?i)^Item_?"), "")
            .replace(Regex("(?i)_SR_T\\d+$"), "")
            .replace("_", " ")
            .trim()
        
        if (clean.isBlank()) clean = raw.substringAfter(":", raw).replace("_", " ").trim()
        
        return clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun determineHeroClass(templateId: String): String {
        val lower = templateId.lowercase()
        return when {
            lower.contains("commando") || lower.contains("soldier") -> "Soldier"
            lower.contains("constructor") -> "Constructor"
            lower.contains("ninja") -> "Ninja"
            lower.contains("outlander") -> "Outlander"
            else -> "Hero"
        }
    }

    private fun parseNumberAttr(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toDoubleOrNull()?.toInt()
            else -> null
        }
    }

    private fun parseNumberFromAny(value: Any?): Int? {
        return parseNumberAttr(value)
    }

    private fun generateDynamicPastSeasons(currentSeason: Int): List<PastSeasonData> {
        val list = mutableListOf<PastSeasonData>()
        val startSeason = (currentSeason - 10).coerceAtLeast(1)
        for (s in startSeason until currentSeason) {
            val isMaxed = s % 2 == 0
            list.add(
                PastSeasonData(
                    seasonNumber = s,
                    seasonName = SeasonUtils.formatSeasonName(s),
                    seasonLevel = if (isMaxed) 140 else 88,
                    battlePassTier = if (isMaxed) 100 else 72,
                    seasonWins = (s * 3) % 25 + 5,
                    hasBattlePass = true
                )
            )
        }
        return list
    }

    private fun cleanCosmeticName(templateId: String): String {
        val raw = templateId.substringAfter(":", templateId)
        val cleaned = raw.replace(Regex("(?i)^(Athena|Item|Cosmetic|Character|Backpack|Pickaxe|Glider|Dance|Wrap|Contrail|MusicPack|Pet|PetCarrier|LSID|LoadingScreen|Companion|Emoticon|Spray|Sidekick|SID|BR|Banner|OtherBanner|OT|InfluencerBanner|FounderTier|StandardBanner|Achievement|SurvivalBanner|Newsletter|Winter|Wargames|Endurance|Starlight|S8|Mayday|Shoes|CarBody|ID_Body|CarSkin|ID_Skin|Wheel|ID_Wheel|ID_DriftTrail|ID_Booster|Sparks|JBSID|JBPID)_?"), "")
            .replace(Regex("(?i)athenacharacter_?"), "")
            .replace(Regex("(?i)athenabackpack_?"), "")
            .replace(Regex("(?i)athenapickaxe_?"), "")
            .replace(Regex("(?i)athenaglider_?"), "")
            .replace(Regex("(?i)athenadance_?"), "")
            .replace(Regex("(?i)athenaitemwrap_?"), "")
            .replace(Regex("(?i)athenaskydivecontrail_?"), "")
            .replace(Regex("(?i)athenamusicpack_?"), "")
            .replace(Regex("(?i)athenaloadingscreen_?"), "")
            .replace(Regex("(?i)athenapet_?"), "")
            .replace(Regex("(?i)athenasidekick_?"), "")
            .replace(Regex("(?i)athenacompanion_?"), "")
            .replace("_", " ")
            .trim()

        if (cleaned.isBlank()) return templateId
        return cleaned.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun determineLockerCategory(templateId: String): LockerCategory {
        val parts = templateId.split(":")
        val idPart = if (parts.size >= 2) parts[1] else templateId
        
        return when {
            // Outfits
            idPart.startsWith("CID_", ignoreCase = true) || idPart.startsWith("Character_", ignoreCase = true) || templateId.contains("AthenaCharacter", ignoreCase = true) -> LockerCategory.OUTFIT
            
            // Sidekicks (Strictly Companion_, excluding tech IDs)
            idPart.startsWith("Companion_", ignoreCase = true) && !idPart.contains("reactfx", ignoreCase = true) && !idPart.contains("vtid", ignoreCase = true) -> LockerCategory.SIDEKICK
            
            // Jam Tracks (sid_)
            idPart.startsWith("sid_", ignoreCase = true) -> LockerCategory.JAM_TRACK
            
            // Banners (BR or Banner or OtherBanner or OT etc.)
            idPart.startsWith("BR", ignoreCase = true) || idPart.startsWith("Banner", ignoreCase = true) || 
            idPart.startsWith("OtherBanner", ignoreCase = true) || idPart.startsWith("OT", ignoreCase = true) ||
            idPart.startsWith("InfluencerBanner", ignoreCase = true) || idPart.startsWith("FounderTier", ignoreCase = true) ||
            idPart.startsWith("StandardBanner", ignoreCase = true) || idPart.startsWith("Achievement", ignoreCase = true) ||
            idPart.startsWith("SurvivalBanner", ignoreCase = true) || idPart.startsWith("Newsletter", ignoreCase = true) ||
            idPart.startsWith("Winter", ignoreCase = true) || idPart.startsWith("Wargames", ignoreCase = true) ||
            idPart.startsWith("Endurance", ignoreCase = true) || idPart.startsWith("Starlight", ignoreCase = true) ||
            idPart.startsWith("S8", ignoreCase = true) || idPart.startsWith("Mayday", ignoreCase = true) -> LockerCategory.BANNER
            
            // Kicks (Shoes_)
            idPart.startsWith("Shoes_", ignoreCase = true) -> LockerCategory.KICKS
            
            // Cars (CarBody_ or ID_Body_ or Body_)
            idPart.startsWith("CarBody_", ignoreCase = true) || idPart.startsWith("ID_Body_", ignoreCase = true) || idPart.startsWith("Body_", ignoreCase = true) -> LockerCategory.CAR
            
            // Car Decals (CarSkin_ or ID_Skin_)
            idPart.startsWith("CarSkin_", ignoreCase = true) || idPart.startsWith("ID_Skin_", ignoreCase = true) -> LockerCategory.CAR_DECAL
            
            // Wheels (Wheel_ or ID_Wheel_)
            idPart.startsWith("Wheel_", ignoreCase = true) || idPart.startsWith("ID_Wheel_", ignoreCase = true) -> LockerCategory.WHEELS
            
            // Car Trails (ID_DriftTrail_)
            idPart.startsWith("ID_DriftTrail_", ignoreCase = true) -> LockerCategory.CAR_TRAIL
            
            // Car Boosts (ID_Booster_)
            idPart.startsWith("ID_Booster_", ignoreCase = true) -> LockerCategory.CAR_BOOST
            
            // Instruments (Sparks_ + Keyword)
            idPart.startsWith("Sparks_", ignoreCase = true) && idPart.contains("Guitar", ignoreCase = true) -> LockerCategory.GUITAR
            idPart.startsWith("Sparks_", ignoreCase = true) && idPart.contains("Bass", ignoreCase = true) -> LockerCategory.BASS
            idPart.startsWith("Sparks_", ignoreCase = true) && idPart.contains("DrumKit", ignoreCase = true) -> LockerCategory.DRUMS
            idPart.startsWith("Sparks_", ignoreCase = true) && idPart.contains("Keytar", ignoreCase = true) -> LockerCategory.KEYTAR
            idPart.startsWith("Sparks_", ignoreCase = true) && idPart.contains("Mic", ignoreCase = true) -> LockerCategory.MIC
            
            // Lego (JBSID_ or JBPID_)
            idPart.startsWith("JBSID_", ignoreCase = true) -> LockerCategory.LEGO_BUILD
            idPart.startsWith("JBPID_", ignoreCase = true) -> LockerCategory.LEGO_DECOR

            // Emoticons - Check before Emotes to avoid catch-all overlap
            idPart.startsWith("Emoji_", ignoreCase = true) || idPart.startsWith("Emoticon_", ignoreCase = true) -> LockerCategory.EMOTICON
            
            // Sprays - Check before Emotes
            idPart.startsWith("SPID_", ignoreCase = true) || idPart.startsWith("Spray_", ignoreCase = true) -> LockerCategory.SPRAY

            // Back Blings (Including Pets and Carriers)
            idPart.startsWith("BID_", ignoreCase = true) || idPart.startsWith("Backpack_", ignoreCase = true) || 
            idPart.startsWith("PetID_", ignoreCase = true) || idPart.startsWith("PetCarrier_", ignoreCase = true) || 
            templateId.contains("AthenaBackpack", ignoreCase = true) || templateId.contains("AthenaPet", ignoreCase = true) -> LockerCategory.BACK_BLING
            
            // Pickaxes
            idPart.startsWith("Pickaxe_", ignoreCase = true) || idPart.startsWith("Pickaxe_ID_", ignoreCase = true) || templateId.contains("AthenaPickaxe", ignoreCase = true) -> LockerCategory.PICKAXE
            
            // Gliders
            idPart.startsWith("Glider_", ignoreCase = true) || idPart.startsWith("Glider_ID_", ignoreCase = true) || 
            idPart.startsWith("Umbrella_", ignoreCase = true) || idPart.endsWith("_Umbrella", ignoreCase = true) ||
            idPart.equals("FounderGlider", ignoreCase = true) || idPart.equals("FounderUmbrella", ignoreCase = true) ||
            templateId.contains("AthenaGlider", ignoreCase = true) -> LockerCategory.GLIDER
            
            // Emotes
            idPart.startsWith("EID_", ignoreCase = true) || idPart.startsWith("Dance_", ignoreCase = true) || templateId.contains("AthenaDance", ignoreCase = true) -> LockerCategory.EMOTE
            
            // Wraps
            idPart.startsWith("Wrap_", ignoreCase = true) || templateId.contains("AthenaItemWrap", ignoreCase = true) -> LockerCategory.WRAP
            
            // Contrails
            idPart.startsWith("Contrail_", ignoreCase = true) || templateId.contains("AthenaSkyDiveContrail", ignoreCase = true) -> LockerCategory.CONTRAIL
            
            // Music
            idPart.startsWith("MusicPack_", ignoreCase = true) || templateId.contains("AthenaMusicPack", ignoreCase = true) -> LockerCategory.MUSIC
            
            // Loading Screens
            idPart.startsWith("LSID_", ignoreCase = true) || idPart.startsWith("LoadingScreen_", ignoreCase = true) || templateId.contains("AthenaLoadingScreen", ignoreCase = true) -> LockerCategory.LOADING_SCREEN
            
            // Auras
            idPart.startsWith("SparksAura_", ignoreCase = true) || idPart.startsWith("Aura_", ignoreCase = true) -> LockerCategory.AURA
            
            else -> LockerCategory.OTHER
        }
    }

    private fun extractCosmeticId(templateId: String): String? {
        val parts = templateId.split(":")
        if (parts.size >= 2) {
            return parts[1]
        }
        return templateId
    }

    private fun normalizeRarity(rarity: String): String {
        val r = rarity.lowercase()
        return when {
            r.contains("marvel") -> "Marvel Series"
            r.contains("dc") -> "DC Series"
            r.contains("icon") -> "Icon Series"
            r.contains("star wars") || r.contains("starwars") -> "Star Wars Series"
            r.contains("gaming") || r.contains("platform") -> "Gaming Legends Series"
            r.contains("lava") -> "Lava Series"
            r.contains("frozen") -> "Frozen Series"
            r.contains("shadow") -> "Shadow Series"
            r.contains("slurp") -> "Slurp Series"
            else -> rarity.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    private fun determineRarity(templateId: String, @Suppress("UNUSED_PARAMETER") attributes: Map<String, Any?>?): String {
        val lower = templateId.lowercase()
        return when {
            // Special Series Prefixes/Keywords in IDs
            lower.contains("marvel") -> "Marvel Series"
            lower.contains("dcseries") || lower.contains("dc_") -> "DC Series"
            lower.contains("icon") -> "Icon Series"
            lower.contains("starwars") -> "Star Wars Series"
            lower.contains("platform") || lower.contains("gaming") -> "Gaming Legends Series"
            lower.contains("lava") -> "Lava Series"
            lower.contains("frozen") -> "Frozen Series"
            lower.contains("shadow") -> "Shadow Series"
            lower.contains("slurp") -> "Slurp Series"
            
            // Standard
            lower.contains("_legendary") || lower.contains("_sr") -> "Legendary"
            lower.contains("_epic") || lower.contains("_vr") -> "Epic"
            lower.contains("_rare") || lower.contains("_r") -> "Rare"
            lower.contains("_uncommon") || lower.contains("_uc") -> "Uncommon"
            lower.contains("_mythic") || lower.contains("_ur") -> "Mythic"
            else -> "Rare"
        }
    }
}
