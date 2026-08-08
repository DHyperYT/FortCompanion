package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.models.NewsData
import com.dhyper.fncompanion.data.repository.FortniteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NewsUiState {
    object Loading : NewsUiState()
    data class Success(val newsData: NewsData, val activeTab: String = "BR") : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}

class NewsViewModel(
    private val repository: FortniteRepository = FortniteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = NewsUiState.Loading
            val result = repository.fetchNews()
            result.fold(
                onSuccess = { news ->
                    _uiState.value = NewsUiState.Success(newsData = news, activeTab = "BR")
                },
                onFailure = { error ->
                    _uiState.value = NewsUiState.Error(error.localizedMessage ?: "Failed to load Fortnite News")
                }
            )
        }
    }

    fun setTab(tab: String) {
        val current = _uiState.value
        if (current is NewsUiState.Success) {
            _uiState.value = current.copy(activeTab = tab)
        }
    }
}
