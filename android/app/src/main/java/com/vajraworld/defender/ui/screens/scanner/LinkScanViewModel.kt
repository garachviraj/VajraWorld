package com.vajraworld.defender.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.domain.model.GuardianLinkAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LinkScanViewModel : ViewModel() {
    private val _inputUrl = MutableStateFlow("http://192.168.1.50/secure-bank-login.xyz/update.apk")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _contextText = MutableStateFlow("URGENT: account blocked! verify OTP immediately")
    val contextText: StateFlow<String> = _contextText.asStateFlow()

    private val _scanResult = MutableStateFlow<GuardianLinkAnalysis?>(null)
    val scanResult: StateFlow<GuardianLinkAnalysis?> = _scanResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun updateUrl(url: String) {
        _inputUrl.value = url
    }

    fun updateContext(ctx: String) {
        _contextText.value = ctx
    }

    fun scanUrl() {
        viewModelScope.launch {
            _isScanning.value = true
            // Synthetic instant on-device inspection
            val isSuspicious = _inputUrl.value.contains("xyz") || _inputUrl.value.contains("apk") || _inputUrl.value.contains("192.168")
            val risk = if (isSuspicious) 88 else 12

            _scanResult.value = GuardianLinkAnalysis(
                url = _inputUrl.value,
                riskScore = risk,
                confidence = 0.92f,
                whyPoints = if (isSuspicious) listOf(
                    "Direct IP-literal / internal gateway hostname",
                    "Brand deception keyword 'secure-bank-login'",
                    "Direct download link to executable/package (.apk)",
                    "Urgent social engineering context detected in message"
                ) else listOf("Standard clean domain structure, HTTPS verified"),
                recommendedAction = if (isSuspicious) "DO NOT OPEN — PHISHING / MALWARE LURE" else "Safe to Open",
                entropy = 3.92f,
                progressionTrajectory = listOf(
                    mapOf("step" to "Link Discovered", "status" to "OBSERVED"),
                    mapOf("step" to "Link Opened", "status" to if (isSuspicious) "BLOCKED" else "ALLOWED"),
                    mapOf("step" to "Credential Page", "status" to "PREVENTED"),
                    mapOf("step" to "Account Takeover", "status" to "PREVENTED")
                ),
                hasUrgentContext = _contextText.value.isNotEmpty()
            )
            _isScanning.value = false
        }
    }
}
