package com.dhyper.fncompanion.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AuthEventType {
    TOKEN_CHECK,
    TOKEN_VALID,
    TOKEN_EXPIRED,
    TOKEN_EXPIRING_SOON,
    REFRESH_STARTED,
    REFRESH_SUCCEEDED,
    REFRESH_FAILED,
    DEVICE_AUTH_FALLBACK_STARTED,
    DEVICE_AUTH_SUCCEEDED,
    DEVICE_AUTH_FAILED,
    CREDENTIALS_PERSISTED,
    AUTH_REQUEST_401,
    AUTH_RETRY_SUCCEEDED,
    AUTH_RETRY_FAILED,
    SESSION_MARKED_INVALID,
    DECRYPTION_SUCCESS,
    DECRYPTION_FAILED,
    API_SUCCESS,
    API_FAILURE,
    LOGOUT,
    TOKEN_RECEIVED,
    TOKEN_PARSED,
    EXPIRY_CALCULATED,
    PERSISTED,
    RELOADED,
    MEMORY_UPDATED,
    HTTP_CLIENT_UPDATED,
    DIAGNOSTICS_UPDATED,
    AUTH_REQUEST_STARTED,
    AUTH_TOKEN_RECEIVED,
    AUTH_TOKEN_PERSISTED,
    AUTH_STATE_RELOADED,
    AUTH_STATE_UPDATED,
    AUTH_TIMER_UPDATED,
    ACCESS_TOKEN_RECOVERED
}

data class AuthDiagnosticEvent(
    val timestamp: Long,
    val type: AuthEventType,
    val accountId: String? = null,
    val details: String? = null,
    val statusCode: Int? = null,
    val durationMs: Long? = null
)

object AuthDiagnosticsManager {
    private val _events = MutableStateFlow<List<AuthDiagnosticEvent>>(emptyList())
    val events: StateFlow<List<AuthDiagnosticEvent>> = _events.asStateFlow()

    fun logEvent(
        type: AuthEventType,
        accountId: String? = null,
        details: String? = null,
        statusCode: Int? = null,
        durationMs: Long? = null
    ) {
        val event = AuthDiagnosticEvent(
            timestamp = System.currentTimeMillis(),
            type = type,
            accountId = accountId?.take(8), // Safe partial ID
            details = details,
            statusCode = statusCode,
            durationMs = durationMs
        )
        _events.value = (listOf(event) + _events.value).take(100)
    }

    fun clear() {
        _events.value = emptyList()
    }
}
