package com.dhyper.fncompanion.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.db.RecentSearchEntity
import com.dhyper.fncompanion.data.models.*
import com.dhyper.fncompanion.ui.theme.FortniteGold
import com.dhyper.fncompanion.ui.theme.SleekAccent
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
import com.dhyper.fncompanion.ui.viewmodels.StatsUiState
import com.dhyper.fncompanion.ui.viewmodels.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsLookupScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedAccountType by viewModel.selectedAccountType.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState(initial = emptyList())
    val apiKey by viewModel.apiKey.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf(apiKey ?: "") }
    val platforms = listOf("epic" to "Epic Games", "psn" to "PlayStation", "xbl" to "Xbox")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(12.dp)
    ) {
        // Search & Platform Bar
        OutlinedTextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stats_search_input"),
            placeholder = { Text("Search any Epic Display Name...", color = SleekTextMuted) },
            leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null, tint = SleekCyan) },
            trailingIcon = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = { searchInput = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SleekTextMuted)
                    }
                }
            },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(platforms) { (key, label) ->
                    val isSelected = selectedAccountType == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setAccountType(key) },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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
                        modifier = Modifier.testTag("stats_platform_$key")
                    )
                }
            }

            Button(
                onClick = { viewModel.searchPlayer(searchInput) },
                enabled = searchInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("stats_search_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Search", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Recent Searches chips
        if (recentSearches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = SleekTextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recent:", color = SleekTextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(recentSearches) { entity ->
                        Box(
                            modifier = Modifier
                                .background(SleekSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    searchInput = entity.accountName
                                    viewModel.searchPlayer(entity.accountName)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entity.accountName, color = SleekTextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = SleekTextMuted,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.deleteRecentSearch(entity.accountName) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val state = uiState) {
            is StatsUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = SleekCyan,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Search Any Fortnite Player",
                            style = MaterialTheme.typography.titleMedium,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Track kills, wins, and K/D across all platforms.",
                            color = SleekTextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is StatsUiState.Searching -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SleekCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching account career data...", color = SleekTextSecondary)
                    }
                }
            }
            is StatsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        val is401 = state.message.contains("401") || state.message.contains("API Key")
                        
                        Text(
                            text = if (is401) "Free API Key Required" else "Account Career Lookup Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (is401) SleekCyan else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (is401) 
                                "fortnite-api.com requires a free API key to look up player career stats. Obtain a free key in 10 seconds." 
                                else state.message,
                            color = SleekTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        
                        if (is401) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dash.fortnite-api.com"))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Get Free API Key at fortnite-api.com", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Paste your API key here...", color = SleekTextMuted) },
                                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = SleekCyan) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                clipboardManager.getText()?.text?.let { apiKeyInput = it.trim() }
                                            }) {
                                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = SleekCyan)
                                            }
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SleekPrimary,
                                            unfocusedBorderColor = SleekSurfaceBorder,
                                            focusedContainerColor = SleekSurfaceVariant,
                                            unfocusedContainerColor = SleekSurfaceVariant,
                                            focusedTextColor = SleekTextPrimary,
                                            unfocusedTextColor = SleekTextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            viewModel.setApiKey(apiKeyInput)
                                            viewModel.searchPlayer(state.lastQuery)
                                        },
                                        enabled = apiKeyInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = FortniteGold),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Key & Search Career", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.searchPlayer(state.lastQuery) },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("stats_retry_button")
                            ) {
                                Text("Retry Search", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            is StatsUiState.Success -> {
                val stats = state.playerStats
                if (stats == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No detailed stats available for this player.", color = SleekTextMuted)
                    }
                } else {
                    var selectedStatTab by remember { mutableStateOf(0) }
                    val tabs = listOf("OVERALL", "BATTLE ROYALE", "LTM")

                    Column(modifier = Modifier.fillMaxSize()) {
                        val allData = stats.battleRoyale ?: stats.stats

                        // Tracker Profile Header
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .border(1.dp, SleekCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(SleekPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stats.account?.name?.take(1)?.uppercase() ?: "?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(stats.account?.name ?: "Unknown", style = MaterialTheme.typography.headlineSmall, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("Battle Pass Level: ${stats.battlePass?.level ?: 0}", color = SleekCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        ScrollableTabRow(
                            selectedTabIndex = selectedStatTab,
                            containerColor = Color.Transparent,
                            contentColor = SleekCyan,
                            edgePadding = 0.dp,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedStatTab]),
                                    color = SleekCyan
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedStatTab == index,
                                    onClick = { selectedStatTab = index },
                                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        var selectedInputMethod by remember { mutableStateOf(0) }
                        val inputMethods = listOf(
                            "ALL" to allData?.all,
                            "KBM" to allData?.keyboardMouse,
                            "PAD" to allData?.gamepad,
                            "TOUCH" to allData?.touch
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            inputMethods.forEachIndexed { index, (label, data) ->
                                val isSelected = selectedInputMethod == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedInputMethod = index },
                                    label = { Text(label, fontSize = 10.sp) },
                                    enabled = data != null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SleekPrimary,
                                        disabledContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        val currentStats = inputMethods[selectedInputMethod].second

                        if (currentStats == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No stats available for this input method.", color = SleekTextMuted)
                            }
                        } else {
                            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                when (selectedStatTab) {
                                    0 -> OverallStatsView(currentStats)
                                    1 -> {
                                        ModeStatsView("BATTLE ROYALE (BUILDS)", listOf(
                                            "Solo" to currentStats.solo,
                                            "Duo" to currentStats.duo,
                                            "Trio" to currentStats.trio,
                                            "Squad" to currentStats.squad
                                        ))
                                    }
                                    2 -> ModeStatsView(
                                        "LTM & SPECIAL MODES",
                                        listOf(
                                            "LTM Combined" to currentStats.ltm
                                        )
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverallStatsView(stats: StatBreakdown) {
    val overall = stats.overall
    Column {
        if (overall != null) {
            Text("LIFETIME SUMMARY", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SleekCyan, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Matches", "${overall.matches ?: 0}", SleekCyan, Modifier.weight(1f))
                StatCard("Wins", "${overall.wins ?: 0}", FortniteGold, Modifier.weight(1f))
                StatCard("K/D", String.format("%.2f", overall.kd ?: 0.0), SleekEmerald, Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SleekSurface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    StatDetailRow("Total Kills", "${overall.kills ?: 0}")
                    StatDetailRow("Win Rate", String.format("%.1f%%", overall.winRate ?: 0.0))
                    StatDetailRow("Kills/Min", String.format("%.2f", overall.killsPerMin ?: 0.0))
                    StatDetailRow("Avg Match Time", "${(overall.minutesPlayed ?: 0) / (overall.matches ?: 1).coerceAtLeast(1)}m")
                    StatDetailRow("Players Outlived", "${overall.playersOutlived ?: 0}")
                }
            }
        } else {
            Text("Overall stats unavailable.", color = SleekTextMuted)
        }
    }
}

@Composable
fun ModeStatsView(title: String, modes: List<Pair<String, SingleStatGroup?>>) {
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = SleekCyan, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))

        val playedModes = modes.filter { it.second != null && (it.second?.matches ?: 0L) > 0 }
        
        if (playedModes.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No games played in this category.", color = SleekTextMuted, fontSize = 13.sp)
            }
        } else {
            playedModes.forEach { (modeName, statGroup) ->
                PlaylistStatCard(modeName, statGroup)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun StatDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SleekTextMuted, fontSize = 13.sp)
        Text(value, color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = SleekTextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = iconColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlaylistStatCard(
    title: String,
    statGroup: SingleStatGroup?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = SleekCyan, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (statGroup != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Wins: ${statGroup.wins ?: 0}", color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Kills: ${statGroup.kills ?: 0}", color = SleekTextSecondary, fontSize = 13.sp)
                    }

                    Column {
                        Text("K/D: ${String.format("%.2f", statGroup.kd ?: 0.0)}", color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Matches: ${statGroup.matches ?: 0}", color = SleekTextSecondary, fontSize = 13.sp)
                    }

                    Column {
                        Text("Win%: ${String.format("%.1f", statGroup.winRate ?: 0.0)}%", color = FortniteGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Score: ${statGroup.score ?: 0}", color = SleekTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                Text("No games played in this playlist", color = SleekTextMuted, fontSize = 12.sp)
            }
        }
    }
}
