package com.vajraworld.defender.ui.screens.radar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val sweepFrequency by viewModel.sweepFrequency.collectAsState()

    // Continuous Live Radar Sweep & Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweepMotion")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePulse"
    )

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
            // Live Radar Discovery & Telemetry Bar
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
                            .background(if (isDiscovering) Warning else Healthy)
                    )
                    Text(
                        text = "${state.nodes.size} DISCOVERED • $sweepFrequency",
                        style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDiscovering) Warning.copy(alpha = 0.12f) else InfoBg)
                        .border(1.dp, if (isDiscovering) Warning else InfoBorder, RoundedCornerShape(6.dp))
                        .clickable { viewModel.triggerImmediateDiscovery() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sweep",
                        tint = if (isDiscovering) Warning else Info,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isDiscovering) "SCANNING..." else "RADAR SWEEP",
                        style = TechnicalValue.copy(
                            fontSize = 9.sp,
                            color = if (isDiscovering) Warning else Info,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Interactive Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface0)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            ) {
                val nodePositions = remember { mutableStateMapOf<String, Offset>() }
                val nodeAngles = remember { mutableStateMapOf<String, Float>() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.nodes) {
                            detectTapGestures { tapOffset ->
                                val hit = nodePositions.entries.find { (_, pos) ->
                                    val dx = pos.x - tapOffset.x
                                    val dy = pos.y - tapOffset.y
                                    (dx * dx + dy * dy) <= (36f * 36f)
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

                    // 4 Concentric Radar Rings
                    val r1 = maxRadius * 0.28f // Local state / device
                    val r2 = maxRadius * 0.52f // Apps, scanned files, links
                    val r3 = maxRadius * 0.74f // Network endpoints, live sockets
                    val r4 = maxRadius * 0.94f // Threat horizon & ATT&CK incidents

                    val rings = listOf(r1, r2, r3, r4)
                    rings.forEachIndexed { idx, r ->
                        drawCircle(
                            color = RadarRingColor,
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // Expanding Sensor Ping Wave
                    val currentWaveRadius = maxRadius * wavePulse
                    drawCircle(
                        color = Info.copy(alpha = (1f - wavePulse) * 0.25f),
                        radius = currentWaveRadius,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )

                    // Sweeping Radar Beam & Trailing Gradient Arc
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val beamEnd = Offset(
                        center.x + (maxRadius * cos(sweepRad)).toFloat(),
                        center.y + (maxRadius * sin(sweepRad)).toFloat()
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.86f to Color.Transparent,
                            1.0f to Info.copy(alpha = 0.18f),
                            center = center
                        ),
                        startAngle = sweepAngle - 50f,
                        sweepAngle = 50f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = androidx.compose.ui.geometry.Size(maxRadius * 2f, maxRadius * 2f)
                    )

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Info.copy(alpha = 0.20f), Info),
                            start = center,
                            end = beamEnd
                        ),
                        start = center,
                        end = beamEnd,
                        strokeWidth = 2.2f
                    )

                    // Monospace Crosshairs
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

                    // Central Node: HARDENED DEVICE SENSOR
                    drawCircle(color = Info.copy(alpha = 0.20f), radius = 22f, center = center)
                    drawCircle(color = Info, radius = 10f, center = center)
                    drawCircle(color = Surface0, radius = 4.5f, center = center)

                    // Group and place nodes by ring level to prevent collision
                    val nodesByRing = state.nodes.groupBy { node ->
                        when {
                            node.ringLevel in 1..4 -> node.ringLevel
                            node.surface in listOf("USER", "OTP", "CLIPBOARD") -> 1
                            node.surface in listOf("NOTIFICATION", "LINK", "FILE") -> 2
                            node.surface in listOf("NETWORK", "IP", "DOMAIN") -> 3
                            else -> 4
                        }
                    }

                    // Compute trigonometric coordinates per ring
                    (1..4).forEach { ringLevel ->
                        val targetRadius = when (ringLevel) {
                            1 -> r1
                            2 -> r2
                            3 -> r3
                            else -> r4
                        }
                        val ringNodes = nodesByRing[ringLevel] ?: emptyList()
                        val ringCount = maxOf(1, ringNodes.size)
                        val angleStep = 360f / ringCount
                        val ringBaseOffset = (ringLevel * 33f) + 15f

                        ringNodes.forEachIndexed { idxInRing, node ->
                            val angleDeg = (ringBaseOffset + (idxInRing * angleStep)) % 360f
                            val angleRad = Math.toRadians(angleDeg.toDouble())
                            val nx = center.x + (targetRadius * cos(angleRad)).toFloat()
                            val ny = center.y + (targetRadius * sin(angleRad)).toFloat()
                            val pos = Offset(nx, ny)
                            nodePositions[node.id] = pos
                            nodeAngles[node.id] = angleDeg
                        }
                    }

                    // 1. Draw Correlation Edges
                    state.edges.forEach { edge ->
                        val srcPos = nodePositions[edge.source]
                        val dstPos = nodePositions[edge.target]
                        if (srcPos != null && dstPos != null) {
                            val edgeColor = if (edge.isPredicted) Warning.copy(alpha = 0.8f) else Info.copy(alpha = 0.7f)
                            val pathEffect = if (edge.isPredicted) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null

                            drawLine(
                                color = edgeColor,
                                start = srcPos,
                                end = dstPos,
                                strokeWidth = if (edge.isPredicted) 1.8f else 2.4f,
                                pathEffect = pathEffect
                            )
                        }
                    }

                    // 2. Draw Nodes with Dynamic Blip Ping Illumination
                    state.nodes.forEach { node ->
                        val pos = nodePositions[node.id] ?: center
                        val nodeAngle = nodeAngles[node.id] ?: 0f
                        val isSelected = selectedNode?.id == node.id

                        val nodeColor = when {
                            node.surface == "OTP" -> Healthy
                            node.risk >= 70 -> Critical
                            node.risk >= 40 -> Warning
                            else -> Info
                        }

                        // Calculate Blip Ping Flash as beam sweeps past
                        val angularDiff = ((sweepAngle - nodeAngle + 360f) % 360f)
                        val blipFactor = if (angularDiff in 0f..45f) {
                            (1f - (angularDiff / 45f))
                        } else 0f

                        // Threat ripple for elevated/critical nodes
                        if (node.risk >= 40) {
                            val nodeRipple = (wavePulse * 26f) + 12f
                            drawCircle(
                                color = nodeColor.copy(alpha = (1f - wavePulse) * 0.45f),
                                radius = nodeRipple,
                                center = pos,
                                style = Stroke(width = 1.5f)
                            )
                        }

                        // Blip Ping Glow Aura
                        if (blipFactor > 0f) {
                            drawCircle(
                                color = nodeColor.copy(alpha = blipFactor * 0.50f),
                                radius = 22f + (blipFactor * 14f),
                                center = pos
                            )
                        }

                        // Outer Halo
                        val haloRadius = if (isSelected) 24f else 17f
                        drawCircle(
                            color = nodeColor.copy(alpha = if (isSelected) 0.35f else 0.15f),
                            radius = haloRadius,
                            center = pos
                        )

                        // Main Node Core
                        drawCircle(color = nodeColor, radius = if (isSelected) 11f else 8.5f, center = pos)
                        drawCircle(color = Surface0, radius = if (isSelected) 4.5f else 3f, center = pos)
                    }
                }

                // Scope Ring Indicators
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(text = "R4 • Threat / Forecast", style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary))
                    Text(text = "R3 • Network / Sockets", style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary))
                    Text(text = "R2 • Apps / Files / Links", style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary))
                    Text(text = "R1 • Hardware / Vault", style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary))
                }
            }

            // Interactive Node Telemetry Detail Sheet
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
                                        text = "SURFACE: ${n.surface} • STATUS: ${n.status}",
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

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = n.plainDescription,
                                style = Typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TOPOLOGY RING: R${n.ringLevel} (${when(n.ringLevel) { 1 -> "Hardware Enclave"; 2 -> "Application Artifact"; 3 -> "Socket Transport"; else -> "Threat Horizon" }})",
                                style = TechnicalValue.copy(fontSize = 9.5.sp, color = Info)
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ACTIVE SIGNALS & ATTESTATION:",
                                style = TechnicalValue.copy(fontSize = 9.sp, color = TextSecondary)
                            )
                            n.threatReasons.forEach { r ->
                                Text(
                                    text = "• $r",
                                    style = MetadataText.copy(
                                        color = if (n.risk >= 50) Critical else TextPrimary,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Surface2)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "REMEDIATION: ${n.remediationAction}",
                                    style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextPrimary)
                                )
                            }

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
                                        text = "TEST DEFENCE IN SIMULATOR",
                                        style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold)
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
                        text = "Tap on any topology node (Device, App, Scanned File/Link, Socket, Threat) to inspect real-time graph relationships and test countermeasures.",
                        modifier = Modifier.padding(12.dp),
                        style = MetadataText.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}
