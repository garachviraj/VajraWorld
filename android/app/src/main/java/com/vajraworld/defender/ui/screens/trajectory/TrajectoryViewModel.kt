package com.vajraworld.defender.ui.screens.trajectory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.FutureBranch
import com.vajraworld.defender.domain.model.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TrajectoryTimelineNode(
    val offset: String,
    val secondsOffset: Int,
    val stage: String,
    val riskPct: Int,
    val isPredicted: Boolean,
    val mitreTactic: String,
    val description: String
)

data class TrajectoryUiState(
    val currentRisk: Float = 0.22f,
    val predictedStage: String = "Continuous Surveillance",
    val threatVelocity: Float = 0.012f,
    val leadTimeSec: Int = 110,
    val scrubTimeOffsetSec: Int = 0,
    val timelineNodes: List<TrajectoryTimelineNode> = listOf(
        TrajectoryTimelineNode("T-60s", -60, "Initial Probe", 12, false, "T1595 Reconnaissance", "Active port & socket reconnaissance observed on local network interface"),
        TrajectoryTimelineNode("T-30s", -30, "Privilege Probe", 18, false, "T1548 Privilege Escalation", "System integrity & binary execution environment inspected"),
        TrajectoryTimelineNode("NOW", 0, "Current State", 22, false, "T1082 System Discovery", "Continuous recurrent latent state vector maintained by World Model"),
        TrajectoryTimelineNode("+30s", 30, "App Infiltration", 36, true, "T1437 App Protocol", "Projected unvalidated package load or accessibility hook attempt"),
        TrajectoryTimelineNode("+60s", 60, "Credential Sniffing", 52, true, "T1056 Keylogging/Overlay", "Anticipated screen overlay or OTP interception lure"),
        TrajectoryTimelineNode("+90s", 90, "Exfiltration Burst", 68, true, "T1041 Exfiltration", "High-entropy C2 beaconing or external DNS tunnel projection"),
        TrajectoryTimelineNode("+120s", 120, "System Lockdown", 80, true, "T1486 Data Encrypted", "Potential lateral persistence or ransomware lock condition")
    ),
    val branches: List<FutureBranch> = listOf(
        FutureBranch("Branch A: Autonomous Micro-Segmentation (Recommended)", 0.65f, "Stabilizing", "Benign / Secured", 0.08f),
        FutureBranch("Branch B: Banking Overlay & Accessibility Hook", 0.22f, "Escalating", "Credential Access", 0.72f),
        FutureBranch("Branch C: Background Sockets & DNS Tunnel Burst", 0.13f, "High Risk", "Exfiltration", 0.85f)
    ),
    val selectedBranch: FutureBranch? = null,
    val isLoading: Boolean = false
)

class TrajectoryViewModel(private val repository: VajraRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TrajectoryUiState())
    val uiState: StateFlow<TrajectoryUiState> = _uiState.asStateFlow()

    init {
        loadTrajectory()
        observeLocalThreatEvents()
    }

    private fun observeLocalThreatEvents() {
        viewModelScope.launch {
            try {
                combine(
                    repository.incidentsFlow,
                    repository.daoSync().getAllSecurityEvents(),
                    repository.daoSync().getFileScanHistory()
                ) { incidents, events, files ->
                    updateTrajectoryFromLocalData(incidents, events, files)
                }.collect()
            } catch (_: Exception) {}
        }
    }

    private fun updateTrajectoryFromLocalData(
        incidents: List<Incident>,
        events: List<SecurityEventEntity>,
        files: List<com.vajraworld.defender.data.local.ScanResultEntity> = emptyList()
    ) {
        val activeIncidents = incidents.filter { it.status != "RESOLVED" }
        val highRiskCount = activeIncidents.size + events.count { it.risk > 0.5f }

        val baseRisk = if (highRiskCount > 0) (0.35f + (highRiskCount * 0.12f)).coerceAtMost(0.88f) else 0.18f
        val velocity = if (highRiskCount > 0) 0.025f else 0.008f
        val stage = when {
            activeIncidents.any { it.incidentId.contains("ROOT", true) } -> "Privilege Escalation Probe"
            activeIncidents.any { it.incidentId.contains("APP", true) } -> "Toxic Application Interception"
            activeIncidents.any { it.incidentId.contains("NET", true) } -> "Network C2 Egress"
            else -> "Continuous Surveillance & Hardening"
        }

        val dynamicNodes = listOf(
            TrajectoryTimelineNode("T-60s", -60, "Network Ingress", (baseRisk * 50).toInt().coerceAtLeast(8), false, "T1595 Reconnaissance", "Socket traffic inspection across active device interfaces"),
            TrajectoryTimelineNode("T-30s", -30, "Privilege Probe", (baseRisk * 70).toInt().coerceAtLeast(14), false, "T1548 Privilege Escalation", "Attestation verification against SELinux, su binaries, & ADB"),
            TrajectoryTimelineNode("NOW", 0, stage, (baseRisk * 100).toInt(), false, "T1082 System Discovery", "Real-time state vector synthesised from local Room DB signals"),
            TrajectoryTimelineNode("+30s", 30, "Package Verification", ((baseRisk + 0.12f) * 100).toInt().coerceIn(15, 95), true, "T1437 App Protocol", "Evaluation of newly written APKs and background download triggers"),
            TrajectoryTimelineNode("+60s", 60, "Overlay & Vault Shield", ((baseRisk + 0.24f) * 100).toInt().coerceIn(25, 98), true, "T1056 Input Capture", "DisplayManager screencast surveillance & clipboard auto-clearing"),
            TrajectoryTimelineNode("+90s", 90, "Exfiltration Defense", ((baseRisk + 0.36f) * 100).toInt().coerceIn(35, 99), true, "T1041 Exfiltration", "High-entropy payload interception and socket termination"),
            TrajectoryTimelineNode("+120s", 120, "System Containment", ((baseRisk + 0.45f) * 100).toInt().coerceIn(40, 100), true, "T1486 Data Protection", "Autonomous isolation of toxic packages and storage sandboxing")
        )

        val branchAProb = if (highRiskCount > 0) 0.45f else 0.78f
        val branchBProb = if (highRiskCount > 0) 0.35f else 0.14f
        val branchCProb = (1.0f - branchAProb - branchBProb).coerceAtLeast(0.05f)

        val dynamicBranches = listOf(
            FutureBranch("Branch A: Autonomous Micro-Segmentation (Recommended)", branchAProb, "Stabilizing", "Benign / Secured", 0.08f),
            FutureBranch("Branch B: Banking Overlay & Accessibility Hook", branchBProb, "Escalating", "Credential Access", 0.72f),
            FutureBranch("Branch C: Background Sockets & DNS Tunnel Burst", branchCProb, "High Risk", "Exfiltration", 0.85f)
        )

        _uiState.value = _uiState.value.copy(
            currentRisk = baseRisk,
            threatVelocity = velocity,
            predictedStage = stage,
            timelineNodes = dynamicNodes,
            branches = dynamicBranches
        )
    }

    fun loadTrajectory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getForecast()
            if (res.isSuccess) {
                val data = res.getOrNull()
                if (data != null) {
                    val risk = (data["forecast_risk"] as? Number)?.toFloat() ?: 0.22f
                    val stage = data["predicted_stage"] as? String ?: "Active Surveillance"
                    val lead = (data["lead_time_sec"] as? Number)?.toInt() ?: 110

                    _uiState.value = _uiState.value.copy(
                        currentRisk = risk,
                        predictedStage = stage,
                        leadTimeSec = lead,
                        isLoading = false
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectBranch(branch: FutureBranch) {
        _uiState.value = _uiState.value.copy(selectedBranch = branch)
    }

    fun setScrubTime(seconds: Int) {
        _uiState.value = _uiState.value.copy(scrubTimeOffsetSec = seconds)
    }
}
