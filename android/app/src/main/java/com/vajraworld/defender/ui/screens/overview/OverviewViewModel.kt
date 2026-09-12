package com.vajraworld.defender.ui.screens.overview

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.DeviceScanProgress
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.engine.AppSecurityAudit
import com.vajraworld.defender.domain.engine.RealDeviceTelemetry
import com.vajraworld.defender.domain.model.DriverSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OverviewUiState(
    val networkHealth: Int = 96,
    val forecastRisk: Int = 4,
    val riskDelta: String = "NOMINAL",
    val predictedStage: String = "ACTIVE DEFENSE",
    val etaSeconds: Int = 180,
    val criticalAsset: String = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }} ${Build.MODEL}",
    val horizonBars: List<Float> = listOf(0.04f, 0.08f, 0.12f, 0.16f, 0.20f),
    val observedHistory: List<Float> = listOf(0.04f, 0.04f, 0.05f, 0.04f, 0.04f),
    val forecastTrajectory: List<Float> = listOf(0.04f, 0.08f, 0.12f),
    val uncertainty: Float = 0.03f,
    val topDrivers: List<DriverSignal> = listOf(
        DriverSignal("sandbox_isolation", 0.05f, "up"),
        DriverSignal("integrity_attestation", 0.02f, "up"),
        DriverSignal("network_gateway", 0.04f, "up")
    ),
    val coverage: String = "ON-DEVICE REAL TIME",
    val activeFlowsCount: Int = 0,
    val eventsPerSec: Float = 14.5f,
    val radarStatus: String = "ACTIVE REAL TIME DEFENDER",
    val isLiveConnected: Boolean = true,
    val isSynthetic: Boolean = false,
    val mode: String = "ON_DEVICE_REAL_TELEMETRY",
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val scanProgress: DeviceScanProgress? = null,
    val deviceTelemetry: RealDeviceTelemetry? = null,
    val scannedAudit: AppSecurityAudit? = null,
    val screenShareStatus: com.vajraworld.defender.domain.engine.ScreenShareStatus? = null,
    val callSecurityStatus: com.vajraworld.defender.domain.engine.CallSecurityStatus? = null
)

class OverviewViewModel(private val repository: VajraRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        initOnDeviceTelemetry()
        startLiveStreamingLoop()
    }

    private fun initOnDeviceTelemetry() {
        viewModelScope.launch {
            val telemetry = repository.getDeviceTelemetry()
            val screenShare = repository.checkScreenSharing()
            val callStatus = repository.checkCallSecurity()
            if (telemetry != null) {
                _uiState.value = _uiState.value.copy(
                    criticalAsset = telemetry.hardware.deviceName,
                    forecastRisk = telemetry.overallRiskScore,
                    networkHealth = 100 - telemetry.overallRiskScore,
                    predictedStage = telemetry.postureLabel,
                    deviceTelemetry = telemetry,
                    screenShareStatus = screenShare,
                    callSecurityStatus = callStatus
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    screenShareStatus = screenShare,
                    callSecurityStatus = callStatus
                )
            }
        }
    }

    private fun startLiveStreamingLoop() {
        viewModelScope.launch {
            while (isActive) {
                fetchLiveData()
                delay(3000) // Live real-time analysis pulse every 3s
            }
        }
    }

    fun startDeviceScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            repository.performFullDeviceScan().collect { progress ->
                val updatedTelemetry = progress.telemetry ?: _uiState.value.deviceTelemetry
                val updatedAudit = progress.auditResult ?: _uiState.value.scannedAudit

                _uiState.value = _uiState.value.copy(
                    scanProgress = progress,
                    deviceTelemetry = updatedTelemetry,
                    scannedAudit = updatedAudit,
                    activeFlowsCount = if (progress.totalApps > 0) progress.totalApps else _uiState.value.activeFlowsCount,
                    criticalAsset = updatedTelemetry?.hardware?.deviceName ?: _uiState.value.criticalAsset,
                    forecastRisk = updatedTelemetry?.overallRiskScore ?: _uiState.value.forecastRisk,
                    networkHealth = if (updatedTelemetry != null) (100 - updatedTelemetry.overallRiskScore) else _uiState.value.networkHealth,
                    predictedStage = updatedTelemetry?.postureLabel ?: _uiState.value.predictedStage,
                    isScanning = !progress.isComplete
                )
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
                val health = (data["network_health"] as? Number)?.toInt() ?: 96
                val risk = (data["current_risk_pct"] as? Number)?.toInt() ?: 4
                val stage = data["predicted_stage"] as? String ?: "ACTIVE DEFENSE"
                val eta = (data["lead_time_sec"] as? Number)?.toInt() ?: 180
                val asset = data["critical_asset"] as? String ?: "${Build.MANUFACTURER} ${Build.MODEL}"
                val flows = (data["active_flows_count"] as? Number)?.toInt() ?: _uiState.value.activeFlowsCount
                val eps = (data["events_per_sec"] as? Number)?.toFloat() ?: 14.5f
                val status = data["radar_status"] as? String ?: "REAL-TIME ON-DEVICE DEFENDER"
                val isSynthetic = data["is_synthetic"] as? Boolean ?: false
                val mode = data["mode"] as? String ?: (if (isSynthetic) "SYNTHETIC_SIMULATION" else "ON_DEVICE_REAL_TELEMETRY")
                val uncertainty = (data["uncertainty"] as? Number)?.toFloat() ?: 0.03f
                val rawHorizons = (data["horizon_risks"] as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() }

                val curRiskFloat = risk / 100f
                val updatedObserved = (_uiState.value.observedHistory + curRiskFloat).takeLast(5)
                val updatedForecast = rawHorizons?.take(3) ?: _uiState.value.forecastTrajectory

                val currentTelemetry = repository.getDeviceTelemetry() ?: _uiState.value.deviceTelemetry
                val currentScreenShare = repository.checkScreenSharing()
                val currentCallStatus = repository.checkCallSecurity()

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
                    isLiveConnected = true,
                    deviceTelemetry = currentTelemetry,
                    screenShareStatus = currentScreenShare,
                    callSecurityStatus = currentCallStatus
                )
                return
            }
        }

        // Fallback to standard forecast
        val forecastResult = repository.getForecast()
        if (forecastResult.isSuccess) {
            val data = forecastResult.getOrNull()
            if (data != null) {
                val risk = ((data["current_risk"] as? Number)?.toFloat() ?: 0.05f) * 100
                val stage = data["predicted_stage"] as? String ?: "NOMINAL"
                val eta = (data["lead_time_sec"] as? Number)?.toInt() ?: 180
                val assets = (data["critical_assets"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("${Build.MANUFACTURER} ${Build.MODEL}")
                val rawHorizons = (data["horizon_risks"] as? List<*>)?.mapNotNull { (it as? Number)?.toFloat() }

                _uiState.value = _uiState.value.copy(
                    forecastRisk = risk.toInt(),
                    predictedStage = stage,
                    etaSeconds = eta,
                    criticalAsset = assets.firstOrNull() ?: "${Build.MANUFACTURER} ${Build.MODEL}",
                    horizonBars = rawHorizons ?: _uiState.value.horizonBars,
                    isLiveConnected = true
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(isLiveConnected = false)
        }
    }
}
