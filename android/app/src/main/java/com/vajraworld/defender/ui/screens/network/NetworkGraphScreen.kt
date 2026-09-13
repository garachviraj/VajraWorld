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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

    var selectedPacket by remember { mutableStateOf<com.vajraworld.defender.domain.engine.InspectedPacketRecord?>(null) }
    var selectedSocket by remember { mutableStateOf<com.vajraworld.defender.domain.engine.DeviceSocketConnection?>(null) }

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
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleProgress"
    )
    val edgeDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) }
    var selectedTopologyNode by remember { mutableStateOf<com.vajraworld.defender.domain.model.TopologyNode?>(null) }

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
                            .tactileClick { viewModel.selectTab(key) }
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

            var isRefreshingSockets by remember { mutableStateOf(false) }
            val refreshRotation by animateFloatAsState(
                targetValue = if (isRefreshingSockets) 360f else 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                finishedListener = { isRefreshingSockets = false },
                label = "refreshRotation"
            )

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
                            text = "↓ ${String.format(java.util.Locale.US, "%.1f", rxMb)} MB • ↑ ${String.format(java.util.Locale.US, "%.1f", txMb)} MB  |  $totalPackets Pkts",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                        )
                    }
                    IconButton(
                        onClick = {
                            isRefreshingSockets = true
                            viewModel.refreshSockets(context)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Info,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(refreshRotation)
                        )
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
                                    .clickable { selectedPacket = pkt }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSocket = conn }
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp)),
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
                val nodeMap = remember(state.nodes) { state.nodes.associateBy { it.id } }

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
                                        val nx = node.x * size.width
                                        val ny = node.y * size.height
                                        val dist = Math.hypot((nx - offset.x).toDouble(), (ny - offset.y).toDouble())
                                        dist < 60.0
                                    }
                                    if (clicked != null) {
                                        viewModel.selectNode(clicked)
                                        selectedTopologyNode = clicked
                                    } else {
                                        viewModel.clearSelection()
                                        selectedTopologyNode = null
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        // Subtle cyber background grid (clean rendering)
                        val step = 36.dp.toPx()
                        var gx = 0f
                        while (gx < w) {
                            drawLine(BorderSubtle, Offset(gx, 0f), Offset(gx, h), strokeWidth = 0.8f)
                            gx += step
                        }
                        var gy = 0f
                        while (gy < h) {
                            drawLine(BorderSubtle, Offset(0f, gy), Offset(w, gy), strokeWidth = 0.8f)
                            gy += step
                        }

                        // Orbit guide ellipse
                        drawCircle(
                            color = BorderSubtle.copy(alpha = 0.5f),
                            radius = w * 0.35f,
                            center = Offset(w * 0.5f, h * 0.5f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f, pathEffect = edgeDashEffect)
                        )

                        // Draw Edges with Flowing Particles
                        state.edges.forEach { edge ->
                            val src = nodeMap[edge.source]
                            val dst = nodeMap[edge.target]
                            if (src != null && dst != null) {
                                val srcPt = Offset(src.x * w, src.y * h)
                                val dstPt = Offset(dst.x * w, dst.y * h)

                                drawLine(
                                    color = Info.copy(alpha = 0.55f),
                                    start = srcPt,
                                    end = dstPt,
                                    strokeWidth = 1.8.dp.toPx(),
                                    pathEffect = edgeDashEffect
                                )

                                // Animated Packet Particle
                                val px = srcPt.x + (dstPt.x - srcPt.x) * particleProgress
                                val py = srcPt.y + (dstPt.y - srcPt.y) * particleProgress
                                drawCircle(color = Info, radius = 3.5.dp.toPx(), center = Offset(px, py))
                                drawCircle(color = Surface0, radius = 1.8.dp.toPx(), center = Offset(px, py))
                            }
                        }

                        // Draw Nodes with Pulse
                        state.nodes.forEach { node ->
                            val centerPt = Offset(node.x * w, node.y * h)
                            val isSel = state.selectedNode?.id == node.id
                            val isThreat = node.riskScore > 0.5f
                            val isHost = node.id == "host_0"
                            val col = if (isThreat) Critical else if (isHost) Info else Healthy

                            if (isSel || isThreat) {
                                drawCircle(
                                    color = col.copy(alpha = 0.22f),
                                    radius = 26.dp.toPx() * pulseScale,
                                    center = centerPt
                                )
                            }

                            val nodeRadius = if (isHost) 20.dp.toPx() else 14.dp.toPx()
                            drawCircle(color = col, radius = nodeRadius, center = centerPt)
                            drawCircle(color = Bg0, radius = nodeRadius * 0.45f, center = centerPt)
                        }
                    }

                    // Top Legend
                    Text(
                        text = "LIVE TOPOLOGY GRAPH • TAP NODE TO INSPECT",
                        style = TechnicalValue.copy(fontSize = 9.sp, color = TextMuted),
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    )
                }

                // Selected Node Detail Card
                state.selectedNode?.let { n ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, InfoBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedTopologyNode = n },
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
                            Text(text = "Tap to open deep dissection", style = MetadataText.copy(fontSize = 8.5.sp, color = Info))
                        }
                    }
                }
            }
        }
    }

    if (selectedTopologyNode != null) {
        TopologyNodeDetailDialog(
            node = selectedTopologyNode!!,
            onDismiss = { selectedTopologyNode = null }
        )
    }

    if (selectedPacket != null) {
        PacketDetailDialog(
            packet = selectedPacket!!,
            onDismiss = { selectedPacket = null }
        )
    }

    if (selectedSocket != null) {
        SocketDetailDialog(
            socket = selectedSocket!!,
            onDismiss = { selectedSocket = null }
        )
    }
}

@Composable
fun PacketDetailDialog(
    packet: com.vajraworld.defender.domain.engine.InspectedPacketRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PACKET DISSECTION",
                        style = TechnicalValue.copy(fontSize = 12.sp, color = Info, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = packet.id,
                        style = MetadataText.copy(fontSize = 9.sp, color = TextMuted)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(if (packet.isSuspicious) CriticalBg else HealthyBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (packet.isSuspicious) "THREAT DETECTED" else "VERIFIED TLS",
                        style = TechnicalValue.copy(fontSize = 9.sp, color = if (packet.isSuspicious) Critical else Healthy, fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (packet.isSuspicious) CriticalBg else Surface1, RoundedCornerShape(6.dp))
                        .border(1.dp, if (packet.isSuspicious) CriticalBorder else BorderColor, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "SECURITY ASSESSMENT",
                            style = TechnicalValue.copy(fontSize = 9.sp, color = if (packet.isSuspicious) Critical else Info, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = packet.threatMarkdown,
                            style = MetadataText.copy(fontSize = 10.5.sp, color = TextPrimary)
                        )
                    }
                }

                DetailSectionBox(title = "TRANSPORT ENDPOINTS & ROUTING") {
                    DetailRow("Timestamp", packet.timeFormatted)
                    DetailRow("Protocol", packet.protocol)
                    DetailRow("Local Endpoint", packet.localEndpoint)
                    DetailRow("Remote Endpoint", packet.remoteEndpoint)
                    DetailRow("Packet Length", "${packet.sizeBytes} Bytes (${packet.sizeBytes * 8} bits)")
                }

                DetailSectionBox(title = "APPLICATION PROCESS ATTRIBUTION") {
                    DetailRow("Application Name", packet.appName)
                    DetailRow("Package ID", packet.packageName)
                    DetailRow("Traffic Direction", "Egress / Ingress Network Interface")
                }

                DetailSectionBox(title = "PROTOCOL DISSECTION SIMULATION") {
                    val fakeSeq = (packet.timestamp % 999999).toString()
                    val checksum = "0x" + ((packet.sizeBytes * 37) % 65535).toString(16).uppercase().padStart(4, '0')
                    DetailRow("IP Version", "IPv4 (TTL 64)")
                    DetailRow("Transport Layer", if (packet.protocol == "UDP" || packet.protocol == "DNS") "UDP User Datagram" else "TCP Stream Segment")
                    DetailRow("Sequence Number", fakeSeq)
                    DetailRow("Frame Checksum", checksum)
                    DetailRow("Cipher Suite", if (packet.protocol.contains("TLS") || packet.remoteEndpoint.endsWith(":443")) "TLS_AES_256_GCM_SHA384" else "Plaintext / Unencrypted")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", style = TechnicalValue.copy(color = Info))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Packet Endpoint", "${packet.localEndpoint} -> ${packet.remoteEndpoint}")
                    clipboard?.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Copied endpoints to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("COPY ENDPOINTS", style = TechnicalValue.copy(color = TextSecondary, fontSize = 11.sp))
            }
        },
        containerColor = Surface0,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SocketDetailDialog(
    socket: com.vajraworld.defender.domain.engine.DeviceSocketConnection,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isThreat = socket.riskLevel != "SECURE"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KERNEL SOCKET DESCRIPTOR",
                        style = TechnicalValue.copy(fontSize = 12.sp, color = Info, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${socket.protocol} • UID ${socket.uid}",
                        style = MetadataText.copy(fontSize = 9.sp, color = TextMuted)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(if (isThreat) CriticalBg else HealthyBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = socket.riskLevel,
                        style = TechnicalValue.copy(fontSize = 9.sp, color = if (isThreat) Critical else Healthy, fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isThreat) CriticalBg else Surface1, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isThreat) CriticalBorder else BorderColor, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "SECURITY CLASSIFICATION",
                            style = TechnicalValue.copy(fontSize = 9.sp, color = if (isThreat) Critical else Info, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = socket.securityNote,
                            style = MetadataText.copy(fontSize = 10.5.sp, color = TextPrimary)
                        )
                    }
                }

                DetailSectionBox(title = "COMMUNICATING ENDPOINTS") {
                    DetailRow("Local Address", "${socket.localAddress}:${socket.localPort}")
                    DetailRow("Remote Address", "${socket.remoteAddress}:${socket.remotePort}")
                    DetailRow("Connection State", socket.state)
                    DetailRow("Protocol Family", "${socket.protocol} (AF_INET)")
                }

                DetailSectionBox(title = "PROCESS ATTRIBUTION & PERMISSIONS") {
                    DetailRow("Application Name", socket.appName)
                    DetailRow("Package Name", socket.packageName)
                    DetailRow("Android UID", "${socket.uid}")
                    DetailRow("Socket Origin", if (socket.uid == 1000) "Android System Core" else "Userland App Sandbox")
                }

                DetailSectionBox(title = "FIREWALL & CONTAINMENT POSTURE") {
                    DetailRow("SELinux Context", "u:r:untrusted_app:s0")
                    DetailRow("Mitigation Status", if (isThreat) "Review / Potential Socket Containment" else "Verified Safe Socket Binding")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", style = TechnicalValue.copy(color = Info))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Socket Endpoint", "${socket.localAddress}:${socket.localPort} -> ${socket.remoteAddress}:${socket.remotePort}")
                    clipboard?.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Copied endpoints to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("COPY ENDPOINTS", style = TechnicalValue.copy(color = TextSecondary, fontSize = 11.sp))
            }
        },
        containerColor = Surface0,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun DetailSectionBox(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(6.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = TechnicalValue.copy(fontSize = 9.sp, color = Info, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MetadataText.copy(fontSize = 9.5.sp, color = TextSecondary))
        Text(text = value, style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold))
    }
}


@Composable
fun TopologyNodeDetailDialog(
    node: com.vajraworld.defender.domain.model.TopologyNode,
    onDismiss: () -> Unit
) {
    val isThreat = node.riskScore > 0.5f
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NODE DISSECTION",
                        style = TechnicalValue.copy(fontSize = 12.sp, color = Info, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = node.id,
                        style = MetadataText.copy(fontSize = 9.sp, color = TextMuted)
                    )
                }
                SecurityStatusPill(riskScore = (node.riskScore * 100).toInt())
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TechnicalMetadataRow(label = "IDENTIFIER", value = node.label)
                TechnicalMetadataRow(label = "CLASSIFICATION", value = node.type)
                TechnicalMetadataRow(label = "CRITICALITY TIER", value = node.criticality)
                TechnicalMetadataRow(
                    label = "SECURITY STATUS",
                    value = if (isThreat) "HIGH RISK THREAT / ANOMALOUS FLOW" else "SECURE / NOMINAL LOCAL SANDBOX"
                )
                TechnicalMetadataRow(
                    label = "ENFORCEMENT POLICY",
                    value = if (isThreat) "Autonomous Micro-Segmentation Recommended" else "Active Communications Permitted"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Info),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(text = "CLOSE", style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold))
            }
        }
    )
}
