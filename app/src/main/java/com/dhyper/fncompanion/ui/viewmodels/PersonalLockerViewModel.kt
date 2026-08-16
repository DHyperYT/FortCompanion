package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.models.LockerCategory
import com.dhyper.fncompanion.data.models.ParsedLockerItem
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import com.dhyper.fncompanion.ui.components.getRarityRank
import com.dhyper.fncompanion.ui.utils.LockerImageGenerator
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LockerSortOption(val displayName: String) {
    FAVORITES_FIRST("Favorites First"),
    NAME_ASC("Name (A-Z)"),
    RARITY_DESC("Rarity (High to Low)"),
    ADDED_DESC("Date Added (New)")
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
        val sortOption: LockerSortOption = LockerSortOption.RARITY_DESC
    ) : LockerUiState()
    data class Error(val message: String) : LockerUiState()
}

class PersonalLockerViewModel(
    private val authRepository: AuthRepository,
    private val accountRepository: EpicAccountRepository = EpicAccountRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LockerUiState>(LockerUiState.Unauthenticated)
    val uiState: StateFlow<LockerUiState> = _uiState.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportedBitmap = MutableStateFlow<Bitmap?>(null)
    val exportedBitmap: StateFlow<Bitmap?> = _exportedBitmap.asStateFlow()

    fun loadLocker(currentSession: AuthEntity?) {
        viewModelScope.launch {
            _uiState.value = LockerUiState.Loading
            
            val sessionResult = authRepository.ensureActiveSession()
            val validSession = sessionResult.getOrNull()
            
            if (validSession == null) {
                _uiState.value = LockerUiState.Unauthenticated
                return@launch
            }
            
            val vbucksResult = accountRepository.fetchVBucksBalance(validSession.accessToken, validSession.accountId)
            val lockerResult = accountRepository.fetchPersonalLockerCosmetics(validSession.accessToken, validSession.accountId)

            val vbucks = vbucksResult.getOrDefault(0L)

            lockerResult.fold(
                onSuccess = { items ->
                    val defaultSort = LockerSortOption.RARITY_DESC
                    _uiState.value = LockerUiState.Success(
                        allItems = items,
                        filteredItems = filterAndSort(items, LockerCategory.OUTFIT, false, "", defaultSort),
                        vbucksBalance = vbucks,
                        selectedCategory = LockerCategory.OUTFIT,
                        sortOption = defaultSort
                    )
                },
                onFailure = { error ->
                    _uiState.value = LockerUiState.Error(
                        error.localizedMessage ?: "Failed to query real-time Athena profile for account ${validSession.displayName}"
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

    fun exportLockerImage(context: Context, title: String) {
        val state = _uiState.value
        if (state !is LockerUiState.Success) return

        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            // First, generate a fast downscaled preview
            val previewBitmap = LockerImageGenerator.generateLockerImage(
                context = context,
                items = state.filteredItems,
                title = title,
                isPreview = true,
                onProgress = { _exportProgress.value = it }
            )
            _exportedBitmap.value = previewBitmap
            _isExporting.value = false
        }
    }

    suspend fun generateFullExport(context: Context, title: String): Bitmap? {
        val state = _uiState.value
        if (state !is LockerUiState.Success) return null
        
        _exportProgress.value = 0f
        return LockerImageGenerator.generateLockerImage(
            context = context,
            items = state.filteredItems,
            title = title,
            isPreview = false,
            onProgress = { _exportProgress.value = it }
        )
    }

    fun clearExportedImage() {
        _exportedBitmap.value = null
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
            val matchQuery = query.isBlank() || 
                             item.name.contains(query, ignoreCase = true) ||
                             item.artist?.contains(query, ignoreCase = true) == true
            
            // Filter out "Rare" car bodies and wheels
            val isRareCarOrWheel = (item.category == LockerCategory.CAR || item.category == LockerCategory.WHEELS) && 
                                   item.rarity.equals("Rare", ignoreCase = true)
            
            matchCat && matchFav && matchQuery && !isRareCarOrWheel
        }

        return when (sortOption) {
            LockerSortOption.FAVORITES_FIRST -> filtered.sortedWith(
                compareByDescending<ParsedLockerItem> { it.isFavorite }.thenBy { it.name }
            )
            LockerSortOption.NAME_ASC -> filtered.sortedBy { it.name }
            LockerSortOption.RARITY_DESC -> filtered.sortedWith(
                compareByDescending<ParsedLockerItem> { getRarityRank(it.rarity) }
                .thenBy { it.rarity }
                .thenBy { it.name }
            )
            LockerSortOption.ADDED_DESC -> filtered.sortedWith(
                compareByDescending<ParsedLockerItem> { it.added ?: "" }
                .thenByDescending { extractNumericFromId(it.templateId) }
                .thenByDescending { 
                    SeasonUtils.getGlobalSeasonNumber(
                        it.introduction?.chapter, 
                        it.introduction?.season
                    )
                }
                .thenBy { it.name }
            )
        }
    }

    private fun extractNumericFromId(id: String): Int {
        val match = Regex("""(?i)[A-Z_]+_(\d+)""").find(id)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

}
