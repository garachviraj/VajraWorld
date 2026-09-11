package com.vajraworld.defender.ui.screens.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.SimulationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimulationUiState(
    val targetAsset: String = "Host-17",
    val selectedAction: String = "ISOLATE_HOST",
    val availableActions: List<String> = listOf(
        "ISOLATE_HOST",
        "BLOCK_PORT",
        "DISABLE_ACCOUNT",
        "SEGMENT_SUBNET",
        "RATE_LIMIT"
    ),
    val lastResult: SimulationResult? = SimulationResult(
        simulationId = "sim_init",
        targetAsset = "Host-17",
        actionType = "ISOLATE_HOST",
        baselineRisk = 0.78f,
        postActionRisk = 0.23f,
        residualRisk = 0.23f,
        riskReductionPct = 55,
        newLikelyStage = "Benign / Contained",
        disruptionRating = "Medium",
        utilityScore = 0.42f,
        isRecommended = true
    ),
    val isRunning: Boolean = false
)

class SimulationViewModel(private val repository: VajraRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    fun selectAction(action: String) {
        _uiState.value = _uiState.value.copy(selectedAction = action)
    }

    fun selectTargetAsset(asset: String) {
        _uiState.value = _uiState.value.copy(targetAsset = asset)
    }

    fun runSimulation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true)
            val res = repository.runSimulation(
                targetAsset = _uiState.value.targetAsset,
                actionType = _uiState.value.selectedAction
            )
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    lastResult = res.getOrNull(),
                    isRunning = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isRunning = false)
            }
        }
    }
}
