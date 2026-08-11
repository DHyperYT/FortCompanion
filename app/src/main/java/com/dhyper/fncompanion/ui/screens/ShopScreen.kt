package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.data.models.ShopEntry
import com.dhyper.fncompanion.ui.components.JamTrackPlayer
import com.dhyper.fncompanion.ui.components.YouTubeButton
import com.dhyper.fncompanion.ui.components.getRarityColor
import com.dhyper.fncompanion.ui.components.getRarityTextColor
import com.dhyper.fncompanion.ui.theme.FortniteBlue
import com.dhyper.fncompanion.ui.theme.FortniteGold
import com.dhyper.fncompanion.ui.theme.SleekBackground
import com.dhyper.fncompanion.ui.theme.SleekCyan
import com.dhyper.fncompanion.ui.theme.SleekEmerald
import com.dhyper.fncompanion.ui.theme.SleekPrimary
import com.dhyper.fncompanion.ui.theme.SleekSurface
import com.dhyper.fncompanion.ui.theme.SleekSurfaceBorder
import com.dhyper.fncompanion.ui.theme.SleekSurfaceVariant
import com.dhyper.fncompanion.ui.theme.SleekTextMuted
import com.dhyper.fncompanion.ui.theme.SleekTextPrimary
import com.dhyper.fncompanion.ui.theme.SleekTextSecondary
import com.dhyper.fncompanion.ui.utils.SeasonUtils
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
        // Battle Royale (Primary)
        "Outfit", "Back Bling", "Pickaxe", "Glider", "Emote", "Wrap", "Contrail", "Music", "Aura",
        // LEGO & Misc
        "Lego Build", "Lego Decor", "Loading Screen", "Emoticon", "Spray", "Sidekick", "Banner", "Kicks",
        // Festival
        "Jam Track", "Guitar", "Bass", "Drums", "Keytar", "Mic",
        // Vehicles (Rocket Racing)
        "Vehicles", "Car", "Car Decal", "Wheels", "Car Trail", "Car Boost"
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
                .fillMaxWidth()
                .testTag("shop_search_input"),
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
                    ),
                    modifier = Modifier.testTag("shop_category_$category")
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
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("shop_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
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
    selectedEntryForDetail?.let { entry ->
        val successState = uiState as? ShopUiState.Success
        
        val videoId by cosmeticsViewModel.selectedVideoId.collectAsState()
        val isSearchingVideo by cosmeticsViewModel.isSearchingVideo.collectAsState()

        ModalBottomSheet(
            onDismissRequest = { selectedEntryForDetail = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ShopItemDetailSheet(
                entry = entry,
                wishlistIds = successState?.wishlistIds ?: emptySet(),
                ownedIds = successState?.ownedIds ?: emptySet(),
                videoId = videoId,
                isSearchingVideo = isSearchingVideo,
                onWishlistToggle = { item -> viewModel.toggleWishlist(item) },
                onCosmeticClick = { cosmetic -> selectedCosmeticDetail = cosmetic },
                onClose = { selectedEntryForDetail = null }
            )
        }
    }

    // Individual Cosmetic Detail Modal
    selectedCosmeticDetail?.let { item ->
        val successState = uiState as? ShopUiState.Success
        val videoId by cosmeticsViewModel.selectedVideoId.collectAsState()
        val isSearchingVideo by cosmeticsViewModel.isSearchingVideo.collectAsState()

        // Trigger search in background when detail sheet opens
        LaunchedEffect(item.id) {
            val isTrack = item.id.startsWith("sid_", ignoreCase = true)
            if (isTrack) {
                cosmeticsViewModel.searchYouTubeForItem(item)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCosmeticDetail = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SleekSurface
        ) {
            CosmeticDetailSheet(
                item = item,
                isOwned = successState?.ownedIds?.contains(item.id.lowercase()) ?: false,
                isWishlisted = successState?.wishlistIds?.contains(item.id) ?: false,
                videoId = videoId,
                isSearchingVideo = isSearchingVideo,
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
        val setItemResult = remember(cosmeticSet) { cosmeticsViewModel.getItemsInSet(cosmeticSet.value ?: "") }

        ModalBottomSheet(
            onDismissRequest = { selectedSet = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SleekBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
    if (rawName.isNullOrBlank()) return "Cosmetic Offer"
    var clean = rawName
    clean = clean.replace(Regex("(?i)\\[virtual\\]"), "")
    clean = clean.replace(Regex("(?i)^\\s*\\d+\\s*x\\s*"), "")
    clean = clean.replace(Regex("(?i)\\s+for\\s+\\d+\\s*v-?bucks"), "")
    if (clean.contains("_")) {
        clean = clean.split("_")
            .filter { !it.equals("outfit", ignoreCase = true) && !it.equals("emote", ignoreCase = true) && !it.equals("pickaxe", ignoreCase = true) && !it.equals("glider", ignoreCase = true) }
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }
    clean = clean.trim()
    return if (clean.isBlank()) "Cosmetic Offer" else clean
}

private fun getShopEntryTitle(entry: ShopEntry): String {
    if (!entry.bundle?.name.isNullOrBlank()) {
        return cleanShopTitle(entry.bundle?.name)
    }
    val firstTrack = entry.tracks?.firstOrNull()
    if (firstTrack != null && !firstTrack.title.isNullOrBlank()) {
        val artist = if (!firstTrack.artist.isNullOrBlank()) " - ${firstTrack.artist}" else ""
        return "${firstTrack.title}$artist"
    }
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

private fun getShopEntryImage(entry: ShopEntry): String? {
    // 1. Prioritize Bundle Image (for bundles)
    if (!entry.bundle?.image.isNullOrBlank()) return entry.bundle?.image

    // 2. Prioritize Modern Asset References (NewDisplayAsset)
    // 2a. Check for direct render images list
    val renderImageUrl = entry.newDisplayAsset?.renderImages?.firstOrNull()?.image
    if (!renderImageUrl.isNullOrBlank()) return renderImageUrl
    
    // 2b. Check for material instance images (Legacy fallback)
    val materialImg = entry.newDisplayAsset?.materialInstances?.firstOrNull()?.images?.get("OfferImage") ?:
                      entry.newDisplayAsset?.materialInstances?.firstOrNull()?.images?.get("Background")
    if (!materialImg.isNullOrBlank()) return materialImg

    // 3. Specialized Jam Track Art (if no bundle image)
    val trackAlbumArt = entry.tracks?.firstOrNull()?.albumArt
    if (!trackAlbumArt.isNullOrBlank()) return trackAlbumArt

    // 4. Resolve by specific Cosmetic ID if referenced in NewDisplayAsset
    val allItems = getItemsForEntry(entry)
    val referencedCosmeticId = entry.newDisplayAsset?.cosmeticId
    if (!referencedCosmeticId.isNullOrBlank()) {
        val referencedItem = allItems.find { it.id.equals(referencedCosmeticId, ignoreCase = true) }
        if (referencedItem != null) {
            val resolved = resolveIncludedItemImage(referencedItem)
            if (!resolved.isNullOrBlank()) return resolved
        }
    }

    // 5. Fallback to the first item's resolved image
    val firstItem = allItems.firstOrNull()
    if (firstItem != null) {
        val resolved = resolveIncludedItemImage(firstItem)
        if (!resolved.isNullOrBlank()) return resolved
    }

    return firstItem?.images?.featured ?: 
           firstItem?.images?.large ?: 
           firstItem?.images?.icon ?: 
           firstItem?.images?.smallIcon ?:
           "https://fortnite-api.com/images/cosmetics/br/${firstItem?.id}/icon.png"
}

fun resolveIncludedItemImage(item: com.dhyper.fncompanion.data.models.CosmeticItem): String? {
    val images = item.images ?: return null
    val id = item.id.lowercase()
    val type = item.type?.value?.lowercase() ?: ""
    
    return when {
        // Vehicles: prioritize large renders as requested
        id.startsWith("car") || id.startsWith("id_") || type.contains("car") || 
        type.contains("wheel") || type.contains("boost") || type.contains("trail") || type.contains("decal") -> {
            images.large ?: images.small ?: images.featured ?: images.decal ?: images.icon ?: images.smallIcon
        }
        
        // Jam Tracks: prioritize cover art
        id.startsWith("sid_") || type.contains("track") -> {
            images.coverart ?: images.albumArt ?: images.other?.albumArt ?: images.large ?: images.small ?: images.icon
        }
        
        // Lego items: prioritize lego-specific assets
        id.startsWith("jbsid") || type.contains("lego") -> {
            images.legoLarge ?: images.legoSmall ?: images.lego?.large ?: images.lego?.small ?: images.lego?.icon ?: images.large ?: images.small ?: images.icon
        }
        
        // Standard BR and others: strictly prioritize icon and smallIcon for detailed cards
        else -> {
            images.icon ?: images.smallIcon ?: images.featured ?: images.largeIcon ?: images.large ?: images.small
        }
    }
}

fun getItemsForEntry(entry: ShopEntry): List<com.dhyper.fncompanion.data.models.CosmeticItem> {
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
    processList(entry.brItems)
    processList(entry.cars)
    processList(entry.vehicles)
    processList(entry.instruments)
    processList(entry.items)

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
            series = null, images = com.dhyper.fncompanion.data.models.CosmeticImages(albumArt, albumArt, albumArt, null, null, albumArt),
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
    onWishlistToggle: (com.dhyper.fncompanion.data.models.CosmeticItem) -> Unit,
    onClick: () -> Unit
) {
    val allItems = remember(entry) { getItemsForEntry(entry) }
    val firstItem = allItems.firstOrNull()
    
    val title = getShopEntryTitle(entry)
    val skin = allItems.find { it.type?.value?.equals("outfit", ignoreCase = true) == true }
    val itemCount = if (!entry.tracks.isNullOrEmpty()) entry.tracks.size else allItems.size.coerceAtLeast(1)
    
    val isRealBundle = (entry.bundle != null || title.contains("Bundle", ignoreCase = true) || itemCount > 1) && 
                       (skin == null || !title.trim().equals(skin.name.trim(), ignoreCase = true) || itemCount > 1)
    
    val ownedCount = allItems.count { ownedIds.contains(it.id.lowercase()) }
    val isFullyOwned = allItems.isNotEmpty() && ownedCount == allItems.size
    
    var finalDisplayPrice = entry.finalPrice ?: entry.regularPrice ?: 0
    var isPartiallyOwned = false

    if (!isFullyOwned && isRealBundle && ownedCount > 0) {
        isPartiallyOwned = true
        var discountValue = 0
        val handledItemIds = mutableSetOf<String>()

        // 1. Process Skin Sets first
        allItems.forEach { item ->
            val itemId = item.id.lowercase()
            if (ownedIds.contains(itemId) && setPrices.containsKey(itemId)) {
                val (price, setIds) = setPrices[itemId]!!
                discountValue += price
                handledItemIds.addAll(setIds)
            }
        }

        // 2. Process remaining owned items
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
    val imageUrl = getShopEntryImage(entry)

    val grayScaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val colorFilter = if (isFullyOwned) ColorFilter.colorMatrix(grayScaleMatrix) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, if(isFullyOwned) Color.Gray.copy(alpha = 0.3f) else if (getRarityTextColor(rarityColor) == Color.White) Color.White.copy(alpha = 0.5f) else rarityColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .testTag("shop_item_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if(isFullyOwned) SleekSurfaceVariant.copy(alpha = 0.5f) else SleekSurfaceVariant)
    ) {
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
                    if (!imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(imageUrl)
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

                    // Banner or Bundle tag if present
                    val bannerText = when {
                        isFullyOwned -> "OWNED"
                        isPartiallyOwned -> "DISCOUNTED"
                        else -> entry.banner?.value ?: entry.bundle?.info ?: if (itemCount > 1) "$itemCount ITEMS" else null
                    }

                    if (!bannerText.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .background(if(isFullyOwned) Color.Gray else if(isPartiallyOwned) SleekEmerald else SleekPrimary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Text(
                                text = bannerText.uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if(isFullyOwned) SleekTextMuted else SleekTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Rarity pill
                        Text(
                            text = rarityName.uppercase(),
                            color = if(isFullyOwned) SleekTextMuted else if (getRarityTextColor(rarityColor) == Color.White) Color.White else rarityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Price Tag
                        if (!isFullyOwned) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(FortniteGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("V", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$finalDisplayPrice",
                                    color = if(isPartiallyOwned) SleekEmerald else FortniteGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekEmerald, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            // Wishlist Toggle
            if (!isFullyOwned && !isRealBundle && firstItem != null) {
                IconButton(
                    onClick = { onWishlistToggle(firstItem) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    val isWishlisted = wishlistIds.any { it.equals(firstItem.id, ignoreCase = true) } || 
                                       wishlistIds.any { it.equals(firstItem.id.replace("sid_", ""), ignoreCase = true) }
                    Icon(
                        imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) Color.Red else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(100.dp), color = SleekTextMuted, fontSize = 13.sp)
        Text(value, color = SleekTextPrimary, fontSize = 13.sp)
    }
}

@Composable
fun ShopItemDetailSheet(
    entry: ShopEntry,
    wishlistIds: Set<String> = emptySet(),
    ownedIds: Set<String> = emptySet(),
    videoId: String? = null,
    isSearchingVideo: Boolean = false,
    onWishlistToggle: ((com.dhyper.fncompanion.data.models.CosmeticItem) -> Unit)? = null,
    onCosmeticClick: (com.dhyper.fncompanion.data.models.CosmeticItem) -> Unit,
    onClose: () -> Unit
) {
    val itemsList = remember(entry) { getItemsForEntry(entry) }
    val firstItem = itemsList.firstOrNull()
    val isOwned = itemsList.isNotEmpty() && itemsList.all { ownedIds.contains(it.id.lowercase()) }
    val title = getShopEntryTitle(entry)
    val imageUrl = getShopEntryImage(entry)
    
    val isRealBundle = entry.bundle != null || itemsList.size > 1 || title.contains("Bundle", ignoreCase = true)
    
    val rarityName = if (isRealBundle && !entry.bundle?.info.isNullOrBlank()) {
        entry.bundle?.info!!
    } else {
        firstItem?.rarity?.value ?: firstItem?.series?.value ?: if (!entry.tracks.isNullOrEmpty()) "Festival" else "Common"
    }
    
    val rarityColor = if (isRealBundle) SleekPrimary else getRarityColor(rarityName)
    val price = entry.finalPrice ?: entry.regularPrice ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SleekSurface)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(rarityColor.copy(alpha = 0.4f), SleekSurfaceVariant)
                    )
                )
        ) {
            val detailIcon = if (isRealBundle && !entry.bundle?.image.isNullOrBlank()) {
                entry.bundle?.image
            } else {
                imageUrl
            }
                             
            if (!detailIcon.isNullOrEmpty()) {
                AsyncImage(
                    model = detailIcon,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            if (!isOwned && firstItem != null && onWishlistToggle != null && !isRealBundle) {
                IconButton(
                    onClick = { onWishlistToggle(firstItem) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    val isWishlisted = wishlistIds.any { it.equals(firstItem.id, ignoreCase = true) }
                    Icon(
                        imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) Color.Red else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = SleekTextPrimary, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = rarityName.uppercase(),
                color = if (isRealBundle) Color.White else if (getRarityTextColor(rarityColor) == Color.White) Color.White else rarityColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = if (isRealBundle) Modifier.background(SleekPrimary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp) else Modifier
            )
            if (!isRealBundle) {
                firstItem?.type?.displayValue?.let { typeVal ->
                    Text(" • ", color = SleekTextMuted)
                    Text(typeVal, color = SleekTextSecondary, fontSize = 14.sp)
                }
            }
        }

        if (!isRealBundle) {
            firstItem?.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        } else {
            // For bundles, we could show a generic bundle description if available, 
            // but the API doesn't usually provide one beyond the 'info' field.
            val bundleDesc = if (!entry.bundle?.info.isNullOrBlank()) "Fortnite Bundle containing ${itemsList.size} items." else ""
            if (bundleDesc.isNotBlank()) {
                Text(text = bundleDesc, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary, modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        val isTrack = firstItem?.id?.startsWith("sid_", true) == true || firstItem?.type?.displayValue?.contains("Track", true) == true
        val isMusicPack = firstItem?.id?.startsWith("MusicPack_", true) == true

        // --- AUDIO/VIDEO ---
        if ((isTrack || isMusicPack) && firstItem?.id?.startsWith("JBSID_", true) == false) {
            if (isTrack && firstItem != null) {
                firstItem.previewUrl?.let { url ->
                    Spacer(modifier = Modifier.height(12.dp))
                    JamTrackPlayer(previewUrl = url)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            val artist = firstItem?.artist ?: ""
            val query = when {
                isTrack && artist.contains("Epic Games", ignoreCase = true) -> "Fortnite ${firstItem?.name} Jam Track -emote"
                isTrack -> "$artist ${firstItem?.name} official audio"
                else -> "Fortnite ${firstItem?.name} Music Pack"
            }
            if (isSearchingVideo) {
                Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SleekCyan, modifier = Modifier.size(24.dp))
                }
            } else {
                YouTubeButton(query = query, videoId = videoId)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- STYLES / VARIANTS ---
        if (itemsList.size == 1 && !itemsList.first().variants.isNullOrEmpty()) {
            val item = itemsList.first()
            if (!item.id.startsWith("JBSID_", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "AVAILABLE STYLES", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                item.variants?.forEach { variant ->
                    if (!variant.options.isNullOrEmpty()) {
                        Text(text = variant.type?.uppercase() ?: "VARIANT", fontSize = 11.sp, color = SleekCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                            items(variant.options) { option ->
                                Card(
                                    modifier = Modifier.size(80.dp).border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp)),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (!option.image.isNullOrEmpty()) {
                                            AsyncImage(model = option.image, contentDescription = option.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        }
                                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(vertical = 2.dp)) {
                                            Text(text = option.name ?: "Style", color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (itemsList.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Included Items (${itemsList.size}):", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(itemsList) { item ->
                    val itemImg = resolveIncludedItemImage(item)
                    val isItemOwned = ownedIds.contains(item.id.lowercase())
                    val grayScaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
                    val colorFilter = if (isItemOwned) ColorFilter.colorMatrix(grayScaleMatrix) else null
                    Card(
                        modifier = Modifier.width(110.dp).border(1.dp, if(isItemOwned) Color.Gray.copy(alpha = 0.3f) else SleekSurfaceBorder, RoundedCornerShape(8.dp)).clickable { onCosmeticClick(item) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = if(isItemOwned) SleekSurfaceVariant.copy(alpha = 0.5f) else SleekSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(70.dp).background(SleekSurface)) {
                                if (!itemImg.isNullOrEmpty()) {
                                    AsyncImage(model = itemImg, contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = colorFilter)
                                }
                                if (isItemOwned) {
                                    Box(modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.7f)).fillMaxWidth()) {
                                        Text("OWNED", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = cleanShopTitle(item.name), fontSize = 10.sp, color = if(isItemOwned) SleekTextMuted else SleekTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOwned) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekEmerald, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "OWNED", style = MaterialTheme.typography.titleLarge, color = SleekEmerald, fontWeight = FontWeight.Black)
                } else {
                    Box(modifier = Modifier.size(24.dp).background(FortniteGold, CircleShape), contentAlignment = Alignment.Center) {
                        Text("V", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val ownedCount = itemsList.count { ownedIds.contains(it.id.lowercase()) }
                    val hasDiscount = isRealBundle && ownedCount > 0
                    Column {
                        Text(text = "$price V-Bucks", style = MaterialTheme.typography.titleLarge, color = if (hasDiscount) SleekEmerald else FortniteGold, fontWeight = FontWeight.Bold)
                        if (hasDiscount) {
                            Text(text = "Reduced Price (Owned $ownedCount/${itemsList.size})", fontSize = 10.sp, color = SleekEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary), shape = RoundedCornerShape(10.dp)) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
