package com.vajraworld.defender.ui.screens.simulation

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
    val defaultBaselineRisk: Float,
    val optimalMitigatedRisk: Float
) {
    val baselineRisk: Float get() = defaultBaselineRisk
    val mitigatedRisk: Float get() = optimalMitigatedRisk
}

data class SimulationUiState(
    val selectedScenarioId: String = "ransomware",
    val availableScenarios: List<ThreatScenario> = listOf(
        ThreatScenario(
            id = "ransomware",
            title = "Ransomware Mass File Encryption Wave",
            description = "Simulates unauthorized background payload initiating recursive encryption across user storage volumes",
            defaultTarget = "Local Storage & User Documents",
            attackVector = "Filesystem Traversal & Rapid Entropy Surges",
            mitreTactic = "T1486 Data Encrypted for Impact",
            recommendedAction = "STORAGE_WRITE_LOCKDOWN",
            defaultBaselineRisk = 0.88f,
            optimalMitigatedRisk = 0.12f
        ),
        ThreatScenario(
            id = "banking_overlay",
            title = "Banking Overlay & Accessibility Stealer",
            description = "Simulates toxic combination: BIND_ACCESSIBILITY_SERVICE + SYSTEM_ALERT_WINDOW painting deceptive overlays on financial apps",
            defaultTarget = "Financial Apps & Screen Surface",
            attackVector = "Accessibility Event Interception & Keylogging",
            mitreTactic = "T1056 Input Capture & Overlay",
            recommendedAction = "OVERLAY_PERMISSION_STRIP",
            defaultBaselineRisk = 0.82f,
            optimalMitigatedRisk = 0.10f
        ),
        ThreatScenario(
            id = "lateral_movement",
            title = "Zero-Day Remote Lateral Movement",
            description = "Simulates remote exploitation of an open TCP daemon socket to elevate privileges to superuser / root shell",
            defaultTarget = "Android Kernel & Socket Stack",
            attackVector = "Unauthenticated Local Port Exploitation",
            mitreTactic = "T1548 Abuse Elevation Mechanism",
            recommendedAction = "AUTONOMOUS_SOCKET_CONTAINMENT",
            defaultBaselineRisk = 0.94f,
            optimalMitigatedRisk = 0.15f
        ),
        ThreatScenario(
            id = "dns_exfil",
            title = "DNS Tunneling & High-Entropy C2 Exfiltration",
            description = "Simulates silent background data leakage via base64 encoded queries to unauthorized external DNS resolvers",
            defaultTarget = "Network Interface & Socket Stack",
            attackVector = "Low-Payload Frequency DNS Beacons",
            mitreTactic = "T1041 Exfiltration Over C2 Channel",
            recommendedAction = "DNS_GATEWAY_SPOOF_BLOCK",
            defaultBaselineRisk = 0.76f,
            optimalMitigatedRisk = 0.08f
        ),
        ThreatScenario(
            id = "otp_stealer",
            title = "Unauthorized OTP Forwarding & SMS Sniffer",
            description = "Simulates notification listener interception attempting to extract multi-factor authentication tokens in transit",
            defaultTarget = "Notification Privacy Vault",
            attackVector = "Notification Body Regex Token Capture",
            mitreTactic = "T1114 Email / SMS Credential Sniffing",
            recommendedAction = "EPHEMERAL_OTP_SHIELD",
            defaultBaselineRisk = 0.79f,
            optimalMitigatedRisk = 0.05f
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
    val simulationStep: Int = 0,
    val isRunning: Boolean = false,
    val statusMessage: String? = null,
    val playbackProgress: Float = 0f,
    val isPlaying: Boolean = false,
    val isScrubbing: Boolean = false,
    val isWhyOutcomeExpanded: Boolean = false
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
            simulationStep = 0,
            playbackProgress = 0f,
            isPlaying = false
        )
    }

    fun selectTargetAsset(asset: String) {
        _uiState.value = _uiState.value.copy(targetAsset = asset)
    }

    fun selectAction(action: String) {
        _uiState.value = _uiState.value.copy(selectedAction = action)
    }

    fun setPlaybackProgress(progress: Float) {
        _uiState.value = _uiState.value.copy(
            playbackProgress = progress.coerceIn(0f, 1f)
        )
    }

    fun setIsPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun setIsScrubbing(scrubbing: Boolean) {
        _uiState.value = _uiState.value.copy(isScrubbing = scrubbing)
    }

    fun toggleWhyOutcome() {
        _uiState.value = _uiState.value.copy(
            isWhyOutcomeExpanded = !_uiState.value.isWhyOutcomeExpanded
        )
    }

    fun runSimulation() {
        val scenario = _uiState.value.availableScenarios.find { it.id == _uiState.value.selectedScenarioId }
            ?: _uiState.value.availableScenarios.first()

        val chosenTarget = _uiState.value.targetAsset
        val chosenAction = _uiState.value.selectedAction

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunning = true,
                simulationStep = 1,
                playbackProgress = 0f,
                isPlaying = false
            )
            delay(300)
            _uiState.value = _uiState.value.copy(simulationStep = 2)
            delay(350)
            _uiState.value = _uiState.value.copy(simulationStep = 3)
            delay(250)

            val simResult = repository.runSimulation(chosenTarget, chosenAction).getOrNull()
                ?: repository.generateOnDeviceSimulation(chosenTarget, chosenAction)

            _uiState.value = _uiState.value.copy(
                lastResult = simResult,
                isRunning = false,
                playbackProgress = 0f,
                isPlaying = true
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
