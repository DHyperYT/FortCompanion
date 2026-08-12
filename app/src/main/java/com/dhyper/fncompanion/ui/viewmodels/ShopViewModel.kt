package com.dhyper.fncompanion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.data.models.ShopData
import com.dhyper.fncompanion.data.models.ShopEntry
import com.dhyper.fncompanion.data.db.SettingsDao
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
        val skinSetPrices: Map<String, Pair<Int, Set<String>>> = emptyMap(),
        val shopItemIds: Set<String> = emptySet(),
        val shownBanners: Set<String> = emptySet(),
        val isJamTracksExpanded: Boolean = false
    ) : ShopUiState()
    data class Error(val message: String) : ShopUiState()
}

class ShopViewModel(
    private val repository: FortniteRepository = FortniteRepository(),
    private val epicAccountRepo: EpicAccountRepository = EpicAccountRepository(),
    private val authRepo: AuthRepository,
    private val wishlistDao: WishlistDao,
    private val settingsDao: com.dhyper.fncompanion.data.db.SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopUiState>(ShopUiState.Loading)
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")

    private val _countdown = MutableStateFlow("")
    val countdown: StateFlow<String> = _countdown.asStateFlow()

    private val _ownedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _wishlistIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistIds: StateFlow<Set<String>> = _wishlistIds.asStateFlow()

    private val _allCosmetics = MutableStateFlow<List<com.dhyper.fncompanion.data.models.CosmeticItem>>(emptyList())

    private val _isJamTracksExpanded = MutableStateFlow(false)

    private val _shownBanners = MutableStateFlow<Set<String>>(emptySet())
    val shownBanners: StateFlow<Set<String>> = _shownBanners.asStateFlow()

    private var currentShopData: ShopData? = null

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery.debounce(300)

    init {
        startCountdown()
        observeWishlistCleanup()
        observeAccountChanges()
        
        // Main UI State Pipeline
        combine(debouncedSearchQuery, _selectedCategory, _wishlistIds, _ownedIds, _isJamTracksExpanded, _allCosmetics, _shownBanners) { args: Array<Any?> ->
            val query = args[0] as String
            val cat = args[1] as String
            val wishlist = args[2] as Set<String>
            val owned = args[3] as Set<String>
            val expanded = args[4] as Boolean
            val allCos = args[5] as List<com.dhyper.fncompanion.data.models.CosmeticItem>
            val shown = args[6] as Set<String>

            val data = currentShopData ?: return@combine null
            val allEntries = data.entries ?: emptyList()
            
            val indPrices = mutableMapOf<String, Int>()
            val setPrices = mutableMapOf<String, Pair<Int, Set<String>>>()
            val allShopIds = mutableSetOf<String>()

            allEntries.forEach { entry ->
                val items = getItemsForEntryInternal(entry, allCos)
                
                items.forEach { allShopIds.add(it.id.lowercase()) }

                val p = entry.finalPrice ?: entry.regularPrice ?: 0
                val t = getShopEntryTitleInternal(entry, allCos)
                val sk = items.find { it.type?.value?.equals("outfit", ignoreCase = true) == true }
                val isB = (entry.bundle != null || t.contains("Bundle", ignoreCase = true)) && (sk == null || !t.trim().equals(sk.name.trim(), ignoreCase = true))
                if (!isB && items.isNotEmpty()) {
                    if (items.size == 1) indPrices[items[0].id.lowercase()] = p
                    else if (sk != null) setPrices[sk.id.lowercase()] = Pair(p, items.map { it.id.lowercase() }.toSet())
                }
            }

            val sorted = sortEntries(allEntries)

            ShopUiState.Success(
                shopData = data,
                filteredEntries = filterEntries(sorted, cat, query, wishlist, allCos),
                selectedCategory = cat,
                ownedIds = owned,
                wishlistIds = wishlist,
                individualPrices = indPrices,
                skinSetPrices = setPrices,
                shopItemIds = allShopIds,
                shownBanners = shown,
                isJamTracksExpanded = expanded
            )
        }.filterNotNull()
         .flowOn(kotlinx.coroutines.Dispatchers.Default)
         .onEach { _uiState.value = it }
         .launchIn(viewModelScope)

        loadShop()
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
                    // Force a full reload to update "OWNED" status
                    loadShop()

                    if (accountId != null) {
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
                        _ownedIds.value = emptySet()
                        _wishlistIds.value = emptySet()
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
                    
                    combine(_wishlistIds, _ownedIds) { wishlist, owned ->
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
            
            // 0. Fetch all cosmetics for derivation logic
            launch {
                repository.fetchAllCosmetics().onSuccess { _allCosmetics.value = it }
            }
            
            // 1. Fetch owned items in parallel
            launch {
                val sessionResult = authRepo.ensureActiveSession()
                sessionResult.onSuccess { session ->
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

    private fun getShopEntryTitleInternal(entry: ShopEntry, allCosmetics: List<com.dhyper.fncompanion.data.models.CosmeticItem> = emptyList()): String {
        if (!entry.bundle?.name.isNullOrBlank()) return entry.bundle?.name!!
        
        val allItems = getItemsForEntryInternal(entry, allCosmetics)
        val firstItemName = allItems.firstOrNull { !it.name.isNullOrBlank() }?.name
        return firstItemName ?: entry.devName ?: "Cosmetic"
    }

    private fun getItemsForEntryInternal(entry: ShopEntry, allCosmetics: List<com.dhyper.fncompanion.data.models.CosmeticItem> = emptyList()): List<com.dhyper.fncompanion.data.models.CosmeticItem> {
        val itemMap = mutableMapOf<String, com.dhyper.fncompanion.data.models.CosmeticItem>()
        val orderedIds = mutableListOf<String>()

        fun processList(list: List<com.dhyper.fncompanion.data.models.CosmeticItem>?) {
            list?.forEach { item ->
                if (item.id.isBlank()) return@forEach
                if (!itemMap.containsKey(item.id)) {
                    orderedIds.add(item.id)
                }
                val existing = itemMap[item.id]
                // Prefer the object with images/metadata
                if (existing == null || (existing.images == null && item.images != null)) {
                    itemMap[item.id] = item
                }
            }
        }

        processList(entry.vehicles)
        processList(entry.cars)
        processList(entry.brItems)
        processList(entry.instruments)
        processList(entry.items)

        // Check for referenced cosmetic ID in NewDisplayAsset
        entry.newDisplayAsset?.cosmeticId?.let { cid ->
            if (!itemMap.containsKey(cid)) {
                val placeholder = com.dhyper.fncompanion.data.models.CosmeticItem(
                    id = cid, name = entry.devName ?: "Item", description = null, type = null, rarity = null, series = null, images = null, variants = null, introduction = null, set = null, added = null
                )
                itemMap[cid] = placeholder
                orderedIds.add(cid)
            }
        }

        // Fix for missing Car Bodies in bundles
        val hasCarBody = orderedIds.any { id -> 
            id.startsWith("Body_", true) || id.startsWith("ID_Body_", true) || id.startsWith("CarBody_", true) 
        }
        val bundleName = entry.bundle?.name ?: entry.layout?.name ?: ""
        val isVehicleBundle = entry.cars?.isNotEmpty() == true || entry.vehicles?.isNotEmpty() == true || 
                             entry.devName?.contains("Car", true) == true || bundleName.contains("Bundle", true) == true
        
        if (!hasCarBody && isVehicleBundle && allCosmetics.isNotEmpty()) {
            val carSearchName = bundleName.replace(" Bundle", "", ignoreCase = true).trim()
            if (carSearchName.isNotBlank()) {
                val matchingCar = allCosmetics.find { 
                    it.name.equals(carSearchName, ignoreCase = true) && 
                    (it.id.startsWith("Body_", true) || it.id.startsWith("ID_Body_", true) || it.id.startsWith("CarBody_", true))
                }
                if (matchingCar != null && !itemMap.containsKey(matchingCar.id)) {
                    itemMap[matchingCar.id] = matchingCar
                    orderedIds.add(0, matchingCar.id) // Add to left as it's the main item
                }
            }
        }

        entry.tracks?.forEach { t ->
            val trackMap = t.track as? Map<*, *>
            val apiCosmeticId = trackMap?.get("id")?.toString()
            val sidFromDevName = Regex("""sid_[a-zA-Z0-9_]+""").find(t.devName ?: "")?.value
            val idField = t.id ?: ""
            val realId = when {
                !apiCosmeticId.isNullOrBlank() -> apiCosmeticId
                !sidFromDevName.isNullOrBlank() -> sidFromDevName
                !idField.startsWith("v2:/") -> idField
                else -> idField
            }
            if (!itemMap.containsKey(realId)) {
                orderedIds.add(realId)
            }
            val albumArt = t.albumArt
            val title = trackMap?.get("title")?.toString() ?: t.title ?: t.devName ?: "Track"
            val artist = trackMap?.get("artist")?.toString() ?: t.artist ?: "Unknown Artist"

            itemMap[realId] = com.dhyper.fncompanion.data.models.CosmeticItem(
                id = realId, name = title, description = "Jam Track by $artist",
                type = com.dhyper.fncompanion.data.models.CosmeticType("Track", "Jam Track"),
                rarity = com.dhyper.fncompanion.data.models.CosmeticRarity("Festival", "Festival"),
                series = null, images = com.dhyper.fncompanion.data.models.CosmeticImages(albumArt, albumArt, albumArt, null, null, albumArt),
                variants = null, introduction = t.introduction, set = t.set, added = t.added,
                artist = artist
            )
        }

        return orderedIds.mapNotNull { itemMap[it] }
    }

    private fun sortEntries(entries: List<ShopEntry>): List<ShopEntry> {
        val sectionOrder = mutableListOf<String>()
        val sectionsMap = mutableMapOf<String, MutableList<ShopEntry>>()

        fun getSectionId(entry: ShopEntry): String {
            // Respect layout.id primarily as the logical section identity
            return entry.layout?.id ?: entry.section?.id ?: entry.section?.name ?: "default"
        }

        entries.forEach { entry ->
            val sid = getSectionId(entry)
            if (!sectionsMap.containsKey(sid)) {
                sectionOrder.add(sid)
                sectionsMap[sid] = mutableListOf()
            }
            sectionsMap[sid]!!.add(entry)
        }

        fun isJamTrackOffer(entry: ShopEntry): Boolean {
            return !entry.tracks.isNullOrEmpty() || entry.layout?.id == "JT080726" || entry.section?.name?.contains("Jam Tracks", true) == true
        }

        fun isVehicleOnlyOffer(entry: ShopEntry): Boolean {
            val allItems = getItemsForEntryInternal(entry)
            val hasVehicleItems = allItems.isNotEmpty() && allItems.all { 
                it.id.startsWith("CarBody_", true) || it.id.startsWith("ID_Body_", true) || it.id.startsWith("Body_", true) ||
                it.id.startsWith("CarSkin_", true) || it.id.startsWith("ID_Skin_", true) ||
                it.id.startsWith("Wheel_", true) || it.id.startsWith("ID_Wheel_", true) ||
                it.id.startsWith("ID_DriftTrail_", true) || it.id.startsWith("ID_Booster_", true)
            }
            // Vehicle only means has cars or pure vehicle IDs, and no BR items, tracks, or instruments
            return (hasVehicleItems || !entry.cars.isNullOrEmpty() || !entry.vehicles.isNullOrEmpty()) && 
                   entry.brItems.isNullOrEmpty() && 
                   entry.tracks.isNullOrEmpty() && 
                   entry.instruments.isNullOrEmpty()
        }

        fun isSpecialOffer(entry: ShopEntry): Boolean {
            return entry.section?.name?.contains("Special Offers", true) == true || 
                   entry.layout?.name?.contains("Special Offers", true) == true
        }

        val normalSectionOrder = mutableListOf<String>()
        val vehicleOnlyEntries = mutableListOf<ShopEntry>()
        val specialOfferEntries = mutableListOf<ShopEntry>()
        val jamTrackEntries = mutableListOf<ShopEntry>()

        sectionOrder.forEach { sid ->
            val group = sectionsMap[sid]!!
            
            val isJamTrack = group.all { isJamTrackOffer(it) }
            val isSpecialOffer = !isJamTrack && group.all { isSpecialOffer(it) }
            val isPureVehicle = !isJamTrack && !isSpecialOffer && group.all { isVehicleOnlyOffer(it) }

            when {
                isJamTrack -> jamTrackEntries.addAll(group)
                isSpecialOffer -> specialOfferEntries.addAll(group)
                isPureVehicle -> vehicleOnlyEntries.addAll(group)
                else -> normalSectionOrder.add(sid)
            }
        }

        val result = mutableListOf<ShopEntry>()
        
        // 1. Add Normal/Mixed sections in their original order
        normalSectionOrder.forEach { sid ->
            result.addAll(sectionsMap[sid]!!)
        }
        
        // 2. Add Combined Vehicles section
        if (vehicleOnlyEntries.isNotEmpty()) {
            val vehicleSection = com.dhyper.fncompanion.data.models.ShopSectionMetadata(
                id = "combined_vehicles", name = "Vehicles", index = null, landingPriority = null
            )
            result.addAll(vehicleOnlyEntries.map { it.copy(section = vehicleSection) })
        }
        
        // 3. Add Combined Special Offers section (above Jam Tracks)
        if (specialOfferEntries.isNotEmpty()) {
            val specialSection = com.dhyper.fncompanion.data.models.ShopSectionMetadata(
                id = "combined_special_offers", name = "Special Offers", index = null, landingPriority = null
            )
            result.addAll(specialOfferEntries.map { it.copy(section = specialSection) })
        }
        
        // 4. Add Combined Jam Tracks section (at the very bottom)
        if (jamTrackEntries.isNotEmpty()) {
            val jamTrackSection = com.dhyper.fncompanion.data.models.ShopSectionMetadata(
                id = "combined_jam_tracks", name = "Jam Tracks", index = null, landingPriority = null
            )
            result.addAll(jamTrackEntries.map { it.copy(section = jamTrackSection) })
        }
        
        return result
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleJamTracks() {
        _isJamTracksExpanded.value = !_isJamTracksExpanded.value
    }

    fun markBannerAsShown(offerId: String) {
        _shownBanners.value += offerId
    }

    fun toggleWishlist(item: com.dhyper.fncompanion.data.models.CosmeticItem) {
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

    private fun filterEntries(entries: List<ShopEntry>, category: String, query: String, wishlist: Set<String>, allCosmetics: List<com.dhyper.fncompanion.data.models.CosmeticItem> = emptyList()): List<ShopEntry> {
        return entries.filter { entry ->
            val trackIds = entry.tracks?.map { t ->
                val trackMap = t.track as? Map<*, *>
                val apiCosmeticId = trackMap?.get("id")?.toString()
                val sidFromDevName = Regex("""sid_[a-zA-Z0-9_]+""").find(t.devName ?: "")?.value
                val idField = t.id ?: ""
                
                when {
                    !apiCosmeticId.isNullOrBlank() -> apiCosmeticId
                    !sidFromDevName.isNullOrBlank() -> sidFromDevName
                    !idField.startsWith("v2:/") -> idField
                    else -> idField
                }
            } ?: emptyList()
            
            val allItems = getItemsForEntryInternal(entry, allCosmetics)
            val title = getShopEntryTitleInternal(entry, allCosmetics)
            val isBundle = title.contains("Bundle", ignoreCase = true)
            
            val matchesCategory = when (category) {
                "All" -> true
                "Bundles" -> isBundle
                "Outfits" -> allItems.any { it.type?.displayValue?.contains("Outfit", ignoreCase = true) == true || it.id.startsWith("CID_", ignoreCase = true) || it.id.startsWith("Character_", ignoreCase = true) }
                "Backblings" -> allItems.any { it.type?.displayValue?.contains("Back Bling", ignoreCase = true) == true || it.id.startsWith("BID_", ignoreCase = true) || it.id.startsWith("Backpack_", ignoreCase = true) || it.id.startsWith("PetID_", ignoreCase = true) || it.id.startsWith("PetCarrier_", ignoreCase = true) || it.id.contains("AthenaPet", ignoreCase = true) }
                "Pickaxes" -> allItems.any { it.type?.displayValue?.contains("Pickaxe", ignoreCase = true) == true || it.id.startsWith("Pickaxe_", ignoreCase = true) || it.id.startsWith("Pickaxe_ID_", ignoreCase = true) }
                "Buried" -> allItems.any { it.type?.displayValue?.contains("Buried", ignoreCase = true) == true }
                "Gliders" -> allItems.any { it.type?.displayValue?.contains("Glider", ignoreCase = true) == true || it.id.startsWith("Glider_", ignoreCase = true) || it.id.startsWith("Glider_ID_", ignoreCase = true) || it.id.startsWith("Umbrella_", ignoreCase = true) || it.id.endsWith("_Umbrella", ignoreCase = true) || it.id.equals("FounderGlider", ignoreCase = true) || it.id.equals("FounderUmbrella", ignoreCase = true) }
                "Emotes" -> allItems.any { it.type?.displayValue?.contains("Emote", ignoreCase = true) == true || it.id.startsWith("EID_", ignoreCase = true) || it.id.startsWith("Dance_", ignoreCase = true) }
                "Wraps" -> allItems.any { it.id.startsWith("Wrap_", ignoreCase = true) }
                "Contrails" -> allItems.any { it.id.startsWith("Contrail_", ignoreCase = true) || it.id.startsWith("Trails_ID_", ignoreCase = true) || it.id.startsWith("ID_DriftTrail_", ignoreCase = true) }
                "Music Packs" -> allItems.any { it.id.startsWith("MusicPack_", ignoreCase = true) }
                "Loading Screens" -> allItems.any { it.id.startsWith("LSID_", ignoreCase = true) || it.id.startsWith("LoadingScreen_", ignoreCase = true) }
                "Emojis" -> allItems.any { it.id.contains("Emoji_", ignoreCase = true) || it.id.contains("Emoticon_", ignoreCase = true) }
                "Sprays" -> allItems.any { it.id.contains("SPID_", ignoreCase = true) || it.id.contains("Spray_", ignoreCase = true) }
                "Sidekicks" -> allItems.any { it.id.startsWith("Companion_", ignoreCase = true) && !it.id.contains("reactfx", ignoreCase = true) && !it.id.contains("vtid", ignoreCase = true) }
                "Jam Tracks" -> !entry.tracks.isNullOrEmpty() || allItems.any { it.id.startsWith("sid_", ignoreCase = true) }
                "Vehicles" -> !entry.cars.isNullOrEmpty() || !entry.vehicles.isNullOrEmpty() || 
                             allItems.any { it.id.startsWith("CarBody_", ignoreCase = true) || it.id.startsWith("ID_Body_", ignoreCase = true) || it.id.startsWith("Body_", ignoreCase = true) || 
                                           it.id.startsWith("CarSkin_", ignoreCase = true) || it.id.startsWith("ID_Skin_", ignoreCase = true) || 
                                           it.id.startsWith("Wheel_", ignoreCase = true) || it.id.startsWith("ID_Wheel_", ignoreCase = true) || 
                                           it.id.startsWith("ID_DriftTrail_", ignoreCase = true) || it.id.startsWith("ID_Booster_", ignoreCase = true) }
                "Banners" -> allItems.any { it.id.startsWith("BR", ignoreCase = true) || it.id.startsWith("Banner", ignoreCase = true) || 
                             it.id.startsWith("OtherBanner", ignoreCase = true) || it.id.startsWith("OT", ignoreCase = true) ||
                             it.id.startsWith("InfluencerBanner", ignoreCase = true) || it.id.startsWith("FounderTier", ignoreCase = true) ||
                             it.id.startsWith("StandardBanner", ignoreCase = true) || it.id.startsWith("Achievement", ignoreCase = true) ||
                             it.id.startsWith("SurvivalBanner", ignoreCase = true) || it.id.startsWith("Newsletter", ignoreCase = true) ||
                             it.id.startsWith("Winter", ignoreCase = true) || it.id.startsWith("Wargames", ignoreCase = true) ||
                             it.id.startsWith("Endurance", ignoreCase = true) || it.id.startsWith("Starlight", ignoreCase = true) ||
                             it.id.startsWith("S8", ignoreCase = true) }
                "Kicks" -> allItems.any { it.id.startsWith("Shoes_", ignoreCase = true) }
                "Car Bodies" -> allItems.any { it.id.startsWith("CarBody_", ignoreCase = true) || it.id.startsWith("ID_Body_", ignoreCase = true) || it.id.startsWith("Body_", ignoreCase = true) }
                "Car Decals" -> allItems.any { it.id.startsWith("CarSkin_", ignoreCase = true) || it.id.startsWith("ID_Skin_", ignoreCase = true) }
                "Car Wheels" -> allItems.any { it.id.startsWith("Wheel_", ignoreCase = true) || it.id.startsWith("ID_Wheel_", ignoreCase = true) }
                "Car Trails" -> allItems.any { it.id.startsWith("ID_DriftTrail_", ignoreCase = true) }
                "Car Boosts" -> allItems.any { it.id.startsWith("ID_Booster_", ignoreCase = true) }
                "Guitars" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Guitar", ignoreCase = true) }
                "Basses" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Bass", ignoreCase = true) }
                "Drums" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("DrumKit", ignoreCase = true) }
                "Keytars" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Keytar", ignoreCase = true) }
                "Mics" -> allItems.any { it.id.startsWith("Sparks_", ignoreCase = true) && it.id.contains("Mic", ignoreCase = true) }
                "Lego Builds" -> allItems.any { it.id.startsWith("JBSID_", ignoreCase = true) }
                "Lego Decors" -> allItems.any { it.id.startsWith("JBPID_", ignoreCase = true) }
                "Auras" -> allItems.any { it.id.startsWith("SparksAura_", ignoreCase = true) || it.id.startsWith("Aura_", ignoreCase = true) }
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
