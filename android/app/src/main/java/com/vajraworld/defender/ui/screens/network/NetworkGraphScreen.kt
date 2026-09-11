package com.vajraworld.defender.ui.screens.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vajraworld.defender.ui.theme.*

@Composable
fun NetworkGraphScreen(viewModel: NetworkGraphViewModel) {
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
                Text(text = "NETWORK TOPOLOGY", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                Text(text = "Dynamic Entity Interaction Graph", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Text(
                    text = "${state.nodes.size} NODES • ${state.edges.size} EDGES",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Topology Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(CardDark, RoundedCornerShape(8.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.nodes) {
                        detectTapGestures { offset ->
                            // Find clicked node within radius
                            val clicked = state.nodes.find { node ->
                                val dist = Math.hypot((node.x - offset.x).toDouble(), (node.y - offset.y).toDouble())
                                dist < 45.0
                            }
                            if (clicked != null) {
                                viewModel.selectNode(clicked)
                            } else {
                                viewModel.clearSelection()
                            }
                        }
                    }
            ) {
                val nodeMap = state.nodes.associateBy { it.id }

                // 1. Draw Edges
                state.edges.forEach { edge ->
                    val srcNode = nodeMap[edge.source]
                    val dstNode = nodeMap[edge.target]
                    if (srcNode != null && dstNode != null) {
                        val isSuspicious = edge.type in listOf("SCANS", "ACCESSES", "AUTHENTICATES_TO") && (srcNode.riskScore > 0.5f)
                        val edgeColor = if (isSuspicious) ThreatRed else BorderDark
                        val strokeWidth = if (isSuspicious) 4.dp.toPx() else 1.5.dp.toPx()
                        
                        drawLine(
                            color = edgeColor,
                            start = Offset(srcNode.x, srcNode.y),
                            end = Offset(dstNode.x, dstNode.y),
                            strokeWidth = strokeWidth,
                            pathEffect = if (edge.type == "SCANS") PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                        )
                    }
                }

                // 2. Draw Nodes
                state.nodes.forEach { node ->
                    val isSelected = state.selectedNode?.id == node.id
                    val isCritical = node.criticality == "Critical"
                    val isThreat = node.riskScore > 0.7f

                    val nodeColor = when {
                        isThreat -> ThreatRed
                        isCritical -> PurpleAccent
                        node.criticality == "High" -> WarningAmber
                        else -> AccentCyan
                    }

                    // Outer pulse ring if selected or critical threat
                    if (isSelected || isThreat) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.25f),
                            radius = 32.dp.toPx(),
                            center = Offset(node.x, node.y)
                        )
                    }

                    // Inner node circle
                    drawCircle(
                        color = nodeColor,
                        radius = 20.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )

                    // Center core
                    drawCircle(
                        color = BgDark,
                        radius = 10.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Node Inspection Card
        if (state.selectedNode != null) {
            val node = state.selectedNode!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentCyan, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = node.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${(node.riskScore * 100).toInt()}% Risk",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (node.riskScore > 0.6f) ThreatRed else SafeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type: ${node.type} | Criticality: ${node.criticality}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Text(
                    text = "Tap any node on the topology to inspect entity state & active telemetry edges.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
