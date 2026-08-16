package com.dhyper.fncompanion.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class AuthEventType {
    TOKEN_VALID,
    TOKEN_EXPIRING_SOON,
    TOKEN_CHECK,
    AUTH_REQUEST_STARTED,
    REFRESH_SUCCEEDED,
    REFRESH_FAILED,
    DEVICE_AUTH_SUCCEEDED,
    DEVICE_AUTH_FAILED,
    SESSION_MARKED_INVALID,
    AUTH_TOKEN_RECEIVED,
    AUTH_TOKEN_PERSISTED,
    AUTH_STATE_RELOADED,
    AUTH_STATE_UPDATED,
    AUTH_TIMER_UPDATED,
    LOGOUT,
    AUTH_REQUEST_401,
    ACCESS_TOKEN_RECOVERED,
    AUTH_RETRY_FAILED,
    AUTH_RETRY_SUCCEEDED,
    TOKEN_EXPIRED,
    API_SUCCESS,
    API_FAILURE
}

data class AuthDiagnosticEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: AuthEventType,
    val accountId: String?,
    val details: String?,
    val statusCode: Int?,
    val durationMs: Long?,
    val timestamp: Long = System.currentTimeMillis()
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
        val newEvent = AuthDiagnosticEvent(
            type = type,
            accountId = accountId,
            details = details,
            statusCode = statusCode,
            durationMs = durationMs
        )
        // Add to the beginning and keep only the last 100 events to prevent memory issues
        _events.value = (listOf(newEvent) + _events.value).take(100)
    }
}
