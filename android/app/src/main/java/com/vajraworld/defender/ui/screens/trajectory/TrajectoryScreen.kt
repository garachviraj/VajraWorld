package com.vajraworld.defender.ui.screens.trajectory

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.FutureBranch
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun TrajectoryScreen(
    viewModel: TrajectoryViewModel,
    onNavigateToSimulation: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Progressive Draw-in Animation for Projected Curve on New Horizon Data
    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(state.horizonRisks) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    // Confidence-Driven Pulse Speed: Faster & Jittery when uncertain, Calm & Slow when confident
    val pulseSpeedMs = remember(state.uncertainty) {
        (1100 + ((1.0f - state.uncertainty) * 1600)).toInt()
    }
    val infiniteTransition = rememberInfiniteTransition(label = "TrajectoryPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseSpeedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeIndex = remember(state.scrubTimeOffsetSec, state.timelineNodes) {
        val idx = state.timelineNodes.indexOfFirst { it.secondsOffset == state.scrubTimeOffsetSec }
        if (idx != -1) idx else 2.coerceAtMost(state.timelineNodes.size - 1)
    }
    val activeNode = state.timelineNodes.getOrElse(activeIndex) {
        state.timelineNodes.firstOrNull() ?: TrajectoryTimelineNode("NOW", 0, "Nominal", 20, false, "T1082", "Nominal")
    }

    // Smooth Scrubber Reticle Animation
    val targetScrubFraction = remember(activeIndex, state.timelineNodes.size) {
        if (state.timelineNodes.size > 1) {
            activeIndex.toFloat() / (state.timelineNodes.size - 1)
        } else 0f
    }
    val animatedScrubFraction by animateFloatAsState(
        targetValue = targetScrubFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scrubGlide"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "ATT&CK TRAJECTORY",
            subtitle = "LATENT RECURRENT MULTI-HORIZON ROLLOUT",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Corroboration & Freshness Ticker Banner
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
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (state.isLoading) Warning else Healthy))
                    Text(
                        text = state.corroborationBadge,
                        style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "T+${state.secondsSinceRefresh}s",
                    style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextSecondary)
                )
            }

            // Threat Velocity & Lead Time Telemetry Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Info, modifier = Modifier.size(16.dp))
                            Text(
                                text = "TEMPORAL KINEMATICS",
                                style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(InfoBg, RoundedCornerShape(4.dp))
                                .border(1.dp, InfoBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "K-STEP ROLLOUT",
                                style = TechnicalValue.copy(fontSize = 9.sp, color = Info, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(text = "VELOCITY", style = MetadataText.copy(fontSize = 8.5.sp))
                                Text(
                                    text = "+${String.format(java.util.Locale.US, "%.3f", state.threatVelocity)} /s",
                                    style = TechnicalValue.copy(fontSize = 11.5.sp, color = Warning, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(text = "LEAD TIME", style = MetadataText.copy(fontSize = 8.5.sp))
                                Text(
                                    text = "~${state.leadTimeCountdownSec}s Left",
                                    style = TechnicalValue.copy(fontSize = 11.5.sp, color = Info, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(text = "UNCERTAINTY", style = MetadataText.copy(fontSize = 8.5.sp))
                                Text(
                                    text = "±${String.format(java.util.Locale.US, "%.2f", state.uncertainty)} (${(state.confidence * 100).toInt()}%)",
                                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // 100% Data-Driven Canvas Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = Info, modifier = Modifier.size(16.dp))
                            Text(
                                text = "MULTISTEP RISK TRAJECTORY",
                                style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp, 2.dp).background(Info))
                                Text(text = "Observed", style = MetadataText.copy(fontSize = 9.sp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp, 2.dp).background(Critical))
                                Text(text = "Projected", style = MetadataText.copy(fontSize = 9.sp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface1)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(state.timelineNodes) {
                                    detectTapGestures { offset ->
                                        if (state.timelineNodes.isNotEmpty()) {
                                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                            val closestIndex = ((fraction * (state.timelineNodes.size - 1)) + 0.5f).toInt().coerceIn(0, state.timelineNodes.size - 1)
                                            val closest = state.timelineNodes[closestIndex]
                                            viewModel.setScrubTime(closest.secondsOffset)
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val nodes = state.timelineNodes

                            if (nodes.isNotEmpty()) {
                                val nodeCount = nodes.size
                                val midIndex = 2.coerceAtMost(nodeCount - 1) // NOW index
                                val midX = (midIndex.toFloat() / (nodeCount - 1)) * w

                                // 1. Map all data points strictly from real nodes
                                val points = nodes.mapIndexed { idx, node ->
                                    val x = (idx.toFloat() / (nodeCount - 1)) * w
                                    val y = h * (1f - (node.riskPct.toFloat() / 100f).coerceIn(0.08f, 0.92f))
                                    Offset(x, y)
                                }

                                // Draw subtle grid vertical dividing line at NOW
                                drawLine(
                                    color = BorderSubtle,
                                    start = Offset(midX, 0f),
                                    end = Offset(midX, h),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                                )

                                // 2. Observed Path (0..midIndex)
                                if (midIndex > 0) {
                                    val obsPath = Path().apply {
                                        moveTo(points[0].x, points[0].y)
                                        for (i in 1..midIndex) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                    }
                                    drawPath(obsPath, Info, style = Stroke(width = 3.dp.toPx()))
                                }

                                // 3. Confidence-Scaled Shaded Uncertainty Envelope
                                if (midIndex < nodeCount - 1) {
                                    val bandPath = Path().apply {
                                        moveTo(points[midIndex].x, points[midIndex].y)
                                        // Forward pass: upper spread
                                        for (i in (midIndex + 1) until nodeCount) {
                                            val factor = (i - midIndex).toFloat() / (nodeCount - 1 - midIndex)
                                            val spread = (h * state.uncertainty * 1.6f * factor).coerceIn(6f, h * 0.42f)
                                            lineTo(points[i].x, (points[i].y - spread).coerceAtLeast(6f))
                                        }
                                        // Backward pass: lower spread
                                        for (i in (nodeCount - 1) downTo (midIndex + 1)) {
                                            val factor = (i - midIndex).toFloat() / (nodeCount - 1 - midIndex)
                                            val spread = (h * state.uncertainty * 1.6f * factor).coerceIn(6f, h * 0.42f)
                                            lineTo(points[i].x, (points[i].y + spread).coerceAtMost(h - 6f))
                                        }
                                        lineTo(points[midIndex].x, points[midIndex].y)
                                        close()
                                    }
                                    drawPath(bandPath, Critical.copy(alpha = 0.12f))

                                    // 4. Projected Curve Drawn Progressively
                                    val predPath = Path().apply {
                                        moveTo(points[midIndex].x, points[midIndex].y)
                                        for (i in (midIndex + 1) until nodeCount) {
                                            val targetPt = points[i]
                                            val prevPt = points[i - 1]
                                            val stepFrac = 1f / (nodeCount - 1 - midIndex)
                                            val stepStart = (i - midIndex - 1) * stepFrac
                                            val localProg = ((drawProgress.value - stepStart) / stepFrac).coerceIn(0f, 1f)
                                            if (localProg > 0f) {
                                                val curX = prevPt.x + (targetPt.x - prevPt.x) * localProg
                                                val curY = prevPt.y + (targetPt.y - prevPt.y) * localProg
                                                lineTo(curX, curY)
                                            }
                                        }
                                    }
                                    drawPath(
                                        predPath,
                                        Critical.copy(alpha = pulseAlpha),
                                        style = Stroke(
                                            width = 3.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                        )
                                    )
                                }

                                // 5. Node Dots on Real Data Coordinates
                                points.forEachIndexed { idx, pt ->
                                    val isObserved = idx <= midIndex
                                    val dotColor = if (isObserved) Info else Critical
                                    drawCircle(color = dotColor, radius = 4.dp.toPx(), center = pt)
                                    drawCircle(color = Surface0, radius = 2.dp.toPx(), center = pt)
                                }

                                // 6. Gliding Scrubber Reticle
                                val scrubX = w * animatedScrubFraction
                                drawLine(
                                    color = Info,
                                    start = Offset(scrubX, 0f),
                                    end = Offset(scrubX, h),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                                )
                                drawCircle(color = Info, radius = 5.dp.toPx(), center = Offset(scrubX, h * 0.5f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Milestone Scrub Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.timelineNodes) { node ->
                            val isSelected = node.secondsOffset == activeNode.secondsOffset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Info else Surface1)
                                    .border(1.dp, if (isSelected) Info else BorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setScrubTime(node.secondsOffset) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = node.offset,
                                        style = TechnicalValue.copy(
                                            fontSize = 9.sp,
                                            color = if (isSelected) Bg0 else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${node.riskPct}%",
                                        style = MetadataText.copy(
                                            fontSize = 8.5.sp,
                                            color = if (isSelected) Bg0.copy(alpha = 0.85f) else TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Inspected Horizon Step Card with AnimatedContent Transition
            AnimatedContent(
                targetState = activeNode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 40)) + slideInHorizontally(animationSpec = tween(220)) { it / 6 })
                        .togetherWith(fadeOut(animationSpec = tween(140)) + slideOutHorizontally(animationSpec = tween(140)) { -it / 6 })
                },
                label = "ActiveNodeInspectionTransition"
            ) { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "HORIZON STEP: ${node.offset} (${if (node.isPredicted) "PROJECTED" else "OBSERVED"})",
                                    style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = node.stage.uppercase(),
                                    style = TechnicalValue.copy(fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                            SecurityStatusPill(riskScore = node.riskPct)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = node.description,
                            style = Typography.bodySmall.copy(color = TextSecondary, lineHeight = 16.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "ATT&CK: ${node.mitreTactic}", style = TechnicalValue.copy(fontSize = 9.sp, color = TextPrimary))
                                Text(
                                    text = if (node.isPredicted) "CONFIDENCE: ${(state.confidence * 100).toInt()}%" else "VERIFIED HARDWARE RECORD",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = if (node.isPredicted) Info else Healthy)
                                )
                            }
                        }
                    }
                }
            }

            // Top Feature Drivers Panel (The Highest-Value Informative Addition)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Info))
                            Text(
                                text = "TOP MODEL DRIVERS & FEATURE ATTRIBUTION",
                                style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(text = "${state.drivers.size} SIGNALS", style = MetadataText.copy(fontSize = 9.sp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.drivers.forEach { driver ->
                            val isSelected = state.selectedDriver?.feature == driver.feature
                            val isRiskUp = driver.direction == "up"
                            val barFraction by animateFloatAsState(
                                targetValue = (kotlin.math.abs(driver.impact) / 0.35f).coerceIn(0.08f, 1.0f),
                                animationSpec = tween(800, easing = FastOutSlowInEasing),
                                label = "driverBar"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Surface2 else Surface1)
                                    .border(1.dp, if (isSelected) Info else BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectDriver(driver) }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = if (isRiskUp) "▲" else "▼",
                                            style = TechnicalValue.copy(fontSize = 11.sp, color = if (isRiskUp) Critical else Healthy)
                                        )
                                        Text(
                                            text = driver.feature,
                                            style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Text(
                                        text = "${if (driver.impact > 0) "+" else ""}${String.format(java.util.Locale.US, "%.2f", driver.impact)}",
                                        style = TechnicalValue.copy(
                                            fontSize = 11.sp,
                                            color = if (isRiskUp) Critical else Healthy,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Proportional animated magnitude bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Surface2)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = barFraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isRiskUp) Critical else Healthy)
                                    )
                                }

                                if (isSelected && driver.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = driver.description,
                                        style = MetadataText.copy(fontSize = 9.5.sp, color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Future Branches & Simulator Bridge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Info))
                            Text(
                                text = "FUTURE ATT&CK PROBABILITY BRANCHES",
                                style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(text = "${state.branches.size} BRANCHES", style = MetadataText.copy(fontSize = 9.sp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.branches.forEach { branch ->
                            val isSelected = state.selectedBranch?.name == branch.name
                            val probFraction by animateFloatAsState(
                                targetValue = branch.probability,
                                animationSpec = tween(900, easing = FastOutSlowInEasing),
                                label = "branchProb"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Surface2 else Surface1)
                                    .border(1.dp, if (isSelected) Info else BorderSubtle, RoundedCornerShape(8.dp))
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
                                        style = TechnicalValue.copy(fontSize = 11.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(InfoBg, RoundedCornerShape(4.dp))
                                            .border(1.dp, InfoBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${(branch.probability * 100).toInt()}% PROB",
                                            style = TechnicalValue.copy(fontSize = 9.sp, color = Info, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Probability Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(Surface2)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = probFraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(if (branch.probability > 0.5f) Info else Warning)
                                    )
                                }

                                // Expanded Branch Details & Direct Simulator Action
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "TERMINAL STAGE: ${branch.terminalStage.uppercase()}",
                                                style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextSecondary)
                                            )
                                            Text(
                                                text = "TREND: ${branch.trajectoryTrend.uppercase()}",
                                                style = TechnicalValue.copy(
                                                    fontSize = 9.5.sp,
                                                    color = if (branch.trajectoryTrend.contains("Stabilizing", true)) Healthy else Warning
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (onNavigateToSimulation != null) {
                                            Button(
                                                onClick = onNavigateToSimulation,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(34.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Info)
                                            ) {
                                                Text(
                                                    text = "SIMULATE DEFENCE AGAINST THIS BRANCH",
                                                    style = TechnicalValue.copy(fontSize = 10.sp, color = Bg0, fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
