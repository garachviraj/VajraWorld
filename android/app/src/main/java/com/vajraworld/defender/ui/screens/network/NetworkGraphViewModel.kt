package com.vajraworld.defender.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.TopologyEdge
import com.vajraworld.defender.domain.model.TopologyNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NetworkGraphUiState(
    val nodes: List<TopologyNode> = listOf(
        TopologyNode("device_core", "Physical Device Core", "Host", "Critical", 0.05f, x = 320f, y = 240f),
        TopologyNode("gw_net", "Wi-Fi / Gateway", "Device", "High", 0.15f, x = 320f, y = 100f),
        TopologyNode("app_sandbox_1", "App Sandbox A", "App", "Medium", 0.20f, x = 120f, y = 200f),
        TopologyNode("app_sandbox_2", "App Sandbox B", "App", "Low", 0.08f, x = 520f, y = 200f),
        TopologyNode("vault_otp", "OTP Privacy Vault", "Vault", "Low", 0.02f, x = 120f, y = 380f),
        TopologyNode("notif_guard", "Notification Guard", "Service", "Low", 0.05f, x = 520f, y = 380f)
    ),
    val edges: List<TopologyEdge> = listOf(
        TopologyEdge("device_core", "gw_net", "UPLINK", 1.0f, 443),
        TopologyEdge("app_sandbox_1", "device_core", "SANDBOX_IPC", 0.8f, 0),
        TopologyEdge("app_sandbox_2", "device_core", "SANDBOX_IPC", 0.8f, 0),
        TopologyEdge("notif_guard", "vault_otp", "TRIAGE_HASH", 0.9f, 0),
        TopologyEdge("device_core", "notif_guard", "LISTENER_BIND", 1.0f, 0)
    ),
    val selectedNode: TopologyNode? = null,
    val isLiveFromBackend: Boolean = false,
    val isLoading: Boolean = false
)

class NetworkGraphViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkGraphUiState())
    val uiState: StateFlow<NetworkGraphUiState> = _uiState.asStateFlow()

    init {
        fetchGraph()
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
