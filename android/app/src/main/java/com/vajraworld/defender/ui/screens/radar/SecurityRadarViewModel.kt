package com.vajraworld.defender.ui.screens.radar

import androidx.lifecycle.ViewModel
import com.vajraworld.defender.domain.model.RadarEdge
import com.vajraworld.defender.domain.model.RadarNode
import com.vajraworld.defender.domain.model.SecurityRadarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityRadarViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        SecurityRadarState(
            radarTitle = "VAJRAWORLD GUARDIAN LIVE SECURITY RADAR",
            overallStatus = "ATTACK TRAJECTORY DETECTED",
            overallHealth = 68,
            nodes = listOf(
                RadarNode("notif", "Notification", "NOTIFICATION", 60, "EVALUATING", x = 200f, y = 140f),
                RadarNode("link", "Link Guardian", "LINK", 75, "SUSPICIOUS", x = 400f, y = 140f),
                RadarNode("file", "File / APK", "FILE", 85, "THREAT", x = 500f, y = 300f),
                RadarNode("network", "Network", "NETWORK", 80, "BEACONING", x = 320f, y = 420f),
                RadarNode("exposure", "User Exposure", "USER", 65, "ELEVATED", x = 140f, y = 300f),
                RadarNode("otp", "OTP Vault", "OTP", 15, "PROTECTED", x = 320f, y = 260f)
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

    fun selectNode(node: RadarNode) {
        _selectedNode.value = node
    }

    fun clearSelection() {
        _selectedNode.value = null
    }
}
