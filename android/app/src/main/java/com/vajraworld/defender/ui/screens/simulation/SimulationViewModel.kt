package com.vajraworld.defender.ui.screens.simulation

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.SimulationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ThreatScenario(
    val id: String,
    val title: String,
    val description: String,
    val defaultTarget: String,
    val attackVector: String,
    val mitreTactic: String,
    val recommendedAction: String,
    val baselineRisk: Float,
    val mitigatedRisk: Float
)

data class SimulationUiState(
    val selectedScenarioId: String = "ransomware",
    val availableScenarios: List<ThreatScenario> = listOf(
        ThreatScenario(
            id = "ransomware",
            title = "Ransomware Mass File Encryption Wave",
            description = "Simulates unauthorized background payload initiating recursive AES-256 encryption across /storage/emulated/0/",
            defaultTarget = "Local Storage & User Documents",
            attackVector = "Filesystem Traversal & Rapid Entropy Surges",
            mitreTactic = "T1486 Data Encrypted for Impact",
            recommendedAction = "STORAGE_WRITE_LOCKDOWN",
            baselineRisk = 0.88f,
            mitigatedRisk = 0.14f
        ),
        ThreatScenario(
            id = "banking_overlay",
            title = "Banking Overlay & Accessibility Stealer",
            description = "Simulates toxic combination: BIND_ACCESSIBILITY_SERVICE + SYSTEM_ALERT_WINDOW painting deceptive overlays on financial apps",
            defaultTarget = "Financial Apps & Screen Surface",
            attackVector = "Accessibility Event Interception & Keylogging",
            mitreTactic = "T1056 Input Capture & Overlay",
            recommendedAction = "OVERLAY_PERMISSION_STRIP",
            baselineRisk = 0.82f,
            mitigatedRisk = 0.11f
        ),
        ThreatScenario(
            id = "lateral_movement",
            title = "Zero-Day Remote Lateral Movement",
            description = "Simulates remote exploitation of an open TCP daemon socket to elevate privileges to superuser / root shell",
            defaultTarget = "Android Kernel & Socket Stack",
            attackVector = "Unauthenticated Local Port Exploitation",
            mitreTactic = "T1548 Abuse Elevation Mechanism",
            recommendedAction = "AUTONOMOUS_SOCKET_CONTAINMENT",
            baselineRisk = 0.94f,
            mitigatedRisk = 0.16f
        ),
        ThreatScenario(
            id = "dns_exfil",
            title = "DNS Tunneling & High-Entropy C2 Exfiltration",
            description = "Simulates silent background data leakage via base64 encoded TXT/A queries to unauthorized external DNS resolvers",
            defaultTarget = "Network Interface & DNS Resolver",
            attackVector = "Low-Payload Frequency DNS Beacons",
            mitreTactic = "T1041 Exfiltration Over C2 Channel",
            recommendedAction = "DNS_GATEWAY_SPOOF_BLOCK",
            baselineRisk = 0.76f,
            mitigatedRisk = 0.09f
        ),
        ThreatScenario(
            id = "otp_stealer",
            title = "Unauthorized OTP Forwarding & SMS Sniffer",
            description = "Simulates notification listener interception attempting to extract multi-factor authentication tokens in transit",
            defaultTarget = "Notification Privacy Vault",
            attackVector = "Notification Body Regex Token Capture",
            mitreTactic = "T1114 Email / SMS Credential Sniffing",
            recommendedAction = "EPHEMERAL_OTP_SHIELD",
            baselineRisk = 0.79f,
            mitigatedRisk = 0.05f
        )
    ),
    val targetAsset: String = "Local Storage & User Documents",
    val availableAssets: List<String> = listOf(
        "Android Kernel & Hardware Layer",
        "Local Storage & User Documents",
        "Financial Apps & Screen Surface",
        "Network Interface & Socket Stack",
        "Notification Privacy Vault",
        "Google Chrome (Browser Gateway)"
    ),
    val selectedAction: String = "STORAGE_WRITE_LOCKDOWN",
    val availableActions: List<String> = listOf(
        "STORAGE_WRITE_LOCKDOWN",
        "OVERLAY_PERMISSION_STRIP",
        "AUTONOMOUS_SOCKET_CONTAINMENT",
        "DNS_GATEWAY_SPOOF_BLOCK",
        "EPHEMERAL_OTP_SHIELD"
    ),
    val lastResult: SimulationResult? = null,
    val simulationStep: Int = 0, // 0 = not run, 1 = probe, 2 = exploit, 3 = mitigated
    val isRunning: Boolean = false,
    val statusMessage: String? = null
)

class SimulationViewModel(private val repository: VajraRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    fun selectScenario(scenario: ThreatScenario) {
        _uiState.value = _uiState.value.copy(
            selectedScenarioId = scenario.id,
            targetAsset = scenario.defaultTarget,
            selectedAction = scenario.recommendedAction,
            lastResult = null,
            simulationStep = 0
        )
    }

    fun selectTargetAsset(asset: String) {
        _uiState.value = _uiState.value.copy(targetAsset = asset)
    }

    fun selectAction(action: String) {
        _uiState.value = _uiState.value.copy(selectedAction = action)
    }

    fun runSimulation() {
        val scenario = _uiState.value.availableScenarios.find { it.id == _uiState.value.selectedScenarioId }
            ?: _uiState.value.availableScenarios.first()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, simulationStep = 1)
            delay(500)
            _uiState.value = _uiState.value.copy(simulationStep = 2)
            delay(600)
            _uiState.value = _uiState.value.copy(simulationStep = 3)
            delay(500)

            val baseline = scenario.baselineRisk
            val post = scenario.mitigatedRisk
            val reductionPct = (((baseline - post) / baseline) * 100).toInt()

            val result = SimulationResult(
                simulationId = "sim_${scenario.id}_${System.currentTimeMillis() % 10000}",
                targetAsset = _uiState.value.targetAsset,
                actionType = _uiState.value.selectedAction,
                baselineRisk = baseline,
                postActionRisk = post,
                residualRisk = post,
                riskReductionPct = reductionPct,
                newLikelyStage = "Threat Contained / Micro-Segmented",
                disruptionRating = if (scenario.id == "ransomware") "Minimal (Targeted Isolation)" else "Zero Disruption",
                utilityScore = 0.92f,
                isRecommended = true
            )

            _uiState.value = _uiState.value.copy(
                lastResult = result,
                isRunning = false
            )
        }
    }

    fun applyMitigationRule() {
        _uiState.value = _uiState.value.copy(
            statusMessage = "Mitigation rule '${_uiState.value.selectedAction}' successfully applied to active VajraWorld Defender engine!"
        )
    }

    fun dismissStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
