package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.data.models.CosmeticSet
import com.dhyper.fncompanion.ui.components.JamTrackPlayer
import com.dhyper.fncompanion.ui.components.YouTubeButton
import com.dhyper.fncompanion.ui.components.getRarityColor
import com.dhyper.fncompanion.ui.components.getRarityTextColor
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import com.dhyper.fncompanion.ui.viewmodels.CosmeticSortOption
import com.dhyper.fncompanion.ui.viewmodels.CosmeticsUiState
import com.dhyper.fncompanion.ui.viewmodels.CosmeticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmeticsScreen(
    viewModel: CosmeticsViewModel,
    shopViewModel: com.dhyper.fncompanion.ui.viewmodels.ShopViewModel? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val shopState by shopViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
    
    var searchText by remember { mutableStateOf("") }
    
    // Sync local typing to ViewModel with background processing
    LaunchedEffect(searchText) {
        viewModel.updateSearch(searchText)
    }

    var selectedCosmetic by remember { mutableStateOf<CosmeticItem?>(null) }
    var selectedSet by remember { mutableStateOf<CosmeticSet?>(null) }
    
    val categories = listOf(
        "All", "Outfit", "Back Bling", "Pickaxe", "Glider", "Emote", "Wrap", 
        "Contrail", "Music", "Loading Screen", "Emoticon", "Spray", "Sidekick", 
        "Jam Track", "Banner", "Kicks", "Car", "Car Decal", "Wheels", "Car Trail", 
        "Car Boost", "Guitar", "Bass", "Drums", "Keytar", "Mic", "Lego Build", 
        "Lego Decor", "Aura"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search all 10,000+ cosmetics...", color = SleekTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SleekCyan) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface,
                focusedBorderColor = SleekPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = if (uiState is CosmeticsUiState.Success) (uiState as CosmeticsUiState.Success).selectedCategory == cat else cat == "All"
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectCategory(cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SleekPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tools Bar: Wishlist Toggle & Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState is CosmeticsUiState.Success) {
                val state = uiState as CosmeticsUiState.Success
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = state.wishlistOnly,
                        onClick = { viewModel.toggleWishlistOnly() },
                        label = { Text("My Wishlist", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Red.copy(alpha = 0.8f),
                            selectedLabelColor = Color.White
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Showing ${state.filteredItems.size} items",
                        fontSize = 11.sp,
                        color = SleekTextMuted
                    )
                }

                Box {
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SleekSurfaceBorder)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(14.dp), tint = SleekCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(state.sortOption.displayName, fontSize = 11.sp, color = SleekTextPrimary)
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(SleekSurfaceVariant)
                    ) {
                        CosmeticSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName, fontSize = 13.sp, color = if(option == state.sortOption) SleekCyan else SleekTextPrimary) },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = uiState) {
            is CosmeticsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SleekCyan)
                }
            }
            is CosmeticsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is CosmeticsUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.filteredItems, key = { it.id }) { item ->
                            val isOwned = state.ownedIds.contains(item.id.lowercase())
                            val isWishlisted = state.wishlistIds.any { it.equals(item.id, ignoreCase = true) }
                            val inShop = if (shopState is com.dhyper.fncompanion.ui.viewmodels.ShopUiState.Success) {
                                (shopState as com.dhyper.fncompanion.ui.viewmodels.ShopUiState.Success).shopItemIds.contains(item.id.lowercase())
                            } else false

                            CosmeticBrowserCard(
                                item = item,
                                isWishlisted = isWishlisted,
                                isOwned = isOwned,
                                inShop = inShop,
                                onWishlistToggle = { viewModel.toggleWishlist(item) },
                                onClick = { selectedCosmetic = item }
                            )
                        }
                    }
                    
                    // Pagination Controls
                    PaginationBar(
                        currentPage = state.currentPage,
                        totalPages = state.totalPages,
                        onPageSelect = { viewModel.setPage(it) }
                    )
                }
            }
        }
    }

    // Detail Modal BottomSheet
    selectedCosmetic?.let { item ->
        val state = uiState as? CosmeticsUiState.Success
        val videoId by viewModel.selectedVideoId.collectAsState()
        val isSearchingVideo by viewModel.isSearchingVideo.collectAsState()

        LaunchedEffect(item.id) {
            val isTrack = item.type?.displayValue?.contains("Track", ignoreCase = true) == true || 
                          item.id.startsWith("sid_", ignoreCase = true)
            val isMusicPack = item.id.startsWith("MusicPack_", ignoreCase = true)
            if (isTrack || isMusicPack) {
                viewModel.searchYouTubeForItem(item)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCosmetic = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SleekSurface
        ) {
            val isOwned = state?.ownedIds?.contains(item.id.lowercase()) ?: false
            val isWishlisted = state?.wishlistIds?.any { it.equals(item.id, ignoreCase = true) } ?: false
            
            CosmeticDetailSheet(
                item = item,
                isOwned = isOwned,
                isWishlisted = isWishlisted,
                videoId = videoId,
                isSearchingVideo = isSearchingVideo,
                onWishlistToggle = { viewModel.toggleWishlist(item) },
                onSetClick = { 
                    selectedSet = item.set
                    selectedCosmetic = null
                },
                onClose = { selectedCosmetic = null }
            )
        }
    }

    // Set Detail Popup
    selectedSet?.let { cosmeticSet ->
        val state = uiState as? CosmeticsUiState.Success
        val setItemResult = remember(cosmeticSet) { viewModel.getItemsInSet(cosmeticSet.value ?: "") }

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
                Text(
                    text = "All items in this set",
                    style = MaterialTheme.typography.labelMedium,
                    color = SleekTextMuted
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    items(setItemResult) { item ->
                        CosmeticBrowserCard(
                            item = item,
                            isWishlisted = state?.wishlistIds?.contains(item.id) ?: false,
                            isOwned = state?.ownedIds?.contains(item.id.lowercase()) ?: false,
                            onWishlistToggle = { viewModel.toggleWishlist(item) },
                            onClick = { 
                                selectedCosmetic = item
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
                    Text("Back to Browser")
                }
            }
        }
    }
}

@Composable
fun CosmeticDetailSheet(
    item: CosmeticItem,
    isOwned: Boolean,
    isWishlisted: Boolean,
    videoId: String?,
    isSearchingVideo: Boolean,
    onWishlistToggle: () -> Unit,
    onSetClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val rarityColor = getRarityColor(item.rarity?.value ?: "")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(rarityColor.copy(alpha = 0.4f), SleekSurfaceVariant)))
        ) {
            val detailIcon = item.images?.large ?: 
                             item.images?.legoLarge ?:
                             item.images?.featured ?: 
                             item.images?.icon ?: 
                             item.images?.smallIcon ?:
                             item.images?.small
                             
            AsyncImage(
                model = detailIcon,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.headlineMedium,
            color = SleekTextPrimary,
            fontWeight = FontWeight.Black
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = item.rarity?.displayValue?.uppercase() ?: "COMMON",
                color = if (getRarityTextColor(rarityColor) == Color.White) Color.White else rarityColor,
                fontWeight = FontWeight.Bold
            )
            Text(" • ", color = SleekTextMuted)
            Text(item.type?.displayValue ?: "Other", color = SleekTextSecondary)
        }

        item.description?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = SleekTextSecondary, modifier = Modifier.padding(vertical = 8.dp))
        }

        val isTrack = item.type?.displayValue?.contains("Track", ignoreCase = true) == true || 
                      item.type?.value?.contains("Track", ignoreCase = true) == true ||
                      item.id.startsWith("sid_", ignoreCase = true)
        val isMusicPack = item.id.startsWith("MusicPack_", ignoreCase = true)

        // --- AUDIO/VIDEO ---
        if ((isTrack || isMusicPack) && item.id.startsWith("CID_", ignoreCase = true) == false && !item.id.startsWith("JBSID_", ignoreCase = true)) {
            // 1. Prioritize 30s Official Preview (for tracks)
            if (isTrack) {
                item.previewUrl?.let { url ->
                    Spacer(modifier = Modifier.height(12.dp))
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
        }

        // --- STYLES / VARIANTS ---
        if (!item.variants.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AVAILABLE STYLES",
                style = MaterialTheme.typography.titleMedium,
                color = SleekTextPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            item.variants.forEach { variant ->
                if (!variant.options.isNullOrEmpty()) {
                    Text(
                        text = variant.type?.uppercase() ?: "VARIANT",
                        fontSize = 11.sp,
                        color = SleekCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(variant.options) { option ->
                            Card(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (!option.image.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = option.image,
                                            contentDescription = option.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = option.name ?: "Style",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekSurfaceBorder)

        CosmeticDetailRow("Item ID", item.id)
        
        // --- JAM TRACK METADATA ---
        if (isTrack) {
            item.bpm?.let { CosmeticDetailRow("BPM", it.toString()) }
            item.duration?.let { 
                val mins = it / 60
                val secs = it % 60
                CosmeticDetailRow("Duration", String.format(java.util.Locale.US, "%d:%02d", mins, secs))
            }
        }

        item.introduction?.let {
            CosmeticDetailRow("Introduced", SeasonUtils.getFormattedIntroduction(it.chapter, it.season))
        }
        item.set?.let { set ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSetClick(set.value ?: "") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set", modifier = Modifier.width(100.dp), color = SleekTextMuted, fontSize = 13.sp)
                Text(set.text ?: "None", color = SleekCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        CosmeticDetailRow("Added", item.added?.substringBefore("T") ?: "Unknown")

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isOwned) {
                Button(
                    onClick = onWishlistToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isWishlisted) Color.Gray else Color.Red)
                ) {
                    Icon(if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isWishlisted) "Remove Wishlist" else "Add to Wishlist")
                }
            }
            Button(onClick = onClose, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)) {
                Text("Close")
            }
        }
    }
}

@Composable
fun CosmeticDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(100.dp), color = SleekTextMuted, fontSize = 13.sp)
        Text(value, color = SleekTextPrimary, fontSize = 13.sp)
    }
}

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onPageSelect(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Page",
                tint = if (currentPage > 1) SleekCyan else SleekTextMuted
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Page $currentPage of $totalPages",
            color = SleekTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
            onClick = { onPageSelect(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Page",
                tint = if (currentPage < totalPages) SleekCyan else SleekTextMuted
            )
        }
    }
}

@Composable
fun CosmeticBrowserCard(
    item: CosmeticItem,
    isWishlisted: Boolean,
    isOwned: Boolean,
    inShop: Boolean = false,
    onWishlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    val rarityColor = getRarityColor(item.series?.value ?: item.rarity?.value ?: "")
    
    val grayScaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val colorFilter = if (isOwned) ColorFilter.colorMatrix(grayScaleMatrix) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp, 
                if(isOwned) Color.Gray.copy(alpha = 0.3f) 
                else if(inShop) FortniteGold 
                else rarityColor.copy(alpha = 0.6f), 
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isOwned) SleekSurfaceVariant.copy(alpha = 0.5f) else SleekSurfaceVariant)
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            if(isOwned) Brush.verticalGradient(listOf(Color.Gray.copy(alpha = 0.2f), Color.Transparent))
                            else Brush.verticalGradient(listOf(rarityColor.copy(alpha = 0.4f), Color.Transparent))
                        )
                ) {
                    val iconToLoad = when {
                        item.type?.value?.contains("lego", true) == true || item.id.startsWith("JBSID_", true) -> 
                            item.images?.legoSmall ?: item.images?.lego?.small ?: item.images?.lego?.large ?: item.images?.lego?.icon ?: item.images?.large ?: item.images?.small ?: item.images?.icon
                        item.type?.value?.contains("track", true) == true || item.id.startsWith("sid_", true) -> 
                            item.images?.coverart ?: item.images?.albumArt ?: item.images?.other?.albumArt ?: item.images?.featured
                        item.type?.value?.contains("car", true) == true || item.type?.value?.contains("wheel", true) == true || 
                        item.type?.value?.contains("boost", true) == true || item.type?.value?.contains("trail", true) == true ->
                            item.images?.featured ?: item.images?.decal ?: item.images?.large ?: item.images?.icon ?: item.images?.smallIcon
                        item.type?.value?.contains("instrument", true) == true || item.id.startsWith("Sparks_", true) ||
                        item.type?.value?.contains("guitar", true) == true || item.type?.value?.contains("bass", true) == true ||
                        item.type?.value?.contains("drum", true) == true || item.type?.value?.contains("mic", true) == true ->
                            item.images?.large ?: item.images?.small ?: item.images?.featured ?: item.images?.icon
                        else -> 
                            item.images?.icon ?: item.images?.smallIcon ?: item.images?.largeIcon ?: item.images?.featured
                    } ?: item.images?.icon_background ?: item.images?.other?.background ?: item.images?.background ?: item.images?.full_background

                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(iconToLoad)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter
                    )
                    
                    if (isOwned) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .fillMaxWidth()
                        ) {
                            Text(
                                "OWNED",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            )
                        }
                    } else if (inShop) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("IN SHOP", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = item.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(isOwned) SleekTextMuted else SleekTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.type?.displayValue ?: "Other",
                        fontSize = 10.sp,
                        color = SleekTextMuted
                    )
                }
            }

            // Wishlist Toggle
            if (!isOwned) {
                IconButton(
                    onClick = onWishlistToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
