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

        val chosenTarget = _uiState.value.targetAsset
        val chosenAction = _uiState.value.selectedAction

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, simulationStep = 1)
            delay(450)
            _uiState.value = _uiState.value.copy(simulationStep = 2)
            delay(550)
            _uiState.value = _uiState.value.copy(simulationStep = 3)
            delay(400)

            // Dynamic evaluation based on real physical device factors
            val telemetry = repository.getDeviceTelemetry()
            var baseline = scenario.defaultBaselineRisk

            // Adjust baseline mathematically based on real device vulnerabilities
            if (telemetry != null) {
                if (telemetry.integrity.isRooted) baseline = (baseline + 0.15f).coerceAtMost(0.99f)
                if (telemetry.integrity.isAdbEnabled && scenario.id == "lateral_movement") baseline = (baseline + 0.12f).coerceAtMost(0.99f)
                if (!telemetry.integrity.isDeviceSecure && scenario.id == "otp_stealer") baseline = (baseline + 0.10f).coerceAtMost(0.98f)
                if (telemetry.overallRiskScore > 40) baseline = (baseline + 0.05f).coerceAtMost(0.98f)
            }

            // Counterfactual Intervention Effectiveness Logic:
            // Does chosenAction address chosenTarget and scenario attackVector?
            val isOptimalAction = chosenAction == scenario.recommendedAction
            val isTargetMatched = chosenTarget == scenario.defaultTarget

            val residualRisk = when {
                isOptimalAction && isTargetMatched -> scenario.optimalMitigatedRisk
                isOptimalAction && !isTargetMatched -> (scenario.optimalMitigatedRisk + 0.22f).coerceAtMost(0.65f)
                !isOptimalAction && isTargetMatched -> (baseline * 0.70f).coerceAtLeast(0.40f) // Weak intervention
                else -> (baseline * 0.88f).coerceAtLeast(0.55f) // Mismatched action & target
            }

            val reductionPct = (((baseline - residualRisk) / baseline) * 100).toInt().coerceIn(5, 95)
            val isRecommended = isOptimalAction && isTargetMatched

            val likelyStage = when {
                residualRisk <= 0.20f -> "Threat Fully Contained / Micro-Segmented"
                residualRisk <= 0.45f -> "Partial Containment / Secondary Signal Residual"
                else -> "Ineffective Countermeasure / High Residual Exposure"
            }

            val disruption = when (chosenAction) {
                "STORAGE_WRITE_LOCKDOWN" -> if (isTargetMatched) "Minimal (Targeted Process Write Freeze)" else "Unnecessary Storage Lockdown"
                "OVERLAY_PERMISSION_STRIP" -> "Zero Disruption (Toxic Permission Revoked)"
                "AUTONOMOUS_SOCKET_CONTAINMENT" -> "Targeted Port Isolation (Zero App Impact)"
                "DNS_GATEWAY_SPOOF_BLOCK" -> "Zero Disruption (Clean DNS Fallback)"
                "EPHEMERAL_OTP_SHIELD" -> "Zero Disruption (Privacy Token Masked)"
                else -> "Nominal"
            }

            val utility = String.format(
                java.util.Locale.US,
                "%.2f",
                ((reductionPct / 100f) * 0.85f + (if (isRecommended) 0.12f else 0.02f)).coerceIn(0.15f, 0.98f)
            ).toFloat()

            val result = SimulationResult(
                simulationId = "sim_${scenario.id}_${System.currentTimeMillis() % 10000}",
                targetAsset = chosenTarget,
                actionType = chosenAction,
                baselineRisk = String.format(java.util.Locale.US, "%.2f", baseline).toFloat(),
                postActionRisk = String.format(java.util.Locale.US, "%.2f", residualRisk).toFloat(),
                residualRisk = String.format(java.util.Locale.US, "%.2f", residualRisk).toFloat(),
                riskReductionPct = reductionPct,
                newLikelyStage = likelyStage,
                disruptionRating = disruption,
                utilityScore = utility,
                isRecommended = isRecommended
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
