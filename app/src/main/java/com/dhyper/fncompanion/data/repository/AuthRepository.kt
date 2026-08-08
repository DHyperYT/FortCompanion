package com.dhyper.fncompanion.data.repository

import com.dhyper.fncompanion.data.api.ApiClient
import com.dhyper.fncompanion.data.db.AuthDao
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.db.PastSeasonEntity
import com.dhyper.fncompanion.data.db.RecentSearchEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val authDao: AuthDao) {
    private val api = ApiClient.epicApi

    val authSession: Flow<AuthEntity?> = authDao.getAuthSession()
    val recentSearches: Flow<List<RecentSearchEntity>> = authDao.getRecentSearches()

    fun getPastSeasons(accountId: String): Flow<List<PastSeasonEntity>> = authDao.getPastSeasons(accountId)

    suspend fun loginWithAuthCode(rawCode: String): Result<AuthEntity> {
        val input = rawCode.trim()
        var cleanCode = ""

        // 1. If it's a simple 32-char hex string (common raw code), use it immediately
        if (input.length == 32 && input.matches(Regex("[a-fA-F0-9]{32}"))) {
            cleanCode = input
        } else {
            // 2. Try to extract from URL or JSON using keywords
            // Handles: code=..., authorizationCode=..., authorization_code=..., authorizarion_code=..., "code": "...", etc.
            val codeMatch = Regex("""(?:code|authorizationCode|authorization_code|authorizarion_code)["']?\s*[:=]\s*["']?([a-fA-F0-9]{32})""").find(input)
                ?: Regex("""(?:code|authorizationCode|authorization_code|authorizarion_code)["']?\s*[:=]\s*["']?([^"'\s,}]+)""").find(input)

            if (codeMatch != null) {
                cleanCode = codeMatch.groupValues[1]
            } else {
                // 3. Last fallback: look for any 32-char hex string inside the text
                val hex32Match = Regex("""[a-fA-F0-9]{32}""").find(input)
                if (hex32Match != null) {
                    cleanCode = hex32Match.value
                } else {
                    // 4. If all fails but input has no spaces/special chars, assume it IS the code
                    if (!input.contains(" ") && !input.contains("{") && !input.contains("=")) {
                        cleanCode = input
                    }
                }
            }
        }

        cleanCode = cleanCode.trim().lowercase()
        if (cleanCode.isBlank()) {
            return Result.failure(Exception("Could not find a valid authorization code in the provided text."))
        }

        return try {
            // 1. Exchange Auth Code for Access Token
            // Use the full redirect URI as seen in the user's login link
            val tokenResponse = api.getAccessTokenWithAuthCode(
                code = cleanCode,
                redirectUri = "https://www.epicgames.com/id/api/redirect?clientId=3f69e56c7649492c8cc29f1af08a8a12&responseType=code"
            )
            
            // 2. Create Device Auth for persistent login
            val deviceAuth = api.createDeviceAuth(
                bearerToken = "Bearer ${tokenResponse.accessToken}",
                accountId = tokenResponse.accountId
            )

            // 3. Create Session Entity
            val expiresAtMs = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
            val session = AuthEntity(
                id = 1,
                accountId = tokenResponse.accountId,
                displayName = tokenResponse.displayName ?: "Epic Player",
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresAtMs = expiresAtMs,
                deviceId = deviceAuth.deviceId,
                deviceSecret = deviceAuth.secret
            )
            
            authDao.saveAuthSession(session)
            Result.success(session)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: e.message()
            Result.failure(Exception("Login failed (HTTP ${e.code()}): $errorBody"))
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.localizedMessage}"))
        }
    }

    suspend fun loginWithExchangeCode(rawExchangeCode: String): Result<AuthEntity> {
        var cleanCode = rawExchangeCode.trim()

        // 1. Extract exchange code if user pasted a full URL or redirect query string
        val urlCodeMatch = Regex("""(?:exchangeCode|exchange_code|code)=([a-fA-F0-9]{32})""").find(cleanCode)
            ?: Regex("""(?:exchangeCode|exchange_code|code)=([^&"'\s]+)""").find(cleanCode)

        if (urlCodeMatch != null) {
            cleanCode = urlCodeMatch.groupValues[1]
        } else if (cleanCode.contains("{") && cleanCode.contains("}")) {
            val jsonCodeMatch = Regex(""""(?:exchange_code|exchangeCode|code)"\s*:\s*"([a-fA-F0-9]{32})"""").find(cleanCode)
                ?: Regex(""""(?:exchange_code|exchangeCode|code)"\s*:\s*"([^"]+)"""").find(cleanCode)
            if (jsonCodeMatch != null) {
                cleanCode = jsonCodeMatch.groupValues[1]
            }
        } else {
            val hex32Match = Regex("""[a-fA-F0-9]{32}""").find(cleanCode)
            if (hex32Match != null) {
                cleanCode = hex32Match.value
            }
        }

        cleanCode = cleanCode.trim().lowercase()
        if (cleanCode.isBlank()) {
            return Result.failure(Exception("Exchange code cannot be empty. Please paste your code from Epic Games."))
        }

        // List of client credentials (Authorization headers) to try in order:
        val clientAuthHeaders = listOf(
            "Basic M2Y2OWU1NmM3NjQ5NDkyYzhjYzI5ZjFhZjA4YThhMTI6YjUxZWU5Y2IxMjIzNGY1MGE2OWVmYTY3ZWY1MzgxMmU=",
            "Basic MzQ0NjcwMDg1MDNmNGU1ZDgxNTU3NGU4NzEyZDk0NzI6QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUE=",
            "Basic YTI3ZjY5MjIyZTRjNDc4NGEwZDliNWMzOTJmMDM2NjY6",
            "Basic ZTlmODIxNjNiMjIyNGVmNDgyNzBjNTM4Y2IzYzY4M2I6",
            "Basic ZWM2NDgwM2M4ZDRkNDY2ZjhiMzZkM2VmNzNmOTYwMzA6YmRmM2UxN2E2M2Q2NDZlOThjZTUxMmM5OWRiOThlM2E="
        )

        var lastException: Exception? = null

        for (authHeader in clientAuthHeaders) {
            try {
                val response = api.getAccessTokenWithExchangeCode(authHeader = authHeader, exchangeCode = cleanCode)
                if (!response.accessToken.isNullOrBlank() && !response.accountId.isNullOrBlank()) {
                    val expiresAtMs = System.currentTimeMillis() + (response.expiresIn * 1000)
                    var session = AuthEntity(
                        id = 1,
                        accountId = response.accountId,
                        displayName = response.displayName ?: "Epic Player",
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        expiresAtMs = expiresAtMs
                    )
                    
                    // Improved: try to get device auth immediately for persistence
                    try {
                        val deviceAuth = api.createDeviceAuth(
                            bearerToken = "Bearer ${response.accessToken}",
                            accountId = response.accountId
                        )
                        session = session.copy(
                            deviceId = deviceAuth.deviceId,
                            deviceSecret = deviceAuth.secret
                        )
                    } catch (e: Exception) {
                        // Log failure internally but still proceed if we have a valid session
                        // This might happen if the client doesn't have permissions to create device auth
                        android.util.Log.e("AuthRepository", "Device Auth creation failed: ${e.localizedMessage}")
                    }
                    
                    authDao.saveAuthSession(session)
                    return Result.success(session)
                }
            } catch (e: HttpException) {
                val errorBodyString = try {
                    e.response()?.errorBody()?.string()
                } catch (ex: Exception) { null }

                val parsedMessage = if (!errorBodyString.isNullOrBlank()) {
                    if (errorBodyString.contains("errors.com.epicgames.account.oauth.exchange_code_not_found")) {
                        "Exchange code expired or invalid. Exchange codes are single-use and expire in 5 minutes. Please generate a fresh code."
                    } else if (errorBodyString.contains("errors.com.epicgames.common.oauth.invalid_grant")) {
                        "Invalid grant or exchange code. Please generate a new exchange code from Epic Games."
                    } else {
                        "Epic Auth Error (HTTP ${e.code()}): $errorBodyString"
                    }
                } else {
                    "HTTP ${e.code()} ${e.message()}: Failed to authenticate with Epic Games."
                }
                lastException = Exception(parsedMessage)
            } catch (e: IOException) {
                lastException = Exception("Network error connecting to Epic Games login servers: ${e.localizedMessage}")
            } catch (e: Exception) {
                lastException = Exception("Authentication error: ${e.localizedMessage}")
            }
        }

        return Result.failure(lastException ?: Exception("Could not authenticate with Epic Games. Please verify your exchange code."))
    }

    suspend fun getValidSession(): AuthEntity? {
        val current = authDao.getAuthSessionDirect() ?: return null
        
        // Token still valid (with 5 min buffer to be safe)
        if (System.currentTimeMillis() < current.expiresAtMs - 300000) {
            return current
        }

        // Token expired or close to expiring, try refreshing with device auth
        if (!current.deviceId.isNullOrBlank() && !current.deviceSecret.isNullOrBlank()) {
            return try {
                val response = api.getAccessTokenWithDeviceAuth(
                    accountId = current.accountId,
                    deviceId = current.deviceId,
                    secret = current.deviceSecret
                )
                val expiresAtMs = System.currentTimeMillis() + (response.expiresIn * 1000)
                val updated = current.copy(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    expiresAtMs = expiresAtMs
                )
                authDao.saveAuthSession(updated)
                updated
            } catch (e: Exception) {
                // Device auth failed (revoked?), clear session
                authDao.clearAuthSession()
                null
            }
        }
        
        return null
    }

    suspend fun logout() {
        authDao.clearAuthSession()
    }

    suspend fun updateSessionStats(
        accountLevel: Int,
        seasonalLevel: Int,
        totalWins: Int,
        pastSeasons: List<PastSeasonEntity> = emptyList()
    ) {
        val current = authDao.getAuthSessionDirect() ?: return
        val updated = current.copy(
            accountLevel = accountLevel,
            seasonalLevel = seasonalLevel,
            totalWins = totalWins
        )
        authDao.saveAuthSession(updated)

        if (pastSeasons.isNotEmpty()) {
            authDao.clearPastSeasons(current.accountId)
            authDao.savePastSeasons(pastSeasons.map { it.copy(accountId = current.accountId) })
        }
    }

    suspend fun addRecentSearch(accountName: String) {
        if (accountName.isNotBlank()) {
            authDao.saveRecentSearch(RecentSearchEntity(accountName = accountName.trim()))
        }
    }

    suspend fun removeRecentSearch(accountName: String) {
        authDao.deleteRecentSearch(accountName)
    }
}
