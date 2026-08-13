package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import com.dhyper.fncompanion.data.models.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
sealed class LoginState {
    object Idle : LoginState()
    object LoggingIn : LoginState()
    data class Error(val message: String) : LoginState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val epicAccountRepository: EpicAccountRepository = EpicAccountRepository()
) : ViewModel() {

    val authSession: StateFlow<AuthState> = authRepository.authSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.NoCredentials
        )

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun loginWithExchangeCode(exchangeCode: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.LoggingIn
            val result = authRepository.loginWithExchangeCode(exchangeCode)
            result.fold(
                onSuccess = { _ ->
                    _loginState.value = LoginState.Idle
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(
                        error.localizedMessage ?: "Failed to authenticate with exchange code."
                    )
                }
            )
        }
    }

    fun loginWithAuthCode(code: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.LoggingIn
            val result = authRepository.loginWithAuthCode(code)
            result.fold(
                onSuccess = { _ ->
                    _loginState.value = LoginState.Idle
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(
                        error.localizedMessage ?: "Failed to authenticate with authorization code."
                    )
                }
            )
        }
    }

    fun generateExchangeCode(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.generateExchangeCode()
            onResult(result.getOrNull())
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            // epicAccountRepository.clearCache() // Removed as clearCache is not implemented
        }
    }

    fun clearLoginError() {
        _loginState.value = LoginState.Idle
    }

    fun verifyCurrentToken(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.verifyCurrentToken()
            result.fold(
                onSuccess = { onResult("Token Valid: ${it.accountId} (Expires in ${it.expiresIn}s)") },
                onFailure = { onResult("Token Invalid/Error: ${it.localizedMessage}") }
            )
        }
    }

    fun refreshAccessToken(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.refreshAccessToken()
            result.fold(
                onSuccess = { onResult("TOKEN_REFRESHED: Success.") },
                onFailure = { onResult("REFRESH_TOKEN_INVALID: ${it.localizedMessage}") }
            )
        }
    }

    fun getNewAccessToken(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.forceNewTokenWithDeviceAuth()
            result.fold(
                onSuccess = { onResult("DEVICE_AUTH_RECOVERED: Success.") },
                onFailure = { onResult("DEVICE_AUTH_INVALID: ${it.localizedMessage}") }
            )
        }
    }

    fun testFullAuthRecovery(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val verify1 = authRepository.verifyCurrentToken()
            val initial = if (verify1.isSuccess) "Initial: VALID. " else "Initial: INVALID. "
            
            val recovery = authRepository.forceTokenRefresh()
            recovery.fold(
                onSuccess = {
                    val verify2 = authRepository.verifyCurrentToken()
                    val final = if (verify2.isSuccess) "Final: VALID." else "Final: INVALID."
                    onResult(initial + "SUCCESS: " + final)
                },
                onFailure = {
                    onResult(initial + "FAILED: ${it.localizedMessage}")
                }
            )
        }
    }

    fun getRawAuthState(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(authRepository.getRawDecryptedSessionJson())
        }
    }

    fun fetchProfileJson(profileId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.queryRawMcpProfile(profileId)
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { onResult("Error fetching $profileId: ${it.localizedMessage}") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = LoginState.Idle
        }
    }
}
