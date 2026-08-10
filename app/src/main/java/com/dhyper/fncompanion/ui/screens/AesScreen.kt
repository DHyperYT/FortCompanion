package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.AesUiState
import com.dhyper.fncompanion.ui.viewmodels.BrExtendedViewModel

@Composable
fun AesScreen(viewModel: BrExtendedViewModel) {
    val state by viewModel.aesState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = Modifier.fillMaxSize().background(SleekBackground)) {
        when (val aesState = state) {
            is AesUiState.Loading -> {
                CircularProgressIndicator(
                    color = SleekCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is AesUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(aesState.message, color = SleekTextSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadAes() },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
            is AesUiState.Success -> {
                val data = aesState.aesData
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text(
                                "AES KEYS",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = SleekTextPrimary
                            )
                            Text(
                                "Build: ${data.build ?: "Unknown"}",
                                fontSize = 14.sp,
                                color = SleekCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Main Key
                    item {
                        KeyCard(
                            title = "Main Key",
                            key = data.mainKey ?: "No Main Key Found",
                            onCopy = { clipboardManager.setText(AnnotatedString(data.mainKey ?: "")) }
                        )
                    }

                    item {
                        Text(
                            "DYNAMIC KEYS (${data.dynamicKeys?.size ?: 0})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(data.dynamicKeys ?: emptyList()) { dynamicKey ->
                        KeyCard(
                            title = dynamicKey.pakFilename ?: "Unknown Pak",
                            key = dynamicKey.key ?: "No Key",
                            subtitle = "GUID: ${dynamicKey.pakGuid ?: "N/A"}",
                            onCopy = { clipboardManager.setText(AnnotatedString(dynamicKey.key ?: "")) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyCard(
    title: String,
    key: String,
    subtitle: String? = null,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            fontSize = 11.sp,
                            color = SleekTextMuted
                        )
                    }
                }
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.background(SleekSurface, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = SleekCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekBackground, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    key,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SleekEmerald,
                    softWrap = true
                )
            }
        }
    }
}
