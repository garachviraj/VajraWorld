package com.vajraworld.defender.ui.screens.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.engine.AppPermissionUsageRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionTimelineUiState(
    val records: List<AppPermissionUsageRecord> = emptyList(),
    val filteredRecords: List<AppPermissionUsageRecord> = emptyList(),
    val selectedFilter: String = "ALL",
    val isLoading: Boolean = false,
    val totalAppsAudited: Int = 0,
    val toxicCombinationCount: Int = 0
)

class PermissionTimelineViewModel(private val repository: VajraRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionTimelineUiState())
    val uiState: StateFlow<PermissionTimelineUiState> = _uiState.asStateFlow()

    init {
        loadPermissionTimeline()
    }

    fun loadPermissionTimeline() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val timeline = repository.getPermissionUsageTimeline()
            val toxicCount = timeline.count { it.toxicCombinations.isNotEmpty() }
            _uiState.value = _uiState.value.copy(
                records = timeline,
                filteredRecords = applyFilter(timeline, _uiState.value.selectedFilter),
                totalAppsAudited = timeline.size,
                toxicCombinationCount = toxicCount,
                isLoading = false
            )
        }
    }

    fun setFilter(filter: String) {
        val filtered = applyFilter(_uiState.value.records, filter)
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredRecords = filtered
        )
    }

    private fun applyFilter(records: List<AppPermissionUsageRecord>, filter: String): List<AppPermissionUsageRecord> {
        return when (filter.uppercase()) {
            "TOXIC" -> records.filter { it.toxicCombinations.isNotEmpty() }
            "CAMERA" -> records.filter { it.sensitivePermissions.any { p -> p.permissionName.contains("CAMERA") } }
            "AUDIO", "MICROPHONE" -> records.filter { it.sensitivePermissions.any { p -> p.permissionName.contains("RECORD_AUDIO") } }
            "LOCATION" -> records.filter { it.sensitivePermissions.any { p -> p.permissionName.contains("LOCATION") } }
            "SMS" -> records.filter { it.sensitivePermissions.any { p -> p.permissionName.contains("SMS") } }
            "OVERLAY" -> records.filter { it.sensitivePermissions.any { p -> p.permissionName.contains("SYSTEM_ALERT_WINDOW") } }
            else -> records
        }
    }
}
