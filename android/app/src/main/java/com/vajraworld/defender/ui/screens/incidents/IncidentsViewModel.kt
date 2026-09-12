package com.vajraworld.defender.ui.screens.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncidentsUiState(
    val incidents: List<Incident> = emptyList(),
    val selectedIncident: Incident? = null,
    val isLoading: Boolean = false
)

class IncidentsViewModel(private val repository: VajraRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(IncidentsUiState())
    val uiState: StateFlow<IncidentsUiState> = _uiState.asStateFlow()

    init {
        loadIncidents()
    }

    fun loadIncidents() {
        viewModelScope.launch {
            repository.incidentsFlow.collect { list ->
                _uiState.value = _uiState.value.copy(
                    incidents = list,
                    selectedIncident = list.firstOrNull()
                )
            }
        }
        viewModelScope.launch {
            repository.refreshIncidents()
        }
    }

    fun selectIncident(incident: Incident) {
        _uiState.value = _uiState.value.copy(selectedIncident = incident)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIncident = null)
    }

    fun acknowledgeIncident(id: String) {
        viewModelScope.launch {
            repository.acknowledgeIncident(id)
        }
    }
}
