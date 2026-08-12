package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.WishlistDao
import com.dhyper.fncompanion.data.db.WishlistEntity
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.data.repository.*
import com.dhyper.fncompanion.ui.components.getRarityRank
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CosmeticSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    ADDED_DESC("Date Added (New)"),
    RARITY_DESC("Rarity (High-Low)")
}

sealed class CosmeticsUiState {
    object Loading : CosmeticsUiState()
    data class Success(
        val allItems: List<CosmeticItem>,
        val filteredItems: List<CosmeticItem>,
        val selectedCategory: String = "All",
        val wishlistIds: Set<String> = emptySet(),
        val ownedIds: Set<String> = emptySet(),
        val wishlistOnly: Boolean = false,
        val sortOption: CosmeticSortOption = CosmeticSortOption.ADDED_DESC,
        val currentPage: Int = 1,
        val totalPages: Int = 1,
        val shopItemIds: Set<String> = emptySet()
    ) : CosmeticsUiState()
    data class Error(val message: String) : CosmeticsUiState()
}

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CosmeticsViewModel(
    private val repository: FortniteRepository = FortniteRepository(),
    private val epicAccountRepo: EpicAccountRepository = EpicAccountRepository(),
    private val ytRepo: YouTubeRepository = YouTubeRepository(),
    private val authRepo: AuthRepository,
    private val wishlistDao: WishlistDao,
    private val settingsDao: com.dhyper.fncompanion.data.db.SettingsDao
) : ViewModel() {

    private val ITEMS_PER_PAGE = 50

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _wishlistOnly = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(CosmeticSortOption.ADDED_DESC)
    private val _currentPage = MutableStateFlow(1)
    private val _setFilter = MutableStateFlow<String?>(null)
    
    private val _selectedVideoId = MutableStateFlow<String?>(null)
    val selectedVideoId: StateFlow<String?> = _selectedVideoId.asStateFlow()
    private val _isSearchingVideo = MutableStateFlow(false)
    val isSearchingVideo: StateFlow<Boolean> = _isSearchingVideo.asStateFlow()

    private val _allItems = MutableStateFlow<List<CosmeticItem>>(emptyList())
    private val _ownedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _wishlistIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistIds: StateFlow<Set<String>> = _wishlistIds.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val debouncedSearchQuery = _searchQuery.debounce(300)

    val uiState: StateFlow<CosmeticsUiState> = combine(
        _allItems, 
        debouncedSearchQuery, 
        _selectedCategory, 
        _wishlistIds, 
        _ownedIds, 
        _wishlistOnly, 
        _sortOption, 
        _currentPage, 
        _isLoading, 
        _errorMessage, 
        _setFilter
    ) { args: Array<Any?> ->
        val all = args[0] as List<CosmeticItem>
        val query = args[1] as String
        val cat = args[2] as String
        val wishlist = args[3] as Set<String>
        val owned = args[4] as Set<String>
        val wishlistOnly = args[5] as Boolean
        val sort = args[6] as CosmeticSortOption
        val page = args[7] as Int
        val loading = args[8] as Boolean
        val error = args[9] as String?
        val setFilter = args[10] as String?

        if (error != null) return@combine CosmeticsUiState.Error(error)
        if (loading) return@combine CosmeticsUiState.Loading

        // Perform filtering and sorting on Dispatchers.Default
        val filteredBase = all.filter { item ->
            val matchesCategory = when (cat) {
                "All" -> true
                "Unreleased" -> item.shopHistory.isNullOrEmpty()
                "Outfits" -> item.type?.displayValue?.contains("Outfit", ignoreCase = true) == true || 
                            item.id.startsWith("CID_", ignoreCase = true) || item.id.startsWith("Character_", ignoreCase = true)
                "Backblings" -> item.type?.displayValue?.contains("Back Bling", ignoreCase = true) == true || 
                                item.id.startsWith("BID_", ignoreCase = true) || item.id.startsWith("Backpack_", ignoreCase = true) ||
                                item.id.startsWith("PetID_", ignoreCase = true) || item.id.startsWith("PetCarrier_", ignoreCase = true) ||
                                item.id.contains("AthenaPet", ignoreCase = true)
                "Pickaxes" -> item.type?.displayValue?.contains("Pickaxe", ignoreCase = true) == true || 
                             item.id.startsWith("Pickaxe_", ignoreCase = true)
                "Gliders" -> item.type?.displayValue?.contains("Glider", ignoreCase = true) == true || 
                            item.id.startsWith("Glider_", ignoreCase = true) ||
                            item.id.startsWith("Umbrella_", ignoreCase = true) || item.id.endsWith("_Umbrella", ignoreCase = true) ||
                            item.id.equals("FounderGlider", ignoreCase = true) || item.id.equals("FounderUmbrella", ignoreCase = true)
                "Emojis" -> item.id.contains("Emoji_", ignoreCase = true) || item.id.contains("Emoticon_", ignoreCase = true)
                "Sprays" -> item.id.contains("SPID_", ignoreCase = true) || item.id.contains("Spray_", ignoreCase = true)
                "Emotes" -> item.type?.displayValue?.contains("Emote", ignoreCase = true) == true || 
                           item.id.startsWith("EID_", ignoreCase = true) || item.id.startsWith("Dance_", ignoreCase = true)
                "Wraps" -> item.id.startsWith("Wrap_", ignoreCase = true)
                "Contrails" -> item.id.startsWith("Contrail_", ignoreCase = true) || item.id.startsWith("Trails_ID_", ignoreCase = true)
                "Music Packs" -> item.id.startsWith("MusicPack_", ignoreCase = true)
                "Loading Screens" -> item.id.startsWith("LSID_", ignoreCase = true) || item.id.startsWith("LoadingScreen_", ignoreCase = true)
                "Sidekicks" -> item.id.startsWith("Companion_", ignoreCase = true) && 
                              !item.id.contains("reactfx", ignoreCase = true) && 
                              !item.id.contains("vtid", ignoreCase = true)
                "Jam Tracks" -> item.id.startsWith("sid_", ignoreCase = true)
                "Banners" -> item.id.startsWith("BR", ignoreCase = true) || item.id.startsWith("Banner", ignoreCase = true) || 
                             item.id.startsWith("OtherBanner", ignoreCase = true) || item.id.startsWith("OT", ignoreCase = true) ||
                             item.id.startsWith("InfluencerBanner", ignoreCase = true) || item.id.startsWith("FounderTier", ignoreCase = true) ||
                             item.id.startsWith("StandardBanner", ignoreCase = true) || item.id.startsWith("Achievement", ignoreCase = true) ||
                             item.id.startsWith("SurvivalBanner", ignoreCase = true) || item.id.startsWith("Newsletter", ignoreCase = true) ||
                             item.id.startsWith("Winter", ignoreCase = true) || item.id.startsWith("Wargames", ignoreCase = true) ||
                             item.id.startsWith("Endurance", ignoreCase = true) || item.id.startsWith("Starlight", ignoreCase = true) ||
                             item.id.startsWith("S8", ignoreCase = true) || item.id.startsWith("Mayday", ignoreCase = true)
                "Kicks" -> item.id.startsWith("Shoes_", ignoreCase = true)
                "Car Bodies" -> item.id.startsWith("CarBody_", ignoreCase = true) || item.id.startsWith("ID_Body_", ignoreCase = true) || 
                                 item.id.startsWith("Body_", ignoreCase = true)
                "Car Decals" -> item.id.startsWith("CarSkin_", ignoreCase = true) || item.id.startsWith("ID_Skin_", ignoreCase = true)
                "Car Wheels" -> item.id.startsWith("Wheel_", ignoreCase = true) || item.id.startsWith("ID_Wheel_", ignoreCase = true)
                "Car Trails" -> item.id.startsWith("ID_DriftTrail_", ignoreCase = true)
                "Car Boosts" -> item.id.startsWith("ID_Booster_", ignoreCase = true)
                "Guitars" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Guitar", ignoreCase = true)
                "Basses" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Bass", ignoreCase = true)
                "Drums" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("DrumKit", ignoreCase = true)
                "Keytars" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Keytar", ignoreCase = true)
                "Mics" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Mic", ignoreCase = true)
                "Lego Builds" -> item.id.startsWith("JBSID_", ignoreCase = true)
                "Lego Decors" -> item.id.startsWith("JBPID_", ignoreCase = true)
                "Auras" -> item.id.startsWith("SparksAura_", ignoreCase = true) || 
                          item.id.startsWith("Aura_", ignoreCase = true) ||
                          item.type?.displayValue?.contains("Aura", ignoreCase = true) == true ||
                          item.type?.value?.contains("Aura", ignoreCase = true) == true
                else -> item.type?.displayValue?.contains(cat, ignoreCase = true) == true || 
                        item.type?.value?.contains(cat, ignoreCase = true) == true
            }

            val matchesSet = setFilter == null || item.set?.value == setFilter

            matchesCategory && matchesSet &&
            (query.isBlank() || 
             item.name.contains(query, ignoreCase = true) || 
             item.artist?.contains(query, ignoreCase = true) == true) &&
            (!wishlistOnly || wishlist.contains(item.id))
        }.let { list ->
            when (sort) {
                CosmeticSortOption.NAME_ASC -> list.sortedBy { it.name }
                CosmeticSortOption.ADDED_DESC -> {
                    list.sortedWith(
                        compareByDescending<CosmeticItem> { it.added ?: "" }
                        .thenByDescending { extractNumericFromId(it.id) }
                        .thenByDescending { 
                            SeasonUtils.getGlobalSeasonNumber(
                                it.introduction?.chapter, 
                                it.introduction?.season
                            )
                        }
                        .thenBy { it.name }
                    )
                }
                CosmeticSortOption.RARITY_DESC -> list.sortedWith(
                    compareByDescending<CosmeticItem> { getRarityRank(it.series?.value ?: it.rarity?.value) }
                    .thenBy { it.series?.value ?: it.rarity?.value }
                    .thenBy { it.name }
                )
            }
        }

        val totalPages = (filteredBase.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
        val safePage = page.coerceIn(1, totalPages.coerceAtLeast(1))
        val pagedList = filteredBase.drop((safePage - 1) * ITEMS_PER_PAGE).take(ITEMS_PER_PAGE)

        CosmeticsUiState.Success(
            allItems = all,
            filteredItems = pagedList,
            selectedCategory = cat,
            wishlistIds = wishlist,
            ownedIds = owned,
            wishlistOnly = wishlistOnly,
            sortOption = sort,
            currentPage = safePage,
            totalPages = totalPages
        )
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CosmeticsUiState.Loading)

    init {
        observeAccountChanges()
        loadData()
        observeWishlistCleanup()
    }

    private fun observeAccountChanges() {
        viewModelScope.launch {
            authRepo.authSession
                .map { state ->
                    when (state) {
                        is AuthState.Active -> state.session.accountId
                        is AuthState.TokenRefreshing -> state.session.accountId
                        is AuthState.TokenExpired -> state.session.accountId
                        is AuthState.NetworkError -> state.session.accountId
                        is AuthState.DecryptionError -> state.session.accountId
                        is AuthState.ReauthRequired -> state.session.accountId
                        else -> null
                    }
                }
                .distinctUntilChanged()
                .collectLatest { accountId ->
                    // Force a clean state before loading new account data
                    _ownedIds.value = emptySet()
                    _wishlistIds.value = emptySet()

                    if (accountId != null) {
                        // 1. Force full reload to update "OWNED" status for the specific account
                        loadData()
                        
                        // 2. Load account-specific or universal wishlist
                        val settings = settingsDao.getSettingsDirect()
                        if (settings?.useUniversalWishlist == true) {
                            wishlistDao.getUniversalWishlist().collect { list ->
                                _wishlistIds.value = list.map { it.id }.toSet()
                            }
                        } else {
                            wishlistDao.getAllWishlistedItems(accountId).collect { list ->
                                _wishlistIds.value = list.map { it.id }.toSet()
                            }
                        }
                    } else {
                        // Logged out
                        loadData()
                    }
                }
        }
    }

    private fun observeWishlistCleanup() {
        viewModelScope.launch {
            authRepo.authSession
                .map { state ->
                    when (state) {
                        is AuthState.Active -> state.session.accountId
                        is AuthState.TokenRefreshing -> state.session.accountId
                        is AuthState.TokenExpired -> state.session.accountId
                        is AuthState.NetworkError -> state.session.accountId
                        is AuthState.DecryptionError -> state.session.accountId
                        is AuthState.ReauthRequired -> state.session.accountId
                        else -> null
                    }
                }
                .distinctUntilChanged()
                .collectLatest { accountId ->
                    if (accountId == null) return@collectLatest

                    combine(wishlistIds, _ownedIds) { wishlist, owned ->
                        // Case-insensitive intersection check
                        val ownedLower = owned.map { it.lowercase() }.toSet()
                        wishlist.filter { it.lowercase() in ownedLower }
                    }.collect { ownedWishlisted ->
                        if (ownedWishlisted.isNotEmpty()) {
                            ownedWishlisted.forEach { id ->
                                wishlistDao.removeFromWishlist(id, accountId)
                            }
                        }
                    }
                }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _ownedIds.value = emptySet() // Clear old owned items
            
            // Clear cache once to ensure we get new categories
            repository.clearCache()
            
            // Fetch all cosmetics
            val cosmeticsResult = repository.fetchAllCosmetics()
            cosmeticsResult.onSuccess { _allItems.value = it }
            cosmeticsResult.onFailure { _errorMessage.value = it.localizedMessage }

            // Fetch owned items if logged in
            val sessionResult = authRepo.ensureActiveSession()
            sessionResult.onSuccess { session ->
                val lockerResult = epicAccountRepo.fetchPersonalLockerCosmetics(session.accessToken, session.accountId)
                lockerResult.onSuccess { items ->
                    _ownedIds.value = items.map { it.cosmeticId.lowercase() }.toSet()
                }
            }

            _isLoading.value = false
        }
    }


    fun updateSearch(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _currentPage.value = 1
    }

    fun toggleWishlistOnly() {
        _wishlistOnly.value = !_wishlistOnly.value
        _currentPage.value = 1
    }

    fun setSortOption(option: CosmeticSortOption) {
        _sortOption.value = option
        _currentPage.value = 1
    }

    fun setPage(page: Int) {
        _currentPage.value = page
    }

    fun setSetFilter(setValue: String?) {
        _setFilter.value = setValue
        _currentPage.value = 1
    }

    fun getItemsInSet(setTag: String): List<CosmeticItem> {
        return _allItems.value.filter { it.set?.value == setTag }
    }

    private fun extractNumericFromId(id: String): Int {
        // Find any sequence of digits after a prefix or underscore (e.g., CID_001, Trails_ID_123)
        val match = Regex("""(?i)[A-Z_]+_(\d+)""").find(id)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    fun searchYouTubeForItem(item: CosmeticItem) {
        viewModelScope.launch {
            _selectedVideoId.value = null
            _isSearchingVideo.value = true
            val artist = item.artist ?: ""
            val isJamTrack = item.id.startsWith("sid_", ignoreCase = true)
            val isMusicPack = item.id.startsWith("MusicPack_", ignoreCase = true) || 
                             item.type?.displayValue?.contains("Music Pack", ignoreCase = true) == true
            
            val baseQuery = when {
                isJamTrack && artist.contains("Epic Games", ignoreCase = true) -> 
                    "Fortnite ${item.name} Jam Track -emote"
                isJamTrack -> 
                    "$artist ${item.name} official audio"
                isMusicPack -> 
                    "Fortnite ${item.name} Music Pack"
                else -> "$artist ${item.name} official audio"
            }
            
            val videoId = ytRepo.searchVideoId(baseQuery)
            _selectedVideoId.value = videoId
            _isSearchingVideo.value = false
        }
    }

    fun toggleWishlist(item: CosmeticItem) {
        val itemId = item.id.lowercase()
        if (_ownedIds.value.contains(itemId)) return

        viewModelScope.launch {
            val session = authRepo.getValidSession() ?: return@launch
            val settings = settingsDao.getSettingsDirect()
            val useUniversal = settings?.useUniversalWishlist == true
            
            val targetAccountId = if (useUniversal) "GLOBAL" else session.accountId
            
            val wishlistSet = _wishlistIds.value.map { it.lowercase() }.toSet()
            if (wishlistSet.contains(itemId)) {
                val originalId = _wishlistIds.value.find { it.lowercase() == itemId } ?: item.id
                wishlistDao.removeFromWishlist(originalId, targetAccountId)
            } else {
                wishlistDao.addToWishlist(
                    WishlistEntity(
                        id = item.id,
                        accountId = targetAccountId,
                        name = item.name,
                        type = item.type?.displayValue ?: "Other",
                        rarity = item.rarity?.value ?: "Rare",
                        iconUrl = item.images?.icon ?: item.images?.smallIcon
                    )
                )
            }
        }
    }

}
