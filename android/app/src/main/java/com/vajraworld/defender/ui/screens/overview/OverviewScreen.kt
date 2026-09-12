package com.vajraworld.defender.ui.screens.overview

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.*
import com.vajraworld.defender.ui.theme.*

import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onNavigateToSimulation: () -> Unit,
    onNavigateToRadar: () -> Unit = {},
    onNavigateToLinkScan: () -> Unit = {},
    onNavigateToFileScan: () -> Unit = {},
    onNavigateToClipboard: () -> Unit = {},
    onNavigateToExplainability: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToTrajectory: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToIncidents: () -> Unit = {},
    onOpenSurfacesHub: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "OverviewFlowPulse")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

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
            isSynthetic = state.isSynthetic,
            onHubClick = onOpenSurfacesHub
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
                            .alpha(liveDotAlpha)
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

            // Comprehensive Cockpit Defence Surfaces Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GUARDIAN DEFENCE SURFACES & MODULES",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                )
                Text(
                    text = "10 ACTIVE SURFACES",
                    style = MetadataText.copy(color = Info)
                )
            }

            val surfaces = listOf(
                Pair(
                    "RADAR DEFENCE",
                    Triple("4-Ring Spatial Scope", Icons.Default.Radar, Info)
                ) to onNavigateToRadar,
                Pair(
                    "LINK SCANNER",
                    Triple("Entropy & Deception", Icons.Default.Link, AccentCyan)
                ) to onNavigateToLinkScan,
                Pair(
                    "FILE / APK",
                    Triple("SAF Stream & Perms", Icons.Default.Folder, PurpleAccent)
                ) to onNavigateToFileScan,
                Pair(
                    "CLIPBOARD VAULT",
                    Triple("0-Retention Secrets", Icons.Default.ContentPaste, Healthy)
                ) to onNavigateToClipboard,
                Pair(
                    "EXPLAINABILITY",
                    Triple("SHAP Feature Attrib", Icons.Default.Info, Info)
                ) to onNavigateToExplainability,
                Pair(
                    "MODEL HEALTH",
                    Triple("Dynamic Benchmark", Icons.Default.Settings, Warning)
                ) to onNavigateToHealth,
                Pair(
                    "TRAJECTORY",
                    Triple("K-Step ATT&CK Futures", Icons.Default.Timeline, Info)
                ) to onNavigateToTrajectory,
                Pair(
                    "NETWORK SOC",
                    Triple("Host Topology Graph", Icons.Default.Hub, AccentCyan)
                ) to onNavigateToNetwork,
                Pair(
                    "INCIDENTS",
                    Triple("ATT&CK Detections", Icons.Default.Warning, Critical)
                ) to onNavigateToIncidents,
                Pair(
                    "SIMULATOR",
                    Triple("Counterfactual Replay", Icons.Default.Security, Healthy)
                ) to onNavigateToSimulation
            )

            // Render in 2-column rows
            for (i in surfaces.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val item1 = surfaces[i]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface0)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable { item1.second() }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item1.first.second.second,
                                    contentDescription = item1.first.first,
                                    tint = item1.first.second.third,
                                    modifier = Modifier.size(18.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Surface2, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "READY",
                                        style = TechnicalValue.copy(fontSize = 8.sp, color = item1.first.second.third)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item1.first.first,
                                style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = item1.first.second.first,
                                style = MetadataText.copy(fontSize = 10.sp),
                                maxLines = 1
                            )
                        }
                    }

                    if (i + 1 < surfaces.size) {
                        val item2 = surfaces[i + 1]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface0)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable { item2.second() }
                            .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item2.first.second.second,
                                        contentDescription = item2.first.first,
                                        tint = item2.first.second.third,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Surface2, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "READY",
                                            style = TechnicalValue.copy(fontSize = 8.sp, color = item2.first.second.third)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item2.first.first,
                                    style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = item2.first.second.first,
                                    style = MetadataText.copy(fontSize = 10.sp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
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
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUN COUNTERFACTUAL DEFENCE SIMULATION",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
