package com.vajraworld.defender.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.VajraBrandHeader
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
            .background(BgLight)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Brand Header with App Logo & Live Indicator
        VajraBrandHeader(isLive = state.isLiveConnected)

        Spacer(modifier = Modifier.height(14.dp))

        // Real-Time Live Traffic Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SafeGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE FLOW ANALYSIS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.activeFlowsCount} active flows • ${state.eventsPerSec} pkts/s",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(BrandBlueLight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = state.radarStatus,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Top Metrics Cards (Health & Risk)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "NETWORK HEALTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.networkHealth}/100",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                        color = SafeGreen
                    )
                    Text(
                        text = "Shield Active • 0 Leak",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "FORECAST RISK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.forecastRisk}%",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                        color = if (state.forecastRisk >= 60) ThreatRed else if (state.forecastRisk >= 35) WarningAmber else SafeGreen
                    )
                    Text(
                        text = "Fluctuation ${state.riskDelta}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.forecastRisk >= 60) ThreatRed else WarningAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center Forecast Box (Next 10 Minutes Horizon)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PREDICTIVE ATTACK HORIZON",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .background(ThreatRedBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ETA ~${state.etaSeconds}s",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThreatRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Forecast Sparkline / Horizon Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    state.horizonBars.forEachIndexed { idx, barVal ->
                        val barHeight = (barVal * 54).dp.coerceAtLeast(6.dp)
                        val color = if (barVal > 0.6f) ThreatRed else if (barVal > 0.35f) WarningAmber else SafeGreen
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(barVal * 100).toInt()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+${(idx + 1) * 30}s",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "PREDICTED ATTACK STAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = state.predictedStage,
                            style = MaterialTheme.typography.titleMedium,
                            color = ThreatRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "AT RISK ASSET",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = state.criticalAsset,
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Vault & Metric Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SafeGreenBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SafeGreenBg)
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
                        style = MaterialTheme.typography.labelSmall,
                        color = SafeGreen
                    )
                    Text(
                        text = "0 Plaintext OTPs or Raw Messages Stored",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(SafeGreen, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "VERIFIED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SurfaceWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Scanners Row
        Text(
            text = "GUARDIAN DEFENCE SURFACES",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToRadar,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text(text = "LIVE RADAR", style = MaterialTheme.typography.labelSmall, color = SurfaceWhite)
            }
            OutlinedButton(
                onClick = onNavigateToLinkScan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Text(text = "SCAN LINK", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = onNavigateToFileScan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Text(text = "SCAN APK", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Call to Action: Test Defence
        Button(
            onClick = onNavigateToSimulation,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
        ) {
            Text(
                text = "TEST COUNTERFACTUAL DEFENCE",
                color = SurfaceWhite,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
