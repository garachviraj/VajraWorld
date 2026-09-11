package com.vajraworld.defender.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onNavigateToSimulation: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VAJRAWORLD",
                style = MaterialTheme.typography.titleLarge,
                color = AccentCyan
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(SafeGreen, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "LIVE SENSOR", color = SafeGreen, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top Metrics Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "NETWORK HEALTH", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${state.networkHealth}/100", style = MaterialTheme.typography.titleLarge, color = SafeGreen)
                    Text(text = "Nominal", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "FORECAST RISK", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${state.forecastRisk}%", style = MaterialTheme.typography.titleLarge, color = ThreatRed)
                    Text(text = "Surge ${state.riskDelta}", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Forecast Box (Next 10 Minutes)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "NEXT 10 MINUTES FORECAST", style = MaterialTheme.typography.labelSmall)
                    Text(text = "ETA: ${state.etaSeconds}s", style = MaterialTheme.typography.labelSmall, color = ThreatRed)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Forecast Sparkline / Horizon Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    state.horizonBars.forEachIndexed { idx, barVal ->
                        val barHeight = (barVal * 50).dp
                        val color = if (barVal > 0.6f) ThreatRed else if (barVal > 0.3f) WarningAmber else SafeGreen
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(barHeight)
                                    .background(color, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "+${(idx + 1) * 30}s", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "PREDICTED STAGE", style = MaterialTheme.typography.labelSmall)
                        Text(text = state.predictedStage, style = MaterialTheme.typography.titleMedium, color = ThreatRed)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "AT RISK ASSET", style = MaterialTheme.typography.labelSmall)
                        Text(text = state.criticalAsset, style = MaterialTheme.typography.titleMedium, color = PurpleAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top 3 Driving Signals
        Text(text = "TOP TRAJECTORY DRIVERS", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        state.topDrivers.forEach { driver ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, BorderDark, RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = driver.feature, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(
                        text = "+${(driver.impact * 100).toInt()}% impact",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThreatRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Call to Action: Test Defence
        Button(
            onClick = onNavigateToSimulation,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            Text(text = "TEST DEFENCE INTERVENTION", color = BgDark, style = MaterialTheme.typography.titleMedium)
        }
    }
}
