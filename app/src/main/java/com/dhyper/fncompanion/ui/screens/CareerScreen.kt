package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.AuthViewModel
import com.dhyper.fncompanion.ui.viewmodels.StatsViewModel
import com.dhyper.fncompanion.ui.viewmodels.StatsUiState

@Composable
fun CareerScreen(
    authViewModel: AuthViewModel,
    statsViewModel: StatsViewModel
) {
    val authState by authViewModel.authSession.collectAsState()
    val statsUiState by statsViewModel.uiState.collectAsState()
    
    val session = when (val state = authState) {
        is AuthState.Active -> state.session
        is AuthState.TokenRefreshing -> state.session
        is AuthState.TokenExpired -> state.session
        is AuthState.NetworkError -> state.session
        is AuthState.DecryptionError -> state.session
        is AuthState.ReauthRequired -> state.session
        else -> null
    }

    LaunchedEffect(session) {
        if (session != null) {
            statsViewModel.loadPersonalCareer(session)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "CAREER HISTORY",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = SleekTextPrimary,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = statsUiState) {
            is StatsUiState.Searching -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SleekCyan)
                }
            }
            is StatsUiState.Error -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
            is StatsUiState.Success -> {
                val career = state.careerDetails
                
                // Personal Career Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SleekCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("OVERALL STATS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CareerStatItem("Account Level", "${career?.accountLevel ?: 0}", SleekTextPrimary)
                            CareerStatItem("Season Level", "${career?.seasonalLevel ?: 0}", SleekTextPrimary)
                            CareerStatItem("Total Wins", "${career?.lifetimeWins ?: 0}", FortniteGold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val pastSeasons = career?.pastSeasons ?: emptyList()
                if (pastSeasons.isNotEmpty()) {
                    Text(
                        text = "PAST SEASONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = SleekCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    pastSeasons.forEach { season ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        season.seasonName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("LVL", fontSize = 9.sp, color = SleekTextMuted)
                                        Text("${season.seasonLevel}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SleekCyan)
                                    }
                                    
                                    val showTier = season.seasonNumber in 1..10 || season.seasonName.contains("Chapter 1", ignoreCase = true)
                                    if (showTier) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("TIER", fontSize = 9.sp, color = SleekTextMuted)
                                            Text("${season.battlePassTier}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = FortniteGold)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("WINS", fontSize = 9.sp, color = SleekTextMuted)
                                        Text("${season.seasonWins}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SleekEmerald)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No season history found.", color = SleekTextMuted)
                    }
                }
            }
            else -> {
                if (session == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Please connect your Epic Games account to view career history.", color = SleekTextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun CareerStatItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = SleekTextMuted)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = valueColor)
    }
}
