package com.vajraworld.defender.ui.screens.health

import androidx.lifecycle.ViewModel
import com.vajraworld.defender.domain.model.ModelHealthData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HealthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        ModelHealthData(
            modelVersion = "vw-0.8.0",
            activeModelId = "m-vw-hybrid-0.8.0",
            status = "HEALTHY",
            calibration = "temperature_v3",
            accuracy = 0.942f,
            brierScore = 0.081f,
            leadTimeSec = 74.5f,
            sensorCoverage = 0.96f,
            telemetryFreshnessSec = 1.2f,
            oodRate = 0.024f,
            driftScore = 0.018f,
            inferenceLatencyMs = 14.2f,
            environmentProfile = "Enterprise IT"
        )
    )
    val uiState: StateFlow<ModelHealthData> = _uiState.asStateFlow()

    fun setEnvironmentProfile(profile: String) {
        _uiState.value = _uiState.value.copy(environmentProfile = profile)
    }
}
