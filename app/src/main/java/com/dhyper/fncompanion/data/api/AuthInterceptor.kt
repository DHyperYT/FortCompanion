package com.dhyper.fncompanion.data.api

import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.repository.AuthDiagnosticsManager
import com.dhyper.fncompanion.data.repository.AuthEventType
import com.dhyper.fncompanion.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(private val authRepository: AuthRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Skip auth for public APIs or auth endpoints themselves to avoid loops/recursion
        val isAuthEndpoint = path.contains("/account/api/oauth/token") || 
                             path.contains("/account/api/oauth/verify") ||
                             path.contains("/deviceAuth")
        
        val authHeader = request.header("Authorization")
        val isPublicApi = path.contains("/fortnite-api.com/") || path.contains("/seebot.dev/")

        if (isAuthEndpoint || isPublicApi || authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.proceed(request)
        }

        val startTime = System.currentTimeMillis()
        
        val sessionResult = runBlocking {
            authRepository.ensureActiveSession()
        }

        val session = sessionResult.getOrNull()
        val accountId = session?.accountId
        
        val newRequest = if (session != null) {
            request.newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .build()
        } else {
            request
        }

        var response = chain.proceed(newRequest)

        if (response.code == 401) {
            AuthDiagnosticsManager.logEvent(
                AuthEventType.AUTH_REQUEST_401,
                accountId = accountId,
                details = "Path: $path",
                statusCode = 401
            )
            
            response.close()

            val retryResult = runBlocking<Result<AuthEntity>> {
                // Use the full recovery path (Refresh -> Device Auth) for this specific account
                authRepository.forceTokenRefresh(accountId)
            }

            val refreshedSession = retryResult.getOrNull()
            if (refreshedSession != null) {
                AuthDiagnosticsManager.logEvent(
                    AuthEventType.ACCESS_TOKEN_RECOVERED,
                    accountId = refreshedSession.accountId,
                    details = "Retrying path: $path"
                )
                val retryRequest = request.newBuilder()
                    .header("Authorization", "Bearer ${refreshedSession.accessToken}")
                    .build()
                response = chain.proceed(retryRequest)
            } else {
                AuthDiagnosticsManager.logEvent(
                    AuthEventType.AUTH_RETRY_FAILED,
                    accountId = accountId,
                    details = "Path: $path"
                )
            }
        }

        val duration = System.currentTimeMillis() - startTime
        if (response.isSuccessful) {
            AuthDiagnosticsManager.logEvent(AuthEventType.API_SUCCESS, accountId = accountId, details = "Path: $path", durationMs = duration)
        } else {
            AuthDiagnosticsManager.logEvent(AuthEventType.API_FAILURE, accountId = accountId, details = "Path: $path", statusCode = response.code, durationMs = duration)
        }

        return response
    }
}
