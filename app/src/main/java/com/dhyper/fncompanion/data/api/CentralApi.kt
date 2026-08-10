package com.dhyper.fncompanion.data.api

import com.dhyper.fncompanion.data.models.ChallengeMappingResponse
import retrofit2.http.GET

interface CentralApi {
    @GET("api/v1/challenges")
    suspend fun getChallenges(): ChallengeMappingResponse
}
