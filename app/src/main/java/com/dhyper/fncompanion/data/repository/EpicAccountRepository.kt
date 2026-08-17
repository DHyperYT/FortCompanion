package com.dhyper.fncompanion.data.repository

import android.content.Context
import com.dhyper.fncompanion.data.api.ApiClient
import com.dhyper.fncompanion.data.models.*
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class EpicAccountRepository {
    private val api = ApiClient.epicApi

    private var cosmeticsCacheMap: Map<String, CosmeticItem>? = null

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

    suspend fun fetchVBucksBalance(accessToken: String, accountId: String): Result<Long> {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "common_core"
            )
            val profile = response.profileChanges?.firstOrNull()?.profile
            val items = profile?.items ?: emptyMap()
            var totalVbucks = 0L

            items.values.forEach { item ->
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
                                showcaseVideo = apiDetails?.showcaseVideo,
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

            Result.success(groupedMap.values.toList().sortedByDescending { it.isFavorite })
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

    private fun determineRarity(templateId: String, attributes: Map<String, Any?>?): String {
        val attrRarity = attributes?.get("starting_rarity")?.toString() 
            ?: attributes?.get("rarity")?.toString()
        
        if (!attrRarity.isNullOrBlank()) {
            val r = attrRarity.lowercase()
            return when {
                r.contains("mythic") || r.contains("ur") -> "Mythic"
                r.contains("legendary") || r.contains("sr") -> "Legendary"
                r.contains("epic") || r.contains("vr") -> "Epic"
                r.contains("rare") || r.contains("r") -> "Rare"
                r.contains("uncommon") || r.contains("uc") -> "Uncommon"
                r.contains("common") || r.contains("c") -> "Common"
                else -> r.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }

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

    private fun parseNumberFromAny(value: Any?): Int? {
        return parseNumberAttr(value)
    }

    suspend fun fetchStwHomebaseData(
        context: Context,
        accessToken: String,
        accountId: String
    ): Result<StwHomebaseData> = coroutineScope {
        return@coroutineScope try {
            StwMetadataRepository.loadMetadata(context)

            val profileIds = listOf(
                "campaign", "common_core", "theater0", "theater1", "theater2", "outpost0", "athena"
            )

            val deferredProfiles = profileIds.associateWith { pid ->
                async { 
                    try {
                        api.queryMcpProfile(bearerToken = "Bearer $accessToken", accountId = accountId, profileId = pid)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val responses = deferredProfiles.mapValues { it.value.await() }
            
            val campaignRes = responses["campaign"] ?: return@coroutineScope Result.failure(Exception("Critical: Campaign profile missing"))
            val campaignProfile = campaignRes.profileChanges?.firstOrNull()?.profile ?: return@coroutineScope Result.failure(Exception("Campaign profile data empty"))
            
            val stats = campaignProfile.stats?.attributes ?: emptyMap()
            val items = campaignProfile.items ?: emptyMap()
            
            // Map revisions
            val revisions = responses.mapNotNull { (pid, res) -> 
                res?.profileRevision?.let { pid to it.toInt() }
            }.toMap()

            val isFounder = responses["athena"]?.profileChanges?.firstOrNull()?.profile?.items?.values?.any { 
                val tid = it.templateId.lowercase()
                tid.contains("cid_095_athena_commando_m_founder") || tid.contains("cid_096_athena_commando_f_founder")
            } ?: false
            
            val resources = items.filter { (_, item) -> 
                val tid = item.templateId.lowercase()
                tid.contains("currency") || tid.contains("xp") || tid.startsWith("reagent") || tid.startsWith("ingredient") || tid.contains("ticket") || tid.contains("voucher")
            }.map { (_, item) ->
                val tid = item.templateId
                StwResource(
                    templateId = tid,
                    name = StwMetadataRepository.resolveName(tid) ?: tid,
                    quantity = item.quantity.toLong(),
                    type = determineResourceType(tid),
                    rarity = determineRarity(tid, item.attributes),
                    iconUrl = getStwRewardIcon(tid)
                )
            }.sortedWith(compareBy({ it.type }, { it.rarity }, { it.name }))

            val gold = resources.find { it.templateId.contains("eventcurrency_scaling", true) || it.templateId.contains("currency:gold", true) }?.quantity ?: 0L
            val xray = resources.find { it.templateId.contains("currency:xraytickets", true) || it.templateId.contains("currency_xrayllama", true) }?.quantity ?: 0L
            val vbucks = sumCurrency(responses["common_core"]?.profileChanges?.firstOrNull()?.profile?.items ?: emptyMap(), "currency:mtx")

            val resStats = stats["research_levels"] as? Map<*, *>
            val research = StwResearch(
                fortitude = parseNumberAttr(resStats?.get("fortitude")) ?: 0,
                offense = parseNumberAttr(resStats?.get("offense")) ?: 0,
                resistance = parseNumberAttr(resStats?.get("resistance")) ?: 0,
                technology = parseNumberAttr(resStats?.get("technology")) ?: 0,
                totalLevels = parseNumberAttr(stats["level"]) ?: 0
            )

            val allHeroes = items.filter { it.value.templateId.startsWith("Hero:", true) }.map { parseStwHero(it.key, it.value) }
            val allSchematics = items.filter { 
                it.value.templateId.startsWith("Schematic:", true) && 
                !it.value.templateId.contains("ammo_", true) && 
                !it.value.templateId.contains("ingredient_", true)
            }.map { parseStwSchematic(it.key, it.value) }
            val allSurvivors = items.filter { it.value.templateId.startsWith("Worker:", true) }.map { parseStwSurvivor(it.key, it.value) }
            val allDefenders = items.filter { it.value.templateId.startsWith("Defender:", true) }.map { parseStwDefender(it.key, it.value) }
            
            val allQuests = items.filter { it.value.templateId.startsWith("Quest:", true) || it.value.templateId.startsWith("Challenge:", true) }
                .mapNotNull { parseStwQuest(it.key, it.value) }
            
            val dailyQuests = allQuests.filter { it.category == "Daily Quests" }
            val storyQuests = allQuests.filter { it.category == "Main Quests" || it.category == "Storm Shield / Main" }

            val heroMap = allHeroes.associateBy { it.id }
            val itemTemplateMap = items.mapValues { it.value.templateId }
            val loadouts = items.filter { it.value.templateId.startsWith("CampaignHeroLoadout:", true) }
                .map { (id, item) -> parseHeroLoadout(id, item, heroMap, itemTemplateMap, id == stats["selected_hero_loadout"]?.toString()) }
                .sortedBy { it.index }

            val survivorMap = allSurvivors.associateBy { it.id }
            val squads = parseSurvivorSquads(survivorMap)

            val achievements = parseAchievements(items)

            val outposts = parseOutposts(responses["metadata"])

            val backpackItems = responses["theater0"]?.profileChanges?.firstOrNull()?.profile?.items
                ?.filter { !it.value.templateId.contains("edittool", true) && !it.value.templateId.contains("buildingitemdata", true) }
                ?.map { (id, item) -> parseToInventoryItem(id, item) } ?: emptyList()

            val eventBackpackItems = responses["theater1"]?.profileChanges?.firstOrNull()?.profile?.items
                ?.filter { !it.value.templateId.contains("edittool", true) && !it.value.templateId.contains("buildingitemdata", true) }
                ?.map { (id, item) -> parseToInventoryItem(id, item) } ?: emptyList()

            val ventureBackpackItems = responses["theater2"]?.profileChanges?.firstOrNull()?.profile?.items
                ?.filter { !it.value.templateId.contains("edittool", true) && !it.value.templateId.contains("buildingitemdata", true) }
                ?.map { (id, item) -> parseToInventoryItem(id, item) } ?: emptyList()

            val storageItems = responses["outpost0"]?.profileChanges?.firstOrNull()?.profile?.items
                ?.filter { !it.value.templateId.contains("edittool", true) && !it.value.templateId.contains("buildingitemdata", true) }
                ?.map { (id, item) -> parseToInventoryItem(id, item) } ?: emptyList()

            val cbCategories = parseCollectionBook(responses)

            val llamas = parseLlamas(responses["shop_storefront"])

            Result.success(StwHomebaseData(
                powerLevel = parseNumberAttr(stats["homebase_rating"])?.toDouble() ?: 1.0,
                commanderLevel = parseNumberAttr(stats["level"]) ?: 1,
                vbucks = vbucks,
                xrayTickets = xray,
                gold = gold,
                resources = resources,
                research = research,
                inventory = StwInventory(
                    backpack = backpackItems.sortedBy { it.name },
                    eventBackpack = eventBackpackItems.sortedBy { it.name },
                    ventureBackpack = ventureBackpackItems.sortedBy { it.name },
                    storage = storageItems.sortedBy { it.name }
                ),
                collectionBook = StwCollectionBook(
                    level = parseNumberAttr((stats["collection_book"] as? Map<*, *>)?.get("maxBookXpLevelAchieved")) ?: 0,
                    maxLevelAchieved = parseNumberAttr((stats["collection_book"] as? Map<*, *>)?.get("maxBookXpLevelAchieved")) ?: 0,
                    xp = parseNumberAttr((responses["collections"]?.profileChanges?.firstOrNull()?.profile?.stats?.attributes?.get("collection_book_xp"))) ?: 0,
                    categories = cbCategories
                ),
                outposts = outposts,
                activeLoadoutId = stats["selected_hero_loadout"]?.toString(),
                loadouts = loadouts,
                squads = squads,
                heroes = allHeroes.sortedByDescending { it.rating },
                schematics = allSchematics.sortedByDescending { it.rating },
                survivors = allSurvivors.sortedByDescending { it.rating },
                defenders = allDefenders.sortedByDescending { it.rating },
                quests = allQuests.sortedBy { it.isCompleted },
                dailyQuests = dailyQuests.sortedBy { it.isCompleted },
                storyQuests = storyQuests.sortedBy { it.isCompleted },
                llamas = llamas,
                totalDaysLoggedIn = (stats["daily_rewards"] as? Map<*, *>)?.get("totalDaysLoggedIn")?.let { parseNumberAttr(it) } ?: 0,
                matchesPlayed = (stats["matches_played"] as? Number)?.toInt() ?: 0,
                zonesCompleted = (stats["gameplay_stats"] as? List<*>)?.find { (it as? Map<*, *>)?.get("statName") == "zonescompleted" }?.let { parseNumberAttr((it as Map<*, *>)["statValue"]) } ?: 0,
                achievements = achievements,
                commanderXp = (stats["xp"] as? Number)?.toLong() ?: 0L,
                packsGranted = parseNumberAttr(stats["packs_granted"]) ?: 0,
                isFounder = isFounder,
                profileLockExpiration = responses["theater0"]?.profileChanges?.firstOrNull()?.profile?.stats?.attributes?.get("profileLockExpiration")?.toString(),
                profileRevisions = revisions
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun sumCurrency(items: Map<String, McpItemData>, vararg tids: String): Long {
        return items.values.filter { item -> tids.any { tid -> item.templateId.contains(tid, true) } }.sumOf { it.quantity.toLong() }
    }

    private fun parseHeroLoadout(id: String, item: McpItemData, heroMap: Map<String, StwHero>, itemTemplateMap: Map<String, String>, isActive: Boolean): StwHeroLoadout {
        val attrs = item.attributes ?: emptyMap()
        val crew = attrs["crew_members"] as? Map<*, *>
        val gadgetsRaw = attrs["gadgets"] as? List<*>
        
        val commander = crew?.get("commanderslot")?.toString()?.let { heroMap[it] }
        val support = (1..5).map { i -> crew?.get("followerslot$i")?.toString()?.let { heroMap[it] } }
        val gadgets = gadgetsRaw?.mapNotNull { 
            val gadgetId = (it as? Map<*, *>)?.get("gadget")?.toString()
            if (gadgetId.isNullOrBlank()) null else gadgetId
        } ?: emptyList()

        val gadgetNames = gadgets.map { tid ->
            StwMetadataRepository.resolveName(tid) ?: tid
        }
        val gadgetIcons = gadgets.map { tid ->
            StwMetadataRepository.resolveStwGadgetIcon(tid)
        }
        
        val teamPerkInstanceId = attrs["team_perk"]?.toString()
        val teamPerkId = if (teamPerkInstanceId != null) itemTemplateMap[teamPerkInstanceId] ?: teamPerkInstanceId else null
        val teamPerkName = teamPerkId?.let { StwMetadataRepository.resolveName(it) ?: it }
        val teamPerkIcon = teamPerkId?.let { tid ->
            StwMetadataRepository.resolveStwTeamPerkIcon(tid)
        }
        
        return StwHeroLoadout(
            id = id,
            name = attrs["loadout_name"]?.toString() ?: "Loadout ${(parseNumberAttr(attrs["loadout_index"]) ?: 0) + 1}",
            commander = commander,
            teamPerkId = teamPerkId,
            teamPerkName = teamPerkName,
            teamPerkIcon = teamPerkIcon,
            support = support,
            gadgets = gadgets,
            gadgetNames = gadgetNames,
            gadgetIcons = gadgetIcons,
            isActive = isActive,
            index = parseNumberAttr(attrs["loadout_index"]) ?: 0
        )
    }

    private fun parseSurvivorSquads(survivorMap: Map<String, StwSurvivor>): List<StwSurvivorSquad> {
        val squadNames = mapOf(
            "fireteamalpha" to "Fire Team Alpha",
            "closeassaultsquad" to "Close Assault Squad",
            "emtsquad" to "EMT Squad",
            "trainingteam" to "Training Team",
            "scoutingparty" to "Scouting Party",
            "gadgeteers" to "Gadgeteers",
            "thethinktank" to "The Think Tank",
            "corpsofengineering" to "Corps of Engineering"
        )

        val squadIds = listOf(
            "squad_attribute_arms_fireteamalpha",
            "squad_attribute_arms_closeassaultsquad",
            "squad_attribute_medicine_emtsquad",
            "squad_attribute_medicine_trainingteam",
            "squad_attribute_scavenging_scoutingparty",
            "squad_attribute_scavenging_gadgeteers",
            "squad_attribute_synthesis_thethinktank",
            "squad_attribute_synthesis_corpsofengineering"
        )

        return squadIds.map { sid ->
            val squadSurvivors = survivorMap.values.filter { it.squadId == sid }
            val lead = squadSurvivors.find { it.squadSlotIdx == 0 }
            val members = (1..7).map { i ->
                squadSurvivors.find { it.squadSlotIdx == i }
            }

            val squadKey = sid.substringAfterLast("_")
            StwSurvivorSquad(
                id = sid,
                name = squadNames[squadKey] ?: squadKey.replaceFirstChar { it.uppercase() },
                leadSlot = lead,
                memberSlots = members,
                unlockedSlots = 8, // Standard
                totalPowerContribution = squadSurvivors.sumOf { it.rating }
            )
        }
    }

    private fun parseAchievements(items: Map<String, McpItemData>): List<StwAchievement> {
        val mapping = mapOf(
            "quest:achievement_killmistmonsters" to Triple("Unspeakable Horrors", 20000L, "unspeakablehorrors"),
            "quest:achievement_playwithothers" to Triple("Plays Well with Others", 1000L, "playswellwithothers"),
            "quest:achievement_loottreasurechests" to Triple("Loot Legend", 300L, "lootlegend"),
            "quest:achievement_buildstructures" to Triple("Talented Builder", 500000L, "talentedbuilder"),
            "quest:achievement_savesurvivors" to Triple("Guardian Angel", 10000L, "guardianangel"),
            "quest:achievement_explorezones" to Triple("World Explorer", 1500L, "worldexplorer"),
            "quest:achievement_destroygnomes" to Triple("Go Gnome!", 100L, "gognome")
        )

        return items.values.filter { it.templateId.lowercase() in mapping.keys }.map { item ->
            val tid = item.templateId.lowercase()
            val entry = mapping[tid]!!
            val name = entry.first
            val target = entry.second
            val slug = entry.third
            val attrs = item.attributes ?: emptyMap()
            var current = 0L
            attrs.forEach { (k, v) ->
                if (k.startsWith("completion_")) {
                    current = (parseNumberAttr(v) ?: 0).toLong()
                }
            }
            StwAchievement(
                id = tid,
                templateId = tid,
                name = name,
                progress = current,
                target = target,
                iconUrl = "https://pennydb.plingindigo.org/images/banners/achievement$slug.png"
            )
        }
    }

    private fun parseOutposts(metadataRes: McpQueryResponse?): List<StwOutpost> {
        val outposts = mutableListOf<StwOutpost>()
        metadataRes?.profileChanges?.firstOrNull()?.profile?.items?.forEach { (id, item) ->
            if (item.templateId.startsWith("Outpost:outpostcore_pve_", ignoreCase = true)) {
                val attrs = item.attributes ?: emptyMap()
                val coreInfo = attrs["outpost_core_info"] as? Map<*, *>
                val endurance = parseNumberAttr(coreInfo?.get("highestEnduranceWaveReached")) ?: 0
                val amplifiers = (coreInfo?.get("placedBuildings") as? List<*>)?.mapNotNull { 
                    val b = it as? Map<*, *>
                    val bTag = b?.get("buildingTag")?.toString()
                    val pTag = b?.get("placedTag")?.toString()
                    if (bTag != null && pTag != null) StwAmplifier(bTag, pTag) else null
                } ?: emptyList()
                
                val name = when {
                    item.templateId.endsWith("_01") -> "Stonewood"
                    item.templateId.endsWith("_02") -> "Plankerton"
                    item.templateId.endsWith("_03") -> "Canny Valley"
                    item.templateId.endsWith("_04") -> "Twine Peaks"
                    else -> "Outpost"
                }
                outposts.add(StwOutpost(id = id, templateId = item.templateId, name = name, level = parseNumberAttr(attrs["level"]) ?: 0, enduranceWave = endurance, amplifiers = amplifiers))
            }
        }
        return outposts.sortedBy { it.templateId }
    }

    private fun parseCollectionBook(responses: Map<String, McpQueryResponse?>): List<StwCollectionCategory> {
        val allPages = mutableListOf<StwCollectionPage>()
        
        fun parseProfilePages(res: McpQueryResponse?) {
            val profileItems = res?.profileChanges?.firstOrNull()?.profile?.items ?: return
            profileItems.filter { it.value.templateId.startsWith("CollectionBookPage:", true) }.forEach { (id, item) ->
                val tid = item.templateId
                val attrs = item.attributes ?: emptyMap()
                val slotted = profileItems.filter { it.value.attributes?.get("collection_book_page_id") == id }
                    .map { parseToInventoryItem(it.key, it.value) }
                
                allPages.add(StwCollectionPage(
                    id = id,
                    templateId = tid,
                    name = StwMetadataRepository.resolveName(tid) ?: cleanCbPageName(tid),
                    state = attrs["state"]?.toString() ?: "Unknown",
                    slottedItems = slotted
                ))
            }
        }
        
        parseProfilePages(responses["collection_book_people0"])
        parseProfilePages(responses["collection_book_schematics0"])
        
        return allPages.groupBy { categorizeCbPage(it.templateId) }
            .map { (name, pages) -> StwCollectionCategory(name, pages.sortedBy { it.name }) }
            .sortedBy { it.name }
    }

    private fun parseLlamas(shopRes: McpQueryResponse?): List<StwLlama> {
        val llamas = mutableListOf<StwLlama>()
        shopRes?.profileChanges?.firstOrNull()?.profile?.items?.forEach { (id, data) ->
            val tid = data.templateId
            if (tid.startsWith("CardPack:CardPack_", ignoreCase = true)) {
                val attrs = data.attributes ?: emptyMap()
                val price = parseNumberAttr(attrs["price"]) ?: 0
                val currency = attrs["currency_type"]?.toString() ?: "X-Ray Tickets"
                llamas.add(StwLlama(id = id, templateId = tid, name = StwMetadataRepository.resolveName(tid) ?: tid, description = StwMetadataRepository.resolveLocalizedItemType("cardpack") ?: "Loot Llama", price = price, currency = currency, iconUrl = "/images/resources/llama.png"))
            }
        }
        return llamas
    }

    suspend fun openLlama(accessToken: String, accountId: String, llamaId: String): Result<Boolean> {
        return executeStwAction(accessToken, accountId, "OpenCardPack", profileId = "shop_storefront", body = mapOf("cardPackItemId" to llamaId)).map { true }
    }

    suspend fun executeStwAction(
        accessToken: String,
        accountId: String,
        action: String,
        profileId: String = "campaign",
        rvn: Int = -1,
        body: Map<String, Any> = emptyMap()
    ): Result<McpQueryResponse> {
        return try {
            val response = api.executeMcpAction(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                action = action,
                profileId = profileId,
                rvn = rvn,
                body = body
            )
            Result.success(response)
        } catch (e: Exception) { 
            val message = if (e is HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    // Extract errorCode from JSON if possible
                    errorBody ?: e.message()
                } catch (ioe: IOException) { e.message() }
            } else e.localizedMessage ?: "Action failed"
            Result.failure(Exception(message))
        }
    }

    suspend fun recycleItems(accessToken: String, accountId: String, itemIds: List<String>, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "RecycleItemBatch", rvn = rvn, body = mapOf("targetItemIds" to itemIds))
    }

    suspend fun upgradeItemBulk(accessToken: String, accountId: String, itemId: String, numLevels: Int, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "UpgradeItemBulk", rvn = rvn, body = mapOf("targetItemId" to itemId, "numLevels" to numLevels))
    }

    suspend fun slotToCollectionBook(accessToken: String, accountId: String, itemId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "SlotCollectionBookItem", rvn = rvn, body = mapOf("targetItemId" to itemId))
    }

    suspend fun unslotFromCollectionBook(accessToken: String, accountId: String, itemId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "UnslotCollectionBookItem", rvn = rvn, body = mapOf("targetItemId" to itemId))
    }

    suspend fun evolveItem(accessToken: String, accountId: String, itemId: String, evolutionIndex: Int, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "EvolveItem", rvn = rvn, body = mapOf("targetItemId" to itemId, "evolutionIndex" to evolutionIndex))
    }

    suspend fun claimResearchPoints(accessToken: String, accountId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "ClaimCollectedResources", rvn = rvn, body = mapOf("collectors" to listOf("research_node_default_page")))
    }

    suspend fun purchaseResearchStat(accessToken: String, accountId: String, statId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "PurchaseResearchStat", rvn = rvn, body = mapOf("statId" to statId))
    }

    suspend fun claimDailyReward(accessToken: String, accountId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "ClaimDailyReward", rvn = rvn)
    }

    suspend fun claimMissions(accessToken: String, accountId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "ClaimMissions", rvn = rvn)
    }

    suspend fun assignHeroToLoadout(accessToken: String, accountId: String, loadoutId: String, slotName: String, heroId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "SetCampaignHeroLoadoutSlot", rvn = rvn, body = mapOf("loadoutId" to loadoutId, "slotName" to slotName, "targetItemId" to heroId))
    }

    suspend fun clearHeroLoadoutSlot(accessToken: String, accountId: String, loadoutId: String, slotName: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "SetCampaignHeroLoadoutSlot", rvn = rvn, body = mapOf("loadoutId" to loadoutId, "slotName" to slotName, "targetItemId" to ""))
    }

    suspend fun assignTeamPerkToLoadout(accessToken: String, accountId: String, loadoutId: String, teamPerkId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "AssignTeamPerkToLoadout", rvn = rvn, body = mapOf("loadoutId" to loadoutId, "teamPerkId" to teamPerkId))
    }

    suspend fun assignGadgetToLoadout(accessToken: String, accountId: String, loadoutId: String, slotIndex: Int, gadgetId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "AssignGadgetToLoadout", rvn = rvn, body = mapOf("loadoutId" to loadoutId, "slotIndex" to slotIndex, "gadgetTemplateId" to gadgetId))
    }

    suspend fun assignWorkerToSquad(accessToken: String, accountId: String, squadId: String, slotIndex: Int, workerId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "AssignWorkerToSquad", rvn = rvn, body = mapOf("squadId" to squadId, "slotIndex" to slotIndex, "targetItemId" to workerId))
    }

    suspend fun claimQuestReward(accessToken: String, accountId: String, questId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "ClaimQuestReward", rvn = rvn, body = mapOf("questId" to questId))
    }

    suspend fun rerollDailyQuest(accessToken: String, accountId: String, questId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "FortRerollDailyQuest", profileId = "campaign", rvn = rvn, body = mapOf("questId" to questId))
    }

    suspend fun disassembleItems(accessToken: String, accountId: String, itemPairs: List<Map<String, Any>>, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "DisassembleWorldItems", profileId = "theater0", rvn = rvn, body = mapOf("targetItemIdAndQuantityPairs" to itemPairs))
    }

    suspend fun destroyItems(accessToken: String, accountId: String, itemIds: List<String>, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "DestroyWorldItems", profileId = "theater0", rvn = rvn, body = mapOf("itemIds" to itemIds))
    }

    suspend fun setActiveHeroLoadout(accessToken: String, accountId: String, loadoutId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "SetActiveCampaignHeroLoadout", rvn = rvn, body = mapOf("selectedId" to loadoutId))
    }

    suspend fun purchaseCatalogEntry(
        accessToken: String, 
        accountId: String, 
        offerId: String, 
        purchaseQuantity: Int, 
        currency: String, 
        currencySubType: String, 
        expectedTotalPrice: Int, 
        rvn: Int
    ): Result<McpQueryResponse> {
        return executeStwAction(
            accessToken, accountId, "PurchaseCatalogEntry", 
            profileId = "common_core", rvn = rvn, 
            body = mapOf(
                "offerId" to offerId,
                "purchaseQuantity" to purchaseQuantity,
                "currency" to currency,
                "currencySubType" to currencySubType,
                "expectedTotalPrice" to expectedTotalPrice,
                "gameContext" to "fn"
            )
        )
    }

    suspend fun fetchStorefront(accessToken: String): Result<Map<String, Any>> {
        return try {
            val response = api.getStwStorefront(bearerToken = "Bearer $accessToken")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStwWorldInfoFull(context: Context, accessToken: String): Result<List<StwMissionAlert>> {
        return try {
            StwMetadataRepository.loadMetadata(context)
            val response = api.getStwWorldInfo(bearerToken = "Bearer $accessToken")
            val theaters = response["theaters"] as? List<*> ?: emptyList<Any>()
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
                val zoneName = theaterNames.entries.find { theaterId.contains(it.key) }?.value ?: theater["displayName"] as? String ?: "Other"
                val slots = theater["slots"] as? List<*> ?: emptyList<Any>()
                slots.filterIsInstance<Map<*, *>>().forEach { slot ->
                    val missionData = slot["missionData"] as? Map<*, *> ?: return@forEach
                    val rewards = mutableListOf<StwReward>()
                    (missionData["missionReward"] as? List<*>)?.forEach { if (it is Map<*, *>) rewards.add(parseStwRewardInternal(it)) }
                    val bonus = mutableListOf<StwReward>()
                    (missionData["bonusMissionRewards"] as? List<*>)?.forEach { if (it is Map<*, *>) bonus.add(parseStwRewardInternal(it)) }

                    allMissions.add(StwMissionAlert(
                        id = UUID.randomUUID().toString(),
                        name = missionData["missionName"] as? String ?: "Mission",
                        zoneName = zoneName,
                        missionType = if (bonus.isNotEmpty()) "Alert" else "Standard",
                        difficulty = (missionData["difficulty"] as? Number)?.toDouble() ?: 1.0,
                        rewards = rewards,
                        bonusRewards = bonus,
                        biome = "", requirements = "", modifiers = emptyList()
                    ))
                }
            }
            Result.success(allMissions.sortedBy { it.difficulty })
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun parseStwRewardInternal(data: Map<*, *>): StwReward {
        val tid = data["itemType"] as? String ?: "Unknown"
        return StwReward(
            id = tid,
            name = StwMetadataRepository.resolveName(tid) ?: tid,
            quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
            rarity = StwMetadataRepository.resolveRarity(tid) ?: determineRarity(tid, null),
            iconUrl = getStwRewardIcon(tid)
        )
    }

    private fun getStwRewardIcon(itemType: String): String? {
        return StwMetadataRepository.resolveStwResourceIcon(itemType)
    }

    private fun parseStwHero(id: String, data: McpItemData): StwHero {
        val tid = data.templateId
        val attrs = data.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val rating = StwMetadataRepository.resolvePowerLevel(tid, level) ?: parseNumberAttr(attrs["hero_rating"]) ?: 0
        val name = StwMetadataRepository.resolveName(tid) ?: tid
        val rarity = determineRarity(tid, attrs)
        val type = StwMetadataRepository.determineHeroClass(tid)
        
        return StwHero(
            id = id,
            templateId = tid,
            name = name,
            rarity = rarity,
            level = level,
            rating = rating,
            classType = type,
            gender = attrs["gender"]?.toString(),
            iconUrl = StwMetadataRepository.resolveStwHeroIcon(tid),
            description = StwMetadataRepository.resolveDescription(tid)
        )
    }

    private fun parseStwSchematic(id: String, data: McpItemData): StwSchematic {
        val tid = data.templateId
        val attrs = data.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val rating = StwMetadataRepository.resolvePowerLevel(tid, level) ?: parseNumberAttr(attrs["rating"]) ?: 0
        val alterationsRaw = (attrs["slotted_alterations"] as? List<*>) ?: (attrs["alterations"] as? List<*>)
        
        val perks = alterationsRaw?.mapNotNull { 
            val raw = it?.toString() ?: ""
            if (raw.contains("Alteration:")) {
                val aid = raw.substringAfter("Alteration:")
                StwPerk(id = aid, name = aid, rarity = determinePerkRarity(aid))
            } else null
        } ?: emptyList()
        
        val name = StwMetadataRepository.resolveName(tid) ?: tid
        val rarity = determineRarity(tid, attrs)
        val type = StwMetadataRepository.resolveType(tid) ?: (if (tid.contains("trap", true)) "Trap" else "Weapon")
        
        return StwSchematic(
            id = id,
            templateId = tid,
            name = name,
            rarity = rarity,
            level = level,
            rating = rating,
            type = type,
            iconUrl = getStwSchematicIcon(tid),
            perks = perks,
            alterations = alterationsRaw?.mapNotNull { it?.toString() } ?: emptyList(),
            durability = (attrs["durability"] as? Number)?.toFloat() ?: (attrs["current_durability"] as? Number)?.toFloat(),
            isSlotted = attrs["collection_book_page_id"] != null
        )
    }

    private fun determinePerkRarity(aid: String): String {
        val lower = aid.lowercase()
        return when {
            lower.endsWith("_t05") || lower.contains("_sr") -> "legendary"
            lower.endsWith("_t04") || lower.contains("_vr") -> "epic"
            lower.endsWith("_t03") || lower.contains("_r") -> "rare"
            lower.endsWith("_t02") || lower.contains("_uc") -> "uncommon"
            else -> "common"
        }
    }

    private fun parseStwSurvivor(id: String, data: McpItemData): StwSurvivor {
        val tid = data.templateId
        val attrs = data.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val rating = StwMetadataRepository.resolvePowerLevel(tid, level) ?: parseNumberAttr(attrs["rating"]) ?: 0
        val name = StwMetadataRepository.resolveName(tid) ?: tid
        val rarity = determineRarity(tid, attrs)
        
        val rawPersonality = attrs["personality"]?.toString() ?: ""
        val personality = rawPersonality.substringAfterLast(".").replace("Is", "").trim()
        val rawSetBonus = attrs["set_bonus"]?.toString() ?: ""
        val setBonus = rawSetBonus.substringAfterLast(".").replace("Is", "").replace("Low", "").replace("High", "").trim()
        
        return StwSurvivor(
            id = id,
            templateId = tid,
            name = name,
            rarity = rarity,
            level = level,
            rating = rating,
            personality = personality,
            setBonus = setBonus,
            squadId = attrs["squad_id"]?.toString(),
            squadSlotIdx = parseNumberAttr(attrs["squad_slot_idx"]) ?: -1,
            gender = attrs["gender"]?.toString(),
            portrait = attrs["portrait"]?.toString(),
            isLead = tid.contains("manager", true) || tid.contains("lead", true),
            synergy = attrs["managerSynergy"]?.toString()?.substringAfterLast(".")?.replace("Is", ""),
            iconUrl = StwMetadataRepository.resolveStwSurvivorIcon(tid, attrs["portrait"]?.toString())
        )
    }

    private fun parseStwDefender(id: String, data: McpItemData): StwDefender {
        val tid = data.templateId
        val attrs = data.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val rating = StwMetadataRepository.resolvePowerLevel(tid, level) ?: parseNumberAttr(attrs["rating"]) ?: 0
        val name = StwMetadataRepository.resolveName(tid) ?: tid
        val rarity = determineRarity(tid, attrs)
        
        val alterationsRaw = (attrs["alterations"] as? List<*>)
        val perks = alterationsRaw?.mapNotNull { it.toString() } ?: emptyList()

        return StwDefender(
            id = id, 
            templateId = tid, 
            name = name, 
            rarity = rarity, 
            type = StwMetadataRepository.resolveType(tid) ?: "Defender", 
            level = level, 
            rating = rating, 
            iconUrl = getStwDefenderIcon(tid),
            perks = perks
        )
    }

    private fun parseStwQuest(id: String, data: McpItemData): FortniteQuest? {
        val tid = data.templateId.lowercase()
        if (!tid.startsWith("quest:daily_")) return null
        
        val attrs = data.attributes ?: emptyMap()
        val state = attrs["quest_state"]?.toString() ?: ""
        if (state.equals("claimed", true)) return null
        
        val dailyMeta = StwMetadataRepository.resolveDailyQuest(tid)
        val nameMap = dailyMeta?.name
        val resolvedName = (nameMap?.get("en") as? String) ?: StwMetadataRepository.resolveName(tid) ?: tid
        
        val objectives = mutableListOf<QuestObjective>()
        dailyMeta?.objectives?.forEach { (objKey, objMeta) ->
            val target = objMeta.count ?: 1
            var current = 0
            
            // Try to find matching completion attribute
            attrs.forEach { (k, v) ->
                if (k.startsWith("completion_")) {
                    val attrKey = k.substringAfter("completion_").lowercase()
                    // Match if attrKey contains key words from objKey or if tid contains attrKey
                    if (attrKey.contains("kill_husk") || attrKey.contains("complete_primary") || tid.contains(attrKey)) {
                        val count = parseNumberAttr(v) ?: 0
                        if (count > current) current = count
                    }
                }
            }
            
            // Heuristic for specific daily types
            if (current == 0) {
                if (tid.contains("huskextermination")) {
                    current = parseNumberAttr(attrs.entries.find { it.key.contains("kill_husk") }?.value) ?: 0
                } else if (tid.contains("mission_specialist")) {
                    current = parseNumberAttr(attrs.entries.find { it.key.contains("complete_primary") || it.key.contains("complete_mission") }?.value) ?: 0
                }
            }
            
            if (state.equals("completed", true)) current = target
            
            objectives.add(QuestObjective(id = objKey, description = objKey, current = current, target = target))
        }

        val totalProgress = objectives.firstOrNull()?.current ?: 0
        val totalTarget = objectives.firstOrNull()?.target ?: 1

        val reward = if (resolvedName.contains("Mission Veteran", true) || resolvedName.contains("All Together Now", true)) 150 else 100

        return FortniteQuest(
            id = id, 
            templateId = tid, 
            name = resolvedName, 
            description = dailyMeta?.objectives?.keys?.firstOrNull() ?: "Daily Quest", 
            category = "Daily Quests", 
            progress = totalProgress, 
            target = totalTarget, 
            isCompleted = state.equals("completed", true), 
            objectives = objectives, 
            rewardXp = 0,
            rewardMtx = reward
        )
    }

    private fun getStwDefenderIcon(templateId: String): String {
        val baseId = templateId.substringAfter(":").lowercase()
            .replace("did_", "")
            .replace(Regex("(_sr|_vr|_r|_uc|_c|_myth|_ur)$"), "")
            .replace(Regex("_t\\d+$"), "")
            .replace(Regex("(_sr|_vr|_r|_uc|_c|_myth|_ur)_t\\d+$"), "")
            .replace(Regex("_(sr|vr|r|uc|c|myth|ur|t\\d+)$"), "")
        
        return "https://pennydb.plingindigo.org/images/defenders/$baseId.png"
    }

    private fun getStwSchematicIcon(templateId: String): String {
        val slug = StwMetadataRepository.resolveSlug(templateId) 
            ?: templateId.substringAfter(":").lowercase().replace(" ", "_").replace("\"", "").replace(".", "")
        
        val isTrap = templateId.contains("trap", true) || 
                     templateId.contains("floor_", true) || 
                     templateId.contains("wall_", true) || 
                     templateId.contains("ceiling_", true) ||
                     templateId.contains("sid_floor", true) ||
                     templateId.contains("sid_wall", true) ||
                     templateId.contains("sid_ceiling", true)
                     
        val folder = if (isTrap) "traps" else "weapons"
        return "https://pennydb.plingindigo.org/images/$folder/$slug.png"
    }

    private fun getStwBackpackIconUrl(templateId: String, type: String): String {
        // Preference 1: Try resolving English name from metadata (lowercase with underscores)
        val slug = StwMetadataRepository.resolveSlug(templateId) 
            ?: templateId.substringAfter(":").lowercase().replace(" ", "_").replace("\"", "").replace(".", "")
        
        val folder = when {
            type == "Ranged" || type == "Melee" -> "weapons"
            type == "Trap" -> "traps"
            type == "Material" || templateId.contains("ingredient", ignoreCase = true) || templateId.contains("worlditem", ignoreCase = true) -> "ingredients"
            else -> "resources"
        }

        return "https://pennydb.plingindigo.org/images/$folder/$slug.png"
    }

    private fun parseNumberAttr(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toDoubleOrNull()?.toInt() ?: value.toIntOrNull()
            else -> null
        }
    }

    private fun parseToInventoryItem(id: String, item: McpItemData): StwInventoryItem {
        val tid = item.templateId
        val attrs = item.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val rating = StwMetadataRepository.resolvePowerLevel(tid, level) ?: 0
        val durability = (attrs["durability"] as? Number)?.toFloat() ?: (attrs["current_durability"] as? Number)?.toFloat()
        
        val name = StwMetadataRepository.resolveName(tid) ?: tid

        val rarity = determineRarity(tid, attrs)
        
        val type = when {
            tid.startsWith("Ingredient", ignoreCase = true) || tid.startsWith("WorldItem", ignoreCase = true) -> "Material"
            tid.startsWith("Ammo", ignoreCase = true) -> "Ammo"
            tid.startsWith("Trap", ignoreCase = true) || tid.contains(":tid_", ignoreCase = true) -> "Trap"
            tid.startsWith("Weapon", ignoreCase = true) || tid.contains(":wid_", ignoreCase = true) -> {
                if (StwMetadataRepository.isMelee(tid)) "Melee" else "Ranged"
            }
            else -> "Item"
        }

        return StwInventoryItem(
            id = id,
            templateId = tid,
            name = name ?: tid,
            quantity = item.quantity,
            level = level,
            rating = rating,
            rarity = rarity,
            type = type,
            durability = durability,
            iconUrl = getStwBackpackIconUrl(tid, type)
        )
    }

    private fun cleanCbPageName(tid: String): String {
        return tid.substringAfter(":").replace("page", "", ignoreCase = true).replace("_", " ").trim().split(" ").filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun determineResourceType(tid: String): StwResourceType {
        val lower = tid.lowercase()
        return when {
            lower.contains("currency:gold") || lower.contains("eventcurrency_scaling") || lower.contains("currency:mtx") || lower.contains("currency:xray") -> StwResourceType.CURRENCY
            lower.contains("xp") -> StwResourceType.XP
            lower.contains("reagent_c_t") || lower.contains("reagent_people") || lower.contains("reagent_schematic") || lower.contains("reagent_trapschematic") -> StwResourceType.EVOLUTION_MATERIAL
            lower.contains("reagent_alteration") -> StwResourceType.PERK_RESOURCE
            lower.contains("reagent_evolverarity") -> StwResourceType.FLUX
            lower.contains("eventcurrency") || lower.contains("cardpackticket") -> StwResourceType.TICKET
            lower.contains("voucher") -> StwResourceType.VOUCHER
            lower.contains("reagent_promotion") -> StwResourceType.SUPERCHARGER
            lower.contains("ingredient") -> StwResourceType.CRAFTING_MATERIAL
            else -> StwResourceType.OTHER
        }
    }

    private fun categorizeCbPage(tid: String): String {
        val lower = tid.lowercase()
        return when {
            lower.contains("hero") -> "Heroes"
            lower.contains("people") || lower.contains("worker") || lower.contains("survivor") || lower.contains("defender") || lower.contains("lead") -> "People"
            lower.contains("ranged") || lower.contains("assault") || lower.contains("shotgun") || lower.contains("pistol") || lower.contains("sniper") || lower.contains("explosive") -> "Ranged Weapons"
            lower.contains("melee") || lower.contains("axe") || lower.contains("club") || lower.contains("scythe") || lower.contains("spear") || lower.contains("sword") || lower.contains("tool") -> "Melee Weapons"
            lower.contains("trap") -> "Traps"
            else -> "Special & Others"
        }
    }
}
