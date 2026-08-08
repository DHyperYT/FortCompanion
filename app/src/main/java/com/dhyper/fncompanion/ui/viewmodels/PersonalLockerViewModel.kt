package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.models.LockerCategory
import com.dhyper.fncompanion.data.models.ParsedLockerItem
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LockerSortOption(val displayName: String) {
    FAVORITES_FIRST("Favorites First"),
    NAME_ASC("Name (A-Z)"),
    RARITY_DESC("Rarity (High to Low)"),
    CATEGORY("Category")
}

sealed class LockerUiState {
    object Unauthenticated : LockerUiState()
    object Loading : LockerUiState()
    data class Success(
        val allItems: List<ParsedLockerItem>,
        val filteredItems: List<ParsedLockerItem>,
        val vbucksBalance: Long,
        val selectedCategory: LockerCategory? = null,
        val favoritesOnly: Boolean = false,
        val searchQuery: String = "",
        val sortOption: LockerSortOption = LockerSortOption.FAVORITES_FIRST
    ) : LockerUiState()
    data class Error(val message: String) : LockerUiState()
}

class PersonalLockerViewModel(
    private val authRepository: AuthRepository,
    private val accountRepository: EpicAccountRepository = EpicAccountRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LockerUiState>(LockerUiState.Unauthenticated)
    val uiState: StateFlow<LockerUiState> = _uiState.asStateFlow()

    fun loadLocker(session: AuthEntity?) {
        if (session == null) {
            _uiState.value = LockerUiState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _uiState.value = LockerUiState.Loading
            
            // 1. Ensure we have a fresh token using Device Auth refresh
            val validSession = authRepository.getValidSession() ?: session
            
            val vbucksResult = accountRepository.fetchVBucksBalance(validSession.accessToken, validSession.accountId)
            val lockerResult = accountRepository.fetchPersonalLockerCosmetics(validSession.accessToken, validSession.accountId)

            val vbucks = vbucksResult.getOrDefault(0L)

            lockerResult.fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        // Force logout if locker is empty (likely expired session)
                        viewModelScope.launch {
                            authRepository.logout()
                        }
                        _uiState.value = LockerUiState.Unauthenticated
                        return@fold
                    }

                    val defaultSort = LockerSortOption.FAVORITES_FIRST
                    _uiState.value = LockerUiState.Success(
                        allItems = items,
                        filteredItems = filterAndSort(items, null, false, "", defaultSort),
                        vbucksBalance = vbucks,
                        sortOption = defaultSort
                    )
                },
                onFailure = { error ->
                    _uiState.value = LockerUiState.Error(
                        error.localizedMessage ?: "Failed to query real-time Athena profile for account ${session.displayName}"
                    )
                }
            )
        }
    }

    fun setCategory(category: LockerCategory?) {
        val current = _uiState.value
        if (current is LockerUiState.Success) {
            val newCategory = if (current.selectedCategory == category) null else category
            _uiState.value = current.copy(
                selectedCategory = newCategory,
                filteredItems = filterAndSort(
                    current.allItems,
                    newCategory,
                    current.favoritesOnly,
                    current.searchQuery,
                    current.sortOption
                )
            )
        }
    }

    fun toggleFavoritesOnly() {
        val current = _uiState.value
        if (current is LockerUiState.Success) {
            val newFav = !current.favoritesOnly
            _uiState.value = current.copy(
                favoritesOnly = newFav,
                filteredItems = filterAndSort(
                    current.allItems,
                    current.selectedCategory,
                    newFav,
                    current.searchQuery,
                    current.sortOption
                )
            )
        }
    }

    fun setSearchQuery(query: String) {
        val current = _uiState.value
        if (current is LockerUiState.Success) {
            _uiState.value = current.copy(
                searchQuery = query,
                filteredItems = filterAndSort(
                    current.allItems,
                    current.selectedCategory,
                    current.favoritesOnly,
                    query,
                    current.sortOption
                )
            )
        }
    }

    fun setSortOption(sortOption: LockerSortOption) {
        val current = _uiState.value
        if (current is LockerUiState.Success) {
            _uiState.value = current.copy(
                sortOption = sortOption,
                filteredItems = filterAndSort(
                    current.allItems,
                    current.selectedCategory,
                    current.favoritesOnly,
                    current.searchQuery,
                    sortOption
                )
            )
        }
    }

    private fun filterAndSort(
        list: List<ParsedLockerItem>,
        category: LockerCategory?,
        favsOnly: Boolean,
        query: String,
        sortOption: LockerSortOption
    ): List<ParsedLockerItem> {
        val filtered = list.filter { item ->
            val matchCat = category == null || item.category == category
            val matchFav = !favsOnly || item.isFavorite
            val matchQuery = query.isBlank() || item.name.contains(query, ignoreCase = true)
            matchCat && matchFav && matchQuery
        }

        return when (sortOption) {
            LockerSortOption.FAVORITES_FIRST -> filtered.sortedWith(
                compareByDescending<ParsedLockerItem> { it.isFavorite }.thenBy { it.name }
            )
            LockerSortOption.NAME_ASC -> filtered.sortedBy { it.name }
            LockerSortOption.RARITY_DESC -> filtered.sortedByDescending { getRarityRank(it.rarity) }
            LockerSortOption.CATEGORY -> filtered.sortedBy { it.category.name }
        }
    }

    private fun getRarityRank(rarity: String): Int {
        return when (rarity.lowercase()) {
            "mythic", "transcendent" -> 6
            "legendary" -> 5
            "epic" -> 4
            "rare" -> 3
            "uncommon" -> 2
            "common" -> 1
            else -> 0
        }
    }
}
