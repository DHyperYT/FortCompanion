package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.models.ShopData
import com.dhyper.fncompanion.data.models.ShopEntry
import com.dhyper.fncompanion.data.db.WishlistDao
import com.dhyper.fncompanion.data.db.WishlistEntity
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.repository.EpicAccountRepository
import com.dhyper.fncompanion.data.repository.FortniteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

sealed class ShopUiState {
    object Loading : ShopUiState()
    data class Success(
        val shopData: ShopData,
        val filteredEntries: List<ShopEntry>,
        val selectedCategory: String = "All",
        val ownedIds: Set<String> = emptySet(),
        val wishlistIds: Set<String> = emptySet(),
        val individualPrices: Map<String, Int> = emptyMap(),
        val skinSetPrices: Map<String, Pair<Int, Set<String>>> = emptyMap()
    ) : ShopUiState()
    data class Error(val message: String) : ShopUiState()
}

class ShopViewModel(
    private val repository: FortniteRepository = FortniteRepository(),
    private val epicAccountRepo: EpicAccountRepository = EpicAccountRepository(),
    private val authRepo: AuthRepository,
    private val wishlistDao: WishlistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopUiState>(ShopUiState.Loading)
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")

    private val _countdown = MutableStateFlow("")
    val countdown: StateFlow<String> = _countdown.asStateFlow()

    private val _ownedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _wishlistIds = wishlistDao.getAllWishlistedItems()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var currentShopData: ShopData? = null

    init {
        startCountdown()
        observeWishlistCleanup()
        
        // Main UI State Pipeline
        combine(_searchQuery, _selectedCategory, _wishlistIds, _ownedIds) { query, cat, wishlist, owned ->
            val data = currentShopData ?: return@combine null
            val allEntries = data.entries ?: emptyList()
            val sorted = sortEntries(allEntries)
            
            val indPrices = mutableMapOf<String, Int>()
            val setPrices = mutableMapOf<String, Pair<Int, Set<String>>>()

            allEntries.forEach { entry ->
                val items = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList())
                val p = entry.finalPrice ?: entry.regularPrice ?: 0
                val t = getShopEntryTitleInternal(entry)
                val sk = items.find { it.type?.value?.equals("outfit", ignoreCase = true) == true }
                val isB = (entry.bundle != null || t.contains("Bundle", ignoreCase = true)) && (sk == null || !t.trim().equals(sk.name.trim(), ignoreCase = true))
                if (!isB && items.isNotEmpty()) {
                    if (items.size == 1) indPrices[items[0].id.lowercase()] = p
                    else if (sk != null) setPrices[sk.id.lowercase()] = Pair(p, items.map { it.id.lowercase() }.toSet())
                }
            }

            ShopUiState.Success(
                shopData = data,
                filteredEntries = filterEntries(sorted, cat, query, wishlist),
                selectedCategory = cat,
                ownedIds = owned,
                wishlistIds = wishlist,
                individualPrices = indPrices,
                skinSetPrices = setPrices
            )
        }.filterNotNull()
         .onEach { _uiState.value = it }
         .launchIn(viewModelScope)

        loadShop()
    }

    private fun observeWishlistCleanup() {
        viewModelScope.launch {
            combine(_wishlistIds, _ownedIds) { wishlist, owned ->
                // Case-insensitive intersection check
                val ownedLower = owned.map { it.lowercase() }.toSet()
                wishlist.filter { it.lowercase() in ownedLower }
            }.collect { ownedWishlisted ->
                if (ownedWishlisted.isNotEmpty()) {
                    ownedWishlisted.forEach { id ->
                        wishlistDao.removeFromWishlist(id)
                    }
                }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (true) {
                _countdown.value = calculateTimeUntilReset()
                delay(1000)
            }
        }
    }

    private fun calculateTimeUntilReset(): String {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var nextReset = calendar.timeInMillis
        if (nextReset <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            nextReset = calendar.timeInMillis
        }
        
        val diff = nextReset - now
        val hours = (diff / (1000 * 60 * 60))
        val minutes = (diff / (1000 * 60)) % 60
        val seconds = (diff / 1000) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun loadShop() {
        viewModelScope.launch {
            _uiState.value = ShopUiState.Loading
            
            // 1. Fetch owned items in parallel
            launch {
                authRepo.authSession.firstOrNull()?.let { session ->
                    val lockerResult = epicAccountRepo.fetchPersonalLockerCosmetics(session.accessToken, session.accountId)
                    lockerResult.onSuccess { items ->
                        _ownedIds.value = items.map { it.cosmeticId.lowercase() }.toSet()
                    }
                }
            }

            // 2. Fetch the shop
            val result = repository.fetchItemShop()
            result.fold(
                onSuccess = { data ->
                    currentShopData = data
                    // Forcing the flow to emit by re-setting search query (simple trigger)
                    _searchQuery.value = _searchQuery.value
                },
                onFailure = { error ->
                    _uiState.value = ShopUiState.Error(error.localizedMessage ?: "Failed to load Item Shop")
                }
            )
        }
    }

    private fun getShopEntryTitleInternal(entry: ShopEntry): String {
        if (!entry.bundle?.name.isNullOrBlank()) return entry.bundle?.name!!
        val allItems = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList())
        val firstItemName = allItems.firstOrNull { !it.name.isNullOrBlank() }?.name
        return firstItemName ?: entry.devName ?: "Cosmetic"
    }

    private fun sortEntries(entries: List<ShopEntry>): List<ShopEntry> {
        return entries.sortedWith(
            compareBy<ShopEntry> { it.section?.index ?: it.layout?.index ?: Int.MAX_VALUE }
                .thenByDescending { it.section?.landingPriority ?: 0 }
                .thenBy { it.section?.name ?: it.layout?.name ?: "Special Offers" }
                .thenBy { it.devName ?: "" }
        )
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleWishlist(item: com.dhyper.fncompanion.data.models.CosmeticItem) {
        if (_ownedIds.value.contains(item.id.lowercase())) return

        viewModelScope.launch {
            if (_wishlistIds.value.contains(item.id)) {
                wishlistDao.removeFromWishlist(item.id)
            } else {
                wishlistDao.addToWishlist(
                    WishlistEntity(
                        id = item.id,
                        name = item.name,
                        type = item.type?.displayValue ?: "Other",
                        rarity = item.rarity?.value ?: "Rare",
                        iconUrl = item.images?.icon ?: item.images?.smallIcon
                    )
                )
            }
        }
    }

    private fun filterEntries(entries: List<ShopEntry>, category: String, query: String, wishlist: Set<String>): List<ShopEntry> {
        return entries.filter { entry ->
            val allItems = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList()) + (entry.cars ?: emptyList()) + (entry.vehicles ?: emptyList()) + (entry.instruments ?: emptyList())
            val title = getShopEntryTitleInternal(entry)
            val skin = allItems.find { it.type?.value?.equals("outfit", ignoreCase = true) == true }
            val isBundle = (entry.bundle != null || title.contains("Bundle", ignoreCase = true)) && 
                           (skin == null || !title.trim().equals(skin.name.trim(), ignoreCase = true))
            
            val matchesCategory = when (category) {
                "All" -> true
                "Bundles" -> isBundle
                "Outfit" -> allItems.any { it.type?.displayValue?.contains("Outfit", ignoreCase = true) == true || it.id.startsWith("CID_", ignoreCase = true) || it.id.startsWith("Character_", ignoreCase = true) }
                "Back Bling" -> allItems.any { it.type?.displayValue?.contains("Back Bling", ignoreCase = true) == true || it.id.startsWith("BID_", ignoreCase = true) || it.id.startsWith("Backpack_", ignoreCase = true) || it.id.startsWith("PetID_", ignoreCase = true) || it.id.startsWith("PetCarrier_", ignoreCase = true) || it.id.contains("AthenaPet", ignoreCase = true) }
                "Pickaxe" -> allItems.any { it.type?.displayValue?.contains("Pickaxe", ignoreCase = true) == true || it.id.startsWith("Pickaxe_", ignoreCase = true) }
                "Glider" -> allItems.any { it.type?.displayValue?.contains("Glider", ignoreCase = true) == true || it.id.startsWith("Glider_", ignoreCase = true) }
                "Emote" -> allItems.any { it.type?.displayValue?.contains("Emote", ignoreCase = true) == true || it.id.startsWith("EID_", ignoreCase = true) || it.id.startsWith("Dance_", ignoreCase = true) }
                "Wrap" -> allItems.any { it.id.startsWith("Wrap_", ignoreCase = true) }
                "Contrail" -> allItems.any { it.id.startsWith("Contrail_", ignoreCase = true) }
                "Music" -> allItems.any { it.id.startsWith("MusicPack_", ignoreCase = true) }
                "Loading Screen" -> allItems.any { it.id.startsWith("LSID_", ignoreCase = true) || it.id.startsWith("LoadingScreen_", ignoreCase = true) }
                "Emoticon" -> allItems.any { it.id.contains("Emoji_", ignoreCase = true) || it.id.contains("Emoticon_", ignoreCase = true) }
                "Spray" -> allItems.any { it.id.contains("SPID_", ignoreCase = true) || it.id.contains("Spray_", ignoreCase = true) }
                "Sidekick" -> allItems.any { it.id.startsWith("Companion_", ignoreCase = true) && !it.id.contains("reactfx", ignoreCase = true) && !it.id.contains("vtid", ignoreCase = true) }
                "Jam Track" -> !entry.tracks.isNullOrEmpty() || allItems.any { it.id.startsWith("sid_", ignoreCase = true) }
                "Banner" -> allItems.any { it.id.startsWith("BRS", ignoreCase = false) || it.id.startsWith("Banner_", ignoreCase = true) }
                "Kicks" -> allItems.any { it.id.startsWith("Shoes_", ignoreCase = true) }
                "Car" -> allItems.any { it.id.startsWith("CarBody_", ignoreCase = true) || it.id.startsWith("ID_Body_", ignoreCase = true) }
                "Car Decal" -> allItems.any { it.id.startsWith("CarSkin_", ignoreCase = true) || it.id.startsWith("ID_Skin_", ignoreCase = true) }
                "Wheels" -> allItems.any { it.id.startsWith("Wheel_", ignoreCase = true) || it.id.startsWith("ID_Wheel_", ignoreCase = true) }
                "Car Trail" -> allItems.any { it.id.startsWith("ID_DriftTrail_", ignoreCase = true) }
                "Car Boost" -> allItems.any { it.id.startsWith("ID_Booster_", ignoreCase = true) }
                "Guitar" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Guitar", ignoreCase = true) }
                "Bass" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Bass", ignoreCase = true) }
                "Drums" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("DrumKit", ignoreCase = true) }
                "Keytar" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Keytar", ignoreCase = true) }
                "Mic" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Mic", ignoreCase = true) }
                "Lego Build" -> allItems.any { it.id.startsWith("JBSID_", ignoreCase = true) }
                "Lego Decor" -> allItems.any { it.id.startsWith("JBPID_", ignoreCase = true) }
                "Aura" -> allItems.any { it.id.startsWith("SparksAura_", ignoreCase = true) || it.id.startsWith("Aura_", ignoreCase = true) }
                else -> true
            }

            val matchesQuery = if (query.isBlank()) true else {
                val devMatch = entry.devName?.contains(query, ignoreCase = true) == true
                val bundleMatch = entry.bundle?.name?.contains(query, ignoreCase = true) == true
                val itemMatch = allItems.any { it.name.contains(query, ignoreCase = true) }
                val trackMatch = entry.tracks?.any { (it.title?.contains(query, ignoreCase = true) == true) || (it.artist?.contains(query, ignoreCase = true) == true) } == true
                devMatch || bundleMatch || itemMatch || trackMatch
            }

            matchesCategory && matchesQuery
        }
    }
}
