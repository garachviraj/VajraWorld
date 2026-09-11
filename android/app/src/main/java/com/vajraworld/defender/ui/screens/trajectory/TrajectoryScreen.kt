package com.vajraworld.defender.ui.screens.trajectory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun TrajectoryScreen(viewModel: TrajectoryViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = "ATTACK TRAJECTORY", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
        Text(
            text = "Evolving Latent State Rollout P(z_t+k | z_t)",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Flagship Horizontal Timeline
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "TEMPORAL ATT&CK PROJECTION", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.timelineNodes) { node ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (node.riskPct > 70) ThreatRed else if (node.riskPct > 50) WarningAmber else AccentCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${node.riskPct}%",
                                    fontSize = 11.sp,
                                    color = BgDark,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = node.offset, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            Text(text = node.stage, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Multi-Future Branches Section
        Text(text = "K-STEP STOCHASTIC FUTURES", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))

        state.branches.forEach { branch ->
            val isSelected = state.selectedBranch?.name == branch.name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AccentCyan else BorderDark,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { viewModel.selectBranch(branch) },
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = branch.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${(branch.probability * 100).toInt()}% prob",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (branch.probability > 0.5f) ThreatRed else AccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Terminal Stage: ${branch.terminalStage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Trend: ${branch.trajectoryTrend}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (branch.trajectoryTrend == "Escalating") ThreatRed else SafeGreen
                        )
                    }
                }
            }
        }

        if (state.selectedBranch != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentCyan, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "BRANCH EVIDENCE DETAILS", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Path: Host-17 -> AD-01 (Port 88) -> Finance-DB-02 (Port 445 SMB)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Expected Lead Time: 74.5s before credential persistence completes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber
                    )
                }
            }
        }
    }
}
