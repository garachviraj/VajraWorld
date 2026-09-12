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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.FutureBranch
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun TrajectoryScreen(viewModel: TrajectoryViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "ATTACK TRAJECTORY",
            subtitle = "LATENT RECURRENT ROLLOUT"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Horizontal Timeline Card (Sections 24-25)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TEMPORAL ATT&CK PROJECTION",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                        )
                        Box(
                            modifier = Modifier
                                .background(InfoBg, RoundedCornerShape(4.dp))
                                .border(1.dp, InfoBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "K-STEP ROLLOUT",
                                style = TechnicalValue.copy(fontSize = 9.sp, color = Info)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.timelineNodes) { node ->
                            val nodeColor = if (node.riskPct > 70) Critical else if (node.riskPct > 40) Warning else Info

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Surface1)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(nodeColor.copy(alpha = 0.2f))
                                        .border(1.5.dp, nodeColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${node.riskPct}%",
                                        style = TechnicalValue.copy(fontSize = 10.sp, color = nodeColor, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = node.offset,
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary)
                                )
                                Text(
                                    text = node.stage.take(10),
                                    style = MetadataText.copy(fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Multi-Future Branches (Section 25)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MULTI-FUTURE PROBABILITY BRANCHES",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    state.branches.forEach { branch ->
                        val isSel = state.selectedBranch?.name == branch.name
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Surface2 else Surface1)
                                .border(1.dp, if (isSel) Info else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectBranch(branch) }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = branch.name,
                                    style = Typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${(branch.probability * 100).toInt()}%",
                                    style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Surface0)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = branch.probability.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(if (branch.probability > 0.5f) Critical else Info)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Terminal Stage: ${branch.terminalStage} • Trend: ${branch.trajectoryTrend}",
                                style = MetadataText.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }
            }

            // Selected Branch Detail Card
            state.selectedBranch?.let { branch ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, InfoBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SELECTED TRAJECTORY BRANCH",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = branch.name,
                            style = Typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TechnicalMetadataRow(label = "Branch Probability", value = "${(branch.probability * 100).toInt()}%")
                        TechnicalMetadataRow(label = "Trajectory Trend", value = branch.trajectoryTrend)
                        TechnicalMetadataRow(label = "Projected Terminal Stage", value = branch.terminalStage)
                    }
                }
            }
        }
    }
}
