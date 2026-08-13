package com.dhyper.fncompanion.data.repository

import com.dhyper.fncompanion.data.api.ApiClient
import com.dhyper.fncompanion.data.models.PennyProfileResponse

class PennyRepository {
    private val api = ApiClient.pennyApi

    suspend fun getProfile(playerId: String): Result<PennyProfileResponse> {
        return try {
            val response = api.getProfile(playerId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
