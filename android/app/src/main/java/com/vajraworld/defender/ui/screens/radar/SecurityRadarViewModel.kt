package com.vajraworld.defender.ui.screens.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.RadarEdge
import com.vajraworld.defender.domain.model.RadarNode
import com.vajraworld.defender.domain.model.SecurityRadarState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SecurityRadarViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SecurityRadarState(
            radarTitle = "VAJRAWORLD GUARDIAN LIVE SECURITY RADAR",
            overallStatus = "ACTIVE RADAR MONITORING",
            overallHealth = 78,
            nodes = listOf(
                RadarNode("notif", "Notification", "NOTIFICATION", 25, "MONITORING", x = 180f, y = 140f),
                RadarNode("link", "Link Guardian", "LINK", 30, "NOMINAL", x = 380f, y = 140f),
                RadarNode("file", "File / APK", "FILE", 20, "SECURE", x = 460f, y = 280f),
                RadarNode("network", "Network", "NETWORK", 35, "STABLE", x = 280f, y = 390f),
                RadarNode("exposure", "User Exposure", "USER", 18, "PROTECTED", x = 120f, y = 280f),
                RadarNode("otp", "OTP Vault", "OTP", 5, "ZERO_STORAGE", x = 280f, y = 240f)
            ),
            edges = listOf(
                RadarEdge("notif", "link", "CONTAINS", isPredicted = false),
                RadarEdge("link", "file", "DOWNLOADS", isPredicted = false),
                RadarEdge("file", "network", "CONNECTS_TO", isPredicted = false),
                RadarEdge("link", "otp", "PROMPTS_FOR", isPredicted = true),
                RadarEdge("network", "exposure", "EXFILTRATES", isPredicted = true)
            )
        )
    )
    val uiState: StateFlow<SecurityRadarState> = _uiState.asStateFlow()

    private val _selectedNode = MutableStateFlow<RadarNode?>(null)
    val selectedNode: StateFlow<RadarNode?> = _selectedNode.asStateFlow()

    init {
        startLiveRadarPolling()
    }

    private fun startLiveRadarPolling() {
        if (repository == null) return
        viewModelScope.launch {
            while (isActive) {
                val res = repository.getSecurityRadar()
                if (res.isSuccess) {
                    val state = res.getOrNull()
                    if (state != null && state.nodes.isNotEmpty()) {
                        _uiState.value = state
                    }
                }
                delay(2500)
            }
        }
    }

    fun selectNode(node: RadarNode) {
        _selectedNode.value = node
    }

    fun clearSelection() {
        _selectedNode.value = null
    }
}
