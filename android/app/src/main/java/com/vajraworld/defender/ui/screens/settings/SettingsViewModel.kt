package com.vajraworld.defender.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val installGuardEnabled: Boolean = true,
    val screenShareShieldEnabled: Boolean = true,
    val maskOtpDuringScreenShare: Boolean = true,
    val callGuardEnabled: Boolean = true,
    val clipboardGuardEnabled: Boolean = true,
    val linkGuardEnabled: Boolean = true,
    val notificationGuardEnabled: Boolean = true,
    val autoClearTimerSec: Int = 30,
    val logRetentionDays: Int = 1,
    val appCodeSizeMb: Float = 14.2f,
    val userDataSizeMb: Float = 1.8f,
    val cacheSizeMb: Float = 0.5f,
    val totalAppSizeMb: Float = 16.5f,
    val isScreenSharingActive: Boolean = false,
    val isClearing: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val repository: VajraRepository,
    context: Context? = null
) : ViewModel() {

    private val prefs: SharedPreferences? = context?.getSharedPreferences("vajra_security_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            installGuardEnabled = prefs?.getBoolean("install_guard", true) ?: true,
            screenShareShieldEnabled = prefs?.getBoolean("screen_share_shield", true) ?: true,
            maskOtpDuringScreenShare = prefs?.getBoolean("mask_otp_screen_share", true) ?: true,
            callGuardEnabled = prefs?.getBoolean("call_guard", true) ?: true,
            clipboardGuardEnabled = prefs?.getBoolean("clipboard_guard", true) ?: true,
            linkGuardEnabled = prefs?.getBoolean("link_guard", true) ?: true,
            notificationGuardEnabled = prefs?.getBoolean("notification_guard", true) ?: true,
            autoClearTimerSec = prefs?.getInt("auto_clear_timer", 30) ?: 30,
            logRetentionDays = prefs?.getInt("log_retention_days", 1) ?: 1
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        if (context != null) {
            calculateAppStorage(context)
            checkScreenSharing(context)
        }
    }

    fun calculateAppStorage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apkSize = try { File(context.packageCodePath).length() } catch (_: Exception) { 14_000_000L }
                val dbSize = try { context.getDatabasePath("vajra_database").length() } catch (_: Exception) { 1_200_000L }
                val cacheSize = try {
                    context.cacheDir.walkTopDown().sumOf { it.length() }
                } catch (_: Exception) { 500_000L }
                val dataSize = try {
                    (context.filesDir.walkTopDown().sumOf { it.length() } + dbSize)
                } catch (_: Exception) { dbSize }

                val codeMb = apkSize / (1024f * 1024f)
                val dataMb = dataSize / (1024f * 1024f)
                val cacheMb = cacheSize / (1024f * 1024f)

                _uiState.value = _uiState.value.copy(
                    appCodeSizeMb = codeMb,
                    userDataSizeMb = dataMb,
                    cacheSizeMb = cacheMb,
                    totalAppSizeMb = codeMb + dataMb + cacheMb
                )
            } catch (_: Exception) {}
        }
    }

    fun checkScreenSharing(context: Context) {
        try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val displays = dm?.displays ?: emptyArray()
            val isSharing = displays.any { it.displayId != Display.DEFAULT_DISPLAY }
            _uiState.value = _uiState.value.copy(isScreenSharingActive = isSharing)
        } catch (_: Exception) {}
    }

    fun clearAppCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isClearing = true)
            try {
                context.cacheDir.deleteRecursively()
                context.cacheDir.mkdirs()
                calculateAppStorage(context)
                _uiState.value = _uiState.value.copy(isClearing = false, statusMessage = "Application cache successfully cleared")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isClearing = false, statusMessage = "Error clearing cache: ${e.message}")
            }
        }
    }

    fun setLogRetentionPolicy(context: Context, days: Int) {
        prefs?.edit()?.putInt("log_retention_days", days)?.apply()
        _uiState.value = _uiState.value.copy(logRetentionDays = days)

        viewModelScope.launch(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - (days * 86400000L)
            try {
                val dao = repository.daoSync()
                dao.pruneSecurityEvents(cutoff)
                dao.pruneScanResults(cutoff)
                dao.pruneClipboardLogs(cutoff)
                calculateAppStorage(context)
                _uiState.value = _uiState.value.copy(statusMessage = "Log retention set to $days Day(s). Older records pruned.")
            } catch (_: Exception) {}
        }
    }

    fun toggleInstallGuard(enabled: Boolean) {
        prefs?.edit()?.putBoolean("install_guard", enabled)?.apply()
        _uiState.value = _uiState.value.copy(installGuardEnabled = enabled)
    }

    fun toggleScreenShareShield(enabled: Boolean) {
        prefs?.edit()?.putBoolean("screen_share_shield", enabled)?.apply()
        _uiState.value = _uiState.value.copy(screenShareShieldEnabled = enabled)
    }

    fun toggleMaskOtpScreenShare(enabled: Boolean) {
        prefs?.edit()?.putBoolean("mask_otp_screen_share", enabled)?.apply()
        _uiState.value = _uiState.value.copy(maskOtpDuringScreenShare = enabled)
    }

    fun toggleCallGuard(enabled: Boolean) {
        prefs?.edit()?.putBoolean("call_guard", enabled)?.apply()
        _uiState.value = _uiState.value.copy(callGuardEnabled = enabled)
    }

    fun toggleClipboardGuard(enabled: Boolean) {
        prefs?.edit()?.putBoolean("clipboard_guard", enabled)?.apply()
        _uiState.value = _uiState.value.copy(clipboardGuardEnabled = enabled)
    }

    fun toggleLinkGuard(enabled: Boolean) {
        prefs?.edit()?.putBoolean("link_guard", enabled)?.apply()
        _uiState.value = _uiState.value.copy(linkGuardEnabled = enabled)
    }

    fun toggleNotificationGuard(enabled: Boolean) {
        prefs?.edit()?.putBoolean("notification_guard", enabled)?.apply()
        _uiState.value = _uiState.value.copy(notificationGuardEnabled = enabled)
    }

    fun setAutoClearTimer(seconds: Int) {
        prefs?.edit()?.putInt("auto_clear_timer", seconds)?.apply()
        _uiState.value = _uiState.value.copy(autoClearTimerSec = seconds)
    }

    fun clearAllScansAndEvents(context: Context) {
        viewModelScope.launch {
            repository.clearAllScanResults()
            repository.clearSecurityEvents()
            repository.clearUrlScanHistory()
            repository.clearClipboardLogs()
            calculateAppStorage(context)
            _uiState.value = _uiState.value.copy(statusMessage = "All on-device audit logs & scan history cleared")
        }
    }

    fun dismissStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
