package com.dhyper.fncompanion.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.BuildConfig
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.AuthViewModel
import com.dhyper.fncompanion.ui.viewmodels.SettingsViewModel
import com.dhyper.fncompanion.ui.viewmodels.StatsViewModel
import com.dhyper.fncompanion.ui.viewmodels.UpdateState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    statsViewModel: StatsViewModel,
    settingsViewModel: SettingsViewModel,
    onAddAccount: () -> Unit,
    onNavigateToDiagnostic: () -> Unit
) {
    val context = LocalContext.current
    val allAccounts by settingsViewModel.allAccounts.collectAsState()
    val isApiKeyVisible by settingsViewModel.isApiKeyVisible.collectAsState()
    val currentApiKey by statsViewModel.apiKey.collectAsState()
    val settings by settingsViewModel.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    
    var apiKeyInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(true) } // true = export, false = import
    
    var importedFileContent by remember { mutableStateOf<String?>(null) }

    val timePicker = TimePickerDialog(
        context,
        { _, hour, min ->
            val time = String.format("%02d:%02d", hour, min)
            settingsViewModel.updateVBucksAlertTime(time)
            scope.launch {
                kotlinx.coroutines.delay(300)
                com.dhyper.fncompanion.worker.VBucksAlertReceiver.scheduleNextAlarm(context)
                com.dhyper.fncompanion.worker.ShopRefreshReceiver.scheduleNextAlarm(context)
            }
        },
        settings?.vbucksAlertTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 0,
        settings?.vbucksAlertTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0,
        true
    )

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            settingsViewModel.exportAccounts(backupPassword) { result ->
                result.onSuccess { data ->
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(data.toByteArray())
                    }
                    Toast.makeText(context, "App data exported successfully!", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Export failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
        showBackupDialog = false
        backupPassword = ""
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                importedFileContent = content
                isExporting = false
                showBackupDialog = true
            }
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
            "SETTINGS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = SleekTextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- API KEY SECTION ---
        Text(
            "FORTNITE-API.COM KEY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isApiKeyVisible) {
                    LaunchedEffect(Unit) {
                        apiKeyInput = currentApiKey ?: ""
                    }
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste API Key...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekSurfaceBorder
                        ),
                        trailingIcon = {
                            IconButton(onClick = { settingsViewModel.hideApiKey() }) {
                                Icon(Icons.Default.VisibilityOff, null, tint = SleekTextMuted)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            statsViewModel.setApiKey(apiKeyInput)
                            settingsViewModel.hideApiKey()
                            apiKeyInput = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekEmerald)
                    ) {
                        Text("Confirm & Hide", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("API Key Status", fontSize = 11.sp, color = SleekTextMuted)
                            Text(
                                if (currentApiKey.isNullOrBlank()) "NOT SET" else "PROTECTED",
                                color = if (currentApiKey.isNullOrBlank()) Color.Red else SleekEmerald,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = {
                                settingsViewModel.authenticate(context, "Authenticate to Manage Key") {
                                    settingsViewModel.showApiKey()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Manage Key")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- ACCOUNT SWITCHER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ACCOUNT SWITCHER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            
            TextButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add New", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        allAccounts.forEach { account ->
            val authState by authViewModel.authSession.collectAsState()
            val isActive = when (val state = authState) {
                is AuthState.Active -> state.session.accountId == account.accountId
                is AuthState.TokenRefreshing -> state.session.accountId == account.accountId
                is AuthState.TokenExpired -> state.session.accountId == account.accountId
                is AuthState.ReauthRequired -> state.session.accountId == account.accountId
                is AuthState.DecryptionError -> state.session.accountId == account.accountId
                is AuthState.NetworkError -> state.session.accountId == account.accountId
                else -> false
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, if(isActive) SleekPrimary else SleekSurfaceBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SleekSurface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if(isActive) SleekPrimary else SleekSurfaceVariant, CircleShape)
                            .clip(CircleShape), 
                        contentAlignment = Alignment.Center
                    ) {
                        if (account.equippedSkinIcon != null) {
                            AsyncImage(
                                model = account.equippedSkinIcon,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(account.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.displayName, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        Text(if(isActive) "Active Session" else "Stored Auth", fontSize = 11.sp, color = if(isActive) SleekEmerald else SleekTextMuted)
                    }
                    if (!isActive) {
                        IconButton(onClick = { settingsViewModel.switchAccount(context, account) }) {
                            Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = account.accountId }) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- PREFERENCES ---
        Text(
            "PREFERENCES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Accent Color
                PreferenceDropdown(
                    label = "Accent Color",
                    options = listOf("Cyan", "Primary", "Emerald", "Gold"),
                    selected = settings?.accentColor ?: "Cyan",
                    onSelected = { settingsViewModel.updateAccentColor(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Universal Wishlist
                PreferenceSwitch(
                    label = "Universal Wishlist",
                    subtitle = "Share wishlist across all accounts",
                    checked = settings?.useUniversalWishlist ?: false,
                    onCheckedChange = { settingsViewModel.updateUniversalWishlist(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wishlist Notifications
                PreferenceSwitch(
                    label = "Wishlist Notifications",
                    subtitle = "Receive shop and wishlist alerts",
                    checked = settings?.notificationsEnabled ?: true,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateNotifications(enabled)
                        scope.launch {
                            kotlinx.coroutines.delay(300)
                            com.dhyper.fncompanion.worker.ShopRefreshReceiver.scheduleNextAlarm(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // V-Bucks Alerts
                PreferenceSwitch(
                    label = "V-Bucks Alerts",
                    subtitle = "Check for StW V-Bucks missions daily",
                    checked = settings?.vbucksAlertsEnabled ?: false,
                    onCheckedChange = { enabled ->
                        settingsViewModel.updateVBucksAlerts(enabled)
                        scope.launch {
                            kotlinx.coroutines.delay(300)
                            com.dhyper.fncompanion.worker.VBucksAlertReceiver.scheduleNextAlarm(context)
                        }
                    }
                )
                
                if (settings?.vbucksAlertsEnabled == true || settings?.notificationsEnabled == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { timePicker.show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SleekSurfaceBorder)
                    ) {
                        Icon(Icons.Default.Alarm, null, tint = SleekCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Alert Time: ${settings?.vbucksAlertTime}", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Battery Optimization Button
                Button(
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BatteryChargingFull, null, tint = SleekEmerald, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Fix Background Alerts", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BACKUP & RESTORE ---
        Text(
            "BACKUP & RESTORE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SleekCyan,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        settingsViewModel.authenticate(context, "Export Backup") {
                            isExporting = true
                            showBackupDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.IosShare, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Export All App Data", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- UPDATES & TOOLS ---
        Text(
            "UPDATES & TOOLS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                settingsViewModel.checkForUpdates(BuildConfig.VERSION_NAME, context)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SleekSurfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            val updateState by settingsViewModel.updateState.collectAsState()
            val text = when (updateState) {
                UpdateState.Checking -> "Checking..."
                UpdateState.NewUpdate -> "New Version Found!"
                UpdateState.NoUpdate -> "Up to date (v${BuildConfig.VERSION_NAME})"
                is UpdateState.Error -> "Check Failed (Retry)"
                else -> "Check for Updates"
            }
            Icon(Icons.Default.Update, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(text, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- ACCOUNT STATUS (Bottom) ---
        val authState = authViewModel.authSession.collectAsState().value
        val session = when (authState) {
            is AuthState.Active -> authState.session
            is AuthState.TokenRefreshing -> authState.session
            is AuthState.TokenExpired -> authState.session
            is AuthState.NetworkError -> authState.session
            is AuthState.DecryptionError -> authState.session
            is AuthState.ReauthRequired -> authState.session
            else -> null
        }

        session?.let { active ->
            Text(
                "SESSION STATUS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SleekSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val countdown by settingsViewModel.tokenExpiryCountdown.collectAsState()
                    val lastRefresh = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(active.lastRefreshTimeMs))

                    StatusRow("Active Account", active.displayName)
                    StatusRow("Token Expiry", countdown)
                    StatusRow("Last Refresh", lastRefresh)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToDiagnostic,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SleekSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.BugReport, null, tint = FortniteGold)
            Spacer(Modifier.width(10.dp))
            Text("Auth Diagnostics", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(if(isExporting) "Set Export Password" else "Enter Backup Password") },
            text = {
                Column {
                    Text(
                        if(isExporting) "This password will be used to encrypt the backup file. You MUST remember it to restore your accounts later."
                        else "Enter the password you used when exporting this file.",
                        fontSize = 13.sp, color = SleekTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SleekTextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isExporting) {
                            exportLauncher.launch("fortnite_app_data_backup.bin")
                        } else {
                            importedFileContent?.let { data ->
                                settingsViewModel.importAccounts(data, backupPassword) { result ->
                                    result.onSuccess { count ->
                                        Toast.makeText(context, "Successfully imported $count accounts!", Toast.LENGTH_LONG).show()
                                        showBackupDialog = false
                                        backupPassword = ""
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Import failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = backupPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekEmerald)
                ) {
                    Text(if(isExporting) "Export" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false; backupPassword = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Account?") },
            text = { Text("This will remove the stored device auth for this account. You will need to log in again to add it back.") },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.deleteAccount(showDeleteConfirm!!)
                    showDeleteConfirm = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PreferenceSwitch(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = SleekTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun PreferenceDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = 14.sp)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = SleekSurface
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = SleekTextPrimary) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SleekTextMuted, fontSize = 12.sp)
        Text(value, color = SleekTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
