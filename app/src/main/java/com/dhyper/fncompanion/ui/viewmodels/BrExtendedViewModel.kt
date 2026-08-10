package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.models.AesData
import com.dhyper.fncompanion.data.repository.FortniteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AesUiState {
    object Loading : AesUiState()
    data class Success(val aesData: AesData) : AesUiState()
    data class Error(val message: String) : AesUiState()
}

class BrExtendedViewModel(
    private val repository: FortniteRepository = FortniteRepository()
) : ViewModel() {

    private val _aesState = MutableStateFlow<AesUiState>(AesUiState.Loading)
    val aesState: StateFlow<AesUiState> = _aesState.asStateFlow()

    init {
        loadAes()
    }

    fun loadAes() {
        viewModelScope.launch {
            _aesState.value = AesUiState.Loading
            val result = repository.fetchAes()
            result.fold(
                onSuccess = { data -> _aesState.value = AesUiState.Success(data) },
                onFailure = { error -> _aesState.value = AesUiState.Error(error.localizedMessage ?: "Failed to load AES keys") }
            )
        }
    }
}
