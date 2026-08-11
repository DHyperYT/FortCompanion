package com.dhyper.fncompanion.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onWishlistToggle: () -> Unit,
    onSetClick: (String) -> Unit,
    onCosmeticClick: ((CosmeticItem) -> Unit)? = null,
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
    
    return when {
        // Vehicles: prioritize standard icons (like BR locker)
        id.startsWith("car") || id.startsWith("id_") || type.contains("car") || 
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
