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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    var selectedEntryForDetail by remember { mutableStateOf<ShopEntry?>(null) }
    var selectedCosmeticDetail by remember { mutableStateOf<com.dhyper.fncompanion.data.models.CosmeticItem?>(null) }
    var selectedSet by remember { mutableStateOf<com.dhyper.fncompanion.data.models.CosmeticSet?>(null) }

    val categories = listOf(
        "All", "Bundles", 
        // Battle Royale (Primary)
        "Outfit", "Back Bling", "Pickaxe", "Glider", "Emote", "Wrap", "Contrail", "Music", "Aura",
        // Vehicles (Rocket Racing) - Grouped Together
        "Vehicles", "Car", "Car Decal", "Wheels", "Car Trail", "Car Boost",
        // Festival
        "Jam Track", "Guitar", "Bass", "Drums", "Keytar", "Mic",
        // LEGO & Misc
        "Lego Build", "Lego Decor", "Loading Screen", "Emoticon", "Spray", "Sidekick", "Banner", "Kicks"
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
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("shop_search_input"),
            placeholder = { Text("Search Item Shop...", color = SleekTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SleekCyan) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
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
                            val currentCategory = entry.section?.name ?: entry.layout?.name ?: "Special Offers"
                            val previousCategory = if (index > 0) {
                                val prev = state.filteredEntries[index - 1]
                                prev.section?.name ?: prev.layout?.name ?: "Special Offers"
                            } else null
                            
                            if (index == 0 || currentCategory != previousCategory) {
                                item(span = { GridItemSpan(2) }) {
                                    ShopCategoryHeader(currentCategory)
                                }
                            }
                            
                            item {
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

    // Detail Modal BottomSheet
    selectedEntryForDetail?.let { entry ->
        val successState = uiState as? ShopUiState.Success
        val allItems = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList()) + (entry.cars ?: emptyList()) + (entry.vehicles ?: emptyList()) + (entry.instruments ?: emptyList())
        val tracksAsCosmetics = entry.tracks?.map { t ->
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
            com.dhyper.fncompanion.data.models.CosmeticItem(
                id = realId,
                name = trackMap?.get("title")?.toString() ?: t.title ?: t.devName ?: "Track",
                description = null, type = null, rarity = null, series = null, images = null, introduction = null, set = null, added = null
            )
        } ?: emptyList()
        val itemsList = allItems + tracksAsCosmetics
        val isOwned = itemsList.isNotEmpty() && itemsList.all { successState?.ownedIds?.contains(it.id.lowercase()) == true }
        
        val videoId by cosmeticsViewModel.selectedVideoId.collectAsState()
        val isSearchingVideo by cosmeticsViewModel.isSearchingVideo.collectAsState()

        // Trigger search for single track or music pack entries
        LaunchedEffect(entry.offerId) {
            val tracks = entry.tracks ?: emptyList()
            val musicPacks = entry.items?.filter { it.id.startsWith("MusicPack_", ignoreCase = true) } ?: emptyList()
            
            if (tracks.size == 1) {
                val t = tracks.first()
                val dummy = com.dhyper.fncompanion.data.models.CosmeticItem(
                    id = t.id ?: "",
                    name = t.title ?: t.devName ?: "Track",
                    artist = t.artist,
                    description = null, type = null, rarity = null, series = null, images = null, introduction = null, set = null, added = null
                )
                cosmeticsViewModel.searchYouTubeForItem(dummy)
            } else if (musicPacks.size == 1) {
                cosmeticsViewModel.searchYouTubeForItem(musicPacks.first())
            }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedEntryForDetail = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ShopItemDetailSheet(
                entry = entry,
                wishlistIds = successState?.wishlistIds ?: emptySet(),
                ownedIds = successState?.ownedIds ?: emptySet(),
                isOwned = isOwned,
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
                        CosmeticBrowserCard(
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
    val allItems = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList()) + (entry.cars ?: emptyList()) + (entry.vehicles ?: emptyList()) + (entry.instruments ?: emptyList())
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
    if (!entry.bundle?.image.isNullOrBlank()) return entry.bundle?.image

    val trackAlbumArt = entry.tracks?.firstOrNull()?.albumArt
    if (!trackAlbumArt.isNullOrBlank()) return trackAlbumArt

    val newAssetImg = entry.newDisplayAsset?.materialInstances?.firstOrNull()?.images?.let { imgMap ->
        imgMap["Background"] ?: imgMap["FullBackground"] ?: imgMap["OfferImage"] ?: imgMap["Texture"] ?: imgMap["Image"] ?: imgMap["Icon"] ?: imgMap["DisplayAsset"]
    }
    if (!newAssetImg.isNullOrBlank()) return newAssetImg

    val displayAssetImg = entry.displayAssets?.firstOrNull()?.let { da ->
        da.full_background ?: da.background ?: da.url
    }
    if (!displayAssetImg.isNullOrBlank()) return displayAssetImg

    val allItems = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList()) + (entry.cars ?: emptyList()) + (entry.vehicles ?: emptyList()) + (entry.instruments ?: emptyList())
    val firstItem = allItems.firstOrNull()

    val featured = firstItem?.images?.featured
    if (!featured.isNullOrBlank()) return featured

    val fullBg = firstItem?.images?.full_background
    if (!fullBg.isNullOrBlank()) return fullBg

    val bg = firstItem?.images?.background
    if (!bg.isNullOrBlank()) return bg

    val icon = firstItem?.images?.icon
    if (!icon.isNullOrBlank()) return icon

    val smallIcon = firstItem?.images?.smallIcon
    if (!smallIcon.isNullOrBlank()) return smallIcon

    if (!firstItem?.id.isNullOrBlank()) {
        return "https://fortnite-api.com/images/cosmetics/br/${firstItem?.id}/icon.png"
    }

    return null
}

@Composable
fun ShopCategoryHeader(category: String) {
    val isModeHeader = category.contains("Racing", ignoreCase = true) || 
                      category.contains("Festival", ignoreCase = true) || 
                      category.contains("LEGO", ignoreCase = true)
    
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
        Text(
            text = category.uppercase(),
            style = if (isModeHeader) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            color = if (isModeHeader) SleekEmerald else SleekCyan,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
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
    val trackItems = entry.tracks?.map { t ->
        val trackMap = t.track as? Map<*, *>
        val apiCosmeticId = trackMap?.get("id")?.toString()
        val realId = apiCosmeticId ?: t.id ?: ""
        com.dhyper.fncompanion.data.models.CosmeticItem(id = realId, name = t.title ?: "", description = null, type = null, rarity = null, series = null, images = null, introduction = null, set = null, added = null)
    } ?: emptyList()

    val allItems = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList()) + (entry.cars ?: emptyList()) + (entry.vehicles ?: emptyList()) + (entry.instruments ?: emptyList()) + trackItems
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
    // Removed duplicate itemCount line

    val grayScaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val colorFilter = if (isFullyOwned) ColorFilter.colorMatrix(grayScaleMatrix) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, if(isFullyOwned) Color.Gray.copy(alpha = 0.3f) else rarityColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
                            model = imageUrl,
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
                            color = if(isFullyOwned) SleekTextMuted else rarityColor,
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
    isOwned: Boolean = false,
    videoId: String? = null,
    isSearchingVideo: Boolean = false,
    onWishlistToggle: ((com.dhyper.fncompanion.data.models.CosmeticItem) -> Unit)? = null,
    onCosmeticClick: (com.dhyper.fncompanion.data.models.CosmeticItem) -> Unit,
    onClose: () -> Unit
) {
    val allItems = (entry.items ?: emptyList()) + (entry.brItems ?: emptyList()) + (entry.cars ?: emptyList()) + (entry.vehicles ?: emptyList()) + (entry.instruments ?: emptyList())
    
    // Add tracks to the items list for display
    val tracksAsCosmetics = entry.tracks?.map { t ->
        val trackMap = t.track as? Map<*, *>
        
        // 1. Try to get cosmetic ID from the nested track metadata first
        val apiCosmeticId = trackMap?.get("id")?.toString()
        
        // 2. Try to extract sid_ from devName (very reliable for Jam Tracks)
        val sidFromDevName = Regex("""sid_[a-zA-Z0-9_]+""").find(t.devName ?: "")?.value
        
        // 3. Fallback to the ID field only if it doesn't look like an offer ID
        val idField = t.id ?: ""
        val realId = when {
            !apiCosmeticId.isNullOrBlank() -> apiCosmeticId
            !sidFromDevName.isNullOrBlank() -> sidFromDevName
            !idField.startsWith("v2:/") -> idField
            else -> idField
        }
        
        val title = trackMap?.get("title")?.toString() ?: t.title ?: t.devName ?: "Track"
        val artist = trackMap?.get("artist")?.toString() ?: t.artist ?: "Unknown Artist"
        val album = trackMap?.get("album")?.toString() ?: t.album
        val albumArt = t.albumArt
        val previewUrl = trackMap?.get("previewUrl")?.toString() ?: t.previewUrl
        val bpm = (trackMap?.get("bpm") as? Number)?.toInt() ?: t.bpm
        val duration = (trackMap?.get("duration") as? Number)?.toInt() ?: t.duration

        val albumName = if (album.isNullOrBlank() || album.contains("unknown", ignoreCase = true)) "" else " from $album"
        
        com.dhyper.fncompanion.data.models.CosmeticItem(
            id = realId,
            name = title,
            description = "Jam Track by $artist$albumName",
            type = com.dhyper.fncompanion.data.models.CosmeticType("Track", "Jam Track"),
            rarity = com.dhyper.fncompanion.data.models.CosmeticRarity("Festival", "Festival"),
            series = null,
            images = com.dhyper.fncompanion.data.models.CosmeticImages(albumArt, albumArt, albumArt, null, null, albumArt),
            introduction = null,
            set = null,
            added = null,
            previewUrl = previewUrl,
            artist = artist,
            album = album,
            bpm = bpm,
            duration = duration
        )
    } ?: emptyList()

    val itemsList = allItems + tracksAsCosmetics
    val firstItem = itemsList.firstOrNull()
    val rarityName = firstItem?.rarity?.value ?: firstItem?.series?.value ?: if (!entry.tracks.isNullOrEmpty()) "Festival" else "Common"
    val rarityColor = getRarityColor(rarityName)
    val price = entry.finalPrice ?: entry.regularPrice ?: 0
    val title = getShopEntryTitle(entry)
    val imageUrl = getShopEntryImage(entry)
    
    val isRealBundle = entry.bundle != null || itemsList.size > 1 || title.contains("Bundle", ignoreCase = true)

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
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
                    // Wishlist toggle in detail view - Disabled for bundles
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

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = SleekTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = rarityName.uppercase(),
                color = rarityColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            firstItem?.type?.displayValue?.let { typeVal ->
                Text(" • ", color = SleekTextMuted)
                Text(typeVal, color = SleekTextSecondary, fontSize = 14.sp)
            }
        }

        firstItem?.description?.let { desc ->
            if (desc.isNotBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        firstItem?.introduction?.text?.let { intro ->
            Text(
                text = intro,
                style = MaterialTheme.typography.labelMedium,
                color = SleekCyan,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // --- ENHANCED DETAILS (Locker/Wishlist Style) ---
        if (!isRealBundle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(SleekSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                val item = itemsList.firstOrNull()
                DetailRow("Item ID", item?.id ?: "Unknown")
                
                // Jam Track Metadata - ONLY if not a bundle
                if (entry.tracks?.isNotEmpty() == true) {
                    val t = entry.tracks!!.first()
                    val trackMap = t.track as? Map<*, *>
                    val bpm = (trackMap?.get("bpm") as? Number)?.toInt() ?: t.bpm
                    val duration = (trackMap?.get("duration") as? Number)?.toInt() ?: t.duration
                    
                    bpm?.let { DetailRow("BPM", it.toString()) }
                    duration?.let { 
                        val mins = it / 60
                        val secs = it % 60
                        DetailRow("Duration", String.format(java.util.Locale.US, "%d:%02d", mins, secs))
                    }
                }

                item?.introduction?.let {
                    DetailRow("Introduced", SeasonUtils.getFormattedIntroduction(it.chapter, it.season))
                }
                
                item?.set?.text?.let { DetailRow("Set", it) }
                item?.added?.let { DetailRow("Added", it.substringBefore("T")) }
                
                // Ownership Price Badge
                Spacer(modifier = Modifier.height(8.dp))
                if (isOwned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SleekEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OWNED", color = SleekEmerald, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(16.dp).background(FortniteGold, CircleShape), contentAlignment = Alignment.Center) {
                            Text("V", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$price V-Bucks", color = FortniteGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // --- SINGLE ITEM JAM TRACK OR MUSIC PACK SUPPORT ---
        if (itemsList.size == 1) {
            val item = itemsList.first()
            val isTrack = item.id.startsWith("sid_", ignoreCase = true) || 
                          item.type?.displayValue?.contains("Track", ignoreCase = true) == true
            val isMusicPack = item.id.startsWith("MusicPack_", ignoreCase = true)
            
            if (isTrack || isMusicPack) {
                // 1. Prioritize 30s Official Preview (for tracks)
                if (isTrack) {
                    item.previewUrl?.let { url ->
                        JamTrackPlayer(previewUrl = url)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                // 2. Direct Link to YouTube
                val artist = item.artist ?: ""
                val query = when {
                    isTrack && artist.contains("Epic Games", ignoreCase = true) -> 
                        "Fortnite ${item.name} Jam Track -emote"
                    isTrack -> 
                        "$artist ${item.name} official audio"
                    else -> "Fortnite ${item.name} Music Pack"
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
        }

        if (itemsList.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Included Items (${itemsList.size}):",
                style = MaterialTheme.typography.titleMedium,
                color = SleekTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(itemsList) { item ->
                    val itemImg = item.images?.featured ?: item.images?.icon ?: item.images?.smallIcon
                    val isItemOwned = ownedIds.contains(item.id.lowercase())
                    val grayScaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
                    val colorFilter = if (isItemOwned) ColorFilter.colorMatrix(grayScaleMatrix) else null
                    
                    Card(
                        modifier = Modifier
                            .width(110.dp)
                            .border(1.dp, if(isItemOwned) Color.Gray.copy(alpha = 0.3f) else SleekSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { onCosmeticClick(item) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = if(isItemOwned) SleekSurfaceVariant.copy(alpha = 0.5f) else SleekSurfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(SleekSurface)
                            ) {
                                if (!itemImg.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = itemImg,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        colorFilter = colorFilter
                                    )
                                }
                                
                                if (isItemOwned) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            "OWNED",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cleanShopTitle(item.name),
                                fontSize = 10.sp,
                                color = if(isItemOwned) SleekTextMuted else SleekTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOwned) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekEmerald, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OWNED",
                        style = MaterialTheme.typography.titleLarge,
                        color = SleekEmerald,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(FortniteGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("V", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val ownedCount = itemsList.count { ownedIds.contains(it.id.lowercase()) }
                    val hasDiscount = isRealBundle && ownedCount > 0
                    
                    Column {
                        Text(
                            text = "$price V-Bucks",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (hasDiscount) SleekEmerald else FortniteGold,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasDiscount) {
                            Text(
                                text = "Reduced Price (Owned $ownedCount/${itemsList.size})",
                                fontSize = 10.sp,
                                color = SleekEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
