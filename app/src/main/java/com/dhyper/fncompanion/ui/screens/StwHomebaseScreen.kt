package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhyper.fncompanion.data.models.*
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.StwViewModel
import com.dhyper.fncompanion.ui.viewmodels.StwUiState

@Composable
fun StwHomebaseScreen(
    viewModel: StwViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val homebaseData by viewModel.homebaseData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHomebaseData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "HOMEBASE DASHBOARD",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = SleekTextPrimary
        )
        Text(
            "BETA v0.1 - Native Profile Data",
            fontSize = 10.sp,
            color = SleekEmerald,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            is StwUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SleekCyan)
                }
            }
            is StwUiState.Error -> {
                Text("Error: ${state.message}", color = Color.Red)
            }
            else -> {
                homebaseData?.let { data ->
                    // --- COMMANDER HEADER ---
                    CommanderHeader(data)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- RESOURCES ---
                    Text("RESOURCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    ResourceGrid(data)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- RESEARCH & F.O.R.T ---
                    Text("F.O.R.T. STATS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    FortStatsCard(data)
                }
            }
        }
    }
}

@Composable
fun CommanderHeader(data: StwHomebaseData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Power Level Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.radialGradient(listOf(FortniteGold, Color.Transparent)), CircleShape)
                    .border(2.dp, FortniteGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.powerLevel.toInt().toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text("Commander Level ${data.commanderLevel}", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                Text("Daily Quests: ${data.dailyQuestsCount}/3", fontSize = 12.sp, color = if(data.dailyQuestsCount >= 3) Color.Red else SleekTextMuted)
                
                if (data.researchStatus.isCapped) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = FortniteGold, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Research Points Capped!", fontSize = 11.sp, color = FortniteGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceGrid(data: StwHomebaseData) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ResourceCard(label = "V-Bucks", value = data.vbucks.toString(), icon = Icons.Default.MonetizationOn, color = FortniteGold, modifier = Modifier.weight(1f))
        ResourceCard(label = "X-Ray", value = data.xrayTickets.toString(), icon = Icons.Default.Receipt, color = SleekCyan, modifier = Modifier.weight(1f))
        ResourceCard(label = "Gold", value = data.gold.toString(), icon = Icons.Default.Savings, color = Color.Yellow, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ResourceCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SleekSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
            Text(label, fontSize = 10.sp, color = SleekTextMuted)
        }
    }
}

@Composable
fun FortStatsCard(data: StwHomebaseData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatRow("Fortitude", data.fortStats.fortitude, data.researchStatus.fortitude, Color(0xFF4CAF50))
            StatRow("Offense", data.fortStats.offense, data.researchStatus.offense, Color(0xFFF44336))
            StatRow("Resistance", data.fortStats.resistance, data.researchStatus.resistance, Color(0xFF2196F3))
            StatRow("Technology", data.fortStats.technology, data.researchStatus.technology, Color(0xFFFF9800))
        }
    }
}

@Composable
fun StatRow(label: String, total: Int, researchLevel: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, modifier = Modifier.width(100.dp), fontSize = 14.sp, color = SleekTextPrimary)
        Text(total.toString(), fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f))
        Text("Research Lvl $researchLevel", fontSize = 11.sp, color = SleekTextMuted)
    }
}
