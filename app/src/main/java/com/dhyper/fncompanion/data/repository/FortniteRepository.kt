package com.dhyper.fncompanion.data.repository

import com.dhyper.fncompanion.BuildConfig
import com.dhyper.fncompanion.data.api.ApiClient
import com.dhyper.fncompanion.data.models.ChallengeBundle
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.data.models.MapData
import com.dhyper.fncompanion.data.models.NewsData
import com.dhyper.fncompanion.data.models.PlayerStatsData
import com.dhyper.fncompanion.data.models.ShopData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import retrofit2.HttpException
import java.io.IOException

class FortniteRepository {
    private val api = ApiClient.publicApi

    companion object {
        private var cachedFullCosmetics: List<CosmeticItem>? = null
        private var cachedChallenges: List<com.dhyper.fncompanion.data.models.ChallengeBundle>? = null
    }

    fun clearCache() {
        cachedFullCosmetics = null
        cachedChallenges = null
    }

    suspend fun fetchChallenges(): Result<List<com.dhyper.fncompanion.data.models.ChallengeBundle>> {
        cachedChallenges?.let { return Result.success(it) }
        return try {
            val response = ApiClient.centralApi.getChallenges()
            if (response.status == 200 && response.data != null) {
                cachedChallenges = response.data
                Result.success(response.data)
            } else {
                Result.failure(Exception("Failed to fetch challenge mappings: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchItemShop(): Result<ShopData> {
        return try {
            val response = api.getShop()
            if (response.status == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Item Shop returned status code: ${response.status}"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP ${e.code()}: ${e.message()} while loading Item Shop"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error while connecting to Fortnite API: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error loading Item Shop: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchNews(): Result<NewsData> {
        return try {
            val response = api.getNews()
            if (response.status == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("News feed returned status code: ${response.status}"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP ${e.code()}: ${e.message()} while loading News"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error while loading News: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error loading News: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchMap(): Result<MapData> {
        return try {
            val response = api.getMap()
            if (response.status == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Map service returned status code: ${response.status}"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP ${e.code()}: ${e.message()} while loading Map"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error while loading Map: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error loading Map: ${e.localizedMessage}"))
        }
    }

    suspend fun searchPlayerStats(accountName: String, accountType: String = "epic", apiKey: String? = null): Result<PlayerStatsData> {
        return try {
            val keyToUse = apiKey?.ifBlank { null }
            if (keyToUse.isNullOrBlank()) {
                return Result.failure(Exception("Fortnite-API.com Key is missing. Please set it in Settings."))
            }
            val response = api.getPlayerStats(apiKey = keyToUse, name = accountName.trim(), accountType = accountType)
            if (response.status == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                val errorMsg = response.error ?: "Player '$accountName' not found or stats are set to private."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                Result.failure(Exception("HTTP 401: Free API Key Required. fortnite-api.com requires a free API key to look up player stats. You can get a free key in 10s at https://dash.fortnite-api.com."))
            } else if (e.code() == 404) {
                Result.failure(Exception("Player '$accountName' was not found on Epic Games servers."))
            } else if (e.code() == 403) {
                Result.failure(Exception("Stats for '$accountName' are private or disabled by user."))
            } else {
                Result.failure(Exception("HTTP ${e.code()}: ${e.message()} while fetching player stats"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error during stats search: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error fetching stats for '$accountName': ${e.localizedMessage}"))
        }
    }

    suspend fun fetchAllCosmetics(): Result<List<CosmeticItem>> = coroutineScope {
        cachedFullCosmetics?.let { return@coroutineScope Result.success(it) }
        
        return@coroutineScope try {
            val deferredBr = async { api.getAllCosmetics().data ?: emptyList() }
            val deferredTracks = async { 
                try {
                    api.getJamTracks().data?.map { t ->
                        val trackMap = t.track as? Map<*, *>
                        val imageMap = t.images as? Map<*, *>

                        val title = trackMap?.get("title")?.toString() ?: t.title ?: t.name ?: "Unknown Song"
                        val artist = trackMap?.get("artist")?.toString() ?: t.artist ?: "Unknown Artist"
                        val album = trackMap?.get("album")?.toString() ?: t.album
                        val albumArt = imageMap?.get("albumArt")?.toString() ?: t.albumArt
                        
                        val bpm = (trackMap?.get("bpm") as? Number)?.toInt() ?: t.bpm
                        val duration = (trackMap?.get("duration") as? Number)?.toInt() ?: t.duration
                        val previewUrl = trackMap?.get("previewUrl")?.toString() ?: t.previewUrl

                        val albumDesc = if (album.isNullOrBlank() || album.contains("unknown", ignoreCase = true)) "" else " from $album"
                        
                        CosmeticItem(
                            id = t.id,
                            name = title,
                            description = "Song by $artist$albumDesc",
                            type = com.dhyper.fncompanion.data.models.CosmeticType("Track", "Jam Track"),
                            rarity = com.dhyper.fncompanion.data.models.CosmeticRarity("Festival", "Festival"),
                            series = null,
                            images = com.dhyper.fncompanion.data.models.CosmeticImages(albumArt, albumArt, albumArt, null, null, albumArt),
                            variants = null,
                            introduction = t.introduction,
                            set = t.set,
                            added = t.added,
                            previewUrl = previewUrl,
                            artist = artist,
                            album = album,
                            bpm = bpm,
                            duration = duration
                        )
                    } ?: emptyList()
                } catch (e: Exception) { emptyList() }
            }
            val deferredCars = async { try { api.getCars().data ?: emptyList() } catch (e: Exception) { emptyList() } }
            val deferredInstruments = async { try { api.getInstruments().data ?: emptyList() } catch (e: Exception) { emptyList() } }
            val deferredLego = async { try { api.getLegoKits().data ?: emptyList() } catch (e: Exception) { emptyList() } }
            val deferredBanners = async {
                try {
                    api.getBanners().data?.map { b ->
                        CosmeticItem(
                            id = b.id,
                            name = b.name ?: b.devName ?: "Banner",
                            description = b.description ?: "Profile Banner",
                            type = com.dhyper.fncompanion.data.models.CosmeticType("Banner", "Banner"),
                            rarity = com.dhyper.fncompanion.data.models.CosmeticRarity("Common", "Common"),
                            series = null,
                            images = b.images,
                            variants = null,
                            introduction = b.introduction,
                            set = b.set,
                            added = b.added
                        )
                    } ?: emptyList()
                } catch (e: Exception) { emptyList() }
            }

            val results = awaitAll(deferredBr, deferredTracks, deferredCars, deferredInstruments, deferredLego, deferredBanners)
            val combined = results.flatten().distinctBy { it.id }
            cachedFullCosmetics = combined
            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDetailedCosmetic(id: String): Result<CosmeticItem?> = withContext(Dispatchers.IO) {
        try {
            val response = when {
                id.startsWith("sid_", true) -> api.searchTrack(id)
                else -> api.searchBRCosmetic(id)
            }
            
            if (response.status == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Failed to fetch details: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchNewCosmetics(): Result<List<CosmeticItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getNewCosmetics()
            if (response.status == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Failed to fetch new cosmetics: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAes(): Result<com.dhyper.fncompanion.data.models.AesData> {
        return try {
            val response = api.getAes()
            if (response.status == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Failed to fetch AES keys: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkForVBucksAlert(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://fortnitedb.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (body.isBlank()) {
                return@withContext Result.failure(Exception("Empty response from FortniteDB"))
            }

            val doc = Jsoup.parse(body)
            val pageText = doc.text()
            // 1. Direct check: If the "No V-Bucks" phrase is present in the visible text, there are no missions.
            if (pageText.contains("No V-Bucks Missions today", ignoreCase = true)) {
                return@withContext Result.success(null)
            }

            // 2. If the phrase is NOT there, missions are highly likely active.
            // We try to extract IDs to avoid duplicate alerts, but fallback to a date-hash if needed.
            val vbucksSection = doc.select("div.new_block_block").firstOrNull { element ->
                element.select("h5, h4, div").text().contains("V-Bucks Missions", ignoreCase = true)
            }

            val alertIds = vbucksSection?.select("tr[data-alertid]")?.mapNotNull { 
                it.attr("data-alertid").takeIf { id -> id.isNotBlank() }
            }?.sorted()

            return@withContext if (!alertIds.isNullOrEmpty()) {
                Result.success(alertIds.joinToString(","))
            } else {
                // Fallback ID based on the current date so it triggers once per day if parsing fails
                val dateId = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                Result.success("FOUND_$dateId")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
