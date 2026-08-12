package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.models.StwMissionAlert
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StwUiState {
    object Unauthenticated : StwUiState()
    object Loading : StwUiState()
    data class Success(
        val alerts: List<StwMissionAlert>
    ) : StwUiState()
    data class Error(val message: String) : StwUiState()
}

class StwViewModel(
    private val authRepo: AuthRepository,
    private val epicAccountRepo: EpicAccountRepository = EpicAccountRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StwUiState>(StwUiState.Loading)
    val uiState: StateFlow<StwUiState> = _uiState.asStateFlow()

    private val _homebaseData = MutableStateFlow<com.dhyper.fncompanion.data.models.StwHomebaseData?>(null)
    val homebaseData: StateFlow<com.dhyper.fncompanion.data.models.StwHomebaseData?> = _homebaseData.asStateFlow()

    init {
        loadStwData()
    }

    fun loadHomebaseData() {
        viewModelScope.launch {
            _uiState.value = StwUiState.Loading
            val sessionResult = authRepo.ensureActiveSession()
            val session = sessionResult.getOrNull()
            if (session == null) {
                _uiState.value = StwUiState.Unauthenticated
                return@launch
            }

            val result = epicAccountRepo.fetchStwHomebaseData(session.accessToken, session.accountId)
            if (result.isSuccess) {
                _homebaseData.value = result.getOrNull()
                _uiState.value = StwUiState.Success(emptyList()) 
            } else {
                _uiState.value = StwUiState.Error(result.exceptionOrNull()?.message ?: "Failed to fetch homebase data")
            }
        }
    }

    fun loadStwData() {
        viewModelScope.launch {
            _uiState.value = StwUiState.Loading
            val sessionResult = authRepo.ensureActiveSession()
            val session = sessionResult.getOrNull()
            if (session == null) {
                _uiState.value = StwUiState.Unauthenticated
                return@launch
            }

            try {
                // Fetch the full world info from Epic API
                val result = epicAccountRepo.fetchStwWorldInfoFull(session.accessToken)
                
                if (result.isSuccess) {
                    _uiState.value = StwUiState.Success(
                        alerts = result.getOrDefault(emptyList())
                    )
                } else {
                    _uiState.value = StwUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch STW data")
                }
            } catch (e: Exception) {
                _uiState.value = StwUiState.Error(e.localizedMessage ?: "Unknown STW error")
            }
        }
    }
}
