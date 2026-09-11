package com.vajraworld.defender.ui.screens.explainability

import androidx.lifecycle.ViewModel
import com.vajraworld.defender.domain.model.AttributionItem
import com.vajraworld.defender.domain.model.ExplainabilityData
import com.vajraworld.defender.domain.model.TemporalEventItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExplainabilityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        ExplainabilityData(
            forecastId = "fc_demo_2041",
            narrative = "The forecast increased because Host-17 contacted 31 new internal destinations in 45 seconds, the RST/SYN ratio changed sharply, and a previously rare SMB/RPC connection appeared between workstation and high-value database segments.",
            attributions = listOf(
                AttributionItem("internal_destination_fanout", 0.19f, "up", "Host-to-host discovery surge"),
                AttributionItem("new_smb_rpc_edge", 0.13f, "up", "Access probing on port 445"),
                AttributionItem("syn_burstiness", 0.11f, "up", "High-frequency TCP handshake burst"),
                AttributionItem("failed_auth_ratio", 0.09f, "up", "Kerberos ticket failure burst"),
                AttributionItem("known_management_traffic", -0.04f, "down", "Routine IT background signal")
            ),
            temporalEvents = listOf(
                TemporalEventItem("T-120s", "NORMAL", "Routine workstation HTTP/DNS activity", 0.12f),
                TemporalEventItem("T-90s", "ANOMALY_LOW", "Port scanning detected across subnet", 0.28f),
                TemporalEventItem("T-60s", "RECON_SURGE", "31 destinations probed on ports 135, 445", 0.49f),
                TemporalEventItem("T-30s", "CRED_BURST", "Kerberos ticket requests and auth failures", 0.68f),
                TemporalEventItem("T-00s", "LATERAL_PREDICTED", "Transition to AD-01 & Finance-DB-02 active", 0.81f),
                TemporalEventItem("T+30s", "FORECAST_PIVOT", "Expected compromise of database credential", 0.88f)
            ),
            centerNode = "Host-17",
            uncertaintyWarning = "Nominal: Sensor telemetry coverage at 96% across enterprise core."
        )
    )
    val uiState: StateFlow<ExplainabilityData> = _uiState.asStateFlow()
}
