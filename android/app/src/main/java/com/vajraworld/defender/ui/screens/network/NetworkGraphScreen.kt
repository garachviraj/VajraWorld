package com.vajraworld.defender.ui.screens.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun NetworkGraphScreen(
    viewModel: NetworkGraphViewModel,
    onNavigateToSimulation: (() -> Unit)? = null,
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
            title = "NETWORK GRAPH",
            subtitle = "SOC TOPOLOGY INVESTIGATION",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Topology Header Banner
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.isLiveFromBackend) Healthy else Info)
                    )
                    Text(
                        text = if (state.isLiveFromBackend) "LIVE TELEMETRY TOPOLOGY" else "ENTERPRISE REFERENCE TOPOLOGY",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${state.nodes.size} NODES • ${state.edges.size} EDGES",
                        style = MetadataText
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { viewModel.fetchGraph() }
                    )
                }
            }

            // Interactive SOC Canvas (Sections 20-23)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface0)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.nodes) {
                            detectTapGestures { offset ->
                                val clicked = state.nodes.find { node ->
                                    val dist = Math.hypot((node.x - offset.x).toDouble(), (node.y - offset.y).toDouble())
                                    dist < 40.0
                                }
                                if (clicked != null) {
                                    viewModel.selectNode(clicked)
                                } else {
                                    viewModel.clearSelection()
                                }
                            }
                        }
                ) {
                    // Dark Technical Grid
                    val step = 32.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(BorderSubtle, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.8f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(BorderSubtle, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
                        y += step
                    }

                    val nodeMap = state.nodes.associateBy { it.id }

                    // 1. Draw Edges
                    state.edges.forEach { edge ->
                        val src = nodeMap[edge.source]
                        val dst = nodeMap[edge.target]
                        if (src != null && dst != null) {
                            val isSuspicious = edge.type in listOf("SCANS", "ACCESSES", "AUTHENTICATES_TO") && (src.riskScore > 0.5f)
                            val edgeColor = if (isSuspicious) Critical else BorderColor
                            val strokeWidth = if (isSuspicious) 2.5.dp.toPx() else 1.2.dp.toPx()

                            drawLine(
                                color = edgeColor,
                                start = Offset(src.x, src.y),
                                end = Offset(dst.x, dst.y),
                                strokeWidth = strokeWidth,
                                pathEffect = if (edge.type == "SCANS") PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null
                            )
                        }
                    }

                    // 2. Draw Nodes
                    state.nodes.forEach { node ->
                        val isSelected = state.selectedNode?.id == node.id
                        val isThreat = node.riskScore > 0.6f

                        val nodeColor = when {
                            isThreat -> Critical
                            node.criticality == "Critical" -> High
                            node.criticality == "High" -> Warning
                            else -> Info
                        }

                        // Outer glow if selected or high risk
                        if (isSelected || isThreat) {
                            drawCircle(
                                color = nodeColor.copy(alpha = if (isSelected) 0.35f else 0.15f),
                                radius = 24.dp.toPx(),
                                center = Offset(node.x, node.y)
                            )
                        }

                        // Node disc
                        drawCircle(
                            color = nodeColor,
                            radius = 12.dp.toPx(),
                            center = Offset(node.x, node.y)
                        )

                        // Core
                        drawCircle(
                            color = Bg0,
                            radius = 5.dp.toPx(),
                            center = Offset(node.x, node.y)
                        )
                    }
                }
            }

            // Selected Node Details Card
            AnimatedVisibility(
                visible = state.selectedNode != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
            ) {
                state.selectedNode?.let { node ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, InfoBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Surface1)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = node.label,
                                        style = TechnicalValue.copy(fontSize = 14.sp, color = TextPrimary)
                                    )
                                    Text(
                                        text = "TYPE: ${node.type} • CRITICALITY: ${node.criticality}",
                                        style = MetadataText
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SecurityStatusPill(riskScore = (node.riskScore * 100).toInt())
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.clearSelection() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(8.dp))

                            TechnicalMetadataRow(label = "Entity ID", value = node.id, allowCopy = true)
                            TechnicalMetadataRow(label = "Risk Evaluation", value = "${(node.riskScore * 100).toInt()}%")
                            TechnicalMetadataRow(label = "Infrastructure Tier", value = node.criticality)

                            if (onNavigateToSimulation != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onNavigateToSimulation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Info)
                                ) {
                                    Text(
                                        text = "SIMULATE CONTAINMENT ON ${node.id}",
                                        style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.selectedNode == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Text(
                        text = "Tap any host, domain controller, or database node on the topology to inspect entity state & active telemetry edges.",
                        modifier = Modifier.padding(12.dp),
                        style = MetadataText.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}
