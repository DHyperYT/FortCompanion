package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.ui.theme.*

@Composable
fun BrHubScreen(
    onNavigateToShop: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // --- BACKGROUND LAYER ---
        AsyncImage(
            model = "file:///android_asset/maps/br_bg.png",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(10.dp),
            contentScale = ContentScale.FillHeight
        )
        
        // Darkened Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // --- CONTENT LAYER ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BATTLE ROYALE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SleekTextPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Grid of big boxes
            Row(modifier = Modifier.fillMaxWidth()) {
                HubCard(
                    title = "ITEM SHOP",
                    subtitle = "Daily Offers",
                    icon = Icons.Default.ShoppingBag,
                    color = SleekPrimary,
                    onClick = onNavigateToShop,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                HubCard(
                    title = "MAPS",
                    subtitle = "View Islands",
                    icon = Icons.Default.Map,
                    color = SleekCyan,
                    onClick = onNavigateToMap,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                HubCard(
                    title = "NEWS",
                    subtitle = "Latest Updates",
                    icon = Icons.Default.Newspaper,
                    color = SleekEmerald,
                    onClick = onNavigateToNews,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                HubCard(
                    title = "TRACKER",
                    subtitle = "Player Stats",
                    icon = Icons.Default.Leaderboard,
                    color = FortniteGold,
                    onClick = onNavigateToTracker,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun HubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.15f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(100f, 100f),
                            radius = 300f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekTextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
