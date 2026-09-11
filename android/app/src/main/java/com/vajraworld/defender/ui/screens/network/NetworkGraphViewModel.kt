package com.vajraworld.defender.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.domain.model.TopologyEdge
import com.vajraworld.defender.domain.model.TopologyNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkGraphUiState(
    val nodes: List<TopologyNode> = listOf(
        TopologyNode("Host-17", "Host-17 (Workstation)", "Host", "Medium", 0.85f, x = 120f, y = 220f),
        TopologyNode("AD-01", "AD-01 (Domain Ctrl)", "Server", "Critical", 0.70f, x = 320f, y = 140f),
        TopologyNode("Finance-DB-02", "Finance-DB-02", "Server", "Critical", 0.90f, x = 520f, y = 220f),
        TopologyNode("DMZ-GW", "DMZ-Gateway", "Device", "High", 0.20f, x = 120f, y = 380f),
        TopologyNode("Host-22", "Host-22 (Client)", "Host", "Low", 0.10f, x = 320f, y = 380f),
        TopologyNode("OT-PLC-04", "OT-PLC-04", "OT-Asset", "Critical", 0.05f, x = 520f, y = 380f)
    ),
    val edges: List<TopologyEdge> = listOf(
        TopologyEdge("Host-17", "AD-01", "AUTHENTICATES_TO", 1.0f, 88),
        TopologyEdge("AD-01", "Finance-DB-02", "ACCESSES", 1.0f, 445),
        TopologyEdge("Host-17", "Finance-DB-02", "SCANS", 0.8f, 445),
        TopologyEdge("Host-17", "DMZ-GW", "CONNECTS_TO", 0.4f, 443),
        TopologyEdge("Host-22", "DMZ-GW", "CONNECTS_TO", 0.2f, 80)
    ),
    val selectedNode: TopologyNode? = null
)

class NetworkGraphViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkGraphUiState())
    val uiState: StateFlow<NetworkGraphUiState> = _uiState.asStateFlow()

    fun selectNode(node: TopologyNode) {
        _uiState.value = _uiState.value.copy(selectedNode = node)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedNode = null)
    }
}
