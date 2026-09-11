package com.vajraworld.defender.ui.screens.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.Incident
import com.vajraworld.defender.ui.theme.*

@Composable
fun IncidentsScreen(
    viewModel: IncidentsViewModel,
    onSelectIncident: (Incident) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "INCIDENTS & ALERTS", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                Text(text = "ATT&CK-Aligned Trajectory Alerts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(text = "${state.incidents.size} ACTIVE", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.incidents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No active critical incidents.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.incidents) { inc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (inc.risk > 0.7f) ThreatRed else BorderDark, RoundedCornerShape(8.dp))
                            .clickable { onSelectIncident(inc) },
                        colors = CardDefaults.cardColors(containerColor = CardDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = inc.incidentId, style = MaterialTheme.typography.titleMedium, color = AccentCyan)
                                Text(
                                    text = "${(inc.risk * 100).toInt()}% RISK",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (inc.risk > 0.7f) ThreatRed else WarningAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = inc.title, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Stage: ${inc.predictedStage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = if (inc.acknowledged) "INVESTIGATING" else "ACTION REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (inc.acknowledged) SafeGreen else ThreatRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
