package com.dhyper.fncompanion.data.repository

import android.content.Context
import android.util.Log
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
    private val publicApi = ApiClient.publicApi

    private var cosmeticsCacheMap: Map<String, CosmeticItem>? = null

    suspend fun fetchBasicProfile(accessToken: String): Result<EpicVerifyResponse> {
        return try {
            val response = api.verifyToken(bearerToken = "Bearer $accessToken")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLocker(
        accessToken: String,
        accountId: String
    ): Result<List<ParsedLockerItem>> = coroutineScope {
        try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "athena"
            )

            val profile = response.profileChanges?.firstOrNull()?.profile ?: return@coroutineScope Result.failure(Exception("No profile found"))
            val items = profile.items ?: emptyMap()

            // Fetch all cosmetics metadata for mapping
            if (cosmeticsCacheMap == null) {
                try {
                    val allCosmetics = publicApi.getAllCosmetics()
                    cosmeticsCacheMap = allCosmetics?.data?.associateBy { it.id.lowercase() }
                } catch (e: Exception) {
                    cosmeticsCacheMap = emptyMap()
                }
            }

            val parsedItems = mutableListOf<ParsedLockerItem>()
            items.forEach { (id, item) ->
                val tid = item.templateId
                val category = determineCategory(tid)
                if (category != LockerCategory.OTHER) {
                    val cosmeticId = tid.substringAfter(":").lowercase()
                    val meta = cosmeticsCacheMap?.get(cosmeticId)
                    
                    val attrs = item.attributes ?: emptyMap()

                    parsedItems.add(
                        ParsedLockerItem(
                            id = id,
                            templateId = tid,
                            cosmeticId = cosmeticId,
                            category = category,
                            name = meta?.name ?: cleanName(tid),
                            description = meta?.description,
                            rarity = meta?.rarity?.value ?: "Common",
                            iconUrl = meta?.images?.icon ?: meta?.images?.smallIcon,
                            largeIconUrl = meta?.images?.featured ?: meta?.images?.icon,
                            isFavorite = attrs["favorite"] == true,
                            isArchived = attrs["archived"] == true,
                            quantity = item.quantity,
                            introduction = meta?.introduction,
                            set = meta?.set,
                            added = meta?.added
                        )
                    )
                }
            }

            Result.success(parsedItems.sortedByDescending { it.isFavorite })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPersonalLockerCosmetics(accessToken: String, accountId: String): Result<List<ParsedLockerItem>> {
        return fetchLocker(accessToken, accountId)
    }

    suspend fun fetchVBucksBalance(accessToken: String, accountId: String): Result<Long> {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "common_core"
            )
            var balance = 0L
            response.profileChanges?.firstOrNull()?.profile?.items?.values?.forEach { item ->
                if (item.templateId.startsWith("Currency:Mtx", true)) balance += item.quantity
            }
            Result.success(balance)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun fetchPersonalCareerDetails(accessToken: String, accountId: String): Result<AccountCareerDetails> {
        return try {
            val response = api.queryMcpProfile(
                bearerToken = "Bearer $accessToken",
                accountId = accountId,
                profileId = "athena"
            )
            val stats = response.profileChanges?.firstOrNull()?.profile?.stats?.attributes ?: emptyMap()
            
            Result.success(AccountCareerDetails(
                accountName = "Epic Player",
                accountId = accountId,
                firstPlayed = "",
                lastPlayed = "",
                lifetimeWins = (stats["wins"] as? Number)?.toInt() ?: 0,
                seasonalWins = 0,
                accountLevel = (stats["account_level"] as? Number)?.toInt() ?: 1,
                seasonalLevel = (stats["level"] as? Number)?.toInt() ?: 1,
                currentSeasonName = "Season",
                currentBattlePassTier = (stats["book_level"] as? Number)?.toInt() ?: 1,
                pastSeasons = emptyList()
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun determineCategory(templateId: String): LockerCategory {
        val tid = templateId.lowercase()
        return when {
            tid.startsWith("athenacharacter:") || tid.startsWith("cid_") || tid.startsWith("character_") -> LockerCategory.OUTFIT
            tid.startsWith("athenabackpack:") || tid.startsWith("bid_") || tid.startsWith("backpack_") || tid.startsWith("petid_") || tid.contains("athenapet") -> LockerCategory.BACK_BLING
            tid.startsWith("athenapickaxe:") || tid.startsWith("pickaxe_") -> LockerCategory.PICKAXE
            tid.startsWith("athenaglider:") || tid.startsWith("glider_") || tid.startsWith("umbrella_") || tid.endsWith("_umbrella") -> LockerCategory.GLIDER
            tid.startsWith("athenaskydivecontrail:") || tid.startsWith("contrail_") || tid.startsWith("trails_id_") -> LockerCategory.CONTRAIL
            tid.startsWith("athenadance:") || tid.startsWith("eid_") || tid.startsWith("dance_") -> LockerCategory.EMOTE
            tid.startsWith("athenaitemwrap:") || tid.startsWith("wrap_") -> LockerCategory.WRAP
            tid.startsWith("athenamusicpack:") || tid.startsWith("musicpack_") -> LockerCategory.MUSIC
            tid.startsWith("athenaloadingscreen:") || tid.startsWith("lsid_") || tid.startsWith("loadingscreen_") -> LockerCategory.LOADING_SCREEN
            tid.startsWith("emoji_") || tid.startsWith("emoticon_") -> LockerCategory.EMOTICON
            tid.startsWith("spid_") || tid.startsWith("spray_") -> LockerCategory.SPRAY
            tid.startsWith("companion_") -> LockerCategory.SIDEKICK
            tid.startsWith("sid_") -> LockerCategory.JAM_TRACK
            tid.startsWith("banner") || tid.startsWith("br") || tid.startsWith("standardbanner") -> LockerCategory.BANNER
            tid.startsWith("shoes_") -> LockerCategory.KICKS
            tid.startsWith("carbody_") || tid.startsWith("id_body_") || tid.startsWith("body_") -> LockerCategory.CAR
            tid.startsWith("carskin_") || tid.startsWith("id_skin_") -> LockerCategory.CAR_DECAL
            tid.startsWith("wheel_") || tid.startsWith("id_wheel_") -> LockerCategory.WHEELS
            tid.startsWith("id_drifttrail_") -> LockerCategory.CAR_TRAIL
            tid.startsWith("id_booster_") -> LockerCategory.CAR_BOOST
            tid.startsWith("sparks_") && tid.contains("guitar") -> LockerCategory.GUITAR
            tid.startsWith("sparks_") && tid.contains("bass") -> LockerCategory.BASS
            tid.startsWith("sparks_") && tid.contains("drumkit") -> LockerCategory.DRUMS
            tid.startsWith("sparks_") && tid.contains("keytar") -> LockerCategory.KEYTAR
            tid.startsWith("sparks_") && tid.contains("mic") -> LockerCategory.MIC
            tid.startsWith("jbsid_") -> LockerCategory.LEGO_BUILD
            tid.startsWith("jbpid_") -> LockerCategory.LEGO_DECOR
            tid.startsWith("sparksaura_") || tid.startsWith("aura_") -> LockerCategory.AURA
            else -> LockerCategory.OTHER
        }
    }

    private fun cleanName(templateId: String): String {
        return templateId.substringAfter(":").replace("_", " ")
            .split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    // --- SAVE THE WORLD (STW) LOGIC ---

    suspend fun fetchStwHomebaseData(
        context: Context,
        accessToken: String,
        accountId: String
    ): Result<StwHomebaseData> = coroutineScope {
        return@coroutineScope try {
            StwMetadataRepository.loadMetadata(context)

            val profileIds = listOf(
                "campaign", "metadata", "common_core",
                "collection_book_people0", "collection_book_schematics0",
                "theater0", "theater1", "theater2", "outpost0", "recycle_bin", "collections", "shop_storefront"
            )

            val deferredProfiles = profileIds.associateWith { pid ->
                async { 
                    try {
                        api.queryMcpProfile(bearerToken = "Bearer $accessToken", accountId = accountId, profileId = pid)
                    } catch (e: Exception) {
                        Log.e("EpicAccountRepo", "Failed to fetch profile $pid", e)
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

            // 1. Resources & Stats
            val resources = items.filter { (id, item) -> 
                val tid = item.templateId.lowercase()
                tid.startsWith("currency") || tid.contains("xp") || tid.startsWith("reagent") || tid.startsWith("ingredient") || tid.contains("ticket") || tid.contains("voucher")
            }.map { (id, item) ->
                val tid = item.templateId
                StwResource(
                    templateId = tid,
                    name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid),
                    quantity = item.quantity.toLong(),
                    type = determineResourceType(tid),
                    rarity = determineRarity(tid, item.attributes),
                    iconUrl = getStwRewardIcon(tid)
                )
            }.sortedWith(compareBy({ it.type }, { it.rarity }, { it.name }))

            val gold = resources.find { it.templateId.contains("currency:gold", true) || it.templateId.contains("eventcurrency_scaling", true) }?.quantity ?: 0L
            val xray = resources.find { it.templateId.contains("currency:xraytickets", true) }?.quantity ?: 0L
            val vbucks = sumCurrency(responses["common_core"]?.profileChanges?.firstOrNull()?.profile?.items ?: emptyMap(), "currency:mtx")

            // 2. Research
            val resStats = stats["research_levels"] as? Map<*, *>
            val research = StwResearch(
                fortitude = parseNumberAttr(resStats?.get("fortitude")) ?: 0,
                offense = parseNumberAttr(resStats?.get("offense")) ?: 0,
                resistance = parseNumberAttr(resStats?.get("resistance")) ?: 0,
                technology = parseNumberAttr(resStats?.get("technology")) ?: 0,
                totalLevels = parseNumberAttr(stats["level"]) ?: 0
            )

            // 3. Main Entities
            val allHeroes = items.filter { it.value.templateId.startsWith("Hero:", true) }.map { parseStwHero(it.key, it.value) }
            val allSchematics = items.filter { it.value.templateId.startsWith("Schematic:", true) }.map { parseStwSchematic(it.key, it.value) }
            val allSurvivors = items.filter { it.value.templateId.startsWith("Worker:", true) }.map { parseStwSurvivor(it.key, it.value) }
            val allDefenders = items.filter { it.value.templateId.startsWith("Defender:", true) }.map { parseStwDefender(it.key, it.value) }
            val dailyQuests = items.filter { it.value.templateId.contains("Quest:Daily", true) || it.value.templateId.contains("Quest:stonewoodquest", true) }.mapNotNull { parseStwQuest(it.key, it.value) }

            // 4. Loadouts
            val heroMap = allHeroes.associateBy { it.id }
            val loadouts = items.filter { it.value.templateId.startsWith("CampaignHeroLoadout:", true) }
                .map { (id, item) -> parseHeroLoadout(id, item, heroMap, id == stats["selected_hero_loadout"]?.toString()) }
                .sortedBy { it.index }

            // 5. Squads
            val survivorMap = allSurvivors.associateBy { it.id }
            val squads = parseSurvivorSquads(stats, survivorMap)

            // 6. Outposts
            val outposts = parseOutposts(responses["metadata"])

            // 7. Inventory (Backpack & Storage)
            val backpackItems = mutableListOf<StwInventoryItem>()
            listOf("theater0", "theater1", "theater2").forEach { pid ->
                responses[pid]?.profileChanges?.firstOrNull()?.profile?.items?.forEach { (id, item) ->
                    backpackItems.add(parseToInventoryItem(id, item))
                }
            }
            val storageItems = responses["outpost0"]?.profileChanges?.firstOrNull()?.profile?.items?.map { (id, item) ->
                parseToInventoryItem(id, item)
            } ?: emptyList()

            // 8. Collection Book
            val cbCategories = parseCollectionBook(responses)

            // 9. Llamas
            val llamas = parseLlamas(responses["shop_storefront"])

            Result.success(StwHomebaseData(
                powerLevel = parseNumberAttr(stats["homebase_rating"])?.toDouble() ?: 1.0,
                commanderLevel = parseNumberAttr(stats["level"]) ?: 1,
                vbucks = vbucks,
                xrayTickets = xray,
                gold = gold,
                resources = resources,
                research = research,
                inventory = StwInventory(backpack = backpackItems.sortedBy { it.name }, storage = storageItems.sortedBy { it.name }),
                collectionBook = StwCollectionBook(
                    level = parseNumberAttr((responses["collections"]?.profileChanges?.firstOrNull()?.profile?.stats?.attributes?.get("collection_book_level"))) ?: 0,
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
                dailyQuests = dailyQuests.sortedBy { it.isCompleted },
                llamas = llamas,
                totalDaysLoggedIn = (stats["daily_rewards"] as? Map<*, *>)?.get("totalDaysLoggedIn")?.let { parseNumberAttr(it) } ?: 0,
                matchesPlayed = parseNumberAttr(stats["matches_played"]) ?: 0,
                zonesCompleted = (stats["gameplay_stats"] as? List<*>)?.find { (it as? Map<*, *>)?.get("statName") == "zonescompleted" }?.let { parseNumberAttr((it as Map<*, *>)["statValue"]) } ?: 0,
                commanderXp = (stats["xp"] as? Number)?.toLong() ?: 0L,
                packsGranted = parseNumberAttr(stats["packs_granted"]) ?: 0,
                profileRevisions = revisions
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun sumCurrency(items: Map<String, McpItemData>, vararg tids: String): Long {
        return items.values.filter { item -> tids.any { tid -> item.templateId.contains(tid, true) } }.sumOf { it.quantity.toLong() }
    }

    private fun parseHeroLoadout(id: String, item: McpItemData, heroMap: Map<String, StwHero>, isActive: Boolean): StwHeroLoadout {
        val attrs = item.attributes ?: emptyMap()
        val crew = attrs["crew_members"] as? Map<*, *>
        val gadgetsRaw = attrs["gadgets"] as? List<*>
        
        val commander = crew?.get("commanderslot")?.toString()?.let { heroMap[it] }
        val support = (1..5).map { i -> crew?.get("followerslot$i")?.toString()?.let { heroMap[it] } }
        val gadgets = gadgetsRaw?.map { (it as? Map<*, *>)?.get("gadget")?.toString() } ?: emptyList()
        val teamPerkId = attrs["team_perk"]?.toString()
        
        return StwHeroLoadout(
            id = id,
            name = attrs["loadout_name"]?.toString() ?: "Loadout ${parseNumberAttr(attrs["loadout_index"]) ?: 0}",
            commander = commander,
            teamPerkId = teamPerkId,
            teamPerkName = teamPerkId?.let { StwMetadataRepository.resolveName(it) },
            support = support,
            gadgets = gadgets,
            isActive = isActive,
            index = parseNumberAttr(attrs["loadout_index"]) ?: 0
        )
    }

    private fun parseSurvivorSquads(stats: Map<String, Any?>, survivorMap: Map<String, StwSurvivor>): List<StwSurvivorSquad> {
        val squadsRaw = stats["squad_info"] as? Map<*, *> ?: return emptyList()
        val assignments = stats["squad_assignments"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        
        return squadsRaw.mapNotNull { (squadId, squadData) ->
            val sid = squadId.toString()
            if (!sid.startsWith("squad_attribute_")) return@mapNotNull null
            
            val data = squadData as? Map<*, *>
            val unlocked = parseNumberAttr(data?.get("slots_unlocked")) ?: 0
            
            val leadId = assignments.entries.find { it.key.toString() == "$sid:0" }?.value?.toString()
            val lead = leadId?.let { survivorMap[it] }
            
            val members = (1..7).map { i ->
                val mid = assignments.entries.find { it.key.toString() == "$sid:$i" }?.value?.toString()
                mid?.let { survivorMap[it] }
            }
            
            StwSurvivorSquad(
                id = sid,
                name = sid.substringAfterLast("_").replaceFirstChar { it.uppercase() },
                leadSlot = lead,
                memberSlots = members,
                unlockedSlots = unlocked,
                totalPowerContribution = 0
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
                val name = when {
                    item.templateId.endsWith("_01") -> "Stonewood"
                    item.templateId.endsWith("_02") -> "Plankerton"
                    item.templateId.endsWith("_03") -> "Canny Valley"
                    item.templateId.endsWith("_04") -> "Twine Peaks"
                    else -> "Outpost"
                }
                outposts.add(StwOutpost(id = id, templateId = item.templateId, name = name, level = parseNumberAttr(attrs["level"]) ?: 0, enduranceWave = endurance))
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
                llamas.add(StwLlama(id = id, templateId = tid, name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid), description = StwMetadataRepository.resolveLocalizedItemType("cardpack") ?: "Loot Llama", price = price, currency = currency))
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

    suspend fun modifyItemAttribute(accessToken: String, accountId: String, itemId: String, attributeName: String, newValue: Any, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "ModifyItemAttribute", rvn = rvn, body = mapOf("targetItemId" to itemId, "attributeName" to attributeName, "newValue" to newValue))
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

    suspend fun setActiveHeroLoadout(accessToken: String, accountId: String, loadoutId: String, rvn: Int): Result<McpQueryResponse> {
        return executeStwAction(accessToken, accountId, "SetActiveCampaignHeroLoadout", rvn = rvn, body = mapOf("selectedId" to loadoutId))
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
            name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid),
            quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
            rarity = StwMetadataRepository.resolveRarity(tid) ?: determineRarity(tid, null),
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
            else -> null
        }
    }

    private fun parseStwHero(id: String, data: McpItemData): StwHero {
        val tid = data.templateId
        val attrs = data.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val rating = StwMetadataRepository.resolvePowerLevel(tid, level) ?: parseNumberAttr(attrs["hero_rating"]) ?: 0
        val name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid)
        val rarity = determineRarity(tid, attrs)
        val type = StwMetadataRepository.resolveType(tid) ?: determineHeroClass(tid)
        
        return StwHero(
            id = id,
            templateId = tid,
            name = name,
            rarity = rarity,
            level = level,
            rating = rating,
            classType = type,
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
                StwPerk(id = aid, name = cleanPerkName(aid), rarity = determinePerkRarity(aid))
            } else null
        } ?: emptyList()
        
        val name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid)
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
            perks = perks,
            durability = (attrs["durability"] as? Number)?.toFloat(),
            isSlotted = attrs["collection_book_page_id"] != null
        )
    }

    private fun cleanPerkName(aid: String): String {
        return aid.replace(Regex("(?i)^AID_ATT_|^AID_|^ATT_"), "").replace(Regex("(?i)(_SR|_VR|_R|_UC|_C|_UR|_MYTH)(?=_|$)"), "").replace(Regex("(?i)(_T\\d+|_ORE|_CRYSTAL)(?=_|$)"), "").replace("_", " ").trim().uppercase()
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
        val name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid)
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
            isLead = tid.contains("manager", true) || tid.contains("lead", true),
            synergy = attrs["managerSynergy"]?.toString()?.substringAfterLast(".")?.replace("Is", "")
        )
    }

    private fun parseStwDefender(id: String, data: McpItemData): StwDefender {
        val tid = data.templateId
        val attrs = data.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val rating = StwMetadataRepository.resolvePowerLevel(tid, level) ?: parseNumberAttr(attrs["rating"]) ?: 0
        val name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid)
        val rarity = determineRarity(tid, attrs)
        
        val alterationsRaw = (attrs["alterations"] as? List<*>)
        val perks = alterationsRaw?.mapNotNull { cleanPerkName(it.toString()) } ?: emptyList()

        return StwDefender(id = id, templateId = tid, name = name, rarity = rarity, type = StwMetadataRepository.resolveType(tid) ?: "Defender", level = level, rating = rating, perks = perks)
    }

    private fun parseStwQuest(id: String, data: McpItemData): FortniteQuest? {
        val tid = data.templateId
        val attrs = data.attributes ?: emptyMap()
        val state = attrs["quest_state"]?.toString() ?: ""
        if (state.equals("claimed", true)) return null
        val resolvedName = StwMetadataRepository.resolveName(tid)
        val resolvedDesc = StwMetadataRepository.resolveDescription(tid)
        val target = StwMetadataRepository.resolveQuestTarget(tid) ?: 1
        var current = 0
        attrs.forEach { (k, v) -> if (k.startsWith("completion_", ignoreCase = true) || k.contains("_count", ignoreCase = true)) { val count = parseNumberAttr(v) ?: 0; if (count > current) current = count } }
        if (current == 0 && state.equals("completed", true)) current = target
        return FortniteQuest(id = id, templateId = tid, name = resolvedName ?: cleanStwName(tid), description = resolvedDesc ?: "Save the World Quest", category = if (tid.contains("daily", true)) "Daily Quests" else if (tid.contains("stonewood", true)) "Stonewood Quests" else "World Quests", progress = current, target = target, isCompleted = state.equals("completed", true), objectives = emptyList(), rewardXp = 0)
    }

    private fun determineHeroClass(tid: String): String {
        return when {
            tid.contains("constructor", true) -> "Constructor"
            tid.contains("ninja", true) -> "Ninja"
            tid.contains("outlander", true) -> "Outlander"
            tid.contains("commando", true) || tid.contains("soldier", true) -> "Soldier"
            else -> "Hero"
        }
    }

    private fun determineRarity(tid: String, attrs: Map<String, Any?>?): String {
        val lower = tid.lowercase()
        return when {
            lower.contains("_ur") || lower.contains("_mythic") || lower.contains("mythic") -> "mythic"
            lower.contains("_sr") || lower.contains("legendary") -> "legendary"
            lower.contains("_vr") || lower.contains("epic") -> "epic"
            lower.contains("_r") || lower.contains("rare") -> "rare"
            lower.contains("_uc") || lower.contains("uncommon") -> "uncommon"
            lower.contains("_c") || lower.contains("common") -> "common"
            else -> "rare"
        }
    }

    private fun cleanStwName(raw: String): String {
        val lower = raw.lowercase()
        val staticMap = mapOf("reagent_c_t01" to "Pure Drop of Rain", "reagent_c_t02" to "Lightning in a Bottle", "reagent_c_t03" to "Eye of the Storm", "reagent_c_t04" to "Storm Shard", "currency:gold" to "Gold", "eventcurrency_scaling" to "Gold", "xraytickets" to "X-Ray Tickets")
        staticMap.entries.find { lower.contains(it.key) }?.let { return it.value }
        var clean = raw.substringAfter(":", raw).replace(Regex("(?i)^HID_|^SID_|^WID_|^DID_|^Worker_|^Defender_|^Schematic_|^Hero_|^CardPack_|^Quest_"), "").replace(Regex("(?i)^(managersoldier|managerinventor|managerengineer|managerdoctor|managerexplorer|managermartialartist|manager|lead|soldier|inventor|engineer|doctor|explorer|martialartist)"), "").replace(Regex("(?i)(_SR|_VR|_R|_UC|_C|_UR|_MYTH)(?=_|$)"), "").replace(Regex("(?i)(_ORE|_CRYSTAL|_T\\d+)(?=_|$)"), "").replace("_", " ").trim()
        return if (clean.isEmpty()) raw else clean.split(" ").filter { it.isNotBlank() }.joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    private fun parseNumberAttr(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toDoubleOrNull()?.toInt() ?: value.toIntOrNull()
            else -> null
        }
    }

    private fun parseCbPage(id: String, item: McpItemData, slotted: List<StwInventoryItem> = emptyList()): StwCollectionPage {
        val tid = item.templateId
        val attrs = item.attributes ?: emptyMap()
        val state = attrs["state"]?.toString() ?: "Unknown"
        val name = StwMetadataRepository.resolveName(tid) ?: cleanCbPageName(tid)
        return StwCollectionPage(id = id, templateId = tid, name = name, state = state, slottedItems = slotted)
    }

    private fun parseToInventoryItem(id: String, item: McpItemData): StwInventoryItem {
        val tid = item.templateId
        val attrs = item.attributes ?: emptyMap()
        val level = parseNumberAttr(attrs["level"]) ?: 1
        val durability = (attrs["durability"] as? Number)?.toFloat()
        return StwInventoryItem(id = id, templateId = tid, name = StwMetadataRepository.resolveName(tid) ?: cleanStwName(tid), quantity = item.quantity, level = level, rarity = determineRarity(tid, attrs), type = if (tid.startsWith("Ingredient")) "Material" else if (tid.startsWith("Ammo")) "Ammo" else if (tid.startsWith("Trap")) "Trap" else "Weapon", durability = durability)
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
