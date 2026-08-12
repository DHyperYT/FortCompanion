package com.dhyper.fncompanion.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- SHOP MODELS ---
@JsonClass(generateAdapter = true)
data class ShopResponse(
    val status: Int,
    val data: ShopData?
)

@JsonClass(generateAdapter = true)
data class ShopData(
    val hash: String?,
    val date: String?,
    val vbucketIcon: String?,
    val featured: ShopSection?,
    val daily: ShopSection?,
    val entries: List<ShopEntry>?
)

@JsonClass(generateAdapter = true)
data class ShopSection(
    val name: String?,
    val entries: List<ShopEntry>?
)

@JsonClass(generateAdapter = true)
data class ShopEntry(
    val offerId: String?,
    val devName: String?,
    val offerTag: OfferTag?,
    val regularPrice: Int?,
    val finalPrice: Int?,
    val bundle: ShopBundle?,
    val banner: ShopBanner?,
    val items: List<CosmeticItem>?,
    val brItems: List<CosmeticItem>?,
    val tracks: List<JamTrackItem>?,
    val cars: List<CosmeticItem>?,
    val vehicles: List<CosmeticItem>?,
    val instruments: List<CosmeticItem>?,
    val newDisplayAsset: NewDisplayAsset?,
    val displayAssets: List<ShopDisplayAsset>?,
    val layout: ShopLayout?,
    val section: ShopSectionMetadata?,
    val categories: List<String>?
)

@JsonClass(generateAdapter = true)
data class JamTrackItem(
    val id: String?,
    val devName: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArt: String?,
    val releaseYear: Int?,
    val bpm: Int?,
    val duration: Int?,
    val genre: String?,
    val previewUrl: String? = null,
    val track: Map<String, Any?>? = null, // Some responses nest this
    val set: CosmeticSet? = null,
    val introduction: CosmeticIntro? = null,
    val added: String? = null
)

@JsonClass(generateAdapter = true)
data class ShopDisplayAsset(
    val id: String?,
    val url: String?,
    val background: String?,
    val full_background: String?
)

@JsonClass(generateAdapter = true)
data class ShopLayout(
    val id: String?,
    val name: String?,
    val category: String?,
    val index: Int?
)

@JsonClass(generateAdapter = true)
data class ShopSectionMetadata(
    val id: String?,
    val name: String?,
    val index: Int?,
    val landingPriority: Int?
)

@JsonClass(generateAdapter = true)
data class OfferTag(
    val id: String?,
    val text: String?
)

@JsonClass(generateAdapter = true)
data class ShopBundle(
    val name: String?,
    val info: String?,
    val image: String?
)

@JsonClass(generateAdapter = true)
data class ShopBanner(
    val value: String?,
    val intensity: String?,
    val backendValue: String?
)

@JsonClass(generateAdapter = true)
data class NewDisplayAsset(
    val id: String?,
    val cosmeticId: String? = null,
    val materialInstances: List<MaterialInstance>?,
    val renderImages: List<RenderImage>? = null
)

@JsonClass(generateAdapter = true)
data class RenderImage(
    val image: String?,
    val productTag: String? = null
)

@JsonClass(generateAdapter = true)
data class MaterialInstance(
    val id: String?,
    val images: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class CosmeticItem(
    val id: String,
    val name: String,
    val description: String?,
    val type: CosmeticType?,
    val rarity: CosmeticRarity?,
    val series: CosmeticSeries?,
    val images: CosmeticImages?,
    val variants: List<CosmeticVariant>?,
    val introduction: CosmeticIntro?,
    val set: CosmeticSet?,
    val added: String?,

    val lastAppearance: String? = null,
    val showcaseVideo: String? = null,
    val previewUrl: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val bpm: Int? = null,
    val duration: Int? = null
)

@JsonClass(generateAdapter = true)
data class CosmeticVariant(
    val channel: String?,
    val type: String?,
    val options: List<CosmeticVariantOption>?
)

@JsonClass(generateAdapter = true)
data class CosmeticVariantOption(
    val tag: String?,
    val name: String?,
    val image: String?,
    val unlock: String?
)

@JsonClass(generateAdapter = true)
data class CosmeticSet(
    val value: String?,
    val text: String?,
    val backendValue: String?
)

@JsonClass(generateAdapter = true)
data class CosmeticType(
    val value: String?,
    val displayValue: String?
)

@JsonClass(generateAdapter = true)
data class CosmeticRarity(
    val value: String?,
    val displayValue: String?
)

@JsonClass(generateAdapter = true)
data class CosmeticSeries(
    val value: String?,
    val image: String?
)

@JsonClass(generateAdapter = true)
data class CosmeticImages(
    val smallIcon: String?,
    val largeIcon: String? = null,
    val featured: String?,
    val background: String?,
    val full_background: String?,
    val icon_background: String? = null,
    val other: OtherImages? = null,
    val lego: LegoImages? = null,
    val bean: BeanImages? = null,
    // Top-level fallbacks (Modern API keys - /v2/cosmetics/instruments, /v2/cosmetics/lego/kits, /v2/cosmetics/cars)
    val small: String? = null,
    val large: String? = null,
    val legoSmall: String? = null,
    val legoLarge: String? = null,
    val legoWide: String? = null,
    val coverart: String? = null,
    val decal: String? = null,
    val albumArt: String? = null,
    val albumArtLarge: String? = null,
    // Direct fallbacks for various endpoints
    val icon: String? = null
)

@JsonClass(generateAdapter = true)
data class LegoImages(
    val small: String?,
    val large: String?,
    val wide: String?,
    val icon: String? = null
)

@JsonClass(generateAdapter = true)
data class BeanImages(
    val small: String?,
    val large: String?
)

@JsonClass(generateAdapter = true)
data class OtherImages(
    @Json(name = "albumArt") val albumArt: String?,
    val background: String?,
    val icon: String?
)

@JsonClass(generateAdapter = true)
data class CosmeticIntro(
    val chapter: String?,
    val season: String?,
    val text: String?
)

// --- NEWS MODELS ---
@JsonClass(generateAdapter = true)
data class NewsResponse(
    val status: Int,
    val data: NewsData?
)

@JsonClass(generateAdapter = true)
data class NewsData(
    val br: NewsCategory?,
    val stw: NewsCategory?,
    val creative: NewsCategory?
)

@JsonClass(generateAdapter = true)
data class NewsCategory(
    val hash: String?,
    val title: String?,
    val image: String?,
    val motds: List<NewsMotd>?
)

@JsonClass(generateAdapter = true)
data class SingleCosmeticResponse(
    val status: Int,
    val data: CosmeticItem?
)

@JsonClass(generateAdapter = true)
data class NewsMotd(
    val id: String,
    val title: String,
    val tabTitle: String?,
    val body: String?,
    val image: String?,
    val tileImage: String?,
    val category: String?
)

// --- MAP MODELS ---
@JsonClass(generateAdapter = true)
data class MapResponse(
    val status: Int,
    val data: MapData?
)

@JsonClass(generateAdapter = true)
data class MapData(
    val images: MapImages?,
    val pois: List<PointOfInterest>?
)

@JsonClass(generateAdapter = true)
data class MapImages(
    val blank: String?,
    val pois: String?
)

@JsonClass(generateAdapter = true)
data class PointOfInterest(
    val id: String,
    val name: String,
    val location: PoiLocation?
)

@JsonClass(generateAdapter = true)
data class PoiLocation(
    val x: Float?,
    val y: Float?,
    val z: Float?
)

// --- STATS LOOKUP MODELS ---
@JsonClass(generateAdapter = true)
data class StatsResponse(
    val status: Int,
    val data: PlayerStatsData?,
    val error: String?
)

@JsonClass(generateAdapter = true)
data class PlayerStatsData(
    val account: AccountInfo?,
    val battlePass: BattlePassInfo?,
    val stats: StatsPlatforms?,
    val battleRoyale: StatsPlatforms? // Modern API often uses this key
)

@JsonClass(generateAdapter = true)
data class AccountInfo(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class BattlePassInfo(
    val level: Int?,
    val progress: Int?
)

@JsonClass(generateAdapter = true)
data class StatsPlatforms(
    val all: StatBreakdown?,
    val keyboardMouse: StatBreakdown?,
    val gamepad: StatBreakdown?,
    val touch: StatBreakdown?
)

@JsonClass(generateAdapter = true)
data class StatBreakdown(
    val overall: SingleStatGroup?,
    val solo: SingleStatGroup?,
    val duo: SingleStatGroup?,
    val trio: SingleStatGroup?,
    val squad: SingleStatGroup?,
    val ltm: SingleStatGroup?
)

@JsonClass(generateAdapter = true)
data class SingleStatGroup(
    val score: Long?,
    val scorePerMin: Double?,
    val scorePerMatch: Double?,
    val wins: Int?,
    val top3: Int?,
    val top5: Int?,
    val top6: Int?,
    val top10: Int?,
    val top12: Int?,
    val top25: Int?,
    val kills: Long?,
    val killsPerMin: Double?,
    val killsPerMatch: Double?,
    val deaths: Long?,
    val kd: Double?,
    val matches: Long?,
    val winRate: Double?,
    val minutesPlayed: Long?,
    val playersOutlived: Long?
)

@JsonClass(generateAdapter = true)
data class AllCosmeticsResponse(
    val status: Int,
    val data: List<CosmeticItem>?
)

@JsonClass(generateAdapter = true)
data class TrackResponse(
    val status: Int,
    val data: List<TrackData>?
)

@JsonClass(generateAdapter = true)
data class TrackData(
    val id: String,
    val devName: String?,
    val name: String?,
    val track: Map<String, Any?>? = null,
    val images: Map<String, Any?>? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArt: String? = null,
    val title: String? = null,
    val bpm: Int? = null,
    val duration: Int? = null,
    val previewUrl: String? = null,
    val set: CosmeticSet? = null,
    val introduction: CosmeticIntro? = null,
    val added: String? = null
)

@JsonClass(generateAdapter = true)
data class BannerResponse(
    val status: Int,
    val data: List<BannerData>?
)

@JsonClass(generateAdapter = true)
data class BannerData(
    val id: String,
    val devName: String?,
    val name: String?,
    val description: String?,
    val category: String?,
    val images: CosmeticImages?,
    val set: CosmeticSet? = null,
    val introduction: CosmeticIntro? = null,
    val added: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistsResponse(
    val status: Int,
    val data: List<PlaylistData>?
)

@JsonClass(generateAdapter = true)
data class PlaylistData(
    val id: String,
    val name: String?,
    val subName: String?,
    val description: String?,
    val images: PlaylistImages?
)

@JsonClass(generateAdapter = true)
data class PlaylistImages(
    val showcase: String?,
    val missionIcon: String?
)

// --- GLOBAL CHALLENGE MAPPINGS (fortnite-central.app) ---
@JsonClass(generateAdapter = true)
data class ChallengeMappingResponse(
    val status: Int,
    val data: List<ChallengeBundle>?
)

@JsonClass(generateAdapter = true)
data class ChallengeBundle(
    val id: String,
    val name: String?,
    val challenges: List<ChallengeDefinition>?
)

@JsonClass(generateAdapter = true)
data class ChallengeDefinition(
    val id: String,
    val title: String?,
    val description: String?,
    val xp: Int?,
    val progressTarget: Int?,
    val type: String?
)

// --- AES MODELS ---
@JsonClass(generateAdapter = true)
data class AesResponse(
    val status: Int,
    val data: AesData?
)

@JsonClass(generateAdapter = true)
data class AesData(
    val build: String?,
    val mainKey: String?,
    val dynamicKeys: List<AesDynamicKey>?
)

@JsonClass(generateAdapter = true)
data class AesDynamicKey(
    val pakFilename: String?,
    val pakGuid: String?,
    val key: String?
)

