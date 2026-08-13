package com.dhyper.fncompanion.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyper.fncompanion.data.db.AuthDao
import com.dhyper.fncompanion.data.db.AuthEntity
import com.dhyper.fncompanion.data.db.SettingsDao
import com.dhyper.fncompanion.data.db.SettingsEntity
import com.dhyper.fncompanion.data.db.WishlistDao
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.dhyper.fncompanion.data.models.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object NewUpdate : UpdateState()
    object NoUpdate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val authDao: AuthDao,
    private val settingsDao: SettingsDao,
    private val wishlistDao: WishlistDao
) : ViewModel() {

    private val _allAccounts = MutableStateFlow<List<AuthEntity>>(emptyList())
    val allAccounts: StateFlow<List<AuthEntity>> = _allAccounts.asStateFlow()

    private val _isApiKeyVisible = MutableStateFlow(false)
    val isApiKeyVisible: StateFlow<Boolean> = _isApiKeyVisible.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _tokenExpiryCountdown = MutableStateFlow("")
    val tokenExpiryCountdown: StateFlow<String> = _tokenExpiryCountdown.asStateFlow()

    val settings: Flow<SettingsEntity?> = settingsDao.getSettings()

    init {
        loadAccounts()
        observeSettings()
        startExpiryCountdown()
    }

    private fun startExpiryCountdown() {
        viewModelScope.launch {
            authRepository.authSession.collectLatest { state ->
                val session = when (state) {
                    is AuthState.Active -> state.session
                    is AuthState.TokenRefreshing -> state.session
                    is AuthState.TokenExpired -> state.session
                    is AuthState.ReauthRequired -> state.session
                    is AuthState.DecryptionError -> state.session
                    is AuthState.NetworkError -> state.session
                    else -> null
                }
                
                if (session == null) {
                    _tokenExpiryCountdown.value = "N/A"
                    return@collectLatest
                }

                while (true) {
                    val remaining = session.expiresAtMs - System.currentTimeMillis()
                    if (remaining > 0) {
                        val min = (remaining / 1000) / 60
                        val sec = (remaining / 1000) % 60
                        _tokenExpiryCountdown.value = String.format("%02d:%02d", min, sec)
                    } else {
                        _tokenExpiryCountdown.value = "Expired"
                        break // Wait for refresh to trigger new session state
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
    }

    fun checkForUpdates(currentVersion: String, context: Context) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://api.github.com/repos/DHyperYT/FortCompanion/releases/latest")
                    .build()
                
                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        _updateState.value = UpdateState.Error("Check failed")
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                _updateState.value = UpdateState.Error("Check failed")
                                return
                            }
                            val body = response.body?.string() ?: ""
                            val json = org.json.JSONObject(body)
                            val latestTag = json.getString("tag_name").replace("v", "")
                            
                            if (latestTag != currentVersion.replace("v", "")) {
                                _updateState.value = UpdateState.NewUpdate
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DHyperYT/FortCompanion/releases/latest"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } else {
                                _updateState.value = UpdateState.NoUpdate
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Offline or check failed.")
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDao.getSettings().collect {
                // Reschedule alarms when settings change
                // We need a context here, but ViewModels shouldn't hold context.
                // However, we can use the application context if we change SettingsViewModel to AndroidViewModel
                // or just rely on the UI calling it (which is already done).
            }
        }
    }

    fun updateApiKey(key: String?) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            settingsDao.saveSettings(current.copy(fortniteApiKey = key))
        }
    }

    fun updateUniversalWishlist(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            settingsDao.saveSettings(current.copy(useUniversalWishlist = enabled))
        }
    }

    fun updateAccentColor(color: String) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            settingsDao.saveSettings(current.copy(accentColor = color))
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            settingsDao.saveSettings(current.copy(notificationsEnabled = enabled))
        }
    }

    fun updateDataSaverMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            settingsDao.saveSettings(current.copy(dataSaverMode = enabled))
        }
    }

    fun updateVBucksAlerts(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            // Reset lastVBucksMissionId when enabling so it alerts immediately if missions are found
            val newSettings = if (enabled) {
                current.copy(vbucksAlertsEnabled = true, lastVBucksMissionId = null)
            } else {
                current.copy(vbucksAlertsEnabled = false)
            }
            settingsDao.saveSettings(newSettings)
        }
    }

    fun updateVBucksAlertTime(time: String) {
        viewModelScope.launch {
            val current = settingsDao.getSettingsDirect() ?: SettingsEntity()
            settingsDao.saveSettings(current.copy(vbucksAlertTime = time))
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            authDao.getAllAccounts().collect {
                _allAccounts.value = it
            }
        }
    }

    fun switchAccount(context: Context, account: AuthEntity) {
        viewModelScope.launch {
            authDao.saveAuthSession(account)
            // Full process restart to ensure all states (Locker, Quests, Wishlist) are clean
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    fun exportAccounts(password: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.exportAppData(password.toCharArray(), settingsDao, wishlistDao)
            onResult(result)
        }
    }

    fun importAccounts(encryptedData: String, password: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.importAppData(encryptedData, password.toCharArray(), settingsDao, wishlistDao)
            onResult(result)
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            authDao.deleteAccountById(accountId)
            // If the deleted account was the active one, clear active session
            val current = authDao.getAuthSessionDirect()
            if (current == null) {
                 // Try to pick another one and mark as active if exists
                 authDao.getAllAccounts().first().firstOrNull()?.let {
                     authDao.saveAuthSession(it)
                 }
            }
        }
    }

    fun authenticate(context: Context, title: String, onAuthenticated: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun showApiKey() {
        _isApiKeyVisible.value = true
    }

    fun hideApiKey() {
        _isApiKeyVisible.value = false
    }
}
