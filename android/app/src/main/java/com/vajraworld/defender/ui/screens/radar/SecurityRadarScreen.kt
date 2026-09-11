package com.vajraworld.defender.ui.screens.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun SecurityRadarScreen(viewModel: SecurityRadarViewModel) {
    val state by viewModel.uiState.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIVE SECURITY RADAR",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Cross-Surface Real-Time Threat Correlation",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        if (state.overallHealth < 60) ThreatRedBg else BrandBlueLight,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (state.overallHealth < 60) ThreatRedBorder else BrandBlue.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = state.overallStatus,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (state.overallHealth < 60) ThreatRed else BrandBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite, RoundedCornerShape(8.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp, 3.dp).background(ThreatRed, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Observed Attack Chain", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp, 3.dp).background(WarningAmber, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Predicted Progression", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Radar Canvas
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
                                Math.hypot((node.x - offset.x).toDouble(), (node.y - offset.y).toDouble()) < 50.0
                            }
                            if (clicked != null) {
                                viewModel.selectNode(clicked)
                            } else {
                                viewModel.clearSelection()
                            }
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background Radar Concentric Rings (Light Theme Aesthetics)
                val ringRadii = listOf(size.width * 0.2f, size.width * 0.35f, size.width * 0.48f)
                ringRadii.forEach { r ->
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = r,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }

                // Crosshairs
                drawLine(
                    color = Color(0xFFF1F5F9),
                    start = Offset(center.x, 20f),
                    end = Offset(center.x, size.height - 20f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFF1F5F9),
                    start = Offset(20f, center.y),
                    end = Offset(size.width - 20f, center.y),
                    strokeWidth = 1.dp.toPx()
                )

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
                    val nodeColor = when {
                        node.surface == "OTP" -> SafeGreen
                        node.risk >= 70 -> ThreatRed
                        node.risk >= 40 -> WarningAmber
                        else -> BrandBlue
                    }

                    // Outer pulse ring
                    drawCircle(
                        color = nodeColor.copy(alpha = if (isSelected) 0.35f else 0.12f),
                        radius = (if (isSelected) 32.dp else 26.dp).toPx(),
                        center = Offset(node.x, node.y)
                    )

                    // Node body
                    drawCircle(
                        color = nodeColor,
                        radius = 16.dp.toPx(),
                        center = Offset(node.x, node.y)
                    )

                    // Core center
                    drawCircle(
                        color = SurfaceWhite,
                        radius = 7.dp.toPx(),
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
                            text = n.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (n.risk > 70) ThreatRedBg else if (n.risk > 40) WarningAmberBg else SafeGreenBg,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${n.risk}% Risk",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (n.risk > 70) ThreatRed else if (n.risk > 40) WarningAmber else SafeGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Surface: ${n.surface} • Operational Status: ${n.status}",
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
                    text = "Tap on any surface node (Link, File, Notification, OTP, User, Network) to inspect its live threat state.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
