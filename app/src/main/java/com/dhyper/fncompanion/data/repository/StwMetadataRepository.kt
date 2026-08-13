package com.dhyper.fncompanion.data.repository

import android.content.Context
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@JsonClass(generateAdapter = true)
data class StwObjectiveMetadata(
    val name: Map<String, String>?,
    val count: Int?
)

@JsonClass(generateAdapter = true)
data class StwItemMetadata(
    val name: Map<String, String>? = null,
    val rarity: String? = null,
    val type: String? = null,
    val objectives: Map<String, StwObjectiveMetadata>? = null
)

data class StwStringList(
    @Json(name = "Items") val Items: Map<String, StwItemMetadata>? = null,
    @Json(name = "Item Types") val ItemTypes: Map<String, Map<String, String>>? = null,
    @Json(name = "Item Rarities") val ItemRarities: Map<String, Map<String, String>>? = null,
    @Json(name = "Llama tiers") val LlamaTiers: Map<String, Map<String, String>>? = null,
    val powerLevels: Map<String, Map<String, Int>>? = null
)
// Removed problematic Item Power Levels and Strings from the data class to ensure Moshi doesn't fail on heterogeneous types

object StwMetadataRepository {
    private val client = OkHttpClient()
    private val heroMap = mutableMapOf<String, String>()
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
    private val adapter = moshi.adapter(StwStringList::class.java)

    private val HERO_URLS = listOf(
        "https://raw.githubusercontent.com/pirica/stw_fortnite_ids/main/Hero_Constructors.txt",
        "https://raw.githubusercontent.com/pirica/stw_fortnite_ids/main/Hero_Ninjas.txt",
        "https://raw.githubusercontent.com/pirica/stw_fortnite_ids/main/Hero_Outlanders.txt",
        "https://raw.githubusercontent.com/pirica/stw_fortnite_ids/main/Hero_Soldiers.txt"
    )

    private val itemMap = mapOf(
        "peoplexp" to "People XP",
        "schematicxp" to "Schematic XP",
        "xray" to "X-Ray Tickets",
        "xraytickets" to "X-Ray Tickets",
        "mtx_currency" to "V-Bucks",
        "mtxpurchasable" to "V-Bucks",
        "pdor" to "Pure Drop of Rain",
        "bottlelightning" to "Lightning in a Bottle",
        "stormeye" to "Eye of the Storm",
        "stormshard" to "Storm Shard",
        "gold" to "Gold",
        "minillama" to "Mini Reward Llama",
        "vbucksvoucher" to "V-Bucks Voucher",
        "trainingmanual" to "Training Manual",
        "trapschematicmanual" to "Trap Design",
        "weaponschematicmanual" to "Weapon Design",
        "legendaryflux" to "Legendary Flux",
        "epicflux" to "Epic Flux",
        "rareflux" to "Rare Flux"
    )

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

                Log.d("StwMetadata", "Loading stringlist.json from assets... (Language: $language)")
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
                
                val prefixSet = setOf("wid", "sid", "hid", "did", "worker", "quest", "managersoldier", "managerinventor", "managerengineer", "managerdoctor", "managerexplorer", "managermartialartist", "leadengineer", "leadexplorer", "leaddoctor", "schematic", "hero", "worker", "defender", "quest", "collectionbookpage", "page")
                val variantSet = setOf("sr", "vr", "r", "uc", "c", "ur", "myth", "ore", "crystal")

                itemsRaw?.forEach { (k, v) ->
                    val data = asMap(v) ?: return@forEach
                    val meta = StwItemMetadata(
                        name = data["name"] as? Map<String, String>,
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
                    ItemTypes = asNestedMap(root["Item Types"]),
                    ItemRarities = asNestedMap(root["Item Rarities"]),
                    LlamaTiers = asNestedMap(root["Llama tiers"]),
                    powerLevels = (asMap(root["Item Power Levels"]))?.mapValues { outer ->
                        val inner = asMap(outer.value) ?: return@mapValues emptyMap<String, Int>()
                        inner.mapKeys { it.key.toDoubleOrNull()?.toInt()?.toString() ?: it.key }
                             .mapValues { it.value.toString().toDoubleOrNull()?.toInt() ?: 0 }
                    }
                )

                Log.d("StwMetadata", "Parsed ${itemItems.size} items from JSON")

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
                    Log.d("StwMetadata", "Loaded ${dailyQuests.size} daily quests")
                } catch (e: Exception) {
                    Log.e("StwMetadata", "Failed to load daily_quests.json", e)
                }

                HERO_URLS.forEach { url ->
                    try {
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) parseHeroLines(response.body?.string() ?: "")
                        }
                    } catch (e: Exception) { Log.e("StwMetadata", "GitHub load failed for $url") }
                }
                isLoaded = true
            } catch (e: Exception) {
                Log.e("StwMetadata", "Critical metadata load failure", e)
            }
        }
    }

    private fun findMetadata(templateId: String): StwItemMetadata? {
        val fullTid = templateId.lowercase().trim()
        val strippedTid = if (fullTid.contains(":")) fullTid.substringAfter(":") else fullTid
        val idNoVersion = strippedTid.substringBefore(".")

        // Priority for quests from our custom daily_quests.json
        if (fullTid.contains("quest:daily", ignoreCase = true) || fullTid.contains("daily_", ignoreCase = true)) {
            dailyQuests[fullTid]?.let { return it }
            dailyQuests[strippedTid]?.let { return it }
            dailyQuests[idNoVersion]?.let { return it }
            
            val questCore = idNoVersion.replace(Regex("_v\\d+$"), "")
            dailyQuests[questCore]?.let { return it }
            
            dailyQuests.entries.find { 
                val keyOnly = it.key.substringAfter(":")
                keyOnly.startsWith(questCore) || questCore.startsWith(keyOnly)
            }?.value?.let { return it }
        }
        
        // 1. Direct matches (Highest priority)
        itemItems[fullTid]?.let { return it }
        itemItems[strippedTid]?.let { return it }
        itemItems[idNoVersion]?.let { return it }
        
        dailyQuests[fullTid]?.let { return it }
        dailyQuests[strippedTid]?.let { return it }
        dailyQuests[idNoVersion]?.let { return it }

        // 2. Safe normalization for lookup
        val prefixSet = setOf("wid", "sid", "hid", "did", "worker", "quest", "managersoldier", "managerinventor", "managerengineer", "managerdoctor", "managerexplorer", "managermartialartist", "leadengineer", "leadexplorer", "leaddoctor", "schematic", "hero", "worker", "defender", "quest", "collectionbookpage", "page")
        val variantSet = setOf("sr", "vr", "r", "uc", "c", "ur", "myth", "ore", "crystal")
        
        val parts = fullTid.split(Regex("[:_]")).filter { part ->
            !prefixSet.contains(part) && !variantSet.contains(part) && !part.matches(Regex("t\\d+")) && !part.startsWith("page")
        }
        val coreId = parts.joinToString("_")

        // 3. Fast fuzzy lookup using pre-indexed normalized keys
        normalizedItems[coreId]?.let { return it }
        
        // 4. More aggressive fuzzy search
        normalizedItems.entries.find { (k, _) ->
            k.isNotEmpty() && (coreId.contains(k) || k.contains(coreId))
        }?.value?.let { return it }

        return null
    }

    fun resolveName(templateId: String): String? {
        val meta = findMetadata(templateId)
        val name = meta?.name?.get(language) ?: meta?.name?.get("en")
        
        Log.d("StwMetadata", "resolveName($templateId) -> meta found: ${meta != null}, name: $name")
        
        if (name != null) return name

        // Fallbacks
        val tid = templateId.lowercase()
        val key = if (tid.contains(":")) tid.substringAfter(":").substringBefore(".") else tid
        itemMap[key]?.let { return it }
        itemMap[tid]?.let { return it }

        heroMap[tid]?.let { return it }
        heroMap[tid.substringAfter(":")]?.let { return it }
        heroMap[tid.replace(Regex("(_sr|_vr|_r|_uc|_c|_ur)?(_t\\d+)?$"), "")]?.let { return it }
        
        return null
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

    fun resolveQuestTarget(templateId: String): Int? {
        return findMetadata(templateId)?.objectives?.values?.sumOf { it.count ?: 0 }
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
        val base = stwData?.ItemTypes?.get(key)?.get(language)
            ?: stwData?.ItemTypes?.entries?.find { it.key.equals(key, true) }?.value?.get(language)
        
        val result = if (plural && base != null) {
            when {
                base.endsWith("y") -> base.dropLast(1) + "ies"
                base.endsWith("o") || base.endsWith("s") || base.endsWith("ch") || base.endsWith("sh") -> base + "es"
                else -> base + "s"
            }
        } else base
        
        Log.d("StwMetadata", "resolveLocalizedItemType($typeKey, plural=$plural) -> $result")
        return result
    }

    fun resolveLocalizedRarity(rarityKey: String): String? {
        val key = rarityKey.lowercase()
        return stwData?.ItemRarities?.get(key)?.get(language)
            ?: stwData?.ItemRarities?.get(key)?.get("en")
            ?: stwData?.ItemRarities?.entries?.find { it.key.equals(key, true) }?.value?.get(language)
            ?: stwData?.ItemRarities?.entries?.find { it.key.equals(key, true) }?.value?.get("en")
    }

    fun resolveLlamaTier(tier: Int): String? {
        return stwData?.LlamaTiers?.get(tier.toString())?.get(language)
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

    private fun parseHeroLines(content: String) {
        content.lines().forEach { line ->
            if (line.isBlank() || !line.contains("Hero:")) return@forEach
            val parts = line.split(Regex("\\t+|\\s{2,}"))
            if (parts.size >= 2) {
                val tid = parts[0].trim().lowercase()
                val name = parts[1].trim()
                if (tid.isNotEmpty() && name.isNotEmpty()) heroMap[tid] = name
            }
        }
    }
}
