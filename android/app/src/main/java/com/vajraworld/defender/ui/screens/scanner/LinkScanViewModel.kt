package com.vajraworld.defender.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.local.ScanResultEntity
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.engine.UrlRuleEngine
import com.vajraworld.defender.domain.model.GuardianLinkAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class LinkScanViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _inputUrl = MutableStateFlow("http://192.168.1.50/secure-bank-login.xyz/update.apk")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _contextText = MutableStateFlow("URGENT: account blocked! verify OTP immediately")
    val contextText: StateFlow<String> = _contextText.asStateFlow()

    private val _scanResult = MutableStateFlow<GuardianLinkAnalysis?>(null)
    val scanResult: StateFlow<GuardianLinkAnalysis?> = _scanResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _urlHistory = MutableStateFlow<List<ScanResultEntity>>(emptyList())
    val urlHistory: StateFlow<List<ScanResultEntity>> = _urlHistory.asStateFlow()

    init {
        observeUrlHistory()
    }

    private fun observeUrlHistory() {
        if (repository == null) return
        viewModelScope.launch {
            repository.urlScanHistoryFlow.collect { history ->
                _urlHistory.value = history
            }
        }
    }

    fun updateUrl(url: String) {
        _inputUrl.value = url
    }

    fun updateContext(ctx: String) {
        _contextText.value = ctx
    }

    fun selectHistoryItem(item: ScanResultEntity) {
        _inputUrl.value = item.target
        scanUrl()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository?.clearUrlScanHistory()
        }
    }

    fun scanUrl() {
        viewModelScope.launch {
            _isScanning.value = true

            // Persist & analyze locally through repository
            if (repository != null) {
                val localResult = repository.analyzeUrlLocally(
                    url = _inputUrl.value,
                    context = _contextText.value.ifBlank { null }
                )
                val isThreat = localResult.riskScore >= 50
                _scanResult.value = GuardianLinkAnalysis(
                    url = localResult.url,
                    riskScore = localResult.riskScore,
                    confidence = localResult.confidence,
                    whyPoints = localResult.signals.ifEmpty {
                        listOf("Clean URL syntax and domain reputation", "Nominal Shannon entropy (${String.format(Locale.US, "%.2f", localResult.entropy)})")
                    },
                    recommendedAction = localResult.recommendedAction,
                    entropy = localResult.entropy,
                    progressionTrajectory = listOf(
                        mapOf("step" to "Link Analysis", "status" to "OBSERVED"),
                        mapOf("step" to "Brand Integrity", "status" to if (localResult.brandDeception != null) "DECEPTIVE" else "VERIFIED"),
                        mapOf("step" to "Host Reputation", "status" to if (isThreat) "SUSPICIOUS" else "NOMINAL"),
                        mapOf("step" to "Execution Verdict", "status" to if (isThreat) "BLOCKED" else "ALLOWED")
                    ),
                    hasUrgentContext = _contextText.value.isNotEmpty()
                )
                _isScanning.value = false
                return@launch
            }

            // Fallback direct engine invocation
            val localResult = UrlRuleEngine.analyze(
                rawUrl = _inputUrl.value,
                context = _contextText.value.ifBlank { null }
            )

            val isThreat = localResult.riskScore >= 50

            _scanResult.value = GuardianLinkAnalysis(
                url = localResult.url,
                riskScore = localResult.riskScore,
                confidence = localResult.confidence,
                whyPoints = localResult.signals.ifEmpty {
                    listOf("Clean URL syntax and domain reputation", "Nominal Shannon entropy (${String.format(Locale.US, "%.2f", localResult.entropy)})")
                },
                recommendedAction = localResult.recommendedAction,
                entropy = localResult.entropy,
                progressionTrajectory = listOf(
                    mapOf("step" to "Link Analysis", "status" to "OBSERVED"),
                    mapOf("step" to "Brand Integrity", "status" to if (localResult.brandDeception != null) "DECEPTIVE" else "VERIFIED"),
                    mapOf("step" to "Host Reputation", "status" to if (isThreat) "SUSPICIOUS" else "NOMINAL"),
                    mapOf("step" to "Execution Verdict", "status" to if (isThreat) "BLOCKED" else "ALLOWED")
                ),
                hasUrgentContext = _contextText.value.isNotEmpty()
            )
            _isScanning.value = false
        }
    }
}
