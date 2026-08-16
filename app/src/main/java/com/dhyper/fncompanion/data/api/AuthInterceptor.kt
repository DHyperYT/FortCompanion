package com.dhyper.fncompanion.data.api

import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

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

        // 1. Proactive check: Ensure session is valid before proceeding
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

        // 2. Reactive check: Handle 401 Unauthorized
        if (response.code == 401) {
            response.close()

            val retryResult = runBlocking<Result<AuthEntity>> {
                // Use the full recovery path (Refresh -> Device Auth) for this specific account
                authRepository.forceTokenRefresh(accountId)
            }

            val refreshedSession = retryResult.getOrNull()
            if (refreshedSession != null) {
                val retryRequest = request.newBuilder()
                    .header("Authorization", "Bearer ${refreshedSession.accessToken}")
                    .build()
                response = chain.proceed(retryRequest)
            }
        }

        return response
    }
}
