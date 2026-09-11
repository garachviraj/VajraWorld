package com.vajraworld.defender.ui.screens.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun SecurityRadarScreen(viewModel: SecurityRadarViewModel) {
    val state by viewModel.uiState.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "LIVE SECURITY RADAR", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                Text(text = "Multi-Surface Threat Graph Correlation", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, ThreatRed)
            ) {
                Text(
                    text = state.overallStatus,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ThreatRed
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp, 3.dp).background(ThreatRed))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Observed Event", fontSize = 11.sp, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp, 3.dp).background(WarningAmber))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Predicted Progression (Dotted)", fontSize = 11.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Radar Canvas
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
                            val clicked = state.nodes.find { node ->
                                Math.hypot((node.x - offset.x).toDouble(), (node.y - offset.y).toDouble()) < 40.0
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

                // 1. Draw Radar Edges
                state.edges.forEach { edge ->
                    val src = nodeMap[edge.source]
                    val dst = nodeMap[edge.target]
                    if (src != null && dst != null) {
                        val color = if (edge.isPredicted) WarningAmber else ThreatRed
                        val stroke = if (edge.isPredicted) 2.dp.toPx() else 3.5.dp.toPx()
                        val pathEffect = if (edge.isPredicted) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null

                        drawLine(
                            color = color,
                            start = Offset(src.x, src.y),
                            end = Offset(dst.x, dst.y),
                            strokeWidth = stroke,
                            pathEffect = pathEffect
                        )
                    }
                }

                // 2. Draw Radar Surface Nodes
                state.nodes.forEach { node ->
                    val isSelected = selectedNode?.id == node.id
                    val color = when {
                        node.surface == "OTP" -> SafeGreen
                        node.risk >= 80 -> ThreatRed
                        node.risk >= 50 -> WarningAmber
                        else -> AccentCyan
                    }

                    // Outer pulse ring
                    drawCircle(
                        color = color.copy(alpha = if (isSelected) 0.35f else 0.15f),
                        radius = 28.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )

                    // Node body
                    drawCircle(
                        color = color,
                        radius = 18.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )

                    // Inner circle
                    drawCircle(
                        color = BgDark,
                        radius = 8.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Node Inspection Card
        if (selectedNode != null) {
            val n = selectedNode!!
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
                        Text(text = n.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${n.risk}% Risk",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (n.risk > 70) ThreatRed else if (n.risk > 40) WarningAmber else SafeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Surface: ${n.surface} | Operational State: ${n.status}",
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
                    text = "Tap on any security surface (Link, File, Notification, OTP, User, Network) to inspect its threat state.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
