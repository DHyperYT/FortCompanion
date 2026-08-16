package com.dhyper.fncompanion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.data.models.ShopEntry
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.ui.components.CosmeticDetailSheet
import com.dhyper.fncompanion.ui.components.getRarityColor
import com.dhyper.fncompanion.ui.components.getRarityTextColor
import com.dhyper.fncompanion.ui.components.resolveCosmeticIcon
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.ShopUiState
import com.dhyper.fncompanion.ui.viewmodels.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: ShopViewModel,
    cosmeticsViewModel: com.dhyper.fncompanion.ui.viewmodels.CosmeticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val cosmeticsState = cosmeticsViewModel.uiState.collectAsState().value as? com.dhyper.fncompanion.ui.viewmodels.CosmeticsUiState.Success
    
    var searchText by remember { mutableStateOf("") }
    
    // Sync local typing to ViewModel
    LaunchedEffect(searchText) {
        viewModel.setSearchQuery(searchText)
    }

    val countdown by viewModel.countdown.collectAsState()
    var selectedEntryForDetail by remember { mutableStateOf<ShopEntry?>(null) }
    var selectedCosmeticDetail by remember { mutableStateOf<com.dhyper.fncompanion.data.models.CosmeticItem?>(null) }
    var selectedSet by remember { mutableStateOf<com.dhyper.fncompanion.data.models.CosmeticSet?>(null) }

    val categories = listOf(
        "All", "Bundles", 
        "Outfits", "Emotes", "Pickaxes", "Backblings", "Gliders", 
        "Sidekicks", "Kicks", "Wraps", "Loading Screens", "Music Packs", "Contrails", 
        "Sprays", "Emojis", "Banners",
        "Auras", "Jam Tracks", "Guitars", "Basses", "Drums", "Keytars", "Mics",
        "Car Bodies", "Car Decals", "Car Wheels", "Car Trails", "Car Boosts",
        "Lego Builds", "Lego Decors"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
    ) {
        // Shop Timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Resets in: $countdown",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextSecondary
                )
            }
        }

        // Search & Filter Header
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = { Text("Search Item Shop...", color = SleekTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SleekCyan) },
            trailingIcon = if (searchText.isNotEmpty()) {
                {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SleekTextMuted)
                    }
                }
            } else null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SleekPrimary,
                unfocusedBorderColor = SleekSurfaceBorder,
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface,
                focusedTextColor = SleekTextPrimary,
                unfocusedTextColor = SleekTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        val currentCategory = (uiState as? ShopUiState.Success)?.selectedCategory ?: "All"
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == currentCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SleekPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = SleekSurface,
                        labelColor = SleekTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = SleekSurfaceBorder,
                        selectedBorderColor = SleekPrimary
                    )
                )
            }
        }

        when (val state = uiState) {
            is ShopUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SleekCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching live Fortnite Item Shop...", color = SleekTextSecondary)
                    }
                }
            }
            is ShopUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Item Shop Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = SleekTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadShop() },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ShopUiState.Success -> {
                if (state.filteredEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No shop items found matching criteria", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        state.filteredEntries.forEachIndexed { index, entry ->
                            val currentSectionId = entry.section?.id ?: entry.section?.name ?: entry.layout?.id ?: "default"
                            val currentCategory = entry.section?.name ?: entry.layout?.name ?: "Special Offers"
                            
                            val previousSectionId = if (index > 0) {
                                val prev = state.filteredEntries[index - 1]
                                prev.section?.id ?: prev.section?.name ?: prev.layout?.id ?: "default"
                            } else null

                            val isJamTrack = currentSectionId == "combined_jam_tracks"
                            val isExpanded = !isJamTrack || state.isJamTracksExpanded

                            if (index == 0 || currentSectionId != previousSectionId) {
                                item(key = "header_${currentSectionId}_$index", span = { GridItemSpan(2) }) {
                                    val count = if (isJamTrack) state.filteredEntries.count { (it.section?.id ?: it.section?.name ?: it.layout?.id ?: "default") == "combined_jam_tracks" } else 0
                                    ShopCategoryHeader(
                                        category = if (isJamTrack) "$currentCategory ($count)" else currentCategory,
                                        isCollapsible = isJamTrack,
                                        isExpanded = state.isJamTracksExpanded,
                                        onToggle = { viewModel.toggleJamTracks() }
                                    )
                                }
                            }
                            
                            if (isExpanded) {
                                item(key = entry.offerId ?: "entry_$index") {
                                    ShopItemCard(
                                        entry = entry,
                                        ownedIds = state.ownedIds,
                                        wishlistIds = state.wishlistIds,
                                        indPrices = state.individualPrices,
                                        setPrices = state.skinSetPrices,
                                        allCosmetics = cosmeticsState?.allItems ?: emptyList(),
                                        isBannerShown = state.shownBanners.contains(entry.offerId ?: ""),
                                        onBannerShown = { id -> viewModel.markBannerAsShown(id) },
                                        onWishlistToggle = { item -> viewModel.toggleWishlist(item) },
                                        onClick = { selectedEntryForDetail = entry }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Modal BottomSheet
    val detailedItem by cosmeticsViewModel.detailedItem.collectAsState()
    
    selectedEntryForDetail?.let { entry ->
        val successState = uiState as? ShopUiState.Success
        val videoId by cosmeticsViewModel.selectedVideoId.collectAsState()
        val isSearchingVideo by cosmeticsViewModel.isSearchingVideo.collectAsState()
        
        val allItemsForEntry = remember(entry, cosmeticsState) { 
            getItemsForEntry(entry, cosmeticsState?.allItems ?: emptyList()) 
        }
        val firstItemRaw = allItemsForEntry.firstOrNull()
        
        val firstItem = remember(firstItemRaw, cosmeticsState, detailedItem) {
            if (firstItemRaw == null) return@remember null
            if (detailedItem?.id == firstItemRaw.id) return@remember detailedItem
            cosmeticsState?.allItems?.find { it.id.equals(firstItemRaw.id, ignoreCase = true) } ?: firstItemRaw
        }

        LaunchedEffect(entry.offerId ?: entry.devName) {
            if (firstItem != null) {
                // Fetch full details (including showcase video) for the item
                cosmeticsViewModel.loadDetailedItem(firstItem)
                
                val isTrack = firstItem.id.startsWith("sid_", ignoreCase = true) || 
                             firstItem.type?.displayValue?.contains("Track", ignoreCase = true) == true
                val isMusicPack = firstItem.id.startsWith("MusicPack_", ignoreCase = true)
                if (isTrack || isMusicPack) {
                    cosmeticsViewModel.searchYouTubeForItem(firstItem)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedEntryForDetail = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SleekSurface
        ) {
            if (firstItem != null) {
                val entryTitle = getShopEntryTitle(entry)
                val isCarOffer = allItemsForEntry.any { 
                    it.id.startsWith("Body_", true) || it.id.startsWith("ID_Body_", true) || it.id.startsWith("CarBody_", true) 
                }
                val isRealBundle = entryTitle.contains("Bundle", ignoreCase = true) || isCarOffer
                
                val displayItem = if (isRealBundle) {
                    val rarityName = firstItem.rarity?.value ?: firstItem.series?.value ?: if (!entry.tracks.isNullOrEmpty()) "Festival" else "Common"
                    val shopImg = getShopEntryImage(entry, cosmeticsState?.allItems ?: emptyList())
                    com.dhyper.fncompanion.data.models.CosmeticItem(
                        id = entry.offerId ?: entry.devName ?: "bundle",
                        name = entryTitle,
                        description = entry.bundle?.info ?: firstItem.description ?: "",
                        type = com.dhyper.fncompanion.data.models.CosmeticType("Bundle", "Bundle"),
                        rarity = com.dhyper.fncompanion.data.models.CosmeticRarity(rarityName, rarityName),
                        series = firstItem.series,
                        images = com.dhyper.fncompanion.data.models.CosmeticImages(
                            smallIcon = shopImg,
                            featured = shopImg,
                            background = null,
                            full_background = null,
                            icon = shopImg
                        ),
                        variants = null, introduction = firstItem.introduction, set = firstItem.set, added = firstItem.added,
                        artist = firstItem.artist, bpm = firstItem.bpm, duration = firstItem.duration,
                        showcaseVideo = firstItem.showcaseVideo, previewUrl = firstItem.previewUrl
                    )
                } else firstItem

                    val isFullyOwned = allItemsForEntry.all { successState?.ownedIds?.contains(it.id.lowercase()) ?: false }
                val price = entry.finalPrice ?: entry.regularPrice ?: 0

                CosmeticDetailSheet(
                    item = displayItem,
                    isOwned = isFullyOwned,
                    isWishlisted = successState?.wishlistIds?.contains(firstItem.id) ?: false,
                    videoId = videoId,
                    isSearchingVideo = isSearchingVideo,
                    price = price,
                    includedItems = allItemsForEntry,
                    ownedIds = successState?.ownedIds ?: emptySet(),
                    renderImages = entry.newDisplayAsset?.renderImages?.mapNotNull { it.image },
                    onWishlistToggle = { viewModel.toggleWishlist(firstItem) },
                    onSetClick = { _ ->
                        selectedSet = firstItem.set
                        selectedEntryForDetail = null
                    },
                    onCosmeticClick = { cosmetic -> 
                        selectedCosmeticDetail = cosmetic
                        selectedEntryForDetail = null 
                    },
                    onClose = { selectedEntryForDetail = null }
                )
            }
        }
    }

    // Individual Cosmetic Detail Modal
    selectedCosmeticDetail?.let { itemRaw ->
        val successState = uiState as? ShopUiState.Success
        val videoId by cosmeticsViewModel.selectedVideoId.collectAsState()
        val isSearchingVideo by cosmeticsViewModel.isSearchingVideo.collectAsState()
        
        val item = remember(itemRaw, cosmeticsState) {
            cosmeticsState?.allItems?.find { it.id.equals(itemRaw.id, ignoreCase = true) } ?: itemRaw
        }

        // Trigger search in background when detail sheet opens
        LaunchedEffect(item.id) {
            cosmeticsViewModel.loadDetailedItem(item)
            
            val isTrack = item.id.startsWith("sid_", ignoreCase = true) || 
                         item.type?.displayValue?.contains("Track", ignoreCase = true) == true
            val isMusicPack = item.id.startsWith("MusicPack_", ignoreCase = true)
            if (isTrack || isMusicPack) {
                cosmeticsViewModel.searchYouTubeForItem(item)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCosmeticDetail = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SleekSurface
        ) {
            val price = successState?.individualPrices?.get(item.id.lowercase())
            CosmeticDetailSheet(
                item = item,
                isOwned = successState?.ownedIds?.contains(item.id.lowercase()) ?: false,
                isWishlisted = successState?.wishlistIds?.contains(item.id) ?: false,
                videoId = videoId,
                isSearchingVideo = isSearchingVideo,
                price = price,
                ownedIds = successState?.ownedIds ?: emptySet(),
                onWishlistToggle = { viewModel.toggleWishlist(item) },
                onSetClick = { 
                    selectedSet = item.set
                    selectedCosmeticDetail = null
                },
                onClose = { selectedCosmeticDetail = null }
            )
        }
    }

    // Set Detail Popup (Shared with Cosmetics logic)
    selectedSet?.let { cosmeticSet ->
        val wishlistState by cosmeticsViewModel.wishlistIds.collectAsState()
        val setItemResult = cosmeticsViewModel.getItemsInSet(cosmeticSet.value ?: "")
        
        ModalBottomSheet(
            onDismissRequest = { selectedSet = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SleekSurface
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = cosmeticSet.text?.uppercase() ?: "COLLECTION",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekCyan,
                    fontWeight = FontWeight.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    items(setItemResult) { setItem ->
                        val isOwned = (uiState as? ShopUiState.Success)?.ownedIds?.contains(setItem.id.lowercase()) ?: false
                        com.dhyper.fncompanion.ui.screens.CosmeticBrowserCard(
                            item = setItem,
                            isWishlisted = wishlistState.contains(setItem.id),
                            isOwned = isOwned,
                            onWishlistToggle = { cosmeticsViewModel.toggleWishlist(setItem) },
                            onClick = { 
                                selectedCosmeticDetail = setItem
                                selectedSet = null
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { selectedSet = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Back to Shop")
                }
            }
        }
    }
}

private fun cleanShopTitle(rawName: String?): String {
    if (rawName == null) return ""
    return rawName.replace(" Bundle", "", ignoreCase = true).trim()
}

fun getShopEntryTitle(entry: ShopEntry): String {
    if (!entry.bundle?.name.isNullOrBlank()) return entry.bundle?.name!!
    
    val allItems = getItemsForEntry(entry)
    val firstItemName = allItems.firstOrNull { !it.name.isNullOrBlank() }?.name
    if (!firstItemName.isNullOrBlank()) {
        return cleanShopTitle(firstItemName)
    }
    if (!entry.layout?.name.isNullOrBlank()) {
        return cleanShopTitle(entry.layout?.name)
    }
    return cleanShopTitle(entry.devName)
}

private fun getShopEntryImage(entry: ShopEntry, allItemsForDerivation: List<com.dhyper.fncompanion.data.models.CosmeticItem> = emptyList()): String? {
    if (!entry.bundle?.image.isNullOrBlank()) return entry.bundle?.image

    val renderImageUrl = entry.newDisplayAsset?.renderImages?.firstOrNull()?.image
    if (!renderImageUrl.isNullOrBlank()) return renderImageUrl
    
    val materialImg = entry.newDisplayAsset?.materialInstances?.firstOrNull()?.images?.get("OfferImage") ?:
                      entry.newDisplayAsset?.materialInstances?.firstOrNull()?.images?.get("Background")
    if (!materialImg.isNullOrBlank()) return materialImg

    val trackAlbumArt = entry.tracks?.firstOrNull()?.albumArt
    if (!trackAlbumArt.isNullOrBlank()) return trackAlbumArt

    val allItems = getItemsForEntry(entry, allItemsForDerivation)
    val referencedCosmeticId = entry.newDisplayAsset?.cosmeticId
    if (!referencedCosmeticId.isNullOrBlank()) {
        val referencedItem = allItems.find { it.id.equals(referencedCosmeticId, ignoreCase = true) }
        if (referencedItem != null) {
            val resolved = resolveCosmeticIcon(referencedItem)
            if (!resolved.isNullOrBlank()) return resolved
        }
    }

    val firstItem = allItems.firstOrNull()
    if (firstItem != null) {
        val resolved = resolveCosmeticIcon(firstItem)
        if (!resolved.isNullOrBlank()) return resolved
    }

    return firstItem?.images?.featured ?: 
           firstItem?.images?.large ?: 
           firstItem?.images?.icon ?: 
           firstItem?.images?.smallIcon ?:
           "https://fortnite-api.com/images/cosmetics/br/${firstItem?.id}/icon.png"
}

fun getItemsForEntry(entry: ShopEntry, allCosmetics: List<com.dhyper.fncompanion.data.models.CosmeticItem> = emptyList()): List<com.dhyper.fncompanion.data.models.CosmeticItem> {
    val itemMap = mutableMapOf<String, com.dhyper.fncompanion.data.models.CosmeticItem>()
    val orderedIds = mutableListOf<String>()

    fun processList(list: List<com.dhyper.fncompanion.data.models.CosmeticItem>?) {
        list?.forEach { item ->
            if (item.id.isBlank()) return@forEach
            if (!itemMap.containsKey(item.id)) {
                orderedIds.add(item.id)
            }
            val existing = itemMap[item.id]
            // Ensure we pick the object that actually has the image and metadata
            if (existing == null || (existing.images == null && item.images != null)) {
                itemMap[item.id] = item
            }
        }
    }

    // Process specific lists first to get better objects
    processList(entry.vehicles)
    processList(entry.cars)
    processList(entry.brItems)
    processList(entry.instruments)
    processList(entry.items)

    // Check for referenced cosmetic ID in NewDisplayAsset if not already present
    entry.newDisplayAsset?.cosmeticId?.let { cid ->
        if (!itemMap.containsKey(cid)) {
            val placeholder = com.dhyper.fncompanion.data.models.CosmeticItem(
                id = cid, name = entry.devName ?: "Item", description = null, type = null, rarity = null, series = null, images = null, variants = null, introduction = null, set = null, added = null
            )
            itemMap[cid] = placeholder
            orderedIds.add(cid)
        }
    }

    // Fix for missing Car Bodies in bundles - Match by Name
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

    // Map and add tracks
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
        
        val title = trackMap?.get("title")?.toString() ?: t.title ?: t.devName ?: "Track"
        val artist = trackMap?.get("artist")?.toString() ?: t.artist ?: "Unknown Artist"
        val album = trackMap?.get("album")?.toString() ?: t.album
        val albumArt = t.albumArt
        val albumName = if (album.isNullOrBlank() || album.contains("unknown", ignoreCase = true)) "" else " from $album"

        itemMap[realId] = com.dhyper.fncompanion.data.models.CosmeticItem(
            id = realId, name = title, description = "Jam Track by $artist$albumName",
            type = com.dhyper.fncompanion.data.models.CosmeticType("Track", "Jam Track"),
            rarity = com.dhyper.fncompanion.data.models.CosmeticRarity("Festival", "Festival"),
            series = null, 
            images = com.dhyper.fncompanion.data.models.CosmeticImages(
                smallIcon = albumArt,
                featured = albumArt,
                albumArt = albumArt,
                icon = albumArt,
                background = null,
                full_background = null
            ),
            variants = null, introduction = null, set = null, added = null, 
            previewUrl = trackMap?.get("previewUrl")?.toString() ?: t.previewUrl, 
            artist = artist, album = album, 
            bpm = (trackMap?.get("bpm") as? Number)?.toInt() ?: t.bpm, 
            duration = (trackMap?.get("duration") as? Number)?.toInt() ?: t.duration
        )
    }

    return orderedIds.mapNotNull { itemMap[it] }
}

@Composable
fun ShopCategoryHeader(
    category: String,
    isCollapsible: Boolean = false,
    isExpanded: Boolean = true,
    onToggle: () -> Unit = {}
) {
    val isModeHeader = category.contains("Racing", ignoreCase = true) || 
                      category.contains("Festival", ignoreCase = true) || 
                      category.contains("LEGO", ignoreCase = true) ||
                      category.equals("Jam Tracks", ignoreCase = true) ||
                      category.equals("Vehicles", ignoreCase = true)
    
    Column(
        modifier = Modifier
            .padding(top = 24.dp, bottom = 12.dp)
            .then(if (isCollapsible) Modifier.clickable { onToggle() } else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.uppercase(),
                style = if (isModeHeader) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                color = if (isModeHeader) SleekEmerald else SleekCyan,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            
            if (isCollapsible) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = SleekEmerald,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isModeHeader) 3.dp else 2.dp)
                .background(Brush.horizontalGradient(listOf(if (isModeHeader) SleekEmerald else SleekCyan, Color.Transparent)))
        )
    }
}

@Composable
fun ShopItemCard(
    entry: ShopEntry,
    ownedIds: Set<String>,
    wishlistIds: Set<String>,
    indPrices: Map<String, Int>,
    setPrices: Map<String, Pair<Int, Set<String>>>,
    allCosmetics: List<com.dhyper.fncompanion.data.models.CosmeticItem> = emptyList(),
    isBannerShown: Boolean = false,
    onBannerShown: (String) -> Unit = {},
    onWishlistToggle: (com.dhyper.fncompanion.data.models.CosmeticItem) -> Unit,
    onClick: () -> Unit
) {
    val allItems = remember(entry, allCosmetics) { getItemsForEntry(entry, allCosmetics) }
    val firstItem = allItems.firstOrNull()
    
    val title = getShopEntryTitle(entry)
    
    val isRealBundle = title.contains("Bundle", ignoreCase = true)
    
    val ownedCount = allItems.count { ownedIds.contains(it.id.lowercase()) }
    val isFullyOwned = allItems.isNotEmpty() && ownedCount == allItems.size
    
    var finalDisplayPrice = entry.finalPrice ?: entry.regularPrice ?: 0
    var isPartiallyOwned = false

    if (!isFullyOwned && isRealBundle && ownedCount > 0) {
        isPartiallyOwned = true
        var discountValue = 0
        val handledItemIds = mutableSetOf<String>()

        allItems.forEach { item ->
            val itemId = item.id.lowercase()
            if (ownedIds.contains(itemId) && setPrices.containsKey(itemId)) {
                val (price, setIds) = setPrices[itemId]!!
                discountValue += price
                handledItemIds.addAll(setIds)
            }
        }

        allItems.forEach { item ->
            val itemId = item.id.lowercase()
            if (ownedIds.contains(itemId) && !handledItemIds.contains(itemId)) {
                discountValue += indPrices[itemId] ?: 0
                handledItemIds.add(itemId)
            }
        }

        finalDisplayPrice = (finalDisplayPrice - discountValue).coerceAtLeast(100)
    }

    val rarityName = firstItem?.rarity?.value ?: firstItem?.series?.value ?: if (!entry.tracks.isNullOrEmpty()) "Festival" else "Common"
    val rarityColor = getRarityColor(rarityName)
    val imageUrl = getShopEntryImage(entry, allCosmetics)

    val grayScaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val colorFilter = if (isFullyOwned) ColorFilter.colorMatrix(grayScaleMatrix) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, if(isFullyOwned) Color.Gray.copy(alpha = 0.3f) else if (getRarityTextColor(rarityColor) == Color.White) Color.White.copy(alpha = 0.5f) else rarityColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if(isFullyOwned) SleekSurfaceVariant.copy(alpha = 0.5f) else SleekSurfaceVariant)
    ) {
        val renderImages = entry.newDisplayAsset?.renderImages?.mapNotNull { it.image } ?: emptyList()
        var currentImageIndex by remember { mutableIntStateOf(0) }
        
        if (renderImages.size > 1) {
            LaunchedEffect(entry.offerId) {
                while (true) {
                    kotlinx.coroutines.delay(3600)
                    currentImageIndex = (currentImageIndex + 1) % renderImages.size
                }
            }
        }

        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(if(isFullyOwned) Color.Gray.copy(alpha = 0.2f) else rarityColor.copy(alpha = 0.35f), SleekSurfaceVariant)
                            )
                        )
                ) {
                    val displayImageUrl = if (renderImages.isNotEmpty()) renderImages[currentImageIndex] else imageUrl
                    
                    Crossfade(
                        targetState = displayImageUrl, 
                        animationSpec = tween(1000),
                        label = "ShopImageSlideshow"
                    ) { targetUrl ->
                        if (!targetUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(targetUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                colorFilter = colorFilter
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = SleekTextMuted, modifier = Modifier.size(48.dp))
                            }
                        }
                    }

                    // Banner or Bundle tag if present
                    val isSessionShown = isBannerShown
                    var visible by remember(entry.offerId, isSessionShown) { mutableStateOf(!isSessionShown) }
                    
                    if (visible) {
                        LaunchedEffect(entry.offerId) {
                            kotlinx.coroutines.delay(3000)
                            visible = false
                            onBannerShown(entry.offerId ?: "")
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(600)),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        if (entry.banner != null) {
                            Surface(
                                color = Color.Red.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
                            ) {
                                Text(
                                    text = entry.banner.value ?: "SALE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else if (isRealBundle) {
                            Surface(
                                color = SleekPrimary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
                            ) {
                                Text(
                                    text = "BUNDLE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Wishlist Indicator
                    if (wishlistIds.contains(firstItem?.id)) {
                        IconButton(
                            onClick = { firstItem?.let { onWishlistToggle(it) } },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { firstItem?.let { onWishlistToggle(it) } },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Price and Title Footer
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = title,
                        color = SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isFullyOwned) {
                            Icon(Icons.Default.CheckCircle, null, tint = SleekEmerald, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("OWNED", color = SleekEmerald, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        } else {
                            Icon(Icons.Default.MonetizationOn, null, tint = FortniteGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = finalDisplayPrice.toString(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (isPartiallyOwned) {
                                Spacer(Modifier.width(4.dp))
                                Text("(Discounted)", color = SleekEmerald, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
