package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.data.models.CosmeticSet
import com.dhyper.fncompanion.data.models.LockerCategory
import com.dhyper.fncompanion.data.models.ParsedLockerItem
import com.dhyper.fncompanion.ui.components.CosmeticDetailSheet
import com.dhyper.fncompanion.ui.components.JamTrackPlayer
import com.dhyper.fncompanion.ui.components.YouTubeButton
import com.dhyper.fncompanion.ui.components.getRarityColor
import com.dhyper.fncompanion.ui.components.getRarityTextColor
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.utils.SeasonUtils
import com.dhyper.fncompanion.ui.viewmodels.LockerSortOption
import com.dhyper.fncompanion.ui.viewmodels.LockerUiState
import com.dhyper.fncompanion.ui.viewmodels.PersonalLockerViewModel
import com.dhyper.fncompanion.ui.viewmodels.CosmeticsViewModel
import com.dhyper.fncompanion.ui.utils.FileSharingUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalLockerScreen(
    authState: AuthState,
    viewModel: PersonalLockerViewModel,
    cosmeticsViewModel: CosmeticsViewModel, // Added parameter
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportedBitmap by viewModel.exportedBitmap.collectAsState()
    
    var selectedItemForDetail by remember { mutableStateOf<ParsedLockerItem?>(null) }
    var selectedSet by remember { mutableStateOf<CosmeticSet?>(null) }
    val context = LocalContext.current

    val session = when (authState) {
        is AuthState.Active -> authState.session
        is AuthState.TokenRefreshing -> authState.session
        is AuthState.TokenExpired -> authState.session
        is AuthState.NetworkError -> authState.session
        is AuthState.DecryptionError -> authState.session
        is AuthState.ReauthRequired -> authState.session
        else -> null
    }

    LaunchedEffect(session) {
        viewModel.loadLocker(session)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(12.dp)
    ) {
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SleekPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Epic Games Account Required",
                            style = MaterialTheme.typography.titleLarge,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "To browse and filter your personal Fortnite Athena Locker cosmetics directly from Epic Games servers, connect your account.",
                            color = SleekTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateToAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Connect Epic Games Account", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            when (val state = uiState) {
                is LockerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SleekCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading Athena Locker for ${session.displayName}...", color = SleekTextSecondary)
                        }
                    }
                }
                is LockerUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Locker Load Error",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = state.message, color = SleekTextSecondary, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadLocker(session) },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is LockerUiState.Success -> {
                    // Header Bar with V-Bucks Balance
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = session.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total Locker Items: ${state.allItems.size}",
                                    fontSize = 12.sp,
                                    color = SleekTextSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(FortniteGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("V", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${state.vbucksBalance}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = FortniteGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = { viewModel.toggleFavoritesOnly() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (state.favoritesOnly) FortniteGold.copy(alpha = 0.2f) else SleekPrimary.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (state.favoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorites",
                                        tint = if (state.favoritesOnly) FortniteGold else SleekTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search and Sorting bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f),
                            placeholder = { Text("Search Locker Items...", color = SleekTextMuted, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SleekCyan) },
                            trailingIcon = if (state.searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = SleekTextMuted)
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

                        Spacer(modifier = Modifier.width(8.dp))

                        // Sort dropdown button
                        SortDropdownButton(
                            currentSort = state.sortOption,
                            onSelectSort = { viewModel.setSortOption(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Chips
                    val categories = listOf(
                        LockerCategory.OUTFIT to "Outfits",
                        LockerCategory.EMOTE to "Emotes",
                        LockerCategory.PICKAXE to "Pickaxes",
                        LockerCategory.BACK_BLING to "Backblings",
                        LockerCategory.GLIDER to "Gliders",
                        LockerCategory.SIDEKICK to "Sidekicks",
                        LockerCategory.KICKS to "Kicks",
                        LockerCategory.WRAP to "Wraps",
                        LockerCategory.LOADING_SCREEN to "Loading Screens",
                        LockerCategory.MUSIC to "Music Packs",
                        LockerCategory.CONTRAIL to "Contrails",
                        LockerCategory.SPRAY to "Sprays",
                        LockerCategory.EMOTICON to "Emojis",
                        LockerCategory.BANNER to "Banners",
                        LockerCategory.AURA to "Auras",
                        LockerCategory.JAM_TRACK to "Jam Tracks",
                        LockerCategory.GUITAR to "Guitars",
                        LockerCategory.BASS to "Basses",
                        LockerCategory.DRUMS to "Drums",
                        LockerCategory.KEYTAR to "Keytars",
                        LockerCategory.MIC to "Mics",
                        LockerCategory.CAR to "Car Bodies",
                        LockerCategory.CAR_DECAL to "Car Decals",
                        LockerCategory.WHEELS to "Car Wheels",
                        LockerCategory.CAR_TRAIL to "Car Trails",
                        LockerCategory.CAR_BOOST to "Car Boosts",
                        LockerCategory.LEGO_BUILD to "Lego Builds",
                        LockerCategory.LEGO_DECOR to "Lego Decors"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(categories) { (cat, label) ->
                            val isSelected = state.selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCategory(cat) },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Showing ${state.filteredItems.size} items",
                                fontSize = 13.sp,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.sortOption.displayName,
                                fontSize = 11.sp,
                                color = SleekTextMuted
                            )
                        }

                        Button(
                            onClick = { 
                                val title = "${session.displayName}'s ${state.selectedCategory?.name?.replace("_", " ") ?: "Locker"}"
                                viewModel.exportLockerImage(context, title) 
                            },
                            enabled = !isExporting && state.selectedCategory != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekCyan,
                                contentColor = Color.Black,
                                disabledContainerColor = SleekSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(progress = { exportProgress }, modifier = Modifier.size(20.dp), strokeWidth = 3.dp, color = Color.Black)
                                Spacer(Modifier.width(10.dp))
                                Text("${(exportProgress * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Black)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("GENERATE IMAGE", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            }
                        }
                    }

                    if (state.filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No locker cosmetics match your criteria.", color = SleekTextMuted)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.filteredItems, key = { it.templateId + it.cosmeticId }) { item ->
                                LockerItemCard(
                                    item = item,
                                    onClick = {
                                        selectedItemForDetail = item
                                    }
                                )
                            }
                        }
                    }

                    // Export Preview Dialog
                                    exportedBitmap?.let { bitmap ->
                        val filename = "FortniteLocker_${session?.displayName ?: "Export"}"
                        var isGeneratingFull by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()

                        AlertDialog(
                            onDismissRequest = { viewModel.clearExportedImage() },
                            title = { Text("Locker Export Preview", color = SleekTextPrimary, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 400.dp)
                                            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(8.dp)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                                contentScale = ContentScale.FillWidth
                                            )
                                            if (isGeneratingFull) {
                                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        CircularProgressIndicator(progress = { exportProgress }, color = SleekCyan)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text("${(exportProgress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text("This is a low-res preview. Share or Save to generate the full high-res image.", fontSize = 11.sp, color = SleekCyan, textAlign = TextAlign.Center)
                                }
                            },
                            confirmButton = {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { 
                                            scope.launch {
                                                isGeneratingFull = true
                                                val title = "${session?.displayName}'s Locker"
                                                val fullBitmap = viewModel.generateFullExport(context, title)
                                                if (fullBitmap != null) {
                                                    FileSharingUtils.shareBitmap(context, fullBitmap, filename)
                                                    fullBitmap.recycle()
                                                }
                                                isGeneratingFull = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isGeneratingFull,
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                                    ) {
                                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Share")
                                    }
                                    Button(
                                        onClick = { 
                                            scope.launch {
                                                isGeneratingFull = true
                                                val title = "${session?.displayName}'s Locker"
                                                val fullBitmap = viewModel.generateFullExport(context, title)
                                                if (fullBitmap != null) {
                                                    val saved = FileSharingUtils.saveBitmapToGallery(context, fullBitmap, filename)
                                                    if (saved) Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                                    else Toast.makeText(context, "Failed to save.", Toast.LENGTH_SHORT).show()
                                                    fullBitmap.recycle()
                                                }
                                                isGeneratingFull = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isGeneratingFull,
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekEmerald)
                                    ) {
                                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Save")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.clearExportedImage() }, enabled = !isGeneratingFull) {
                                    Text("Close", color = SleekTextMuted)
                                }
                            },
                            containerColor = SleekSurface,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // Item Detail Bottom Sheet
                    selectedItemForDetail?.let { item ->
                        val videoId by cosmeticsViewModel.selectedVideoId.collectAsState()
                        val isSearchingVideo by cosmeticsViewModel.isSearchingVideo.collectAsState()

                        LaunchedEffect(item.cosmeticId) {
                            val isTrack = item.category == LockerCategory.JAM_TRACK || item.templateId.contains("sid_", ignoreCase = true)
                            val isMusicPack = item.category == LockerCategory.MUSIC || item.templateId.contains("MusicPack_", ignoreCase = true)
                            if (isTrack || isMusicPack) {
                                // Convert locker item to cosmetic item for search
                                val dummy = CosmeticItem(
                                    id = item.cosmeticId,
                                    name = item.name,
                                    description = item.description,
                                    artist = item.artist,
                                    showcaseVideo = item.showcaseVideo,
                                    type = if (isMusicPack) com.dhyper.fncompanion.data.models.CosmeticType("Music", "Music Pack") else null,
                                    rarity = null, series = null, images = null, variants = null, introduction = null, set = null, added = null
                                )
                                cosmeticsViewModel.searchYouTubeForItem(dummy)
                            }
                        }

                        ModalBottomSheet(
                            onDismissRequest = { selectedItemForDetail = null },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = SleekSurface
                        ) {
                            // Convert locker item to cosmetic item for shared sheet
                            val detailItem = remember(item) {
                                CosmeticItem(
                                    id = item.cosmeticId,
                                    name = item.name,
                                    description = item.description,
                                    type = com.dhyper.fncompanion.data.models.CosmeticType(item.category.name, item.category.getDisplayName()),
                                    rarity = com.dhyper.fncompanion.data.models.CosmeticRarity(item.rarity, item.rarity),
                                    series = null,
                                    images = com.dhyper.fncompanion.data.models.CosmeticImages(
                                        smallIcon = item.iconUrl,
                                        largeIcon = item.largeIconUrl,
                                        featured = item.largeIconUrl,
                                        background = item.backgroundUrl,
                                        full_background = item.backgroundUrl,
                                        other = com.dhyper.fncompanion.data.models.OtherImages(
                                            albumArt = null,
                                            background = item.backgroundUrl,
                                            icon = item.iconUrl
                                        ),
                                        lego = com.dhyper.fncompanion.data.models.LegoImages(
                                            small = item.legoIconUrl,
                                            large = item.legoIconUrl,
                                            wide = null
                                        ),
                                        bean = com.dhyper.fncompanion.data.models.BeanImages(
                                            small = item.beanIconUrl,
                                            large = item.beanIconUrl
                                        )
                                    ),
                                    variants = item.variants,
                                    introduction = item.introduction,
                                    set = item.set,
                                    added = item.added,
                                    artist = item.artist,
                                    showcaseVideo = item.showcaseVideo,
                                    previewUrl = item.previewUrl,
                                    bpm = item.bpm,
                                    duration = item.duration
                                )
                            }

                            CosmeticDetailSheet(
                                item = detailItem,
                                isOwned = true,
                                isWishlisted = false,
                                videoId = videoId,
                                isSearchingVideo = isSearchingVideo,
                                ownedIds = state.allItems.map { it.cosmeticId.lowercase() }.toSet(),
                                onWishlistToggle = { },
                                onSetClick = { setId ->
                                    selectedSet = item.set
                                    selectedItemForDetail = null
                                },
                                onClose = { selectedItemForDetail = null }
                            )
                        }
                    }

                    // Set Detail Popup
                    selectedSet?.let { cosmeticSet ->
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
                                    items(setItemResult) { item ->
                                        // Locker items are ParsedLockerItem, while set detail results are CosmeticItem.
                                        // Use the shared browser card style.
                                        val isOwned = state.allItems.any { it.cosmeticId.equals(item.id, ignoreCase = true) }
                                        CosmeticBrowserCard(
                                            item = item,
                                            isWishlisted = false,
                                            isOwned = isOwned,
                                            onWishlistToggle = { },
                                            onClick = { 
                                                // Switch detail if possible
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
                                    Text("Back to Locker")
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SortDropdownButton(
    currentSort: LockerSortOption,
    onSelectSort: (LockerSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SleekSurface,
                contentColor = SleekTextPrimary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(SleekSurfaceBorder, SleekSurfaceBorder))),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Sort, contentDescription = null, tint = SleekCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sort", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SleekSurfaceVariant)
        ) {
            LockerSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            color = if (option == currentSort) SleekCyan else SleekTextPrimary,
                            fontWeight = if (option == currentSort) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelectSort(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LockerItemCard(
    item: ParsedLockerItem,
    onClick: () -> Unit
) {
    val rarityColor = getRarityColor(item.rarity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, rarityColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(rarityColor.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            ) {
                val iconToLoad = item.iconUrl ?: "" 
                
                AsyncImage(
                    model = iconToLoad,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize().padding(if (item.category == LockerCategory.OUTFIT) 0.dp else 4.dp),
                    contentScale = ContentScale.Fit
                )

                if (item.isFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = FortniteGold,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    )
                }

                if (item.quantity > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "x${item.quantity}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.rarity,
                    fontSize = 10.sp,
                    color = if (getRarityTextColor(rarityColor) == Color.White) Color.White else rarityColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
