package com.vajraworld.defender.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.ModelHealthData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ModelHealthData(
            modelVersion = "vw-0.8.0-local",
            activeModelId = "m-vw-hybrid-0.8.0",
            status = "TRAINED",
            calibration = "temperature_v3",
            accuracy = 0.95f,
            brierScore = 0.06f,
            leadTimeSec = 180.0f,
            sensorCoverage = 0.98f,
            telemetryFreshnessSec = 1.2f,
            oodRate = 0.02f,
            driftScore = 0.01f,
            inferenceLatencyMs = 14.2f,
            environmentProfile = "Enterprise IT"
        )
    )
    val uiState: StateFlow<ModelHealthData> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchModelHealth()
    }

    fun fetchModelHealth() {
        if (repository == null) return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getModelStatus()
            if (res.isSuccess) {
                val data = res.getOrNull()
                if (data != null) {
                    _uiState.value = data
                }
            }
            _isLoading.value = false
        }
    }

    fun setEnvironmentProfile(profile: String) {
        _uiState.value = _uiState.value.copy(environmentProfile = profile)
    }
}
