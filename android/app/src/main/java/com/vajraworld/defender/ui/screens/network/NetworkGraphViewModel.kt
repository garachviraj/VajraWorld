package com.vajraworld.defender.ui.screens.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.engine.NetworkConnectionMonitor
import com.vajraworld.defender.domain.engine.NetworkTrafficOverview
import com.vajraworld.defender.domain.model.TopologyEdge
import com.vajraworld.defender.domain.model.TopologyNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NetworkGraphUiState(
    val nodes: List<TopologyNode> = emptyList(),
    val edges: List<TopologyEdge> = emptyList(),
    val selectedNode: TopologyNode? = null,
    val isLiveFromBackend: Boolean = false,
    val isLoading: Boolean = false,
    val trafficOverview: NetworkTrafficOverview? = null,
    val selectedTab: String = "PACKETS" // "SOCKETS", "PACKETS", "TOPOLOGY"
)

class NetworkGraphViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkGraphUiState())
    val uiState: StateFlow<NetworkGraphUiState> = _uiState.asStateFlow()

    fun selectTab(tab: String) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun refreshSockets(context: Context) {
        viewModelScope.launch {
            try {
                val overview = NetworkConnectionMonitor.inspectActiveConnections(context)
                _uiState.value = _uiState.value.copy(trafficOverview = overview)

                // Dynamically build real topology from device sockets if needed
                buildDynamicTopologyFromSockets(overview)
            } catch (_: Exception) {}
        }
    }

    private fun buildDynamicTopologyFromSockets(overview: NetworkTrafficOverview) {
        val dynNodes = mutableListOf<TopologyNode>()
        val dynEdges = mutableListOf<TopologyEdge>()

        // Gateway Hub
        dynNodes.add(
            TopologyNode(
                id = "gw_0",
                label = "Android Gateway",
                type = "Gateway",
                riskScore = 0.05f,
                criticality = "Critical",
                x = 180f,
                y = 150f
            )
        )

        val uniqueApps = overview.activeConnections.distinctBy { it.packageName }.take(5)
        uniqueApps.forEachIndexed { idx, app ->
            val angle = (idx * (2 * Math.PI / uniqueApps.size)).toFloat()
            val radius = 100f
            val nx = 180f + radius * kotlin.math.cos(angle)
            val ny = 150f + radius * kotlin.math.sin(angle)

            val isThreat = app.riskLevel != "SECURE"
            val node = TopologyNode(
                id = "app_$idx",
                label = app.appName.take(12),
                type = "App Socket",
                riskScore = if (isThreat) 0.65f else 0.15f,
                criticality = if (isThreat) "High" else "Normal",
                x = nx,
                y = ny
            )
            dynNodes.add(node)
            dynEdges.add(
                TopologyEdge(
                    source = "gw_0",
                    target = "app_$idx",
                    type = "SOCKET_FLOW"
                )
            )
        }

        _uiState.value = _uiState.value.copy(
            nodes = dynNodes,
            edges = dynEdges
        )
    }

    fun selectNode(node: TopologyNode) {
        _uiState.value = _uiState.value.copy(selectedNode = node)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedNode = null)
    }
}
