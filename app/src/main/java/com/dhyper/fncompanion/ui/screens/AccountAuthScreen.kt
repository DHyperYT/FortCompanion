package com.dhyper.fncompanion.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.AuthViewModel
import com.dhyper.fncompanion.ui.viewmodels.LoginState
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAuthScreen(
    viewModel: AuthViewModel,
    onNavigateToLocker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authSession by viewModel.authSession.collectAsState()
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var exchangeCodeInput by remember { mutableStateOf("") }
    var showWebView by remember { mutableStateOf(false) }
    var currentWebViewUrl by remember { mutableStateOf("") }
    val loginUrl = "https://www.epicgames.com/id/login?prompt=login&redirectUrl=https%3A%2F%2Fwww.epicgames.com%2Fid%2Fapi%2Fredirect%3FclientId%3D3f69e56c7649492c8cc29f1af08a8a12%26responseType%3Dcode"

    if (showWebView) {
        Dialog(
            onDismissRequest = { showWebView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(SleekBackground)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern URL Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SleekSurface,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Secure", tint = SleekEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = currentWebViewUrl.replace("https://", ""),
                                color = SleekTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showWebView = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = SleekTextMuted)
                            }
                        }
                    }

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        currentWebViewUrl = url ?: ""
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        currentWebViewUrl = url ?: ""
                                        if (url?.contains("/id/api/redirect") == true) {
                                            view?.evaluateJavascript("(function() { return document.body.innerText; })();") { result ->
                                                val jsonString = result?.trim('"')?.replace("\\\"", "\"")?.replace("\\\\", "\\")
                                                try {
                                                    val json = JSONObject(jsonString ?: "")
                                                    val code = json.optString("authorizationCode") ?: json.optString("code")
                                                    if (code.isNotBlank()) {
                                                        viewModel.loginWithAuthCode(code)
                                                        showWebView = false
                                                    }
                                                } catch (e: Exception) {
                                                    val hex32Match = Regex("[a-fA-F0-9]{32}").find(jsonString ?: "")
                                                    if (hex32Match != null) {
                                                        viewModel.loginWithAuthCode(hex32Match.value)
                                                        showWebView = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                loadUrl(loginUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (authSession != null) {
            // Logged in state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(SleekPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = authSession?.displayName ?: "Epic Games Account",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Connected", tint = SleekEmerald, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Account ID: ${authSession?.accountId}",
                                fontSize = 12.sp,
                                color = SleekTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your Epic Games account is connected!",
                        color = SleekTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // BR Locker Entry
                    Button(
                        onClick = onNavigateToLocker,
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Checkroom, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Open BR Locker", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Personal Career Summary
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SleekCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CAREER SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Account Level", fontSize = 10.sp, color = SleekTextMuted)
                                    Text("${authSession?.accountLevel ?: 0}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                                }
                                Column {
                                    Text("Season Level", fontSize = 10.sp, color = SleekTextMuted)
                                    Text("${authSession?.seasonalLevel ?: 0}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                                }
                                Column {
                                    Text("Wins", fontSize = 10.sp, color = SleekTextMuted)
                                    Text("${authSession?.totalWins ?: 0}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = com.dhyper.fncompanion.ui.theme.FortniteGold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Past Seasons History
                    val pastSeasons by viewModel.pastSeasons.collectAsState()
                    if (pastSeasons.isNotEmpty()) {
                        Text(
                            text = "PAST SEASONS HISTORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekCyan,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        pastSeasons.forEach { season ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(season.seasonName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("LVL", fontSize = 9.sp, color = SleekTextMuted)
                                            Text("${season.seasonLevel}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekCyan)
                                        }
                                        
                                        // Hide TIER for all seasons after Season 10 (Chapter 1)
                                        val showTier = season.seasonNumber in 1..10 || season.seasonName.contains("Chapter 1", ignoreCase = true)
                                        
                                        if (showTier) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("TIER", fontSize = 9.sp, color = SleekTextMuted)
                                                Text("${season.battlePassTier}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = com.dhyper.fncompanion.ui.theme.FortniteGold)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("WINS", fontSize = 9.sp, color = SleekTextMuted)
                                            Text("${season.seasonWins}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekEmerald)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Debug/Test Tools
                    Text(
                        text = "TOOLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekCyan,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Button(
                        onClick = {
                            viewModel.refreshStats() // Use this as a proxy for refreshing data/clearing cache
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SleekEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh All Data", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SleekCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fix Background Notifications", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect Account", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Login Required View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SleekPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Epic Games Login",
                                style = MaterialTheme.typography.titleLarge,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Required for locker and career",
                                fontSize = 12.sp,
                                color = SleekCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "How to Login:",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Click the button below to sign in. The app will automatically connect once you finish logging into your Epic Games account.",
                        fontSize = 13.sp,
                        color = SleekTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // External URL Button
                    Button(
                        onClick = { showWebView = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_get_code_button")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In with Epic Games", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Or paste code manually:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SleekTextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = exchangeCodeInput,
                        onValueChange = {
                            exchangeCodeInput = it
                            viewModel.clearLoginError()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_exchange_code_input"),
                        label = { Text("Authorization Code (32 hex characters)", color = SleekTextMuted) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = SleekCyan) },
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.getText()?.text?.let { exchangeCodeInput = it.trim() }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = SleekCyan)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    if (loginState is LoginState.LoggingIn) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = SleekCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting to Epic Games...", color = SleekTextSecondary)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.loginWithAuthCode(exchangeCodeInput) },
                            enabled = exchangeCodeInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_connect_button")
                        ) {
                            Text("Connect Epic Account", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (loginState is LoginState.Error) {
                        val errorMsg = (loginState as LoginState.Error).message
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

