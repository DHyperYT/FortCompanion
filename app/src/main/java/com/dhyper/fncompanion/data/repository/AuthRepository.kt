package com.dhyper.fncompanion.data.repository

import android.util.Log
import com.dhyper.fncompanion.data.api.ApiClient
import com.dhyper.fncompanion.data.db.AuthDao
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.db.PastSeasonEntity
import com.dhyper.fncompanion.data.db.RecentSearchEntity
import com.dhyper.fncompanion.ui.utils.SecurityManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val authDao: AuthDao) {
    private val api = ApiClient.epicApi
    private val epicAccountRepository = EpicAccountRepository()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // In-memory cache for valid sessions (includes current access tokens)
    private val sessionCache = MutableStateFlow<Map<String, AuthEntity>>(emptyMap())

    val authSession: Flow<AuthEntity?> = authDao.getAuthSession().map { encrypted ->
        val decrypted = decryptSession(encrypted)
        decrypted?.let { sessionCache.value[it.accountId] ?: it }
    }
    
    val recentSearches: Flow<List<RecentSearchEntity>> = authDao.getRecentSearches()

    private fun encryptSession(session: AuthEntity): AuthEntity {
        return session.copy(
            deviceId = SecurityManager.encrypt(session.deviceId),
            deviceSecret = SecurityManager.encrypt(session.deviceSecret)
        )
    }

    private fun decryptSession(session: AuthEntity?): AuthEntity? {
        if (session == null) return null
        return try {
            session.copy(
                deviceId = SecurityManager.decrypt(session.deviceId),
                deviceSecret = SecurityManager.decrypt(session.deviceSecret)
            )
        } catch (e: Exception) {
            logSafe("Decryption failed for account ${session.accountId.take(4)}...")
            session
        }
    }

    private fun logSafe(message: String, accountId: String? = null, hasDeviceAuth: Boolean = false, hasToken: Boolean = false, statusCode: Int? = null) {
        val details = mutableListOf<String>()
        accountId?.let { details.add("acc=${it.take(4)}...") }
        details.add("hasDA=$hasDeviceAuth")
        details.add("hasAT=$hasToken")
        statusCode?.let { details.add("status=$it") }
        Log.d("AuthRepo", "$message [${details.joinToString(", ")}]")
    }

    fun getPastSeasons(accountId: String): Flow<List<PastSeasonEntity>> = authDao.getPastSeasons(accountId)

    suspend fun loginWithAuthCode(rawCode: String): Result<AuthEntity> {
        val input = rawCode.trim()
        var cleanCode = ""

        if (input.length == 32 && input.matches(Regex("[a-fA-F0-9]{32}"))) {
            cleanCode = input
        } else {
            val codeMatch = Regex("""(?:code|authorizationCode|authorization_code|authorizarion_code)["']?\s*[:=]\s*["']?([a-fA-F0-9]{32})""").find(input)
                ?: Regex("""(?:code|authorizationCode|authorization_code|authorizarion_code)["']?\s*[:=]\s*["']?([^"'\s,}]+)""").find(input)
            if (codeMatch != null) cleanCode = codeMatch.groupValues[1]
            else {
                val hex32Match = Regex("""[a-fA-F0-9]{32}""").find(input)
                if (hex32Match != null) cleanCode = hex32Match.value
            }
        }

        cleanCode = cleanCode.trim().lowercase()
        if (cleanCode.isBlank()) return Result.failure(Exception("Invalid authorization code format."))

        return try {
            logSafe("Exchanging auth code", hasToken = false)
            val tokenResponse = api.getAccessTokenWithAuthCode(
                code = cleanCode,
                redirectUri = "https://www.epicgames.com/id/api/redirect?clientId=3f69e56c7649492c8cc29f1af08a8a12&responseType=code"
            )
            
            logSafe("Registering device auth", accountId = tokenResponse.accountId, hasToken = true)
            val deviceAuth = api.createDeviceAuth(
                bearerToken = "Bearer ${tokenResponse.accessToken}",
                accountId = tokenResponse.accountId
            )

            val iconUrl = epicAccountRepository.fetchEquippedSkinIcon(tokenResponse.accessToken, tokenResponse.accountId)
            val expiresAtMs = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
            
            val session = AuthEntity(
                accountId = tokenResponse.accountId,
                displayName = tokenResponse.displayName ?: "Epic Player",
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresAtMs = expiresAtMs,
                deviceId = deviceAuth.deviceId,
                deviceSecret = deviceAuth.secret,
                equippedSkinIcon = iconUrl,
                isActive = true
            )
            
            sessionCache.value = sessionCache.value + (session.accountId to session)
            authDao.saveAuthSession(encryptSession(session))
            Result.success(session)
        } catch (e: Exception) {
            logSafe("Login failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun loginWithExchangeCode(rawExchangeCode: String): Result<AuthEntity> {
        var cleanCode = rawExchangeCode.trim()
        val hex32Match = Regex("""[a-fA-F0-9]{32}""").find(cleanCode)
        if (hex32Match != null) cleanCode = hex32Match.value

        if (cleanCode.isBlank()) return Result.failure(Exception("Exchange code cannot be empty."))

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
                    val iconUrl = epicAccountRepository.fetchEquippedSkinIcon(response.accessToken, response.accountId)

                    var session = AuthEntity(
                        accountId = response.accountId,
                        displayName = response.displayName ?: "Epic Player",
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        expiresAtMs = expiresAtMs,
                        equippedSkinIcon = iconUrl,
                        isActive = true
                    )
                    
                    try {
                        val deviceAuth = api.createDeviceAuth(bearerToken = "Bearer ${response.accessToken}", accountId = response.accountId)
                        session = session.copy(deviceId = deviceAuth.deviceId, deviceSecret = deviceAuth.secret)
                    } catch (e: Exception) { logSafe("Device Auth creation failed: ${e.message}") }
                    
                    sessionCache.value = sessionCache.value + (session.accountId to session)
                    authDao.saveAuthSession(encryptSession(session))
                    return Result.success(session)
                }
            } catch (e: Exception) { lastException = e }
        }
        return Result.failure(lastException ?: Exception("Exchange code authentication failed."))
    }

    suspend fun getValidSession(): AuthEntity? {
        val activeEncrypted = authDao.getAuthSessionDirect() ?: return null
        val session = decryptSession(activeEncrypted) ?: return null
        
        val cached = sessionCache.value[session.accountId]
        if (cached != null && System.currentTimeMillis() < (cached.expiresAtMs - 60000)) {
            return cached
        }

        if (!session.deviceId.isNullOrBlank() && !session.deviceSecret.isNullOrBlank()) {
            logSafe("Refreshing session with device auth", accountId = session.accountId, hasDeviceAuth = true)
            return try {
                val response = api.getAccessTokenWithDeviceAuth(
                    accountId = session.accountId,
                    deviceId = session.deviceId,
                    secret = session.deviceSecret
                )
                val expiresAtMs = System.currentTimeMillis() + (response.expiresIn * 1000)
                val updated = session.copy(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    expiresAtMs = expiresAtMs,
                    lastRefreshTimeMs = System.currentTimeMillis()
                )
                sessionCache.value = sessionCache.value + (updated.accountId to updated)
                logSafe("Session refresh success", accountId = updated.accountId, hasToken = true)
                updated
            } catch (e: HttpException) {
                logSafe("Refresh failed", accountId = session.accountId, statusCode = e.code())
                val errorBody = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { "" }
                if (e.code() == 400 && (errorBody?.contains("device_auth_not_found") == true || errorBody?.contains("invalid_grant") == true)) {
                    logSafe("Device auth revoked for ${session.accountId}")
                    // Don't auto-delete here, let the UI handle REAUTH state if needed
                    null
                } else null
            } catch (e: Exception) {
                logSafe("Refresh transient error: ${e.message}")
                null
            }
        }
        return null
    }

    suspend fun exportAccounts(password: CharArray): Result<String> {
        return try {
            val accounts = authDao.getAllAccounts().first().map { decryptSession(it)!! }
            val type = Types.newParameterizedType(List::class.java, AuthEntity::class.java)
            val json = moshi.adapter<List<AuthEntity>>(type).toJson(accounts)
            Result.success(SecurityManager.encryptWithPassword(json, password))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun importAccounts(encryptedData: String, password: CharArray): Result<Int> {
        return try {
            val json = SecurityManager.decryptWithPassword(encryptedData, password) ?: return Result.failure(Exception("Invalid password."))
            val type = Types.newParameterizedType(List::class.java, AuthEntity::class.java)
            val accounts = moshi.adapter<List<AuthEntity>>(type).fromJson(json) ?: emptyList()
            accounts.forEach { authDao.upsertAuthSession(encryptSession(it)) }
            Result.success(accounts.size)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun generateExchangeCode(): Result<String> {
        return try {
            val session = getValidSession() ?: return Result.failure(Exception("Session invalid."))
            val response = api.getExchangeCode("Bearer ${session.accessToken}")
            val code = response["code"]?.toString()
            if (!code.isNullOrBlank()) Result.success(code) else Result.failure(Exception("No code returned."))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun logout() {
        sessionCache.value = emptyMap()
        authDao.clearAllSessions()
    }

    suspend fun updateSessionStats(accountLevel: Int, seasonalLevel: Int, totalWins: Int, pastSeasons: List<PastSeasonEntity> = emptyList()) {
        val current = decryptSession(authDao.getAuthSessionDirect()) ?: return
        val updated = current.copy(accountLevel = accountLevel, seasonalLevel = seasonalLevel, totalWins = totalWins)
        sessionCache.value = sessionCache.value + (updated.accountId to updated)
        authDao.saveAuthSession(encryptSession(updated))
        if (pastSeasons.isNotEmpty()) {
            authDao.clearPastSeasons(current.accountId)
            authDao.savePastSeasons(pastSeasons.map { it.copy(accountId = current.accountId) })
        }
    }

    suspend fun addRecentSearch(name: String) { if (name.isNotBlank()) authDao.saveRecentSearch(RecentSearchEntity(accountName = name.trim())) }
    suspend fun removeRecentSearch(name: String) { authDao.deleteRecentSearch(name) }
}
