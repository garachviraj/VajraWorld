package com.vajraworld.defender.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val installGuardEnabled: Boolean = true,
    val screenShareShieldEnabled: Boolean = true,
    val callGuardEnabled: Boolean = true,
    val clipboardGuardEnabled: Boolean = true,
    val linkGuardEnabled: Boolean = true,
    val notificationGuardEnabled: Boolean = true,
    val autoClearTimerSec: Int = 30,
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
            callGuardEnabled = prefs?.getBoolean("call_guard", true) ?: true,
            clipboardGuardEnabled = prefs?.getBoolean("clipboard_guard", true) ?: true,
            linkGuardEnabled = prefs?.getBoolean("link_guard", true) ?: true,
            notificationGuardEnabled = prefs?.getBoolean("notification_guard", true) ?: true,
            autoClearTimerSec = prefs?.getInt("auto_clear_timer", 30) ?: 30
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleInstallGuard(enabled: Boolean) {
        prefs?.edit()?.putBoolean("install_guard", enabled)?.apply()
        _uiState.value = _uiState.value.copy(installGuardEnabled = enabled)
    }

    fun toggleScreenShareShield(enabled: Boolean) {
        prefs?.edit()?.putBoolean("screen_share_shield", enabled)?.apply()
        _uiState.value = _uiState.value.copy(screenShareShieldEnabled = enabled)
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

    fun clearUrlHistory() {
        viewModelScope.launch {
            repository.clearUrlScanHistory()
            _uiState.value = _uiState.value.copy(statusMessage = "URL Scan History cleared")
        }
    }

    fun clearClipboardLogs() {
        viewModelScope.launch {
            repository.clearClipboardLogs()
            _uiState.value = _uiState.value.copy(statusMessage = "Daily Clipboard Log cleared")
        }
    }

    fun clearAllScansAndEvents() {
        viewModelScope.launch {
            repository.clearAllScanResults()
            repository.clearSecurityEvents()
            _uiState.value = _uiState.value.copy(statusMessage = "Local security records purged")
        }
    }

    fun dismissStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
