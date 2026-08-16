package com.dhyper.fncompanion.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dhyper.fncompanion.data.models.CosmeticItem
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.utils.SeasonUtils

@Composable
fun CosmeticDetailSheet(
    item: CosmeticItem,
    isOwned: Boolean,
    isWishlisted: Boolean,
    videoId: String?,
    isSearchingVideo: Boolean,
    price: Int? = null,
    includedItems: List<CosmeticItem>? = null,
    ownedIds: Set<String> = emptySet(), // Added parameter
    renderImages: List<String>? = null,
    onWishlistToggle: () -> Unit,
    onSetClick: (String) -> Unit,
    onCosmeticClick: ((CosmeticItem) -> Unit)? = null,
    onClose: () -> Unit
) {
    val rarityColor = getRarityColor(item.rarity?.value ?: "")
    val context = LocalContext.current
    
    var viewerImageUrl by remember { mutableStateOf<String?>(null) }
    
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
            val isOutfit = item.type?.value?.contains("outfit", ignoreCase = true) == true || 
                            item.id.startsWith("CID_", ignoreCase = true) ||
                            item.id.startsWith("Character_", ignoreCase = true)
            
            val isLoadingScreen = item.type?.value?.contains("loadingscreen", ignoreCase = true) == true || 
                                 item.id.startsWith("LSID_", ignoreCase = true) || 
                                 item.id.startsWith("LoadingScreen_", ignoreCase = true)

            val detailIcon = when {
                isLoadingScreen -> item.images?.other?.background ?: item.images?.background ?: item.images?.full_background ?: item.images?.large
                isOutfit -> item.images?.featured ?: item.images?.large ?: item.images?.icon ?: item.images?.smallIcon
                else -> item.images?.large ?: 
                        item.images?.legoLarge ?:
                        item.images?.featured ?: 
                        item.images?.icon ?: 
                        item.images?.smallIcon ?:
                        item.images?.small
            }

            val imagesToScroll = if (!renderImages.isNullOrEmpty()) renderImages else listOf(detailIcon ?: "")

            if (imagesToScroll.size > 1) {
                val listState = rememberLazyListState()
                LazyRow(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(imagesToScroll) { imageUrl ->
                        Box(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .fillMaxHeight()
                                .clickable { viewerImageUrl = imageUrl },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                
                // Indicators
                val currentIndex by remember {
                    derivedStateOf { listState.firstVisibleItemIndex }
                }

                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(imagesToScroll.size) { index ->
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(
                                    if (currentIndex == index) Color.White else Color.White.copy(alpha = 0.5f), 
                                    CircleShape
                                )
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = detailIcon,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize().clickable { if (!detailIcon.isNullOrEmpty()) viewerImageUrl = detailIcon },
                    contentScale = ContentScale.Fit
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.ZoomIn, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
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
            
            val isBanner = item.id.startsWith("BR", true) || item.id.startsWith("Banner", true) || 
                          item.id.startsWith("OtherBanner", true) || item.id.startsWith("OT", true) ||
                          item.id.startsWith("InfluencerBanner", true) || item.id.startsWith("FounderTier", true) ||
                          item.id.startsWith("StandardBanner", true) || item.id.startsWith("Achievement", true) ||
                          item.id.startsWith("SurvivalBanner", true) || item.id.startsWith("Newsletter", true) ||
                          item.id.startsWith("Winter", true) || item.id.startsWith("Wargames", true) ||
                          item.id.startsWith("Endurance", true) || item.id.startsWith("Starlight", true) ||
                          item.id.startsWith("S8", true)

            val displayType = when {
                item.type?.displayValue != null -> item.type.displayValue
                isBanner -> "Banner"
                else -> "Other"
            }
            Text(displayType, color = SleekTextSecondary)
        }

        item.description?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = SleekTextSecondary, modifier = Modifier.padding(vertical = 8.dp))
        }

        val isTrack = item.type?.displayValue?.contains("Track", ignoreCase = true) == true || 
                      item.type?.value?.contains("Track", ignoreCase = true) == true ||
                      item.id.startsWith("sid_", ignoreCase = true)
        val isMusicPack = item.id.startsWith("MusicPack_", ignoreCase = true)

        // --- AUDIO/VIDEO ---
        val showcaseVideo = item.showcaseVideo
        if (showcaseVideo != null || ((isTrack || isMusicPack) && item.id.startsWith("CID_", ignoreCase = true) == false && !item.id.startsWith("JBSID_", ignoreCase = true))) {
            
            if (isTrack) {
                item.previewUrl?.let { url ->
                    Spacer(modifier = Modifier.height(12.dp))
                    JamTrackPlayer(previewUrl = url)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            val artist = item.artist ?: ""
            val query = when {
                isTrack && artist.contains("Epic Games", ignoreCase = true) -> 
                    "Fortnite ${item.name} Jam Track -emote"
                isTrack -> 
                    "$artist ${item.name} official audio"
                else -> "Fortnite ${item.name} Music Pack"
            }
            
            if (isSearchingVideo && showcaseVideo == null) {
                Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SleekCyan, modifier = Modifier.size(24.dp))
                }
            } else {
                // If there's a showcase video, we use it directly. 
                // If not, we fall back to the searched videoId (if any).
                val effectiveVideoId = showcaseVideo ?: videoId
                
                YouTubeButton(
                    query = query, 
                    videoId = effectiveVideoId,
                    label = if (showcaseVideo != null) "Watch Showcase" else null
                )
            }
        }

        // --- STYLES / VARIANTS ---
        val legoIcon = item.images?.lego?.large ?: item.images?.lego?.small ?: item.images?.legoLarge ?: item.images?.legoSmall
        val beanIcon = item.images?.bean?.large ?: item.images?.bean?.small
        val hasVariants = !item.variants.isNullOrEmpty()
        
        if (hasVariants || legoIcon != null || beanIcon != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AVAILABLE STYLES",
                style = MaterialTheme.typography.titleMedium,
                color = SleekTextPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (legoIcon != null || beanIcon != null) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (legoIcon != null) {
                        item {
                            StylePreviewCard(imageUrl = legoIcon, name = "LEGO", onClick = { viewerImageUrl = legoIcon })
                        }
                    }
                    if (beanIcon != null) {
                        item {
                            StylePreviewCard(imageUrl = beanIcon, name = "Fall Guys", onClick = { viewerImageUrl = beanIcon })
                        }
                    }
                }
            }

            item.variants?.forEach { variant ->
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
                            StylePreviewCard(imageUrl = option.image, name = option.name ?: "Style", onClick = { if(!option.image.isNullOrEmpty()) viewerImageUrl = option.image })
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

        if (includedItems != null && includedItems.size > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "INCLUDED ITEMS (${includedItems.size})",
                style = MaterialTheme.typography.titleMedium,
                color = SleekTextPrimary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(includedItems) { subItem ->
                    Card(
                        modifier = Modifier
                            .width(100.dp)
                            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onCosmeticClick?.invoke(subItem) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(70.dp).background(SleekSurface)) {
                                val subIcon = resolveCosmeticIcon(subItem)
                                if (!subIcon.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = subIcon,
                                        contentDescription = subItem.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val isSubOwned = ownedIds.contains(subItem.id.lowercase())
                            if (isSubOwned) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = SleekEmerald, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("OWNED", color = SleekEmerald, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(
                                text = subItem.name,
                                fontSize = 10.sp,
                                color = SleekTextPrimary,
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

        if (price != null && !isOwned) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(FortniteGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$price V-Bucks", style = MaterialTheme.typography.titleLarge, color = FortniteGold, fontWeight = FontWeight.Bold)
            }
        }

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
            } else if (isOwned && price != null) {
                // In Shop and owned
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = SleekEmerald)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("OWNED", color = Color.White)
                }
            }
            Button(onClick = onClose, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)) {
                Text("Close")
            }
        }
    }

    // --- FULL SCREEN IMAGE VIEWER ---
    viewerImageUrl?.let { url ->
        Dialog(
            onDismissRequest = { viewerImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset += pan
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = 1f
                                    offset = androidx.compose.ui.geometry.Offset.Zero
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                // Top Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewerImageUrl = null },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                downloadImage(context, url, item.name)
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.White)
                        }
                    }
                }
                
                if (scale > 1f) {
                    Text(
                        "Reset Zoom",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { scale = 1f; offset = androidx.compose.ui.geometry.Offset.Zero }
                    )
                }
            }
        }
    }
}

private fun downloadImage(context: android.content.Context, url: String, fileName: String) {
    try {
        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val uri = android.net.Uri.parse(url)
        val request = android.app.DownloadManager.Request(uri).apply {
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle(fileName)
            setDescription("Downloading image from Fortnite Companion")
            setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "$fileName.png")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        downloadManager.enqueue(request)
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to start download: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun StylePreviewCard(imageUrl: String?, name: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
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
                    text = name,
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

@Composable
fun CosmeticDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(100.dp), color = SleekTextMuted, fontSize = 13.sp)
        Text(value, color = SleekTextPrimary, fontSize = 13.sp)
    }
}

fun resolveCosmeticIcon(item: CosmeticItem): String? {
    val images = item.images ?: return null
    val id = item.id.lowercase()
    val type = item.type?.value?.lowercase() ?: ""
    
    val isBanner = id.startsWith("br") || id.startsWith("banner") || 
                  id.startsWith("otherbanner") || id.startsWith("ot") ||
                  id.startsWith("influencerbanner") || id.startsWith("foundertier") ||
                  id.startsWith("standardbanner") || id.startsWith("achievement") ||
                  id.startsWith("survivalbanner") || id.startsWith("newsletter") ||
                  id.startsWith("winter") || id.startsWith("wargames") ||
                  id.startsWith("endurance") || id.startsWith("starlight") ||
                  id.startsWith("s8") || type.contains("banner")

    return when {
        // Loading Screens
        id.startsWith("lsid_") || id.startsWith("loadingscreen_") || type.contains("loadingscreen") -> {
            images.large ?: images.featured ?: images.icon ?: images.smallIcon ?: images.small
        }

        // Banners
        isBanner -> {
            images.icon ?: images.smallIcon ?: images.featured ?: images.large ?: images.small
        }

        // Vehicles: prioritize standard icons (like BR locker)
        id.startsWith("car") || id.startsWith("id_") || id.startsWith("body_") || type.contains("car") || 
        type.contains("wheel") || type.contains("boost") || type.contains("trail") || type.contains("decal") ||
        id.startsWith("id_body_") || id.startsWith("carbody_") ||
        id.startsWith("id_skin_") || id.startsWith("carskin_") ||
        id.startsWith("id_wheel_") || id.startsWith("wheel_") ||
        id.startsWith("id_booster_") || id.startsWith("id_drifttrail_") -> {
            images.icon ?: images.smallIcon ?: images.featured ?: images.decal ?: images.large ?: images.small
        }
        
        // Jam Tracks: prioritize cover art
        id.startsWith("sid_") || type.contains("track") -> {
            images.coverart ?: images.albumArt ?: images.other?.albumArt ?: images.featured ?: images.smallIcon ?: images.large ?: images.small ?: images.icon
        }
        
        // Lego items: prioritize lego-specific assets
        id.startsWith("jbsid") || type.contains("lego") -> {
            images.legoLarge ?: images.legoSmall ?: images.lego?.large ?: images.lego?.small ?: images.lego?.icon ?: images.large ?: images.small ?: images.icon
        }
        
        // Standard BR and others
        else -> {
            images.icon ?: images.smallIcon ?: images.featured ?: images.largeIcon ?: images.large ?: images.small
        }
    } ?: images.icon_background ?: images.other?.background ?: images.background ?: images.full_background
}
