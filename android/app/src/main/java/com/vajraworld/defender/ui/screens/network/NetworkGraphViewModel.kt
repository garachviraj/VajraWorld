package com.vajraworld.defender.ui.screens.network
 
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.engine.NetworkConnectionMonitor
import com.vajraworld.defender.domain.engine.NetworkTrafficOverview
import com.vajraworld.defender.domain.model.TopologyEdge
import com.vajraworld.defender.domain.model.TopologyNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NetworkGraphUiState(
    val nodes: List<TopologyNode> = emptyList(),
    val edges: List<TopologyEdge> = emptyList(),
    val selectedNode: TopologyNode? = null,
    val isLiveFromBackend: Boolean = false,
    val isLoading: Boolean = false,
    val trafficOverview: NetworkTrafficOverview? = null,
    val selectedTab: String = "CONNECTIONS" // "CONNECTIONS" or "TOPOLOGY"
)

class NetworkGraphViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkGraphUiState())
    val uiState: StateFlow<NetworkGraphUiState> = _uiState.asStateFlow()

    init {
        fetchGraph()
    }

    fun selectTab(tab: String) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun refreshSockets(context: Context) {
        viewModelScope.launch {
            try {
                val overview = NetworkConnectionMonitor.inspectActiveConnections(context)
                _uiState.value = _uiState.value.copy(trafficOverview = overview)
            } catch (_: Exception) {}
        }
    }

    fun fetchGraph() {
        if (repository == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getCurrentGraph()
            if (res.isSuccess) {
                val pair = res.getOrNull()
                if (pair != null && pair.first.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        nodes = pair.first,
                        edges = pair.second,
                        isLiveFromBackend = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun selectNode(node: TopologyNode) {
        _uiState.value = _uiState.value.copy(selectedNode = node)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedNode = null)
    }
}
