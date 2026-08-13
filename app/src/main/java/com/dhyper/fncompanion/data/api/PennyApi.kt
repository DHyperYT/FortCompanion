package com.dhyper.fncompanion.data.api

import com.dhyper.fncompanion.data.models.PennyProfileResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface PennyApi {
    @GET("api/search-profiles/{playerid}")
    suspend fun getProfile(
        @Path("playerid") playerId: String
    ): PennyProfileResponse
}
