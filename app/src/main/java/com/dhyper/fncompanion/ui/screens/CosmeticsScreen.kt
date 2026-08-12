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
import com.dhyper.fncompanion.ui.components.CosmeticDetailSheet
import com.dhyper.fncompanion.ui.components.resolveCosmeticIcon
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
        "All", "Outfits", "Emotes", "Pickaxes", "Backblings", "Gliders", 
        "Sidekicks", "Kicks", "Wraps", "Loading Screens", "Music Packs", "Contrails",
        "Sprays", "Emojis", "Banners",
        "Auras", "Jam Tracks", "Guitars", "Basses", "Drums", "Keytars", "Mics",
        "Car Bodies", "Car Decals", "Car Wheels", "Car Trails", "Car Boosts",
        "Lego Builds", "Lego Decors"
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
                                onClick = { 
                                    selectedCosmetic = item
                                    viewModel.loadDetailedItem(item)
                                }
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
    val detailedItem by viewModel.detailedItem.collectAsState()
    
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
            onDismissRequest = { 
                selectedCosmetic = null
                viewModel.clearDetailedItem()
            },
            sheetState = rememberModalBottomSheetState(),
            containerColor = SleekSurface
        ) {
            val isOwned = state?.ownedIds?.contains(item.id.lowercase()) ?: false
            val isWishlisted = state?.wishlistIds?.any { it.equals(item.id, ignoreCase = true) } ?: false
            
            // Use detailedItem if it matches the current selection, otherwise fallback to basic item
            val itemToDisplay = if (detailedItem?.id == item.id) detailedItem!! else item

            CosmeticDetailSheet(
                item = itemToDisplay,
                isOwned = isOwned,
                isWishlisted = isWishlisted,
                videoId = videoId,
                isSearchingVideo = isSearchingVideo,
                onWishlistToggle = { viewModel.toggleWishlist(item) },
                onSetClick = { 
                    selectedSet = item.set
                    selectedCosmetic = null
                    viewModel.clearDetailedItem()
                },
                onClose = { 
                    selectedCosmetic = null
                    viewModel.clearDetailedItem()
                }
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
                    val iconToLoad = resolveCosmeticIcon(item)

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
