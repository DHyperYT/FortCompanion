package com.dhyper.fncompanion.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dhyper.fncompanion.ui.viewmodels.PersonalLockerViewModel
import com.dhyper.fncompanion.ui.viewmodels.LockerUiState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dhyper.fncompanion.data.models.AuthState
import com.dhyper.fncompanion.data.models.LockerCategory
import com.dhyper.fncompanion.ui.theme.*
import com.dhyper.fncompanion.ui.viewmodels.AuthViewModel
import com.dhyper.fncompanion.ui.viewmodels.LoginState
import com.dhyper.fncompanion.ui.viewmodels.SettingsViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAuthScreen(
    viewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    lockerViewModel: PersonalLockerViewModel,
    stwViewModel: com.dhyper.fncompanion.ui.viewmodels.StwViewModel,
    onNavigateToLocker: () -> Unit,
    onNavigateToCareer: () -> Unit,
    forceLogin: Boolean = false,
    onLoginSuccess: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authSession.collectAsState()
    val loginState by viewModel.loginState.collectAsState()
    val lockerState by lockerViewModel.uiState.collectAsState()

    val session = when (val state = authState) {
        is AuthState.Active -> state.session
        is AuthState.TokenRefreshing -> state.session
        is AuthState.TokenExpired -> state.session
        is AuthState.ReauthRequired -> state.session
        is AuthState.DecryptionError -> state.session
        is AuthState.NetworkError -> state.session
        else -> null
    }

    var loginAccomplished by remember { mutableStateOf(false) }

    var exchangeCodeInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Idle && forceLogin && loginAccomplished) {
            android.widget.Toast.makeText(context, "Login Successful!", android.widget.Toast.LENGTH_SHORT).show()
            onLoginSuccess?.invoke()
        }
    }

    var generatedExchangeCode by remember { mutableStateOf<String?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var showMobileLoginDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var currentWebViewUrl by remember { mutableStateOf("") }
    
    LaunchedEffect(session) {
        if (session != null) {
            lockerViewModel.loadLocker(session)
        }
    }

    val loginUrl = "https://www.epicgames.com/id/login?prompt=login&redirectUrl=https%3A%2F%2Fwww.epicgames.com%2Fid%2Fapi%2Fredirect%3FclientId%3D3f69e56c7649492c8cc29f1af08a8a12%26responseType%3Dcode"

    if (showWebView) {
        Dialog(
            onDismissRequest = { showWebView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(SleekBackground)) {
                Column(modifier = Modifier.fillMaxSize()) {
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
                            Icon(Icons.Default.Lock, null, tint = SleekEmerald, modifier = Modifier.size(16.dp))
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
                            IconButton(onClick = { showWebView = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = SleekTextMuted)
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
        if (session != null && !forceLogin) {
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
                                .size(60.dp)
                                .clickable { showIconPicker = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SleekSurface, CircleShape)
                                    .border(2.dp, SleekPrimary, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (session.equippedSkinIcon != null) {
                                    AsyncImage(
                                        model = session.equippedSkinIcon,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.AddAPhoto, null, tint = SleekPrimary, modifier = Modifier.size(24.dp))
                                }
                            }

                            // Edit Icon at corner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .background(SleekPrimary, CircleShape)
                                    .border(1.dp, SleekSurfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = session.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                when (authState) {
                                    is AuthState.Active -> Icon(Icons.Default.CheckCircle, null, tint = SleekEmerald, modifier = Modifier.size(18.dp))
                                    is AuthState.TokenRefreshing -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SleekCyan, strokeWidth = 2.dp)
                                    is AuthState.NetworkError -> Icon(Icons.Default.WifiOff, null, tint = FortniteGold, modifier = Modifier.size(18.dp))
                                    is AuthState.DecryptionError -> Icon(Icons.Default.VpnKeyOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    else -> Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Account ID: ${session.accountId}", fontSize = 12.sp, color = SleekTextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (authState) {
                        is AuthState.ReauthRequired -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Re-authentication Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("The connection to Epic Games was revoked or expired. Please sign in again.", fontSize = 12.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { showWebView = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                        Text("Sign In Again")
                                    }
                                }
                            }
                        }
                        is AuthState.DecryptionError -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Storage Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Could not decrypt your credentials. This can happen after a system update or security change.", fontSize = 12.sp)
                                }
                            }
                        }
                        is AuthState.NetworkError -> {
                            Text(text = "Offline: ${(authState as AuthState.NetworkError).message}", color = FortniteGold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        is AuthState.TokenRefreshing -> {
                            Text(text = "Updating session...", color = SleekCyan, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        else -> {
                            Text(text = "Your Epic Games account is connected!", color = SleekTextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    AccountButton(text = "Open BR Locker", icon = Icons.Default.Checkroom, color = MaterialTheme.colorScheme.primary, onClick = onNavigateToLocker)
                    Spacer(modifier = Modifier.height(12.dp))
                    AccountButton(text = "Career & History", icon = Icons.Default.History, color = MaterialTheme.colorScheme.primary, onClick = onNavigateToCareer)
                    Spacer(modifier = Modifier.height(12.dp))
                    AccountButton(text = "Login on Fortnite Mobile", icon = Icons.Default.Smartphone, color = MaterialTheme.colorScheme.primary, onClick = { showMobileLoginDialog = true })

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = SleekSurfaceBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("ACCOUNT MANAGEMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            settingsViewModel.authenticate(context, "Generate Exchange Code") {
                                isGeneratingCode = true
                                viewModel.generateExchangeCode { code ->
                                    isGeneratingCode = false
                                    generatedExchangeCode = code
                                    if (code != null) clipboardManager.setText(AnnotatedString(code))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isGeneratingCode
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(if (isGeneratingCode) "Generating..." else "Generate Exchange Code", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (generatedExchangeCode != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SleekBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(generatedExchangeCode!!, modifier = Modifier.weight(1f), color = SleekEmerald, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                                Text("COPIED", color = SleekTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            settingsViewModel.authenticate(context, "Manage Epic Account") {
                                viewModel.generateExchangeCode { code ->
                                    val link = if (code != null) "https://www.epicgames.com/id/exchange?exchangeCode=$code" else "https://www.epicgames.com/id/login"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ManageAccounts, null, tint = FortniteGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Manage Epic Account", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, SleekSurfaceBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(SleekPrimary, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Epic Games Login", style = MaterialTheme.typography.titleLarge, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Required for locker and career",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "How to Login:", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Click the button below to sign in. The app will automatically connect once you finish logging into your Epic Games account.", fontSize = 13.sp, color = SleekTextSecondary, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { showWebView = true }, colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().testTag("auth_get_code_button")) {
                        Icon(Icons.Default.Link, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In with Epic Games", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Or paste code manually:", style = MaterialTheme.typography.labelMedium, color = SleekTextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = exchangeCodeInput,
                        onValueChange = { exchangeCodeInput = it; viewModel.clearLoginError() },
                        modifier = Modifier.fillMaxWidth().testTag("auth_exchange_code_input"),
                        label = { Text("Exchange Code (32 hex characters)", color = SleekTextMuted) },
                        leadingIcon = { Icon(Icons.Default.Key, null, tint = SleekCyan) },
                        trailingIcon = {
                            IconButton(onClick = { clipboardManager.getText()?.text?.let { exchangeCodeInput = it.trim() } }) {
                                Icon(Icons.Default.ContentPaste, null, tint = SleekCyan)
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SleekPrimary, unfocusedBorderColor = SleekSurfaceBorder, focusedContainerColor = SleekSurface, unfocusedContainerColor = SleekSurface, focusedTextColor = SleekTextPrimary, unfocusedTextColor = SleekTextPrimary),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (loginState is LoginState.LoggingIn) {
                        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = SleekCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Connecting to Epic Games...", color = SleekTextSecondary)
                            }
                        }
                    } else {
                        Button(
                            onClick = { 
                                loginAccomplished = true
                                viewModel.loginWithExchangeCode(exchangeCodeInput) 
                            },
                            enabled = exchangeCodeInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_connect_button")
                        ) {
                            Text("Connect via Exchange Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (loginState is LoginState.Error) {
                        val errorMsg = (loginState as LoginState.Error).message
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(text = errorMsg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        val outfits = (lockerState as? LockerUiState.Success)?.allItems?.filter { it.category == LockerCategory.OUTFIT } ?: emptyList()
        
        ModalBottomSheet(
            onDismissRequest = { showIconPicker = false },
            containerColor = SleekSurfaceVariant,
            dragHandle = { BottomSheetDefaults.DragHandle(color = SleekTextMuted) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp)) {
                Text("Choose Profile Icon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = SleekTextPrimary)
                Text("Select an owned outfit to use as your profile picture", fontSize = 13.sp, color = SleekTextMuted)
                
                Spacer(Modifier.height(20.dp))
                
                if (outfits.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        if (lockerState is LockerUiState.Loading) {
                            CircularProgressIndicator(color = SleekCyan)
                        } else {
                            Text("No outfits found in locker", color = SleekTextMuted)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(70.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(outfits) { outfit ->
                            AsyncImage(
                                model = outfit.iconUrl,
                                contentDescription = outfit.name,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, SleekSurfaceBorder, CircleShape)
                                    .clickable {
                                        settingsViewModel.updateAccountIcon(session!!.accountId, outfit.iconUrl)
                                        showIconPicker = false
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        settingsViewModel.updateAccountIcon(session!!.accountId, null)
                        showIconPicker = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Current Icon", color = Color.Red)
                }
            }
        }
    }

    if (showMobileLoginDialog) {
        AlertDialog(
            onDismissRequest = { showMobileLoginDialog = false },
            title = { Text("Login on Fortnite Mobile", fontWeight = FontWeight.Black, color = SleekTextPrimary) },
            text = {
                Column {
                    Text(text = "Follow these steps carefully:", fontWeight = FontWeight.Bold, color = SleekCyan, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "1. In the game's login screen, select \"YES, Find my account\" and immediately return to Fortnite Companion.\n\n" +
                        "2. Click the button below to complete the secure handshake.",
                        fontSize = 13.sp, color = SleekTextSecondary, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Valid for 5 minutes, until used, or until logout.", fontSize = 11.sp, color = SleekTextMuted, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.generateExchangeCode { code ->
                            if (code != null) {
                                val mobileLoginUrl = "https://www.epicgames.com/id/logout?redirectUrl=https%3A%2F%2Fwww.epicgames.com%2Fid%2Fexchange%3FexchangeCode%3D$code%26clientId%3D3f69e56c7649492c8cc29f1af08a8a12%26responseType%3Dcode"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mobileLoginUrl)))
                                showMobileLoginDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Finish Login", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMobileLoginDialog = false }) { Text("Cancel") }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun AccountButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(text, color = SleekTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = SleekTextMuted)
        }
    }
}
