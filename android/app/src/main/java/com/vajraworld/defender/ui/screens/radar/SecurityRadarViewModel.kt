package com.vajraworld.defender.ui.screens.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vajraworld.defender.data.local.ClipboardLogEntity
import com.vajraworld.defender.data.local.ScanResultEntity
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.model.Incident
import com.vajraworld.defender.domain.model.RadarEdge
import com.vajraworld.defender.domain.model.RadarNode
import com.vajraworld.defender.domain.model.SecurityRadarState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SecurityRadarViewModel(private val repository: VajraRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SecurityRadarState(
            radarTitle = "VAJRAWORLD GUARDIAN LIVE SECURITY RADAR",
            overallStatus = "ACTIVE RADAR MONITORING",
            overallHealth = 85,
            nodes = listOf(
                RadarNode("notif", "Notification", "NOTIFICATION", 20, "MONITORING", x = 180f, y = 140f),
                RadarNode("link", "Link Guardian", "LINK", 22, "NOMINAL", x = 380f, y = 140f),
                RadarNode("file", "File / APK", "FILE", 18, "SECURE", x = 460f, y = 280f),
                RadarNode("network", "Network", "NETWORK", 25, "STABLE", x = 280f, y = 390f),
                RadarNode("exposure", "User Exposure", "USER", 15, "PROTECTED", x = 120f, y = 280f),
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

        // 1. Dynamic on-device Room DB reactive updates
        viewModelScope.launch {
            try {
                combine(
                    repository.incidentsFlow,
                    repository.daoSync().getUrlScanHistory(),
                    repository.daoSync().getFileScanHistory(),
                    repository.daoSync().getAllClipboardLogs()
                ) { incidents, urls, files, clips ->
                    updateRadarFromLocalState(incidents, urls, files, clips)
                }.collect()
            } catch (_: Exception) {}
        }

        // 2. Fallback / supplementary backend sync
        viewModelScope.launch {
            while (isActive) {
                try {
                    val res = repository.getSecurityRadar()
                    if (res.isSuccess) {
                        val state = res.getOrNull()
                        if (state != null && state.nodes.isNotEmpty()) {
                            _uiState.value = state
                        }
                    }
                } catch (_: Exception) {}
                delay(5000)
            }
        }
    }

    private fun updateRadarFromLocalState(
        incidents: List<Incident>,
        urls: List<ScanResultEntity>,
        files: List<ScanResultEntity>,
        clips: List<ClipboardLogEntity>
    ) {
        val activeIncidents = incidents.filter { it.status != "RESOLVED" }
        val maxUrlRisk = urls.take(5).maxOfOrNull { it.riskScore } ?: 18
        val maxFileRisk = files.take(5).maxOfOrNull { it.riskScore } ?: 15
        val hasSensitiveClip = clips.any { it.isSensitive }
        val clipRisk = if (hasSensitiveClip) 45 else 8
        val incRisk = if (activeIncidents.isNotEmpty()) (activeIncidents.maxOf { it.risk } * 100).toInt() else 18
        val netRisk = if (activeIncidents.any { it.incidentId.contains("NET", true) }) 78 else 22
        val notifRisk = if (activeIncidents.any { it.incidentId.contains("ADB", true) || it.incidentId.contains("APP", true) }) 65 else 20
        val exposureRisk = maxOf(maxUrlRisk, maxFileRisk, clipRisk)
        val otpRisk = if (hasSensitiveClip) 35 else 5

        val updatedNodes = listOf(
            RadarNode(
                "notif", "Notification", "NOTIFICATION", notifRisk,
                if (notifRisk > 50) "ALERT" else "MONITORING",
                x = 180f, y = 140f
            ),
            RadarNode(
                "link", "Link Guardian", "LINK", maxUrlRisk,
                if (maxUrlRisk > 55) "FLAGGED" else if (maxUrlRisk > 30) "ELEVATED" else "NOMINAL",
                x = 380f, y = 140f
            ),
            RadarNode(
                "file", "File / APK", "FILE", maxFileRisk,
                if (maxFileRisk > 55) "SUSPICIOUS" else if (maxFileRisk > 30) "AUDITED" else "SECURE",
                x = 460f, y = 280f
            ),
            RadarNode(
                "network", "Network", "NETWORK", netRisk,
                if (netRisk > 50) "HIGH_FLOW" else "STABLE",
                x = 280f, y = 390f
            ),
            RadarNode(
                "exposure", "User Exposure", "USER", exposureRisk,
                if (exposureRisk > 50) "EXPOSED" else "PROTECTED",
                x = 120f, y = 280f
            ),
            RadarNode(
                "otp", "OTP Vault", "OTP", otpRisk,
                if (otpRisk > 30) "MASKING_ACTIVE" else "ZERO_STORAGE",
                x = 280f, y = 240f
            )
        )

        val maxRisk = updatedNodes.maxOf { it.risk }
        val overallHealth = (100 - (maxRisk * 0.75f).toInt()).coerceIn(20, 98)
        val overallStatus = when {
            maxRisk >= 60 -> "ACTIVE THREAT DETECTED"
            maxRisk >= 35 -> "ELEVATED HEURISTIC SURVEILLANCE"
            else -> "NOMINAL RADAR DEFENSE"
        }

        _uiState.value = _uiState.value.copy(
            nodes = updatedNodes,
            overallHealth = overallHealth,
            overallStatus = overallStatus
        )
    }

    fun selectNode(node: RadarNode) {
        _selectedNode.value = node
    }

    fun clearSelection() {
        _selectedNode.value = null
    }
}
