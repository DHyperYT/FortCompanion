package com.dhyper.fncompanion.data.api

import com.dhyper.fncompanion.data.models.DeviceAuthResponse
import com.dhyper.fncompanion.data.models.EpicTokenResponse
import com.dhyper.fncompanion.data.models.McpQueryResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EpicAccountApi {
    @FormUrlEncoded
    @POST("https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/token")
    suspend fun getAccessTokenWithExchangeCode(
        @Header("Authorization") authHeader: String = "Basic M2Y2OWU1NmM3NjQ5NDkyYzhjYzI5ZjFhZjA4YThhMTI6YjUxZWU5Y2IxMjIzNGY1MGE2OWVmYTY3ZWY1MzgxMmU=",
        @Field("grant_type") grantType: String = "exchange_code",
        @Field("exchange_code") exchangeCode: String
    ): EpicTokenResponse

    @FormUrlEncoded
    @POST("https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/token")
    suspend fun getAccessTokenWithAuthCode(
        @Header("Authorization") authHeader: String = "Basic M2Y2OWU1NmM3NjQ5NDkyYzhjYzI5ZjFhZjA4YThhMTI6YjUxZWU5Y2IxMjIzNGY1MGE2OWVmYTY3ZWY1MzgxMmU=",
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String = "https://www.epicgames.com/id/api/redirect"
    ): EpicTokenResponse

    @FormUrlEncoded
    @POST("https://account-public-service-prod03.ol.epicgames.com/account/api/oauth/token")
    suspend fun getAccessTokenWithDeviceAuth(
        @Header("Authorization") authHeader: String = "Basic M2Y2OWU1NmM3NjQ5NDkyYzhjYzI5ZjFhZjA4YThhMTI6YjUxZWU5Y2IxMjIzNGY1MGE2OWVmYTY3ZWY1MzgxMmU=",
        @Field("grant_type") grantType: String = "device_auth",
        @Field("account_id") accountId: String,
        @Field("device_id") deviceId: String,
        @Field("secret") secret: String
    ): EpicTokenResponse

    @POST("https://account-public-service-prod03.ol.epicgames.com/account/api/public/account/{accountId}/deviceAuth")
    suspend fun createDeviceAuth(
        @Header("Authorization") bearerToken: String,
        @Path("accountId") accountId: String,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): DeviceAuthResponse

    @POST("https://fortnite-public-service-prod11.ol.epicgames.com/fortnite/api/game/v2/profile/{accountId}/client/QueryProfile")
    suspend fun queryMcpProfile(
        @Header("Authorization") bearerToken: String,
        @Header("User-Agent") userAgent: String = "Fortnite/++Fortnite+Release-25.11-CL-25831038 Android/13",
        @Path("accountId") accountId: String,
        @Query("profileId") profileId: String,
        @Query("rvn") rvn: Int = -1,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): McpQueryResponse

    @POST("https://fortnite-public-service-prod11.ol.epicgames.com/fortnite/api/game/v2/profile/{accountId}/client/SetActiveHeroLoadout")
    suspend fun setActiveHeroLoadout(
        @Header("Authorization") bearerToken: String,
        @Header("User-Agent") userAgent: String = "Fortnite/++Fortnite+Release-25.11-CL-25831038 Android/13",
        @Path("accountId") accountId: String,
        @Query("profileId") profileId: String = "campaign",
        @Query("rvn") rvn: Int = -1,
        @Body body: Map<String, String>
    ): McpQueryResponse

    @GET("https://fortnite-public-service-prod11.ol.epicgames.com/fortnite/api/game/v2/world/info")
    suspend fun getStwWorldInfo(
        @Header("Authorization") bearerToken: String,
        @Header("User-Agent") userAgent: String = "Fortnite/++Fortnite+Release-25.11-CL-25831038 Android/13"
    ): Map<String, Any?>
}
