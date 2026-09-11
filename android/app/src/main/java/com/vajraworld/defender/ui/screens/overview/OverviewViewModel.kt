package com.vajraworld.defender.ui.screens.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.DriverSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OverviewUiState(
    val networkHealth: Int = 82,
    val forecastRisk: Int = 68,
    val riskDelta: String = "+14%",
    val predictedStage: String = "Lateral Movement",
    val etaSeconds: Int = 134,
    val criticalAsset: String = "Finance-DB-02",
    val horizonBars: List<Float> = listOf(0.18f, 0.31f, 0.57f, 0.74f, 0.81f),
    val topDrivers: List<DriverSignal> = listOf(
        DriverSignal("east_west_fanout", 0.19f, "up"),
        DriverSignal("smb_edge_novelty", 0.13f, "up"),
        DriverSignal("syn_burstiness", 0.11f, "up")
    ),
    val coverage: String = "NOMINAL (96%)",
    val isLoading: Boolean = false
)

class OverviewViewModel(private val repository: VajraRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        refreshOverview()
    }

    fun refreshOverview() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getForecast()
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) {
                    val risk = ((data["current_risk"] as? Number)?.toFloat() ?: 0.68f) * 100
                    val stage = data["predicted_stage"] as? String ?: "Lateral Movement"
                    val eta = (data["lead_time_sec"] as? Number)?.toInt() ?: 134
                    val assets = (data["critical_assets"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("Finance-DB-02")
                    val rawHorizons = (data["horizon_risks"] as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() }
                    
                    _uiState.value = _uiState.value.copy(
                        forecastRisk = risk.toInt(),
                        predictedStage = stage,
                        etaSeconds = eta,
                        criticalAsset = assets.firstOrNull() ?: "Finance-DB-02",
                        horizonBars = rawHorizons ?: listOf(0.18f, 0.31f, 0.57f, 0.74f, 0.81f),
                        isLoading = false
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
