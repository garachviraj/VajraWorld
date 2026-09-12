package com.vajraworld.defender.ui.screens.trajectory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.ForecastPoint
import com.vajraworld.defender.domain.model.FutureBranch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrajectoryTimelineNode(
    val offset: String,
    val stage: String,
    val riskPct: Int,
    val isPredicted: Boolean
)

data class TrajectoryUiState(
    val currentRisk: Float = 0.74f,
    val predictedStage: String = "Lateral Movement",
    val timelineNodes: List<TrajectoryTimelineNode> = listOf(
        TrajectoryTimelineNode("NOW", "Reconnaissance", 48, false),
        TrajectoryTimelineNode("+30s", "Discovery", 62, true),
        TrajectoryTimelineNode("+60s", "Credential Access", 74, true),
        TrajectoryTimelineNode("+90s", "Lateral Movement", 81, true),
        TrajectoryTimelineNode("+120s", "C2 / Exfil", 88, true)
    ),
    val branches: List<FutureBranch> = listOf(
        FutureBranch("Internal App Pivot & Permission Elevation", 0.32f, "Escalating", "Privilege Escalation", 0.44f),
        FutureBranch("Background Location & SMS Intercept Probing", 0.18f, "Contained", "Discovery", 0.25f),
        FutureBranch("Autonomous Sensor Stabilization", 0.50f, "Stabilizing", "Nominal", 0.08f)
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
                    val rawBranches = data["branches"] as? List<Map<String, Any>>
                    val parsedBranches = rawBranches?.map { b ->
                        FutureBranch(
                            name = b["name"] as? String ?: "Branch",
                            probability = (b["probability"] as? Number)?.toFloat() ?: 0.5f,
                            trajectoryTrend = b["trajectory_trend"] as? String ?: "Normal",
                            terminalStage = b["terminal_stage"] as? String ?: "Benign",
                            meanFinalRisk = (b["mean_final_risk"] as? Number)?.toFloat() ?: 0.5f
                        )
                    } ?: _uiState.value.branches

                    _uiState.value = _uiState.value.copy(
                        branches = parsedBranches,
                        currentRisk = (data["current_risk"] as? Number)?.toFloat() ?: 0.74f,
                        predictedStage = data["predicted_stage"] as? String ?: "Lateral Movement",
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
}
