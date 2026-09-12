package com.vajraworld.defender.ui.screens.trajectory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.FutureBranch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val currentRisk: Float = 0.28f,
    val predictedStage: String = "Continuous Surveillance",
    val threatVelocity: Float = 0.015f,
    val leadTimeSec: Int = 110,
    val scrubTimeOffsetSec: Int = 0,
    val timelineNodes: List<TrajectoryTimelineNode> = listOf(
        TrajectoryTimelineNode("T-60s", -60, "Initial Probe", 12, false, "T1595 Reconnaissance", "Active port & socket reconnaissance observed on local network interface"),
        TrajectoryTimelineNode("T-30s", -30, "Privilege Probe", 18, false, "T1548 Privilege Escalation", "System integrity & binary execution environment inspected"),
        TrajectoryTimelineNode("NOW", 0, "Current State", 28, false, "T1082 System Discovery", "Continuous recurrent latent state vector maintained by World Model"),
        TrajectoryTimelineNode("+30s", 30, "App Infiltration", 42, true, "T1437 App Protocol", "Projected unvalidated package load or accessibility hook attempt"),
        TrajectoryTimelineNode("+60s", 60, "Credential Sniffing", 65, true, "T1056 Keylogging/Overlay", "Anticipated screen overlay or OTP interception lure"),
        TrajectoryTimelineNode("+90s", 90, "Exfiltration Burst", 78, true, "T1041 Exfiltration", "High-entropy C2 beaconing or external DNS tunnel projection"),
        TrajectoryTimelineNode("+120s", 120, "System Lockdown", 88, true, "T1486 Data Encrypted", "Potential lateral persistence or ransomware lock condition")
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
    }

    fun loadTrajectory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getForecast()
            if (res.isSuccess) {
                val data = res.getOrNull()
                if (data != null) {
                    val risk = (data["forecast_risk"] as? Number)?.toFloat() ?: 0.28f
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
