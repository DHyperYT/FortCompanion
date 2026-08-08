package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.db.PastSeasonEntity
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
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

    val authSession: StateFlow<AuthEntity?> = authRepository.authSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val pastSeasons: StateFlow<List<PastSeasonEntity>> = authSession
        .flatMapLatest { session ->
            if (session != null) authRepository.getPastSeasons(session.accountId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun loginWithExchangeCode(exchangeCode: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.LoggingIn
            val result = authRepository.loginWithExchangeCode(exchangeCode)
            result.fold(
                onSuccess = { session ->
                    fetchAndSaveCareer(session)
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
                onSuccess = { session ->
                    fetchAndSaveCareer(session)
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

    private fun fetchAndSaveCareer(session: AuthEntity) {
        viewModelScope.launch {
            val careerResult = epicAccountRepository.fetchPersonalCareerDetails(
                accessToken = session.accessToken,
                accountId = session.accountId,
                displayName = session.displayName
            )
            careerResult.onSuccess { details ->
                authRepository.updateSessionStats(
                    accountLevel = details.accountLevel,
                    seasonalLevel = details.seasonalLevel,
                    totalWins = details.lifetimeWins,
                    pastSeasons = details.pastSeasons.map {
                        PastSeasonEntity(
                            accountId = session.accountId,
                            seasonNumber = it.seasonNumber,
                            seasonName = it.seasonName,
                            seasonLevel = it.seasonLevel,
                            battlePassTier = it.battlePassTier,
                            seasonWins = it.seasonWins,
                            hasBattlePass = it.hasBattlePass
                        )
                    }
                )
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            epicAccountRepository.clearCache()
            val session = authRepository.getValidSession() ?: return@launch
            fetchAndSaveCareer(session)
        }
    }

    fun clearLoginError() {
        _loginState.value = LoginState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = LoginState.Idle
        }
    }
}
