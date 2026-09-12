package com.vajraworld.defender.ui.screens.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.DriverSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OverviewUiState(
    val networkHealth: Int = 84,
    val forecastRisk: Int = 32,
    val riskDelta: String = "+4%",
    val predictedStage: String = "Reconnaissance",
    val etaSeconds: Int = 118,
    val criticalAsset: String = "Finance-DB-02",
    val horizonBars: List<Float> = listOf(0.18f, 0.24f, 0.32f, 0.45f, 0.58f),
    val observedHistory: List<Float> = listOf(0.12f, 0.16f, 0.18f, 0.22f, 0.28f),
    val forecastTrajectory: List<Float> = listOf(0.32f, 0.45f, 0.58f),
    val uncertainty: Float = 0.08f,
    val topDrivers: List<DriverSignal> = listOf(
        DriverSignal("east_west_fanout", 0.19f, "up"),
        DriverSignal("smb_edge_novelty", 0.13f, "up"),
        DriverSignal("syn_burstiness", 0.11f, "up")
    ),
    val coverage: String = "NOMINAL (98%)",
    val activeFlowsCount: Int = 142,
    val eventsPerSec: Float = 24.5f,
    val radarStatus: String = "ACTIVE SURVEILLANCE",
    val isLiveConnected: Boolean = true,
    val isSynthetic: Boolean = false,
    val mode: String = "LIVE",
    val isLoading: Boolean = false
)

class OverviewViewModel(private val repository: VajraRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        startLiveStreamingLoop()
    }

    private fun startLiveStreamingLoop() {
        viewModelScope.launch {
            while (isActive) {
                fetchLiveData()
                delay(2500) // Live real-time analysis pulse every 2.5s
            }
        }
    }

    fun refreshOverview() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            fetchLiveData()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun fetchLiveData() {
        val liveResult = repository.getLiveSummary()
        if (liveResult.isSuccess) {
            val data = liveResult.getOrNull()
            if (data != null) {
                val health = (data["network_health"] as? Number)?.toInt() ?: 84
                val risk = (data["current_risk_pct"] as? Number)?.toInt() ?: 32
                val stage = data["predicted_stage"] as? String ?: "Reconnaissance"
                val eta = (data["lead_time_sec"] as? Number)?.toInt() ?: 118
                val asset = data["critical_asset"] as? String ?: "Finance-DB-02"
                val flows = (data["active_flows_count"] as? Number)?.toInt() ?: 142
                val eps = (data["events_per_sec"] as? Number)?.toFloat() ?: 24.5f
                val status = data["radar_status"] as? String ?: "ACTIVE SURVEILLANCE"
                val isSynthetic = data["is_synthetic"] as? Boolean ?: false
                val mode = data["mode"] as? String ?: (if (isSynthetic) "SYNTHETIC_SIMULATION" else "LIVE_DEVICE")
                val uncertainty = (data["uncertainty"] as? Number)?.toFloat() ?: 0.08f
                val rawHorizons = (data["horizon_risks"] as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() }

                val curRiskFloat = risk / 100f
                val updatedObserved = (_uiState.value.observedHistory + curRiskFloat).takeLast(5)
                val updatedForecast = rawHorizons?.take(3) ?: _uiState.value.forecastTrajectory

                _uiState.value = _uiState.value.copy(
                    networkHealth = health,
                    forecastRisk = risk,
                    predictedStage = stage,
                    etaSeconds = eta,
                    criticalAsset = asset,
                    activeFlowsCount = flows,
                    eventsPerSec = eps,
                    radarStatus = status,
                    horizonBars = rawHorizons ?: _uiState.value.horizonBars,
                    observedHistory = updatedObserved,
                    forecastTrajectory = updatedForecast,
                    uncertainty = uncertainty,
                    isSynthetic = isSynthetic,
                    mode = mode,
                    isLiveConnected = true
                )
                return
            }
        }

        // Fallback to standard forecast
        val forecastResult = repository.getForecast()
        if (forecastResult.isSuccess) {
            val data = forecastResult.getOrNull()
            if (data != null) {
                val risk = ((data["current_risk"] as? Number)?.toFloat() ?: 0.5f) * 100
                val stage = data["predicted_stage"] as? String ?: "Lateral Movement"
                val eta = (data["lead_time_sec"] as? Number)?.toInt() ?: 120
                val assets = (data["critical_assets"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("Finance-DB-02")
                val rawHorizons = (data["horizon_risks"] as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() }

                _uiState.value = _uiState.value.copy(
                    forecastRisk = risk.toInt(),
                    predictedStage = stage,
                    etaSeconds = eta,
                    criticalAsset = assets.firstOrNull() ?: "Finance-DB-02",
                    horizonBars = rawHorizons ?: _uiState.value.horizonBars,
                    isLiveConnected = true
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(isLiveConnected = false)
        }
    }
}
