package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.WishlistDao
import com.dhyper.fncompanion.data.db.WishlistEntity
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.data.repository.*
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class CosmeticSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    ADDED_DESC("Date Added (New)"),
    RARITY_DESC("Rarity (High-Low)"),
    TYPE("Type")
}

sealed class CosmeticsUiState {
    object Loading : CosmeticsUiState()
    data class Success(
        val allItems: List<CosmeticItem>,
        val filteredItems: List<CosmeticItem>,
        val searchQuery: String = "",
        val selectedCategory: String = "All",
        val wishlistIds: Set<String> = emptySet(),
        val ownedIds: Set<String> = emptySet(),
        val wishlistOnly: Boolean = false,
        val sortOption: CosmeticSortOption = CosmeticSortOption.NAME_ASC,
        val currentPage: Int = 1,
        val totalPages: Int = 1
    ) : CosmeticsUiState()
    data class Error(val message: String) : CosmeticsUiState()
}

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
    private val _sortOption = MutableStateFlow(CosmeticSortOption.NAME_ASC)
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

    val uiState: StateFlow<CosmeticsUiState> = combine(
        _allItems, _searchQuery, _selectedCategory, _wishlistIds, _ownedIds, _wishlistOnly, _sortOption, _currentPage, _isLoading, _errorMessage, _setFilter
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

        val filteredBase = all.filter { item ->
            val matchesCategory = when (cat) {
                "All" -> true
                "Outfit" -> item.type?.displayValue?.contains("Outfit", ignoreCase = true) == true || 
                            item.id.startsWith("CID_", ignoreCase = true) || item.id.startsWith("Character_", ignoreCase = true)
                "Back Bling" -> item.type?.displayValue?.contains("Back Bling", ignoreCase = true) == true || 
                                item.id.startsWith("BID_", ignoreCase = true) || item.id.startsWith("Backpack_", ignoreCase = true) ||
                                item.id.startsWith("PetID_", ignoreCase = true) || item.id.startsWith("PetCarrier_", ignoreCase = true) ||
                                item.id.contains("AthenaPet", ignoreCase = true)
                "Pickaxe" -> item.type?.displayValue?.contains("Pickaxe", ignoreCase = true) == true || 
                             item.id.startsWith("Pickaxe_", ignoreCase = true)
                "Glider" -> item.type?.displayValue?.contains("Glider", ignoreCase = true) == true || 
                            item.id.startsWith("Glider_", ignoreCase = true)
                "Emote" -> item.type?.displayValue?.contains("Emote", ignoreCase = true) == true || 
                           item.id.startsWith("EID_", ignoreCase = true) || item.id.startsWith("Dance_", ignoreCase = true)
                "Emoticon" -> item.id.contains("Emoji_", ignoreCase = true) || item.id.contains("Emoticon_", ignoreCase = true)
                "Spray" -> item.id.contains("SPID_", ignoreCase = true) || item.id.contains("Spray_", ignoreCase = true)
                "Wrap" -> item.id.startsWith("Wrap_", ignoreCase = true)
                "Contrail" -> item.id.startsWith("Contrail_", ignoreCase = true)
                "Music" -> item.id.startsWith("MusicPack_", ignoreCase = true)
                "Loading Screen" -> item.id.startsWith("LSID_", ignoreCase = true) || item.id.startsWith("LoadingScreen_", ignoreCase = true)
                "Sidekick" -> item.id.startsWith("Companion_", ignoreCase = true) && 
                              !item.id.contains("reactfx", ignoreCase = true) && 
                              !item.id.contains("vtid", ignoreCase = true)
                "Jam Track" -> item.id.startsWith("sid_", ignoreCase = true)
                "Banner" -> item.id.startsWith("BRS", ignoreCase = false) || item.id.startsWith("Banner_", ignoreCase = true)
                "Kicks" -> item.id.startsWith("Shoes_", ignoreCase = true)
                "Car" -> item.id.startsWith("CarBody_", ignoreCase = true) || item.id.startsWith("ID_Body_", ignoreCase = true)
                "Car Decal" -> item.id.startsWith("CarSkin_", ignoreCase = true) || item.id.startsWith("ID_Skin_", ignoreCase = true)
                "Wheels" -> item.id.startsWith("Wheel_", ignoreCase = true) || item.id.startsWith("ID_Wheel_", ignoreCase = true)
                "Car Trail" -> item.id.startsWith("ID_DriftTrail_", ignoreCase = true)
                "Car Boost" -> item.id.startsWith("ID_Booster_", ignoreCase = true)
                "Guitar" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Guitar", ignoreCase = true)
                "Bass" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Bass", ignoreCase = true)
                "Drums" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("DrumKit", ignoreCase = true)
                "Keytar" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Keytar", ignoreCase = true)
                "Mic" -> item.id.startsWith("Sparks_", ignoreCase = true) && item.id.contains("Mic", ignoreCase = true)
                "Lego Build" -> item.id.startsWith("JBSID_", ignoreCase = true)
                "Lego Decor" -> item.id.startsWith("JBPID_", ignoreCase = true)
                "Aura" -> item.id.startsWith("SparksAura_", ignoreCase = true) || 
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
                    // Optimized sort: Date first, then ID number or Global Season as a reliable fallback for Chapter 1 items
                    list.sortedWith { a, b ->
                        val dateA = a.added ?: ""
                        val dateB = b.added ?: ""
                        
                        // Treat "2019-11-20" as the cutoff point where date sorting becomes unreliable
                        val isFallbackDate = dateA.startsWith("2019-11-20") && dateB.startsWith("2019-11-20")

                        if (!isFallbackDate) {
                            val dateCmp = dateB.compareTo(dateA)
                            if (dateCmp != 0) return@sortedWith dateCmp
                        }

                        // Try to extract numeric part from CID_, EID_, etc.
                        val numA = extractNumericFromId(a.id)
                        val numB = extractNumericFromId(b.id)
                        
                        if (numA > 0 && numB > 0) {
                            val numCmp = numB.compareTo(numA)
                            if (numCmp != 0) return@sortedWith numCmp
                        }

                        val globalA = SeasonUtils.getGlobalSeasonNumber(
                            a.introduction?.chapter?.toIntOrNull() ?: 1,
                            a.introduction?.season?.toIntOrNull() ?: 1
                        )
                        val globalB = SeasonUtils.getGlobalSeasonNumber(
                            b.introduction?.chapter?.toIntOrNull() ?: 1,
                            b.introduction?.season?.toIntOrNull() ?: 1
                        )
                        val seasonCmp = globalB.compareTo(globalA)
                        if (seasonCmp != 0) return@sortedWith seasonCmp

                        a.name.compareTo(b.name)
                    }
                }
                CosmeticSortOption.RARITY_DESC -> list.sortedByDescending { getRarityRank(it.rarity?.value ?: "") }
                CosmeticSortOption.TYPE -> list.sortedBy { it.type?.displayValue ?: "" }
            }
        }

        val totalPages = (filteredBase.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
        val safePage = page.coerceIn(1, totalPages.coerceAtLeast(1))
        
        val pagedList = filteredBase
            .drop((safePage - 1) * ITEMS_PER_PAGE)
            .take(ITEMS_PER_PAGE)

        CosmeticsUiState.Success(
            allItems = all,
            filteredItems = pagedList,
            searchQuery = query,
            selectedCategory = cat,
            wishlistIds = wishlist,
            ownedIds = owned,
            wishlistOnly = wishlistOnly,
            sortOption = sort,
            currentPage = safePage,
            totalPages = totalPages
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CosmeticsUiState.Loading)

    init {
        observeAccountChanges()
        loadData()
        observeWishlistCleanup()
    }

    private fun observeAccountChanges() {
        viewModelScope.launch {
            authRepo.authSession.collectLatest { session ->
                // Force a clean state before loading new account data
                _ownedIds.value = emptySet()
                _wishlistIds.value = emptySet()
                
                if (session != null) {
                    // 1. Force full reload to update "OWNED" status for the specific account
                    loadData()
                    
                    // 2. Load account-specific or universal wishlist
                    val settings = settingsDao.getSettingsDirect()
                    if (settings?.useUniversalWishlist == true) {
                        wishlistDao.getUniversalWishlist().collect { list ->
                            _wishlistIds.value = list.map { it.id }.toSet()
                        }
                    } else {
                        wishlistDao.getAllWishlistedItems(session.accountId).collect { list ->
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
            authRepo.authSession.collectLatest { session ->
                if (session == null) return@collectLatest

                combine(wishlistIds, _ownedIds) { wishlist, owned ->
                    // Case-insensitive intersection check
                    val ownedLower = owned.map { it.lowercase() }.toSet()
                    wishlist.filter { it.lowercase() in ownedLower }
                }.collect { ownedWishlisted ->
                    if (ownedWishlisted.isNotEmpty()) {
                        ownedWishlisted.forEach { id ->
                            wishlistDao.removeFromWishlist(id, session.accountId)
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
            authRepo.authSession.firstOrNull()?.let { session ->
                val lockerResult = epicAccountRepo.fetchPersonalLockerCosmetics(session.accessToken, session.accountId)
                lockerResult.onSuccess { items ->
                    _ownedIds.value = items.map { it.cosmeticId.lowercase() }.toSet()
                }
            }

            _isLoading.value = false
        }
    }

    private fun getRarityRank(rarity: String): Int {
        return when (rarity.lowercase()) {
            "mythic" -> 6
            "legendary" -> 5
            "epic" -> 4
            "rare" -> 3
            "uncommon" -> 2
            "common" -> 1
            else -> 0
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
        // Handle CID_123, EID_123, BID_123, etc.
        val match = Regex("(?i)[A-Z]+_([0-9]+)").find(id)
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

    fun isItemInShop(itemId: String, shopState: ShopUiState?): Boolean {
        if (shopState !is ShopUiState.Success) return false
        val searchId = itemId.lowercase()
        return shopState.shopData.entries?.any { entry ->
            val allIds = ((entry.items ?: emptyList()) + (entry.brItems ?: emptyList())).map { it.id.lowercase() }
            val trackIds = entry.tracks?.map { t ->
                val trackMap = t.track as? Map<*, *>
                trackMap?.get("id")?.toString()?.lowercase() ?: t.id?.lowercase() ?: ""
            } ?: emptyList()
            
            allIds.contains(searchId) || trackIds.contains(searchId)
        } ?: false
    }
}
