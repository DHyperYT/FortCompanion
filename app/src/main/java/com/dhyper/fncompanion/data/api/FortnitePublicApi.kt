package com.dhyper.fncompanion.data.api

import com.dhyper.fncompanion.data.models.MapResponse
import com.dhyper.fncompanion.data.models.NewsResponse
import com.dhyper.fncompanion.data.models.ShopResponse
import com.dhyper.fncompanion.data.models.StatsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FortnitePublicApi {
    @GET("v2/shop")
    suspend fun getShop(): ShopResponse

    @GET("v2/news")
    suspend fun getNews(): NewsResponse

    @GET("v1/map")
    suspend fun getMap(): MapResponse

    @GET("v2/stats/br/v2")
    suspend fun getPlayerStats(
        @retrofit2.http.Header("Authorization") apiKey: String? = null,
        @Query("name") name: String,
        @Query("accountType") accountType: String = "epic",
        @Query("timeWindow") timeWindow: String = "lifetime"
    ): StatsResponse

    @GET("v2/cosmetics/br")
    suspend fun getAllCosmetics(): com.dhyper.fncompanion.data.models.AllCosmeticsResponse

    @GET("v2/cosmetics/tracks")
    suspend fun getJamTracks(): com.dhyper.fncompanion.data.models.TrackResponse

    @GET("v2/cosmetics/cars")
    suspend fun getCars(): com.dhyper.fncompanion.data.models.AllCosmeticsResponse

    @GET("v2/cosmetics/instruments")
    suspend fun getInstruments(): com.dhyper.fncompanion.data.models.AllCosmeticsResponse

    @GET("v2/cosmetics/lego/kits")
    suspend fun getLegoKits(): com.dhyper.fncompanion.data.models.AllCosmeticsResponse

    @GET("v1/banners")
    suspend fun getBanners(): com.dhyper.fncompanion.data.models.BannerResponse

    @GET("v2/playlists")
    suspend fun getPlaylists(): com.dhyper.fncompanion.data.models.PlaylistsResponse

    @GET("v2/aes")
    suspend fun getAes(): com.dhyper.fncompanion.data.models.AesResponse
}
