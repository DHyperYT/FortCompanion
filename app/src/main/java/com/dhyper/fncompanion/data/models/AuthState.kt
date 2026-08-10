package com.dhyper.fncompanion.data.models

import com.dhyper.fncompanion.data.db.AuthEntity

sealed class AuthState {
    object NoCredentials : AuthState()
    data class StoredAccount(val session: AuthEntity) : AuthState()
    data class TokenRefreshing(val session: AuthEntity) : AuthState()
    data class Active(val session: AuthEntity) : AuthState()
    data class TokenExpired(val session: AuthEntity) : AuthState()
    data class DeviceAuthAvailable(val session: AuthEntity) : AuthState()
    data class SessionValid(val session: AuthEntity) : AuthState()
    data class SessionExpired(val session: AuthEntity) : AuthState()
    data class AuthenticationFailed(val session: AuthEntity, val error: String) : AuthState()
    data class ReauthRequired(val session: AuthEntity, val reason: String) : AuthState()
    data class DecryptionError(val session: AuthEntity) : AuthState()
    data class NetworkError(val session: AuthEntity, val message: String) : AuthState()
}

enum class DeviceAuthStatus {
    VALID,
    REVOKED,
    UNKNOWN
}
