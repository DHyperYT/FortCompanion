package com.dhyper.fncompanion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.data.repository.AuthDiagnosticsManager
import com.dhyper.fncompanion.data.repository.AuthEventType
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.AuthViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuthDiagnosticScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.authSession.collectAsState()
    val events by AuthDiagnosticsManager.events.collectAsState()
    var diagnosticResult by remember { mutableStateOf<String?>(null) }
    var currentProfileId by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val session = when (authState) {
        is AuthState.Active -> (authState as AuthState.Active).session
        is AuthState.TokenExpired -> (authState as AuthState.TokenExpired).session
        is AuthState.TokenRefreshing -> (authState as AuthState.TokenRefreshing).session
        is AuthState.ReauthRequired -> (authState as AuthState.ReauthRequired).session
        is AuthState.StoredAccount -> (authState as AuthState.StoredAccount).session
        is AuthState.DeviceAuthAvailable -> (authState as AuthState.DeviceAuthAvailable).session
        is AuthState.SessionValid -> (authState as AuthState.SessionValid).session
        is AuthState.SessionExpired -> (authState as AuthState.SessionExpired).session
        is AuthState.AuthenticationFailed -> (authState as AuthState.AuthenticationFailed).session
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(16.dp)
    ) {
        Text(
            "ADVANCED OPTIONS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = SleekTextPrimary
        )
        
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                DiagnosticHeader("SESSION STATUS")
                DiagnosticInfoCard(session, authState)
                Spacer(Modifier.height(24.dp))
                
                DiagnosticHeader("OPERATIONS")
                DiagnosticActions(
                    viewModel = viewModel, 
                    isTesting = isTesting, 
                    onDiagnostic = { result, profileId -> 
                        diagnosticResult = result
                        currentProfileId = profileId
                    }
                )
                
                if (diagnosticResult != null) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    currentProfileId?.let { "PROFILE: $it" } ?: "RESULT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextMuted
                                )
                                if (currentProfileId != null && !diagnosticResult!!.startsWith("Error")) {
                                    TextButton(
                                        onClick = {
                                            try {
                                                val fileName = "${currentProfileId}_${System.currentTimeMillis()}.json"
                                                val file = File(context.getExternalFilesDir(null), fileName)
                                                file.writeText(diagnosticResult!!)
                                                Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("EXPORT", fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(diagnosticResult!!, fontSize = 11.sp, color = SleekCyan)
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                DiagnosticHeader("EVENT TIMELINE")
            }
            
            items(events) { event ->
                EventItem(event)
            }
            
            item {
                Spacer(Modifier.height(32.dp))
                Text("Safety: No secrets are displayed.", fontSize = 10.sp, color = SleekTextMuted)
            }
        }
    }
}

@Composable
fun DiagnosticHeader(title: String) {
    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekCyan, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun DiagnosticInfoCard(session: AuthEntity?, state: AuthState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            val now = System.currentTimeMillis()
            val expires = session?.expiresAtMs ?: 0L
            val remaining = (expires - now) / 1000
            
            InfoRow("Account ID", session?.accountId?.take(8) ?: "N/A")
            InfoRow("UI State", state.javaClass.simpleName)
            
            val tokenState = when {
                session == null -> "MISSING"
                remaining > 300 -> "VALID"
                remaining > 0 -> "EXPIRING_SOON"
                else -> "EXPIRED"
            }
            InfoRow("Token State", tokenState, if (tokenState == "VALID") SleekEmerald else Color(0xFFEF4444))
            
            InfoRow("Device Auth", if (session?.deviceId != null) "PRESENT" else "MISSING")
            InfoRow("Refresh Token", if (session?.refreshToken != null) "PRESENT" else "MISSING")
            
            if (session != null) {
                // Timer that forces recomposition every second
                var currentNow by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(session.expiresAtMs) {
                    while (true) {
                        currentNow = System.currentTimeMillis()
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                val currentRemaining = (expires - currentNow) / 1000
                InfoRow("Expires In", if (currentRemaining > 0) "${currentRemaining}s" else "Expired")
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                InfoRow("Last Refresh", sdf.format(Date(session.lastRefreshTimeMs)))
            }
        }
    }
}

@Composable
fun DiagnosticActions(
    viewModel: AuthViewModel, 
    isTesting: Boolean, 
    onDiagnostic: (String, String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton(Icons.Default.Visibility, "Verify Current Access Token", isTesting) {
            onDiagnostic("Verifying...", null)
            viewModel.verifyCurrentToken { onDiagnostic(it, null) }
        }
        ActionButton(Icons.Default.Refresh, "Refresh Access Token", isTesting) {
            onDiagnostic("Refreshing...", null)
            viewModel.refreshAccessToken { onDiagnostic(it, null) }
        }
        ActionButton(Icons.Default.VpnKey, "Get New Access Token", isTesting) {
            onDiagnostic("Recovering with Device Auth...", null)
            viewModel.getNewAccessToken { onDiagnostic(it, null) }
        }

        Spacer(Modifier.height(8.dp))
        DiagnosticHeader("PROFILE DUMP")
        
        val profiles = listOf(
            "athena" to "Athena (BR)",
            "campaign" to "Campaign (STW)",
            "metadata" to "Metadata",
            "collection_book_people0" to "CB People",
            "collection_book_schematics0" to "CB Schematics",
            "outpost0" to "Outpost 0",
            "theater0" to "Theater 0",
            "theater1" to "Theater 1",
            "theater2" to "Theater 2",
            "recycle_bin" to "Recycle Bin"
        )

        profiles.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, label) ->
                    ActionButton(
                        icon = Icons.Default.CloudDownload,
                        label = label,
                        enabled = isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        onDiagnostic("Fetching $id...", id)
                        viewModel.fetchProfileJson(id) { onDiagnostic(it, id) }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    enabled: Boolean, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = !enabled,
        border = BorderStroke(1.dp, SleekSurfaceBorder),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun InfoRow(label: String, value: String, color: Color = SleekTextPrimary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = SleekTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun EventItem(event: com.dhyper.fncompanion.data.repository.AuthDiagnosticEvent) {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(getEventColor(event.type), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(event.type.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            Spacer(Modifier.weight(1f))
            Text(sdf.format(Date(event.timestamp)), fontSize = 10.sp, color = SleekTextMuted)
        }
        if (event.details != null || event.statusCode != null) {
            Text(
                "${event.statusCode ?: ""} ${event.details ?: ""}".trim(),
                fontSize = 10.sp,
                color = SleekTextMuted,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

fun getEventColor(type: AuthEventType): Color = when (type) {
    AuthEventType.REFRESH_SUCCEEDED, AuthEventType.DEVICE_AUTH_SUCCEEDED, AuthEventType.AUTH_RETRY_SUCCEEDED, AuthEventType.API_SUCCESS -> SleekEmerald
    AuthEventType.REFRESH_FAILED, AuthEventType.DEVICE_AUTH_FAILED, AuthEventType.AUTH_RETRY_FAILED, AuthEventType.API_FAILURE, AuthEventType.AUTH_REQUEST_401 -> Color(0xFFEF4444)
    AuthEventType.TOKEN_EXPIRING_SOON, AuthEventType.TOKEN_EXPIRED -> FortniteGold
    else -> SleekCyan
}
