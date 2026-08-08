package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import com.dhyper.fncompanion.ui.viewmodels.FortniteMapMode
import com.dhyper.fncompanion.ui.viewmodels.MapUiState
import com.dhyper.fncompanion.ui.viewmodels.MapViewModel

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(12.dp)
    ) {
        when (val state = uiState) {
            is MapUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SleekCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading real-time Fortnite island maps...", color = SleekTextSecondary)
                    }
                }
            }
            is MapUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Map Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = SleekTextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadMap() },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("map_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is MapUiState.Success -> {
                val variants = state.getVariants()
                val currentVariant = state.getCurrentVariant()
                
                val mapImage = if (state.selectedMode == FortniteMapMode.BATTLE_ROYALE) {
                    if (state.showPois) state.brMapData?.images?.pois ?: "https://fortnite-api.com/images/map_en.png"
                    else state.brMapData?.images?.blank ?: "https://fortnite-api.com/images/map.png"
                } else {
                    currentVariant.imageUrl
                }

                // Map Mode Selector Tabs (BR, Reload, Blitz, OG)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FortniteMapMode.entries.forEach { mode ->
                        val isSelected = state.selectedMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) SleekCyan else SleekSurfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SleekCyan else SleekSurfaceBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.selectMapMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("map_mode_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else SleekTextPrimary
                            )
                        }
                    }
                }

                // Map Variant Selector (for modes with multiple maps like Reload/Blitz)
                if (variants.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        variants.forEachIndexed { index, variant ->
                            val isSelected = state.selectedVariantIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectVariant(index) },
                                label = { Text(variant.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = SleekSurfaceVariant,
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
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = SleekCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.selectedMode.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = state.selectedMode.subtitle,
                            fontSize = 11.sp,
                            color = SleekTextMuted
                        )
                    }

                    if (state.selectedMode == FortniteMapMode.BATTLE_ROYALE) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show POIs", color = SleekTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = state.showPois,
                                onCheckedChange = { viewModel.togglePois() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SleekPrimary,
                                    uncheckedThumbColor = SleekTextMuted,
                                    uncheckedTrackColor = SleekSurfaceVariant
                                ),
                                modifier = Modifier.testTag("map_poi_switch")
                            )
                        }
                    }
                }

                // Map Canvas View
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                LaunchedEffect(state.selectedMode, state.selectedVariantIndex) {
                    scale = 1f
                    offset = androidx.compose.ui.geometry.Offset.Zero
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2
                                    offset = androidx.compose.ui.geometry.Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                    ) {
                        if (!mapImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = mapImage,
                                contentDescription = "${state.selectedMode.displayName} Island Map",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Map image unavailable", color = SleekTextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // POI Locations List
                val poiNames = currentVariant.poiNames
                val fullPois = currentVariant.fullPois

                if (poiNames.isNotEmpty() || fullPois.isNotEmpty()) {
                    Text(
                        text = if (state.selectedMode == FortniteMapMode.BATTLE_ROYALE) "Points of Interest (${fullPois.size})" else "Named POIs",
                        style = MaterialTheme.typography.titleSmall,
                        color = SleekCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.heightIn(max = 400.dp) // Prevent infinite height issues
                    ) {
                        if (state.selectedMode == FortniteMapMode.BATTLE_ROYALE) {
                            items(fullPois) { poi ->
                                PoiCard(name = poi.name, x = poi.location?.x, y = poi.location?.y)
                            }
                        } else {
                            items(poiNames) { name ->
                                PoiCard(name = name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PoiCard(name: String, x: Float? = null, y: Float? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SleekPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = SleekTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            if (x != null && y != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "X: ${x.toInt()} Y: ${y.toInt()}",
                    fontSize = 11.sp,
                    color = SleekTextMuted
                )
            }
        }
    }
}
