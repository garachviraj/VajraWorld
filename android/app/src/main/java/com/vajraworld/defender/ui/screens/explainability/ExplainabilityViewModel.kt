package com.vajraworld.defender.ui.screens.explainability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.AttributionItem
import com.vajraworld.defender.domain.model.ExplainabilityData
import com.vajraworld.defender.domain.model.TemporalEventItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExplainabilityViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ExplainabilityData(
            forecastId = "fc_latest",
            narrative = "On-device threat telemetry continuously evaluates installed packages, IPC endpoints, and system integrity indicators. Hardware sensors and permission matrices are nominal.",
            attributions = listOf(
                AttributionItem("package_permission_audit", 0.14f, "up", "Forensic analysis of installed package permissions"),
                AttributionItem("system_integrity_attestation", 0.08f, "up", "Root binary and Magisk heuristic inspection"),
                AttributionItem("network_gateway_transport", 0.06f, "up", "Wi-Fi link and active socket inspection"),
                AttributionItem("otp_privacy_vault", -0.15f, "down", "Zero-storage verified in-flight SHA-256 telemetry"),
                AttributionItem("notification_guard", -0.08f, "down", "Continuous phishing & scam pattern filter active")
            ),
            temporalEvents = listOf(
                TemporalEventItem("T-120s", "NORMAL", "Hardware profiler & sensor telemetry initialized", 0.04f),
                TemporalEventItem("T-90s", "NOMINAL", "Root binary check: su, Magisk, test-keys clear", 0.04f),
                TemporalEventItem("T-60s", "INSPECTION", "Audited installed user packages & toxic permission sets", 0.12f),
                TemporalEventItem("T-30s", "NOMINAL", "Wi-Fi gateway transport and VPN status validated", 0.08f),
                TemporalEventItem("T-00s", "NOMINAL", "Active on-device surveillance operational", 0.05f),
                TemporalEventItem("T+30s", "FORECAST", "Zero-trust autonomous defense posture maintained", 0.04f)
            ),
            centerNode = "Device-Core",
            uncertaintyWarning = "Nominal: On-device sensor and permission coverage at 100%."
        )
    )
    val uiState: StateFlow<ExplainabilityData> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchExplanations()
    }

    fun fetchExplanations() {
        if (repository == null) return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getForecastExplanations("fc_latest")
            if (res.isSuccess) {
                val data = res.getOrNull()
                if (data != null) {
                    _uiState.value = data
                }
            }
            _isLoading.value = false
        }
    }
}
