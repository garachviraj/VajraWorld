package com.vajraworld.defender.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
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
import com.vajraworld.defender.ui.components.*
import com.vajraworld.defender.ui.theme.*

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onNavigateToSimulation: () -> Unit,
    onNavigateToRadar: () -> Unit = {},
    onNavigateToLinkScan: () -> Unit = {},
    onNavigateToFileScan: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        // Cockpit Top Bar (Section 9 & 50)
        VajraTopBar(
            title = "VAJRAWORLD",
            subtitle = "GUARDIAN COCKPIT",
            isSynthetic = state.isSynthetic
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Flow Status Bar
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.isLiveConnected) Healthy else Warning)
                    )
                    Text(
                        text = "${state.activeFlowsCount} ACTIVE FLOWS",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                    )
                    Text(
                        text = "• ${String.format(java.util.Locale.US, "%.1f", state.eventsPerSec)} evt/s",
                        style = MetadataText
                    )
                }

                SecurityStatusPill(riskScore = state.forecastRisk)
            }

            // Hero Security State Orb (Section 10)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURITY WORLD STATE",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                        )
                        Text(
                            text = "COVERAGE: ${state.coverage}",
                            style = MetadataText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SecurityStatusOrb(
                        currentRisk = state.forecastRisk / 100f,
                        forecastRisk = (state.forecastRisk + 12).coerceAtMost(100) / 100f,
                        uncertainty = state.uncertainty,
                        size = 180.dp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time threat landscape continuously evaluated by World Model",
                        style = MetadataText,
                        fontSize = 11.sp
                    )
                }
            }

            // Live Risk Graph (Section 11)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEMPORAL RISK TRAJECTORY",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                    )
                    Text(
                        text = "OBSERVED → FORECAST",
                        style = MetadataText.copy(color = Info)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LiveRiskGraph(
                    observedPoints = state.observedHistory,
                    forecastPoints = state.forecastTrajectory,
                    uncertainty = state.uncertainty,
                    height = 130.dp
                )
            }

            // Correlated Threat Story Card (Section 32)
            ThreatStoryCard(
                title = "Suspicious Reconnaissance Surge",
                stage = state.predictedStage,
                correlatedEventsCount = 4,
                predictedNextStage = "Lateral Probing -> Credential Access",
                riskScore = state.forecastRisk,
                onClick = onNavigateToRadar,
                onSimulate = onNavigateToSimulation
            )

            // Predictive Horizon & Critical Asset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREDICTED ATTACK HORIZON",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                        )
                        Box(
                            modifier = Modifier
                                .background(CriticalBg, RoundedCornerShape(4.dp))
                                .border(1.dp, CriticalBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ETA ~${state.etaSeconds}s",
                                style = TechnicalValue.copy(fontSize = 10.sp, color = Critical)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        state.horizonBars.forEachIndexed { idx, barVal ->
                            val barHeight = (barVal * 42).dp.coerceAtLeast(6.dp)
                            val barColor = if (barVal > 0.6f) Critical else if (barVal > 0.35f) Warning else Healthy
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(barVal * 100).toInt()}%",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = barColor)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${(idx + 1) * 30}s",
                                    style = MetadataText.copy(fontSize = 9.sp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "CURRENT STAGE", style = MetadataText)
                            Text(
                                text = state.predictedStage,
                                style = TechnicalValue.copy(fontSize = 12.sp, color = Warning)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "AT RISK ASSET", style = MetadataText)
                            Text(
                                text = state.criticalAsset,
                                style = TechnicalValue.copy(fontSize = 12.sp, color = Info)
                            )
                        }
                    }
                }
            }

            // OTP Privacy Vault Guarantee Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HealthyBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = HealthyBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OTP PRIVACY VAULT GUARANTEE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Healthy
                        )
                        Text(
                            text = "Zero Plaintext OTPs or Raw Messages Stored (Verified In-Flight SHA-256)",
                            style = MetadataText.copy(color = TextPrimary)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Healthy, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VERIFIED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Bg0
                        )
                    }
                }
            }

            // Quick Defence Actions Row
            Text(
                text = "GUARDIAN DEFENCE SURFACES",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToRadar,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Radar",
                        tint = Info,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "RADAR", style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary))
                }
                Button(
                    onClick = onNavigateToLinkScan,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "LINK", style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary))
                }
                Button(
                    onClick = onNavigateToFileScan,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "File",
                        tint = PurpleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "FILE/APK", style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary))
                }
            }

            // Counterfactual Defence Simulator Action Button
            Button(
                onClick = onNavigateToSimulation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Info)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Simulate",
                    tint = Bg0,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUN COUNTERFACTUAL DEFENCE SIMULATION",
                    color = Bg0,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
