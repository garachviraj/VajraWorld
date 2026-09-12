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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.Incident
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun IncidentsScreen(
    viewModel: IncidentsViewModel,
    onSelectIncident: (Incident) -> Unit,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
    ) {
        VajraTopBar(
            title = "INCIDENTS & ALERTS",
            subtitle = "ATT&CK TRAJECTORY SURVEILLANCE",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface0)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE INTRUSION CORRELATIONS",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                )
                Box(
                    modifier = Modifier
                        .background(CriticalBg, RoundedCornerShape(4.dp))
                        .border(1.dp, CriticalBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${state.incidents.size} ACTIVE",
                        style = TechnicalValue.copy(fontSize = 10.sp, color = Critical, fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (state.incidents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active critical incidents. System state nominal.",
                        style = MetadataText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.incidents) { inc ->
                        val isHighRisk = inc.risk > 0.6f
                        val borderCol = when (inc.status) {
                            "RESOLVED" -> HealthyBorder
                            "CONTAINED" -> InfoBorder
                            else -> if (isHighRisk) CriticalBorder else BorderColor
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                .clickable { onSelectIncident(inc) },
                            colors = CardDefaults.cardColors(containerColor = Surface0)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = inc.incidentId,
                                            style = TechnicalValue.copy(fontSize = 12.sp, color = Info, fontWeight = FontWeight.Bold)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when (inc.status) {
                                                        "RESOLVED" -> HealthyBg
                                                        "CONTAINED" -> InfoBg
                                                        else -> CriticalBg
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = inc.status,
                                                style = TechnicalValue.copy(
                                                    fontSize = 9.sp,
                                                    color = when (inc.status) {
                                                        "RESOLVED" -> Healthy
                                                        "CONTAINED" -> Info
                                                        else -> Critical
                                                    }
                                                )
                                            )
                                        }
                                    }
                                    SecurityStatusPill(riskScore = (inc.risk * 100).toInt())
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = inc.title,
                                    style = Typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = inc.mitreTactic,
                                        style = TechnicalValue.copy(fontSize = 10.sp, color = Warning)
                                    )
                                    Text(
                                        text = "ETA ~${inc.etaSeconds}s",
                                        style = MetadataText.copy(fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
