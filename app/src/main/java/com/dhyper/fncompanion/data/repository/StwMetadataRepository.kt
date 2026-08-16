package com.dhyper.fncompanion.data.repository

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class StwObjectiveMetadata(
    val name: Map<String, String>?,
    val count: Int?
)

@JsonClass(generateAdapter = true)
data class StwItemMetadata(
    val name: Map<String, Any>? = null,
    val rarity: String? = null,
    val type: String? = null,
    val objectives: Map<String, StwObjectiveMetadata>? = null
)

data class StwStringList(
    @Json(name = "Items") val items: Map<String, StwItemMetadata>? = null,
    @Json(name = "Item Types") val itemTypes: Map<String, Map<String, String>>? = null,
    @Json(name = "Item Rarities") val itemRarities: Map<String, Map<String, String>>? = null,
    @Json(name = "Llama tiers") val llamaTiers: Map<String, Map<String, String>>? = null,
    val powerLevels: Map<String, Map<String, Int>>? = null
)

object StwMetadataRepository {
    private var stwData: StwStringList? = null
    private val itemItems = mutableMapOf<String, StwItemMetadata>()
    private val normalizedItems = mutableMapOf<String, StwItemMetadata>()
    private val dailyQuests = mutableMapOf<String, StwItemMetadata>()
    private var isLoaded = false
    private val loadMutex = Mutex()
    private var language = "en"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun loadMetadata(context: Context) = withContext(Dispatchers.IO) {
        if (isLoaded && dailyQuests.isNotEmpty()) return@withContext
        loadMutex.withLock {
            if (isLoaded && dailyQuests.isNotEmpty()) return@withLock
            
            try {
                val locale = context.resources.configuration.locales[0].language
                val supportedLanguages = listOf("ar", "de", "en", "es", "fr", "it", "ja", "ko", "pl", "pt", "ru", "zh", "id", "th", "tr", "vi")
                language = supportedLanguages.find { it == locale } ?: "en"
                if (language == "pt") language = "pt-BR"
                if (language == "zh") language = "zh-Hans"

                val json = context.assets.open("stringlist.json").bufferedReader().use { it.readText() }
                
                val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val rawAdapter = moshi.adapter<Map<String, Any>>(type)
                val root = rawAdapter.fromJson(json) ?: return@withLock

                fun asMap(any: Any?): Map<String, Any>? = any as? Map<String, Any>
                fun asNestedMap(any: Any?): Map<String, Map<String, String>>? {
                    val outer = any as? Map<String, Any> ?: return null
                    return outer.mapValues { entry -> 
                        (entry.value as? Map<String, String>) ?: emptyMap()
                    }
                }

                val itemsRaw = asMap(root["Items"]) ?: asMap(root["items"])
                itemItems.clear()
                normalizedItems.clear()
                
                val prefixSet = setOf("wid", "sid", "hid", "did", "tid", "uid", "qid", "worker", "quest", "managersoldier", "managerinventor", "managerengineer", "managerdoctor", "managerexplorer", "managermartialartist", "leadengineer", "leadexplorer", "leaddoctor", "schematic", "hero", "worker", "defender", "quest", "collectionbookpage", "page", "worlditem", "ingredient", "item", "ammo", "trap", "weapon", "cardpack", "consumable", "reagent", "token", "ingredient", "trap", "weapon")
                val variantSet = setOf("sr", "vr", "r", "uc", "c", "ur", "myth", "ore", "crystal", "highperf", "highcapacity", "highvolt", "founders", "military", "halloween", "christmas", "winter", "vindertech", "steampunk", "medieval", "retroscifi", "artdeco", "t01", "t02", "t03", "t04", "t05", "t06", "t07")

                itemsRaw?.forEach { (k, v) ->
                    val data = asMap(v) ?: return@forEach
                    val meta = StwItemMetadata(
                        name = data["name"] as? Map<String, Any>, // Use Any to avoid cast issues
                        rarity = data["rarity"] as? String,
                        type = data["type"] as? String
                    )
                    val lowerKey = k.lowercase().trim()
                    itemItems[lowerKey] = meta
                    
                    // Safe normalization: split by underscores and colons
                    val parts = lowerKey.split(Regex("[:_]")).filter { part ->
                        !prefixSet.contains(part) && !variantSet.contains(part) && !part.matches(Regex("t\\d+")) && !part.startsWith("page")
                    }
                    val coreKey = parts.joinToString("_")

                    if (coreKey.isNotEmpty()) {
                        val existing = normalizedItems[coreKey]
                        if (existing == null || lowerKey.contains("_sr") || lowerKey.contains("_vr") || lowerKey.contains("_myth")) {
                            normalizedItems[coreKey] = meta
                        }
                    }

                    if (lowerKey.contains(":")) {
                        itemItems[lowerKey.substringAfter(":")] = meta
                    }
                }
                
                stwData = StwStringList(
                    itemTypes = asNestedMap(root["Item Types"]),
                    itemRarities = asNestedMap(root["Item Rarities"]),
                    llamaTiers = asNestedMap(root["Llama tiers"]),
                    powerLevels = (asMap(root["Item Power Levels"]))?.mapValues { outer ->
                        val inner = asMap(outer.value) ?: return@mapValues emptyMap<String, Int>()
                        inner.mapKeys { it.key.toDoubleOrNull()?.toInt()?.toString() ?: it.key }
                             .mapValues { it.value.toString().toDoubleOrNull()?.toInt() ?: 0 }
                    }
                )

                try {
                    val questJson = context.assets.open("daily_quests.json").bufferedReader().use { it.readText() }
                    val questMap = asMap(rawAdapter.fromJson(questJson))
                    questMap?.forEach { (k, v) ->
                        val data = asMap(v) ?: return@forEach
                        val objRaw = asMap(data["objectives"])
                        val objectives = objRaw?.mapValues { 
                            StwObjectiveMetadata(name = mapOf("en" to it.key), count = it.value.toString().toDoubleOrNull()?.toInt() ?: 1)
                        }
                        val meta = StwItemMetadata(
                            name = mapOf("en" to (data["name"] as? String ?: "")),
                            objectives = objectives,
                            type = "quest"
                        )
                        val lowerKey = k.lowercase().trim()
                        val strippedKey = if (lowerKey.contains(":")) lowerKey.substringAfter(":") else lowerKey
                        
                        dailyQuests[lowerKey] = meta
                        dailyQuests[strippedKey] = meta
                        
                        // Also store key without common version suffixes for easier matching
                        val coreKey = strippedKey.replace(Regex("_v\\d+$"), "")
                        if (coreKey != strippedKey) {
                            dailyQuests[coreKey] = meta
                            dailyQuests["quest:$coreKey"] = meta
                        }
                    }
                } catch (e: Exception) {
                }

                isLoaded = true
            } catch (e: Exception) {
            }
        }
    }

    private fun findMetadata(templateId: String): StwItemMetadata? {
        var fullTid = templateId.lowercase().trim()
        
        // 1. Normalize profile-specific prefixes to stringlist.json format
        fullTid = when {
            fullTid.startsWith("trap:tid_") -> fullTid.replace("trap:tid_", "schematic:sid_")
            fullTid.startsWith("weapon:wid_") -> fullTid.replace("weapon:wid_", "schematic:sid_")
            fullTid.startsWith("worlditem:ingredient_") -> fullTid.replace("worlditem:ingredient_", "ingredient:ingredient_")
            else -> fullTid
        }
        
        // 2. Normalize variants (crystal -> ore)
        if (fullTid.contains("_crystal_")) {
            fullTid = fullTid.replace("_crystal_", "_ore_")
        }

        val strippedTid = if (fullTid.contains(":")) fullTid.substringAfter(":") else fullTid
        val idNoVersion = strippedTid.substringBefore(".")

        // Priority for quests from our custom daily_quests.json
        if (fullTid.contains("quest:daily", ignoreCase = true) || fullTid.contains("daily_", ignoreCase = true)) {
            dailyQuests[fullTid]?.let { return it }
            dailyQuests[strippedTid]?.let { return it }
            dailyQuests[idNoVersion]?.let { return it }
            
            val questCore = idNoVersion.replace(Regex("_v\\d+$"), "")
            dailyQuests[questCore]?.let { return it }
        }
        
        // 3. Direct matches with potential tier fallback
        fun tryMatch(id: String): StwItemMetadata? {
            // Prefer T01 version if available to satisfy "get the t01 names for everyone"
            if (id.contains(Regex("_t\\d+"))) {
                val t01Id = id.replace(Regex("_t\\d+"), "_t01")
                itemItems[t01Id]?.let { return it }
            }
            itemItems[id]?.let { return it }
            return null
        }

        tryMatch(fullTid)?.let { return it }
        tryMatch(strippedTid)?.let { return it }
        tryMatch(idNoVersion)?.let { return it }
        
        dailyQuests[fullTid]?.let { return it }
        dailyQuests[strippedTid]?.let { return it }
        dailyQuests[idNoVersion]?.let { return it }

        // 4. Safe normalization for fuzzy lookup
        val prefixSet = setOf("wid", "sid", "hid", "did", "tid", "uid", "qid", "worker", "quest", "managersoldier", "managerinventor", "managerengineer", "managerdoctor", "managerexplorer", "managermartialartist", "leadengineer", "leadexplorer", "leaddoctor", "schematic", "hero", "worker", "defender", "quest", "collectionbookpage", "page", "worlditem", "ingredient", "item", "ammo", "trap", "weapon", "cardpack", "consumable", "reagent", "token")
        val variantSet = setOf("sr", "vr", "r", "uc", "c", "ur", "myth", "ore", "crystal", "highperf", "highcapacity", "highvolt", "founders", "military", "halloween", "christmas", "winter", "vindertech", "steampunk", "medieval", "retroscifi", "artdeco", "t01", "t02", "t03", "t04", "t05", "t06", "t07")
        
        val parts = fullTid.split(Regex("[:_]")).filter { part ->
            !prefixSet.contains(part) && !variantSet.contains(part) && !part.matches(Regex("t\\d+")) && !part.startsWith("page")
        }
        val coreId = parts.joinToString("_")

        normalizedItems[coreId]?.let { return it }
        
        return null
    }

    fun resolveName(templateId: String): String? {
        if (!templateId.contains(":")) return null
        
        val meta = findMetadata(templateId)
        val name = meta?.name?.get(language)?.toString() ?: meta?.name?.get("en")?.toString()
        
        return name
    }

    fun resolveEnglishName(templateId: String): String? {
        val meta = findMetadata(templateId)
        val name = meta?.name?.get("en")?.toString()
        
        return name
    }

    fun resolveSlug(templateId: String): String? {
        val meta = findMetadata(templateId)
        val engName = meta?.name?.get("en")?.toString() ?: return null
        return engName.trim().lowercase().replace(" ", "_").replace("\"", "").replace(".", "")
    }

    fun resolveDailyQuest(templateId: String): StwItemMetadata? {
        val tid = templateId.lowercase().trim().substringAfter(":")
        return dailyQuests[tid] ?: dailyQuests["quest:$tid"]
    }

    fun isInternalItem(templateId: String): Boolean {
        val tid = templateId.lowercase()
        return tid.contains("edittool") || 
               tid.contains("buildingtool") || 
               tid.contains("upgrade_visual") ||
               tid.contains("commontest") ||
               tid.contains("technical")
    }

    fun resolveDescription(templateId: String): String? {
        val metadata = findMetadata(templateId) ?: return null
        if (metadata.objectives != null) {
            val objectives = metadata.objectives.values.joinToString("\n") { obj ->
                val name = obj.name?.get(language) ?: obj.name?.get("en") ?: ""
                val count = obj.count ?: 0
                if (count > 0) "$name ($count)" else name
            }
            if (objectives.isNotEmpty()) return objectives
        }
        return null
    }

    fun resolveType(templateId: String): String? {
        val key = findMetadata(templateId)?.type?.lowercase() ?: return null
        return resolveLocalizedItemType(key)
    }

    fun resolveRarity(templateId: String): String? {
        val key = findMetadata(templateId)?.rarity?.lowercase() ?: return null
        return resolveLocalizedRarity(key)
    }

    fun resolveLocalizedItemType(typeKey: String, plural: Boolean = false): String? {
        val key = typeKey.lowercase()
        val base = stwData?.itemTypes?.get(key)?.get(language)
            ?: stwData?.itemTypes?.entries?.find { it.key.equals(key, true) }?.value?.get(language)
        
        val result = if (plural && base != null) {
            when {
                base.endsWith("y") -> base.dropLast(1) + "ies"
                base.endsWith("o") || base.endsWith("s") || base.endsWith("ch") || base.endsWith("sh") -> base + "es"
                else -> base + "s"
            }
        } else base
        
        return result
    }

    fun resolveLocalizedRarity(rarityKey: String): String? {
        val key = rarityKey.lowercase()
        return stwData?.itemRarities?.get(key)?.get(language)
            ?: stwData?.itemRarities?.get(key)?.get("en")
            ?: stwData?.itemRarities?.entries?.find { it.key.equals(key, true) }?.value?.get(language)
            ?: stwData?.itemRarities?.entries?.find { it.key.equals(key, true) }?.value?.get("en")
    }

    fun resolveStwHeroIcon(templateId: String): String {
        val slug = resolveSlug(templateId) ?: templateId.substringAfter(":").lowercase()
        return "https://pennydb.plingindigo.org/images/heroes/$slug.png"
    }

    fun resolveStwSurvivorIcon(templateId: String, portrait: String?): String {
        val engName = resolveEnglishName(templateId) ?: ""
        
        // 1. Generic Survivors (Named "Survivor")
        if (engName.equals("Survivor", true) && portrait != null) {
            val portraitId = portrait.substringAfterLast(":")
            val parts = portraitId.lowercase().split("-")
            val personalityIdx = parts.indexOf("workerportrait") + 1
            if (personalityIdx > 0 && personalityIdx + 1 < parts.size) {
                val personality = parts[personalityIdx]
                val id = parts[personalityIdx + 1]
                return "https://pennydb.plingindigo.org/images/survivors/t-icon-workers-portrait-worker-$personality-$id-l.png"
            }
        }
        
        // 2. Generic Lead Survivors (Name contains "Lead Survivor")
        if (engName.contains("Lead Survivor", true) && portrait != null) {
            val portraitId = portrait.substringAfterLast(":")
            val parts = portraitId.lowercase().split("-")
            val jobIdx = parts.indexOf("managerportrait") + 1
            if (jobIdx > 0 && jobIdx + 1 < parts.size) {
                var job = parts[jobIdx]
                val id = parts[jobIdx + 1]
                // Apply specific mappings requested/observed in JSON
                job = when (job) {
                    "engineer" -> "inventor"
                    "soldier" -> "marksman"
                    "trainer" -> "personaltrainer"
                    else -> job
                }
                return "https://pennydb.plingindigo.org/images/survivors/t-icon-leaders-portrait-$job-$id-l.png"
            }
        }

        // 3. Mythic and Unique Survivors (Karolina, Joel, Joe "Ramsie" Bo, etc.)
        // Uses the same rule as heroes: slug based on name with quotes/dots removed.
        val slug = resolveSlug(templateId) ?: templateId.substringAfter(":").lowercase()
        return "https://pennydb.plingindigo.org/images/survivors/$slug.png"
    }

    fun resolveStwGadgetIcon(templateId: String): String {
        val name = resolveEnglishName(templateId)
        val slug = if (name != null) {
            when {
                name.contains("Supply Drop", true) -> "supply_drop"
                name.contains("Adrenaline Rush", true) -> "adrenaline_rush"
                name.contains("Airstrike", true) -> "airstrike"
                name.contains("Banner", true) -> "banner"
                name.contains("Stationary Hover Turret", true) -> "stationary_hover_turret"
                name.contains("Proximity Mine", true) -> "proximity_mine"
                name.contains("Slow Field", true) -> "slow_field"
                name.contains("Teleporter", true) -> "teleporter"
                else -> name.trim().lowercase().replace(" ", "_").replace(".", "").replace("\"", "")
            }
        } else templateId.substringAfter(":").lowercase()
        return "https://pennydb.plingindigo.org/images/gadgets/$slug.png"
    }

    fun resolveStwTeamPerkIcon(templateId: String): String {
        val name = resolveEnglishName(templateId) ?: ""
        val slug = name.trim().lowercase().replace(" ", "_").replace(".", "").replace("\"", "")
        return "https://pennydb.plingindigo.org/images/team_perks/$slug.png"
    }

    fun resolveStwResourceIcon(templateId: String): String {
        val lower = templateId.lowercase()
        return when {
            lower.contains("vbucks") || lower.contains("mtx") -> "file:///android_asset/vbucks.png"
            lower.contains("xraytickets") || lower.contains("currency_xrayllama") -> "file:///android_asset/xray.png"
            lower.contains("gold") || lower.contains("eventcurrency_scaling") -> "https://pennydb.plingindigo.org/images/resources/gold.png"
            lower.contains("peoplexp") || lower.contains("heroxp") -> "https://pennydb.plingindigo.org/images/resources/hero_xp.png"
            lower.contains("schematicxp") -> "https://pennydb.plingindigo.org/images/resources/schematic_xp.png"
            lower.contains("reagent_c_t01") -> "https://pennydb.plingindigo.org/images/resources/pure_drops_of_rain.png"
            lower.contains("reagent_c_t02") -> "https://pennydb.plingindigo.org/images/resources/lightning_in_a_bottle.png"
            lower.contains("reagent_c_t03") -> "https://pennydb.plingindigo.org/images/resources/eye_of_the_storm.png"
            lower.contains("reagent_c_t04") -> "https://pennydb.plingindigo.org/images/resources/storm_shard.png"
            else -> "https://pennydb.plingindigo.org/images/resources/v-bucks.png"
        }
    }

    fun determineHeroClass(tid: String): String {
        return when {
            tid.contains("constructor", true) -> "constructor"
            tid.contains("ninja", true) -> "ninja"
            tid.contains("outlander", true) -> "outlander"
            tid.contains("commando", true) || tid.contains("soldier", true) -> "commando"
            else -> "commando"
        }
    }

    fun isMelee(tid: String): Boolean {
        val lower = tid.lowercase()
        return lower.contains("edged") || lower.contains("blunt") || lower.contains("piercing") ||
                lower.contains("sword") || lower.contains("axe") || lower.contains("hammer") ||
                lower.contains("scythe") || lower.contains("spear") || lower.contains("club")
    }

    fun resolvePowerLevel(templateId: String, level: Int): Int? {
        val lowerId = templateId.lowercase()
        val rarity = when {
            lowerId.contains("_myth") || lowerId.contains("_ur") || lowerId.contains("mythic") -> "_UR"
            lowerId.contains("_sr") || lowerId.contains("legendary") -> "_SR"
            lowerId.contains("_vr") || lowerId.contains("epic") -> "_VR"
            lowerId.contains("_r") || lowerId.contains("rare") -> "_R"
            lowerId.contains("_uc") || lowerId.contains("uncommon") -> "_UC"
            lowerId.contains("_c") || lowerId.contains("common") -> "_C"
            else -> "_R"
        }
        val tier = when {
            lowerId.contains("_t06") -> "_T06"
            lowerId.contains("_t05") -> "_T05"
            lowerId.contains("_t04") -> "_T04"
            lowerId.contains("_t03") -> "_T03"
            lowerId.contains("_t02") -> "_T02"
            else -> "_T01"
        }
        return stwData?.powerLevels?.get(rarity + tier)?.get(level.toString())
    }
}
