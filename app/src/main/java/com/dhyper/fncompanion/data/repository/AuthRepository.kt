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
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.data.models.DeviceAuthStatus
import com.dhyper.fncompanion.data.models.EpicTokenResponse
import com.dhyper.fncompanion.data.models.EpicVerifyResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val authDao: AuthDao) {
    private val api = ApiClient.epicApi
    private val epicAccountRepository = EpicAccountRepository()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val refreshMutex = Mutex()

    // In-memory cache for valid sessions
    private val sessionCache = MutableStateFlow<Map<String, AuthEntity>>(emptyMap())
    private val _isRefreshing = MutableStateFlow(false)

    // Ticker flow to trigger UI state updates for token expiry without DB changes
    private val ticker = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(30000) // Emit every 30 seconds
        }
    }

    val authSession: Flow<AuthState> = combine(
        authDao.getAuthSession(),
        sessionCache,
        _isRefreshing,
        ticker
    ) { encrypted, cache, refreshing, now ->
        if (encrypted == null) return@combine AuthState.NoCredentials
        
        val decrypted = try {
            decryptSession(encrypted)
        } catch (e: Exception) {
            return@combine AuthState.DecryptionError(encrypted)
        }
        
        val session = decrypted?.let { cache[it.accountId] ?: it } ?: return@combine AuthState.NoCredentials
        
        val isExpired = now >= (session.expiresAtMs - 30000)
        
        when {
            refreshing -> AuthState.TokenRefreshing(session)
            session.deviceAuthStatus == DeviceAuthStatus.REVOKED.name -> AuthState.ReauthRequired(session, "Device auth revoked")
            isExpired -> AuthState.TokenExpired(session)
            else -> AuthState.Active(session)
        }
    }

    private fun encryptSession(session: AuthEntity): AuthEntity {
        return session.copy(
            deviceId = SecurityManager.encrypt(session.deviceId),
            deviceSecret = SecurityManager.encrypt(session.deviceSecret)
        )
    }

    private fun decryptSession(session: AuthEntity?): AuthEntity? {
        if (session == null) return null
        return session.copy(
            deviceId = SecurityManager.decrypt(session.deviceId),
            deviceSecret = SecurityManager.decrypt(session.deviceSecret)
        )
    }

    suspend fun ensureActiveSession(accountId: String? = null): Result<AuthEntity> {
        val encrypted = if (accountId != null) {
            authDao.getAccountByIdDirect(accountId)
        } else {
            authDao.getAuthSessionDirect()
        } ?: return Result.failure(Exception("No active account"))
        
        val session = try {
            decryptSession(encrypted) ?: return Result.failure(Exception("Failed to load session"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
        
        val now = System.currentTimeMillis()
        
        // Load from memory cache if available, otherwise use the session from DB
        val current = sessionCache.value[session.accountId] ?: session
        
        // Only refresh if token is missing from cache OR expiring in less than 5 minutes
        if (now < (current.expiresAtMs - 300000)) {
            // Update cache if it was empty but the DB token is still valid
            if (sessionCache.value[session.accountId] == null) {
                sessionCache.value = sessionCache.value + (session.accountId to session)
            }
            AuthDiagnosticsManager.logEvent(AuthEventType.TOKEN_VALID, session.accountId)
            return Result.success(current)
        }

        AuthDiagnosticsManager.logEvent(
            if (now < current.expiresAtMs) AuthEventType.TOKEN_EXPIRING_SOON else AuthEventType.TOKEN_CHECK,
            session.accountId
        )

        return performTokenRenewal(session)
    }

    suspend fun forceTokenRefresh(accountId: String? = null): Result<AuthEntity> {
        val encrypted = if (accountId != null) {
            authDao.getAccountByIdDirect(accountId)
        } else {
            authDao.getAuthSessionDirect()
        } ?: return Result.failure(Exception("No active account"))
        
        val session = try {
            decryptSession(encrypted) ?: return Result.failure(Exception("Failed to load session"))
        } catch (e: Exception) { return Result.failure(e) }
        return performTokenRenewal(session, force = true)
    }

    suspend fun refreshAccessToken(accountId: String? = null): Result<AuthEntity> {
        val encrypted = if (accountId != null) {
            authDao.getAccountByIdDirect(accountId)
        } else {
            authDao.getAuthSessionDirect()
        } ?: return Result.failure(Exception("No active account"))

        val session = try {
            decryptSession(encrypted) ?: return Result.failure(Exception("Failed to load session"))
        } catch (e: Exception) { return Result.failure(e) }
        return performTokenRenewal(session, tryRefresh = true, tryDeviceAuth = false, force = true)
    }

    suspend fun forceNewTokenWithDeviceAuth(accountId: String? = null): Result<AuthEntity> {
        val encrypted = if (accountId != null) {
            authDao.getAccountByIdDirect(accountId)
        } else {
            authDao.getAuthSessionDirect()
        } ?: return Result.failure(Exception("No active account"))

        val session = try {
            decryptSession(encrypted) ?: return Result.failure(Exception("Failed to load session"))
        } catch (e: Exception) { return Result.failure(e) }
        return performTokenRenewal(session, tryRefresh = false, tryDeviceAuth = true, force = true)
    }

    private suspend fun performTokenRenewal(
        session: AuthEntity,
        tryRefresh: Boolean = true,
        tryDeviceAuth: Boolean = true,
        force: Boolean = false
    ): Result<AuthEntity> = refreshMutex.withLock {
        _isRefreshing.value = true
        val startTime = System.currentTimeMillis()
        try {
            // Re-check cache after acquiring lock
            val reCheck = sessionCache.value[session.accountId]
            if (!force && reCheck != null && System.currentTimeMillis() < (reCheck.expiresAtMs - 300000)) {
                return@withLock Result.success(reCheck)
            }

            // 1. Try Refresh Token
            if (tryRefresh && !session.refreshToken.isNullOrBlank()) {
                AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_REQUEST_STARTED, session.accountId, "Grant: refresh_token")
                try {
                    val response = api.getAccessTokenWithRefreshToken(refreshToken = session.refreshToken)
                    val updated = applyTokenResponse(session, response)
                    AuthDiagnosticsManager.logEvent(AuthEventType.REFRESH_SUCCEEDED, session.accountId, durationMs = System.currentTimeMillis() - startTime)
                    return@withLock Result.success(updated)
                } catch (e: Exception) {
                    AuthDiagnosticsManager.logEvent(AuthEventType.REFRESH_FAILED, session.accountId, e.message, getStatusCode(e))
                    if (!tryDeviceAuth) return@withLock Result.failure(e)
                }
            }

            // 2. Try Device Auth Fallback
            if (tryDeviceAuth && !session.deviceId.isNullOrBlank() && !session.deviceSecret.isNullOrBlank()) {
                AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_REQUEST_STARTED, session.accountId, "Grant: device_auth")
                try {
                    val response = api.getAccessTokenWithDeviceAuth(
                        accountId = session.accountId,
                        deviceId = session.deviceId,
                        secret = session.deviceSecret
                    )
                    val updated = applyTokenResponse(session, response)
                    AuthDiagnosticsManager.logEvent(AuthEventType.DEVICE_AUTH_SUCCEEDED, session.accountId, durationMs = System.currentTimeMillis() - startTime)
                    return@withLock Result.success(updated)
                } catch (e: Exception) {
                    val status = getStatusCode(e)
                    AuthDiagnosticsManager.logEvent(AuthEventType.DEVICE_AUTH_FAILED, session.accountId, e.message, status)
                    
                    if (status == 400 || status == 401) {
                        val errorBody = (e as? HttpException)?.response()?.errorBody()?.string() ?: ""
                        if (errorBody.contains("device_auth_not_found") || errorBody.contains("invalid_grant")) {
                            val revoked = session.copy(deviceAuthStatus = DeviceAuthStatus.REVOKED.name)
                            authDao.upsertAuthSession(encryptSession(revoked))
                            AuthDiagnosticsManager.logEvent(AuthEventType.SESSION_MARKED_INVALID, session.accountId, "Revoked")
                        }
                    }
                    return@withLock Result.failure(e)
                }
            }

            Result.failure(Exception("No usable credentials for renewal"))
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun applyTokenResponse(session: AuthEntity, response: EpicTokenResponse): AuthEntity {
        AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_TOKEN_RECEIVED, session.accountId)
        
        val now = System.currentTimeMillis()
        val expiresAtMs = now + (response.expiresIn * 1000)
        
        val updated = session.copy(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken ?: session.refreshToken,
            expiresAtMs = expiresAtMs,
            lastRefreshTimeMs = now,
            deviceAuthStatus = DeviceAuthStatus.VALID.name
        )
        
        // 1. Atomic Persistence
        authDao.upsertAuthSession(encryptSession(updated))
        AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_TOKEN_PERSISTED, updated.accountId)
        
        // 2. Immediate Reload and Verification
        val reloadedEncrypted = authDao.getAccountByIdDirect(updated.accountId)
        val reloaded = decryptSession(reloadedEncrypted)
        
        if (reloaded != null && reloaded.expiresAtMs == updated.expiresAtMs) {
            AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_STATE_RELOADED, updated.accountId, "Expiry: ${reloaded.expiresAtMs}")
            
            // 3. Update In-Memory Cache (Source of truth for UI and Interceptor)
            sessionCache.value = sessionCache.value + (reloaded.accountId to reloaded)
            AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_STATE_UPDATED, reloaded.accountId)
            
            // 4. Force Ticker to update timer
            AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_TIMER_UPDATED, reloaded.accountId)
            
            return reloaded
        } else {
            AuthDiagnosticsManager.logEvent(AuthEventType.AUTH_STATE_RELOADED, updated.accountId, "Persistence check FAILED")
            // Fallback to updating cache with 'updated' anyway so app remains usable
            sessionCache.value = sessionCache.value + (updated.accountId to updated)
            return updated
        }
    }

    private fun getStatusCode(e: Exception): Int? = (e as? HttpException)?.code()

    // Existing methods adapted for diagnostics
    suspend fun loginWithAuthCode(rawCode: String): Result<AuthEntity> {
        val input = rawCode.trim()
        var cleanCode = ""
        if (input.length == 32 && input.matches(Regex("[a-fA-F0-9]{32}"))) cleanCode = input
        else {
            val codeMatch = Regex("""(?:code|authorizationCode|authorization_code|authorizarion_code)["']?\s*[:=]\s*["']?([a-fA-F0-9]{32})""").find(input)
            if (codeMatch != null) cleanCode = codeMatch.groupValues[1]
            else {
                val hex32Match = Regex("""[a-fA-F0-9]{32}""").find(input)
                if (hex32Match != null) cleanCode = hex32Match.value
            }
        }
        cleanCode = cleanCode.trim().lowercase()
        if (cleanCode.isBlank()) return Result.failure(Exception("Invalid code format"))

        return try {
            val response = api.getAccessTokenWithAuthCode(code = cleanCode)
            val iconUrl = epicAccountRepository.fetchEquippedSkinIcon(response.accessToken, response.accountId)
            
            var session = AuthEntity(
                accountId = response.accountId,
                displayName = response.displayName ?: "Epic Player",
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAtMs = System.currentTimeMillis() + (response.expiresIn * 1000),
                equippedSkinIcon = iconUrl,
                isActive = true
            )
            
            try {
                val deviceAuth = api.createDeviceAuth(bearerToken = "Bearer ${response.accessToken}", accountId = response.accountId)
                session = session.copy(deviceId = deviceAuth.deviceId, deviceSecret = deviceAuth.secret)
                AuthDiagnosticsManager.logEvent(AuthEventType.DEVICE_AUTH_SUCCEEDED, session.accountId, "Initial Login")
            } catch (e: Exception) {
                AuthDiagnosticsManager.logEvent(AuthEventType.DEVICE_AUTH_FAILED, session.accountId, "Device Auth registration failed: ${e.message}")
            }

            sessionCache.value = sessionCache.value + (session.accountId to session)
            authDao.saveAuthSession(encryptSession(session))
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithExchangeCode(rawExchangeCode: String): Result<AuthEntity> {
        var cleanCode = rawExchangeCode.trim()
        val hex32Match = Regex("""[a-fA-F0-9]{32}""").find(cleanCode)
        if (hex32Match != null) cleanCode = hex32Match.value
        if (cleanCode.isBlank()) return Result.failure(Exception("Empty code"))

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
                if (!response.accessToken.isNullOrBlank()) {
                    val iconUrl = epicAccountRepository.fetchEquippedSkinIcon(response.accessToken, response.accountId)
                    var session = AuthEntity(
                        accountId = response.accountId,
                        displayName = response.displayName ?: "Epic Player",
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        expiresAtMs = System.currentTimeMillis() + (response.expiresIn * 1000),
                        equippedSkinIcon = iconUrl,
                        isActive = true
                    )
                    try {
                        val da = api.createDeviceAuth("Bearer ${response.accessToken}", response.accountId)
                        session = session.copy(deviceId = da.deviceId, deviceSecret = da.secret)
                        AuthDiagnosticsManager.logEvent(AuthEventType.DEVICE_AUTH_SUCCEEDED, session.accountId, "Initial Login (Exchange)")
                    } catch (e: Exception) {
                        AuthDiagnosticsManager.logEvent(AuthEventType.DEVICE_AUTH_FAILED, session.accountId, "Device Auth registration failed: ${e.message}")
                    }
                    
                    sessionCache.value = sessionCache.value + (session.accountId to session)
                    authDao.saveAuthSession(encryptSession(session))
                    return Result.success(session)
                }
            } catch (e: Exception) { lastException = e }
        }
        return Result.failure(lastException ?: Exception("Failed"))
    }


    suspend fun verifyCurrentToken(): Result<EpicVerifyResponse> {
        val session = ensureActiveSession().getOrNull() ?: return Result.failure(Exception("No session"))
        return try {
            val response = api.verifyToken("Bearer ${session.accessToken}")
            Result.success(response)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun logout() {
        val session = decryptSession(authDao.getAuthSessionDirect())
        session?.let {
            try {
                api.killSessions("Bearer ${it.accessToken}")
                AuthDiagnosticsManager.logEvent(AuthEventType.LOGOUT, it.accountId, "Session Killed")
            } catch (e: Exception) { }
        }
        sessionCache.value = emptyMap()
        authDao.clearAllSessions()
    }

    suspend fun exportAccounts(password: CharArray): Result<String> {
        val accounts = authDao.getAllAccounts().first().map { decryptSession(it)!! }
        val type = Types.newParameterizedType(List::class.java, AuthEntity::class.java)
        val json = moshi.adapter<List<AuthEntity>>(type).toJson(accounts)
        return Result.success(SecurityManager.encryptWithPassword(json, password))
    }

    suspend fun importAccounts(encryptedData: String, password: CharArray): Result<Int> {
        val json = SecurityManager.decryptWithPassword(encryptedData, password) ?: return Result.failure(Exception("Bad password"))
        val type = Types.newParameterizedType(List::class.java, AuthEntity::class.java)
        val accounts = moshi.adapter<List<AuthEntity>>(type).fromJson(json) ?: emptyList()
        accounts.forEach { authDao.upsertAuthSession(encryptSession(it)) }
        return Result.success(accounts.size)
    }

    // Pass-throughs
    fun getPastSeasons(accountId: String): Flow<List<PastSeasonEntity>> = authDao.getPastSeasons(accountId)
    val recentSearches: Flow<List<RecentSearchEntity>> = authDao.getRecentSearches()
    suspend fun addRecentSearch(name: String) { if (name.isNotBlank()) authDao.saveRecentSearch(RecentSearchEntity(accountName = name.trim())) }
    suspend fun removeRecentSearch(name: String) { authDao.deleteRecentSearch(name) }

    suspend fun updateSessionStats(
        accountLevel: Int,
        seasonalLevel: Int,
        totalWins: Int,
        pastSeasons: List<PastSeasonEntity> = emptyList()
    ) {
        val current = decryptSession(authDao.getAuthSessionDirect()) ?: return
        val updated = current.copy(accountLevel = accountLevel, seasonalLevel = seasonalLevel, totalWins = totalWins)
        sessionCache.value = sessionCache.value + (updated.accountId to updated)
        authDao.saveAuthSession(encryptSession(updated))
        if (pastSeasons.isNotEmpty()) {
            authDao.clearPastSeasons(current.accountId)
            authDao.savePastSeasons(pastSeasons.map { it.copy(accountId = current.accountId) })
        }
    }

    suspend fun getValidSession(): AuthEntity? = ensureActiveSession().getOrNull()

    suspend fun generateExchangeCode(): Result<String> {
        return try {
            val session = getValidSession() ?: return Result.failure(Exception("Session invalid."))
            val response = api.getExchangeCode("Bearer ${session.accessToken}")
            val code = response["code"]?.toString()
            if (!code.isNullOrBlank()) Result.success(code) else Result.failure(Exception("No code returned."))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getRawDecryptedSessionJson(): String {
        val encrypted = authDao.getAuthSessionDirect() ?: return "{\"error\": \"No session in database\"}"
        val session = try {
            decryptSession(encrypted) ?: return "{\"error\": \"Decryption returned null\"}"
        } catch (e: Exception) {
            return "{\"error\": \"Decryption failed: ${e.message}\"}"
        }
        
        return try {
            val adapter = moshi.adapter(AuthEntity::class.java).indent("  ")
            adapter.toJson(session)
        } catch (e: Exception) {
            "{\"error\": \"Serialization failed: ${e.message}\"}"
        }
    }
}
