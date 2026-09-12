package com.vajraworld.defender.ui.screens.network

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshSockets(context)
            kotlinx.coroutines.delay(2500)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "NetworkTopologyMotion")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
    ) {
        VajraTopBar(
            title = "NETWORK & PACKET DEFENDER",
            subtitle = "LIVE Sockets • PACKET STREAM • TOPOLOGY",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tab Selector: PACKETS vs SOCKETS vs TOPOLOGY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface0)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("PACKETS", "LIVE PACKETS", Icons.Default.Sensors),
                    Triple("SOCKETS", "SOCKETS", Icons.Default.Dns),
                    Triple("TOPOLOGY", "TOPOLOGY", Icons.Default.Hub)
                ).forEach { (key, title, icon) ->
                    val isSel = state.selectedTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Info else Surface1)
                            .clickable { viewModel.selectTab(key) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSel) Bg0 else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = title,
                                style = TechnicalValue.copy(
                                    fontSize = 10.sp,
                                    color = if (isSel) Bg0 else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Traffic Telemetry Stats Bar
            val traffic = state.trafficOverview
            val rxMb = (traffic?.totalRxBytes ?: 0L) / (1024.0 * 1024.0)
            val txMb = (traffic?.totalTxBytes ?: 0L) / (1024.0 * 1024.0)
            val totalPackets = (traffic?.totalRxPackets ?: 0L) + (traffic?.totalTxPackets ?: 0L)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "NETWORK INTERFACE TELEMETRY", style = TechnicalValue.copy(fontSize = 10.5.sp, color = Info, fontWeight = FontWeight.Bold))
                        Text(
                            text = "↓ ${String.format("%.1f", rxMb)} MB • ↑ ${String.format("%.1f", txMb)} MB  |  $totalPackets Pkts",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshSockets(context) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Info, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Tab 1: LIVE PACKETS
            if (state.selectedTab == "PACKETS") {
                val livePackets = traffic?.livePackets ?: emptyList()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE IN-FLIGHT PACKET STREAM (${livePackets.size})",
                        style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                    Text(text = "REAL SOCKET DELTAS", style = MetadataText.copy(fontSize = 9.sp, color = Info))
                }

                if (livePackets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = "Polling kernel socket table for active packets...", style = MetadataText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(livePackets) { pkt ->
                            val borderCol = if (pkt.isSuspicious) CriticalBorder else BorderColor
                            val cardBg = if (pkt.isSuspicious) CriticalBg.copy(alpha = 0.3f) else Surface0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = cardBg)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .background(if (pkt.protocol == "TCP") InfoBg else WarningBg, RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(text = pkt.protocol, style = TechnicalValue.copy(fontSize = 8.5.sp, color = if (pkt.protocol == "TCP") Info else Warning))
                                            }
                                            Text(
                                                text = pkt.appName,
                                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Text(text = pkt.timeFormatted, style = MetadataText.copy(fontSize = 8.5.sp))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${pkt.localEndpoint}  →  ${pkt.remoteEndpoint}  (${pkt.sizeBytes} bytes)",
                                        style = MetadataText.copy(fontSize = 9.5.sp, color = TextSecondary)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pkt.threatMarkdown,
                                        style = TechnicalValue.copy(
                                            fontSize = 9.sp,
                                            color = if (pkt.isSuspicious) Critical else Healthy
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: ACTIVE SOCKETS
            if (state.selectedTab == "SOCKETS") {
                val conns = traffic?.activeConnections ?: emptyList()
                Text(
                    text = "ACTIVE LINUX DEVICE SOCKETS (${conns.size})",
                    style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                if (conns.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = "No open outbound sockets detected.", style = MetadataText)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(conns) { conn ->
                            val isThreat = conn.riskLevel != "SECURE"
                            val borderCol = if (isThreat) CriticalBorder else BorderColor

                            Card(
                                modifier = Modifier.fillMaxWidth().border(1.dp, borderCol, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = Surface0)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = conn.appName, style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold))
                                        Box(
                                            modifier = Modifier
                                                .background(if (conn.state == "ESTABLISHED") HealthyBg else Surface1, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = conn.state, style = TechnicalValue.copy(fontSize = 8.5.sp, color = if (conn.state == "ESTABLISHED") Healthy else TextSecondary))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${conn.localAddress}:${conn.localPort} → ${conn.remoteAddress}:${conn.remotePort} (${conn.protocol})", style = MetadataText.copy(fontSize = 9.sp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = conn.securityNote, style = MetadataText.copy(fontSize = 8.5.sp, color = if (isThreat) Critical else TextSecondary))
                                }
                            }
                        }
                    }
                }
            }

            // Tab 3: TOPOLOGY GRAPH (Item 7)
            if (state.selectedTab == "TOPOLOGY") {
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

                        // Draw Edges
                        state.edges.forEach { edge ->
                            val src = nodeMap[edge.source]
                            val dst = nodeMap[edge.target]
                            if (src != null && dst != null) {
                                drawLine(
                                    color = Info.copy(alpha = 0.6f),
                                    start = Offset(src.x, src.y),
                                    end = Offset(dst.x, dst.y),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )
                            }
                        }

                        // Draw Nodes with Pulse
                        state.nodes.forEach { node ->
                            val isSel = state.selectedNode?.id == node.id
                            val isThreat = node.riskScore > 0.5f
                            val col = if (isThreat) Critical else Info

                            if (isSel || isThreat) {
                                drawCircle(
                                    color = col.copy(alpha = 0.25f),
                                    radius = 28.dp.toPx() * pulseScale,
                                    center = Offset(node.x, node.y)
                                )
                            }

                            drawCircle(color = col, radius = 16.dp.toPx(), center = Offset(node.x, node.y))
                            drawCircle(color = Bg0, radius = 7.dp.toPx(), center = Offset(node.x, node.y))
                        }
                    }

                    // Top Legend
                    Text(
                        text = "LIVE TOPOLOGY GRAPH • INTERACTIVE",
                        style = TechnicalValue.copy(fontSize = 9.sp, color = TextMuted),
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    )
                }

                // Selected Node Detail Card
                state.selectedNode?.let { n ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, InfoBorder, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Surface1)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = n.label, style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold))
                                SecurityStatusPill(riskScore = (n.riskScore * 100).toInt())
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "TYPE: ${n.type} • CRITICALITY: ${n.criticality}", style = MetadataText.copy(fontSize = 9.sp))
                        }
                    }
                }
            }
        }
    }
}
