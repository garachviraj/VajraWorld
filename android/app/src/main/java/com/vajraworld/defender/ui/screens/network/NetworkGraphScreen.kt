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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun NetworkGraphScreen(viewModel: NetworkGraphViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NETWORK TOPOLOGY",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Dynamic Entity Interaction & Asset Graph",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(BrandBlueLight, RoundedCornerShape(8.dp))
                    .border(1.dp, BrandBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${state.nodes.size} NODES • ${state.edges.size} EDGES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = BrandBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Topology Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .background(SurfaceWhite, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.nodes) {
                        detectTapGestures { offset ->
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
                // Background subtle technical grid lines
                val step = 40.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color(0xFFF1F5F9), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color(0xFFF1F5F9), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                val nodeMap = state.nodes.associateBy { it.id }

                // 1. Draw Edges
                state.edges.forEach { edge ->
                    val srcNode = nodeMap[edge.source]
                    val dstNode = nodeMap[edge.target]
                    if (srcNode != null && dstNode != null) {
                        val isSuspicious = edge.type in listOf("SCANS", "ACCESSES", "AUTHENTICATES_TO") && (srcNode.riskScore > 0.5f)
                        val edgeColor = if (isSuspicious) ThreatRed else Color(0xFFCBD5E1)
                        val strokeWidth = if (isSuspicious) 3.5.dp.toPx() else 1.5.dp.toPx()

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
                        else -> BrandBlue
                    }

                    // Outer pulse ring if selected or critical threat
                    if (isSelected || isThreat) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.22f),
                            radius = 30.dp.toPx(),
                            center = Offset(node.x, node.y)
                        )
                    }

                    // Inner node circle
                    drawCircle(
                        color = nodeColor,
                        radius = 18.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )

                    // Center core
                    drawCircle(
                        color = SurfaceWhite,
                        radius = 8.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Node Inspection Card
        if (state.selectedNode != null) {
            val node = state.selectedNode!!
            val isThreat = node.riskScore > 0.6f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, BrandBlue, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isThreat) ThreatRedBg else SafeGreenBg,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${(node.riskScore * 100).toInt()}% Risk",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isThreat) ThreatRed else SafeGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Entity: ${node.type} • Asset Criticality: ${node.criticality}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Text(
                    text = "Tap any host or gateway node on the topology to inspect entity state & active telemetry edges.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
