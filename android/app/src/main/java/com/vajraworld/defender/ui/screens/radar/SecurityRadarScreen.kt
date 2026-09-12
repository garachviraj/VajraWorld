package com.vajraworld.defender.ui.screens.radar

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
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.RadarNode
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SecurityRadarScreen(
    viewModel: SecurityRadarViewModel,
    onNavigateToSimulation: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
    ) {
        VajraTopBar(
            title = "SECURITY RADAR",
            subtitle = "CROSS-SURFACE SURVEILLANCE",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Radar Status & Scope Legend
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
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Info))
                    Text(
                        text = "4-RING DEFENCE TOPOLOGY",
                        style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp, 2.dp).background(Critical))
                        Text(text = "Observed", style = MetadataText.copy(fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp, 2.dp).background(Warning))
                        Text(text = "Forecast", style = MetadataText.copy(fontSize = 10.sp))
                    }
                }
            }

            // Interactive Radar Canvas (Sections 13-19)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface0)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            ) {
                // Calculated positions map (id to Offset)
                val nodePositions = remember { mutableStateMapOf<String, Offset>() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.nodes) {
                            detectTapGestures { tapOffset ->
                                val hit = nodePositions.entries.find { (_, pos) ->
                                    val dx = pos.x - tapOffset.x
                                    val dy = pos.y - tapOffset.y
                                    (dx * dx + dy * dy) <= (32f * 32f)
                                }
                                if (hit != null) {
                                    val matchedNode = state.nodes.find { it.id == hit.key }
                                    if (matchedNode != null) viewModel.selectNode(matchedNode)
                                } else {
                                    viewModel.clearSelection()
                                }
                            }
                        }
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (size.minDimension / 2f) * 0.90f

                    // 4 Concentric Radar Rings (Section 13)
                    val r1 = maxRadius * 0.28f // Local state
                    val r2 = maxRadius * 0.52f // Apps/files/links
                    val r3 = maxRadius * 0.74f // Network destinations
                    val r4 = maxRadius * 0.94f // Threat intelligence / Forecast

                    val rings = listOf(r1, r2, r3, r4)
                    rings.forEachIndexed { idx, r ->
                        drawCircle(
                            color = RadarRingColor,
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // Crosshair guides
                    drawLine(
                        color = BorderSubtle,
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    )
                    drawLine(
                        color = BorderSubtle,
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    )

                    // Draw Central Node: DEVICE (Section 13)
                    drawCircle(color = Info.copy(alpha = 0.2f), radius = 22f, center = center)
                    drawCircle(color = Info, radius = 10f, center = center)
                    drawCircle(color = Bg0, radius = 4f, center = center)

                    // Compute deterministic coordinates for nodes if not explicitly laid out
                    state.nodes.forEachIndexed { index, node ->
                        val targetRingRadius = when (node.surface.uppercase()) {
                            "USER", "OTP", "CLIPBOARD" -> r1
                            "NOTIFICATION", "LINK", "FILE" -> r2
                            "NETWORK", "IP", "DOMAIN" -> r3
                            else -> r4
                        }

                        // Deterministic angle based on index or stable hash
                        val angleDeg = if (state.nodes.isNotEmpty()) {
                            (index * (360f / state.nodes.size) + 25f)
                        } else 0f
                        val angleRad = Math.toRadians(angleDeg.toDouble())

                        val nodeX = center.x + (targetRingRadius * cos(angleRad)).toFloat()
                        val nodeY = center.y + (targetRingRadius * sin(angleRad)).toFloat()
                        val pos = Offset(nodeX, nodeY)
                        nodePositions[node.id] = pos
                    }

                    // 1. Draw Edges
                    state.edges.forEach { edge ->
                        val srcPos = nodePositions[edge.source]
                        val dstPos = nodePositions[edge.target]
                        if (srcPos != null && dstPos != null) {
                            val edgeColor = if (edge.isPredicted) Warning.copy(alpha = 0.8f) else Critical.copy(alpha = 0.8f)
                            val pathEffect = if (edge.isPredicted) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null

                            drawLine(
                                color = edgeColor,
                                start = srcPos,
                                end = dstPos,
                                strokeWidth = if (edge.isPredicted) 2f else 3f,
                                pathEffect = pathEffect
                            )
                        }
                    }

                    // 2. Draw Nodes
                    state.nodes.forEach { node ->
                        val pos = nodePositions[node.id] ?: center
                        val isSelected = selectedNode?.id == node.id

                        val nodeColor = when {
                            node.surface == "OTP" -> Healthy
                            node.risk >= 70 -> Critical
                            node.risk >= 40 -> Warning
                            else -> Info
                        }

                        // Outer halo
                        val haloRadius = if (isSelected) 26f else 18f
                        drawCircle(
                            color = nodeColor.copy(alpha = if (isSelected) 0.35f else 0.15f),
                            radius = haloRadius,
                            center = pos
                        )

                        // Body
                        drawCircle(color = nodeColor, radius = if (isSelected) 12f else 9f, center = pos)

                        // Core
                        drawCircle(color = Bg0, radius = if (isSelected) 5f else 3.5f, center = pos)
                    }
                }

                // Overlay Ring Labels
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(text = "R4 • Threat / Forecast", style = MetadataText.copy(fontSize = 9.sp))
                    Text(text = "R3 • Network Dst", style = MetadataText.copy(fontSize = 9.sp))
                    Text(text = "R2 • Apps / Files / Links", style = MetadataText.copy(fontSize = 9.sp))
                    Text(text = "R1 • Local State / OTP", style = MetadataText.copy(fontSize = 9.sp))
                }
            }

            // Node Inspection Detail Sheet / Card (Section 17)
            AnimatedVisibility(
                visible = selectedNode != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
            ) {
                selectedNode?.let { n ->
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
                                        text = n.label.uppercase(),
                                        style = TechnicalValue.copy(fontSize = 14.sp, color = TextPrimary)
                                    )
                                    Text(
                                        text = "CATEGORY: ${n.surface} • STATUS: ${n.status}",
                                        style = MetadataText
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SecurityStatusPill(riskScore = n.risk)
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.clearSelection() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "• Correlated with active telemetry chain\n• No anomalous lateral propagation from this entity\n• Monitored continuously via World Model state vector",
                                style = MetadataText.copy(color = TextSecondary, lineHeight = 16.sp)
                            )

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
                                        text = "SIMULATE INTERVENTION ON ${n.label}",
                                        style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedNode == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Text(
                        text = "Tap on any topology node (Notification, Link, File, Network, OTP) to inspect live graph relationships and test defence actions.",
                        modifier = Modifier.padding(12.dp),
                        style = MetadataText.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}
