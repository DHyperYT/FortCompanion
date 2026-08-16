package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.data.models.NewsMotd
import com.dhyper.fncompanion.ui.theme.FortniteFont
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
import com.dhyper.fncompanion.ui.viewmodels.NewsUiState
import com.dhyper.fncompanion.ui.viewmodels.NewsViewModel

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf(
        "BR" to "Battle Royale",
        "STW" to "Save The World",
        "Creative" to "Creative"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
    ) {
        when (val state = uiState) {
            is NewsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SleekCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching Fortnite live news feed...", color = SleekTextSecondary)
                    }
                }
            }
            is NewsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "News Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = SleekTextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadNews() },
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
            is NewsUiState.Success -> {
                val activeTab = state.activeTab
                val selectedIndex = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)

                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = SleekSurface,
                    contentColor = SleekCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = SleekCyan
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, pair ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { viewModel.setTab(pair.first) },
                            text = {
                                Text(
                                    text = pair.second,
                                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium,
                                    color = if (index == selectedIndex) SleekCyan else SleekTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                val newsCategory = when (activeTab) {
                    "STW" -> state.newsData.stw
                    "Creative" -> state.newsData.creative
                    else -> state.newsData.br
                }

                val motds = newsCategory?.motds ?: emptyList()

                if (motds.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active news entries available for this category.", color = SleekTextMuted)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(motds, key = { it.id }) { motd ->
                            NewsCard(motd = motd)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(motd: NewsMotd) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
    ) {
        Column {
            Box {
                val imageUrl = motd.image ?: motd.tileImage
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = motd.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Live/New Badge
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .background(SleekEmerald, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        "LIVE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = motd.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FortniteFont,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                motd.tabTitle?.let { tabT ->
                    if (tabT.isNotBlank() && tabT != motd.title) {
                        Text(
                            text = tabT,
                            style = MaterialTheme.typography.labelMedium,
                            color = SleekCyan,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                motd.body?.let { body ->
                    if (body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekTextSecondary
                        )
                    }
                }
            }
        }
    }
}

