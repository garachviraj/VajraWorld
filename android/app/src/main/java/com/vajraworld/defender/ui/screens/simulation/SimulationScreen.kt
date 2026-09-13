package com.vajraworld.defender.ui.screens.simulation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.FactorAttribution
import com.vajraworld.defender.domain.model.NarrativeEvent
import com.vajraworld.defender.domain.model.SimulationResult
import com.vajraworld.defender.domain.model.TimelinePoint
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SimulationScreen(
    viewModel: SimulationViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Staggered entrance state for initial load
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }

    // Replay progress controller (0.0f to 1.0f)
    val replayProgress = remember { Animatable(0f) }
    var isPlaying by remember { mutableStateOf(false) }

    // When a simulation result is produced, automatically trigger progressive replay
    LaunchedEffect(state.lastResult) {
        state.lastResult?.let {
            replayProgress.snapTo(0f)
            isPlaying = true
            replayProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
            )
            isPlaying = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "DEFENCE SIMULATOR",
            subtitle = "ANIMATED INCIDENT REPLAY & COUNTERMEASURES",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Read-Only Safety Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HealthyBg)
                    .border(1.dp, HealthyBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "READ-ONLY LATENT WORLD MODEL",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy)
                )
                Text(
                    text = "Zero Device Disruption",
                    style = MetadataText
                )
            }

            // Notification / Status Message if rule applied
            state.statusMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HealthyBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Healthy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 1: Threat Scenario Selector with Staggered Entrance
            Text(
                text = "1. SELECT CYBER ATTACK SCENARIO",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableScenarios.forEachIndexed { index, scenario ->
                    val isSelected = scenario.id == state.selectedScenarioId
                    AnimatedVisibility(
                        visible = entered,
                        enter = fadeIn(animationSpec = tween(250, delayMillis = index * 35)) +
                                slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(250, delayMillis = index * 35)
                                )
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tactileClick { viewModel.selectScenario(scenario) }
                                .border(
                                    1.dp,
                                    if (isSelected) Info else BorderColor,
                                    RoundedCornerShape(10.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) InfoBg else Surface0
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = scenario.title,
                                        style = TechnicalValue.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Info else TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = scenario.mitreTactic.split(" ").firstOrNull() ?: "",
                                        style = TechnicalValue.copy(fontSize = 10.sp, color = Warning, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = scenario.description,
                                    style = MetadataText.copy(color = TextSecondary, fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vector: ${scenario.attackVector}",
                                        style = MetadataText.copy(fontSize = 10.sp, color = TextMuted)
                                    )
                                    Text(
                                        text = "Base: ${(scenario.baselineRisk * 100).toInt()}% -> ${(scenario.mitigatedRisk * 100).toInt()}%",
                                        style = TechnicalValue.copy(fontSize = 10.sp, color = if (isSelected) Healthy else TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Target Containment Entity
            Text(
                text = "2. CONTAINMENT TARGET (DEVICE LAYER)",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
            )

            val assetScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(assetScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableAssets.forEach { asset ->
                    val isSelected = asset == state.targetAsset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) InfoBg else Surface0)
                            .border(1.dp, if (isSelected) Info else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectTargetAsset(asset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = asset,
                            style = TechnicalValue.copy(
                                fontSize = 11.sp,
                                color = if (isSelected) Info else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Section 3: Countermeasure Strategy
            Text(
                text = "3. DEFENSIVE COUNTERMEASURE",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.availableActions.forEach { action ->
                    val isSelected = state.selectedAction == action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) InfoBg else Surface0)
                            .border(1.dp, if (isSelected) InfoBorder else BorderColor, RoundedCornerShape(8.dp))
                            .tactileClick { viewModel.selectAction(action) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action.replace("_", " "),
                            style = TechnicalValue.copy(
                                fontSize = 12.sp,
                                color = if (isSelected) Info else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isSelected) Info else TextMuted, CircleShape)
                                .background(if (isSelected) Info else Color.Transparent)
                        )
                    }
                }
            }

            // Section 4: Multi-Step Rollout Progression (when running)
            if (state.isRunning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, InfoBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = InfoBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "COMPUTING LATENT WORLD MODEL TRAJECTORY...",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StepIndicator(stepNum = 1, label = "Ingress State", active = state.simulationStep >= 1, done = state.simulationStep > 1)
                            StepIndicator(stepNum = 2, label = "Intervention", active = state.simulationStep >= 2, done = state.simulationStep > 2)
                            StepIndicator(stepNum = 3, label = "Containment", active = state.simulationStep >= 3, done = state.simulationStep > 3)
                        }
                    }
                }
            }

            // Run Simulation CTA Button with Shimmer and Tactile Click
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (state.isRunning) Info.copy(alpha = 0.6f) else Info)
                    .then(
                        if (!state.isRunning) Modifier.tactileClick { viewModel.runSimulation() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!state.isRunning) {
                    Box(modifier = Modifier.matchParentSize().background(shimmerBrush()))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Simulate",
                        tint = Bg0,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isRunning) "SIMULATING IN LATENT SPACE..." else "RUN COUNTERFACTUAL SIMULATION",
                        color = Bg0,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Animated Story-Driven Incident Replay Experience Card
            AnimatedVisibility(
                visible = state.lastResult != null && !state.isRunning,
                enter = fadeIn() + expandVertically()
            ) {
                state.lastResult?.let { res ->
                    val progress = replayProgress.value
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HealthyBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Surface0)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Healthy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "INCIDENT TRAJECTORY REPLAY",
                                        style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(HealthyBg, RoundedCornerShape(4.dp))
                                        .border(1.dp, HealthyBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (progress >= 1f) "CONTAINED" else if (progress >= res.interventionT) "INTERVENED" else "ATTACK ACTIVE",
                                        style = TechnicalValue.copy(
                                            fontSize = 9.sp,
                                            color = if (progress >= 1f) Healthy else if (progress >= res.interventionT) Info else Critical
                                        )
                                    )
                                }
                            }

                            // 1. Replay Scrubber & Control Bar
                            ReplayScrubberBar(
                                progress = progress,
                                isPlaying = isPlaying,
                                onTogglePlayPause = {
                                    if (isPlaying) {
                                        coroutineScope.launch {
                                            replayProgress.stop()
                                            isPlaying = false
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            isPlaying = true
                                            if (replayProgress.value >= 1f) {
                                                replayProgress.snapTo(0f)
                                            }
                                            val remainingMs = ((1f - replayProgress.value) * 3000).toInt().coerceAtLeast(200)
                                            replayProgress.animateTo(1f, tween(remainingMs, easing = LinearEasing))
                                            isPlaying = false
                                        }
                                    }
                                },
                                onRestart = {
                                    coroutineScope.launch {
                                        replayProgress.snapTo(0f)
                                        isPlaying = true
                                        replayProgress.animateTo(1f, tween(3000, easing = LinearOutSlowInEasing))
                                        isPlaying = false
                                    }
                                },
                                onSeek = { target ->
                                    coroutineScope.launch {
                                        isPlaying = false
                                        replayProgress.snapTo(target)
                                    }
                                }
                            )

                            // 2. Progressive Risk Trajectory Canvas Chart
                            DualRiskComparisonCanvas(
                                timeline = res.timeline,
                                baselineRisk = res.baselineRisk,
                                residualRisk = res.residualRisk,
                                interventionT = res.interventionT,
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )

                            // 3. Mini Topology / Attack-Path View with Defensive Shield Ripple
                            MiniTopologyAttackPath(
                                progress = progress,
                                interventionT = res.interventionT,
                                targetAsset = res.targetAsset,
                                actionType = res.actionType,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                            )

                            // 4. Animated Risk Counter & Radial Gauge
                            AnimatedRiskSummaryRow(
                                res = res,
                                progress = progress
                            )

                            // 5. Live Narrative Feed synced with playback progress
                            LiveNarrativeFeed(
                                narrative = res.narrative,
                                progress = progress
                            )

                            // 6. Explainability: "Why this outcome?" SHAP Attribution Panel
                            WhyThisOutcomePanel(
                                factors = res.topFactors,
                                isExpanded = state.isWhyOutcomeExpanded,
                                onToggle = { viewModel.toggleWhyOutcome() }
                            )

                            HorizontalDivider(color = BorderSubtle)

                            // Metadata rows
                            TechnicalMetadataRow(label = "Mitigated State", value = res.newLikelyStage)
                            TechnicalMetadataRow(label = "Operational Disruption", value = res.disruptionRating)
                            TechnicalMetadataRow(label = "Defence Utility Score", value = String.format(Locale.US, "%.2f", res.utilityScore))
                            TechnicalMetadataRow(label = "Simulation ID", value = res.simulationId, allowCopy = true)

                            // CTA: Apply Mitigation Rule to Active Defender Engine
                            Button(
                                onClick = { viewModel.applyMitigationRule() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Healthy)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Bg0,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "APPLY MITIGATION RULE TO DEFENDER",
                                    color = Bg0,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- SUB-COMPONENTS ----------------------

@Composable
private fun StepIndicator(stepNum: Int, label: String, active: Boolean, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) Healthy else if (active) Info else Surface2)
                .border(1.dp, if (done) Healthy else if (active) Info else BorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNum",
                style = TechnicalValue.copy(
                    fontSize = 11.sp,
                    color = if (active || done) Bg0 else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MetadataText.copy(fontSize = 9.sp, color = if (active || done) TextPrimary else TextMuted)
        )
    }
}

@Composable
private fun ReplayScrubberBar(
    progress: Float,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Bg0)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Info,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format(Locale.US, "T+%.1fs / 3.0s", progress * 3.0f),
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                )
            }

            Text(
                text = if (progress >= 1f) "Replay Complete" else "Scrub Timeline",
                style = MetadataText.copy(fontSize = 10.sp, color = TextMuted)
            )
        }

        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            colors = SliderDefaults.colors(
                thumbColor = Healthy,
                activeTrackColor = Healthy,
                inactiveTrackColor = BorderColor
            )
        )
    }
}

@Composable
private fun DualRiskComparisonCanvas(
    timeline: List<TimelinePoint>,
    baselineRisk: Float,
    residualRisk: Float,
    interventionT: Float,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Bg0, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val w = size.width
        val h = size.height

        // Subtle background grid lines
        val gridLevels = listOf(0.25f, 0.50f, 0.75f)
        for (lvl in gridLevels) {
            val y = h * (1f - lvl)
            drawLine(
                color = BorderSubtle.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // 1. Draw Baseline trajectory (unmitigated attack escalation in faded red)
        val baselinePath = Path().apply {
            moveTo(0f, h * (1f - baselineRisk * 0.85f))
            cubicTo(
                w * 0.35f, h * (1f - baselineRisk * 0.90f),
                w * 0.70f, h * (1f - baselineRisk * 0.96f),
                w, h * (1f - baselineRisk)
            )
        }
        drawPath(
            path = baselinePath,
            color = Critical.copy(alpha = 0.35f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. Draw Progressive Mitigated trajectory based on model timeline
        if (timeline.isNotEmpty()) {
            val visiblePoints = timeline.filter { it.t <= progress }
            if (visiblePoints.isNotEmpty()) {
                val mitigatedPath = Path()
                val fillPath = Path()

                var first = true
                var lastX = 0f
                var lastY = 0f

                for (pt in visiblePoints) {
                    val px = pt.t * w
                    val py = h * (1f - pt.risk.coerceIn(0.05f, 0.98f))
                    if (first) {
                        mitigatedPath.moveTo(px, py)
                        fillPath.moveTo(px, h)
                        fillPath.lineTo(px, py)
                        first = false
                    } else {
                        mitigatedPath.lineTo(px, py)
                        fillPath.lineTo(px, py)
                    }
                    lastX = px
                    lastY = py
                }

                // Close fill path
                fillPath.lineTo(lastX, h)
                fillPath.close()

                // Gradient fill underneath
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        (if (progress >= interventionT) Healthy else Critical).copy(alpha = 0.28f),
                        Color.Transparent
                    )
                )
                drawPath(path = fillPath, brush = fillBrush)

                // Progressive curve stroke
                val curveColor = if (progress >= interventionT) Healthy else Critical
                drawPath(
                    path = mitigatedPath,
                    color = curveColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Glowing marker dot at leading head
                drawCircle(
                    color = curveColor.copy(alpha = 0.35f),
                    radius = 7.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = curveColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
            }
        } else {
            // Fallback smooth curve
            val currRisk = if (progress < interventionT) baselineRisk else residualRisk
            val targetY = h * (1f - currRisk)
            drawLine(
                color = Healthy,
                start = Offset(0f, h * (1f - baselineRisk)),
                end = Offset(w * progress, targetY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Vertical Intervention marker line at interventionT
        val markerX = w * interventionT
        drawLine(
            color = Info,
            start = Offset(markerX, 0f),
            end = Offset(markerX, h),
            strokeWidth = 1.5.dp.toPx()
        )
        // Indicator dot on intervention line
        drawCircle(
            color = Info,
            radius = 3.dp.toPx(),
            center = Offset(markerX, 6.dp.toPx())
        )
    }
}

@Composable
private fun MiniTopologyAttackPath(
    progress: Float,
    interventionT: Float,
    targetAsset: String,
    actionType: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    Canvas(
        modifier = modifier
            .background(Bg0, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val w = size.width
        val h = size.height
        val centerY = h * 0.45f

        val node1X = w * 0.12f // Threat Origin
        val node2X = w * 0.50f // Ingress Gateway / Intervention Point
        val node3X = w * 0.88f // Target Protected Asset

        // Draw connecting link lines
        drawLine(
            color = BorderColor,
            start = Offset(node1X, centerY),
            end = Offset(node2X, centerY),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = if (progress >= interventionT) Healthy.copy(alpha = 0.4f) else BorderColor,
            start = Offset(node2X, centerY),
            end = Offset(node3X, centerY),
            strokeWidth = 2.dp.toPx()
        )

        // Draw Nodes
        // Node 1: Origin
        drawCircle(color = Surface2, radius = 9.dp.toPx(), center = Offset(node1X, centerY))
        drawCircle(color = Critical, radius = 5.dp.toPx(), center = Offset(node1X, centerY))

        // Node 2: Ingress Gateway / Defense Hook
        val node2Color = if (progress >= interventionT) Healthy else Info
        drawCircle(color = Surface2, radius = 10.dp.toPx(), center = Offset(node2X, centerY))
        drawCircle(color = node2Color, radius = 6.dp.toPx(), center = Offset(node2X, centerY))

        // Shield Ripple Effect at Node 2 if intervention active
        if (progress >= interventionT) {
            drawCircle(
                color = Healthy.copy(alpha = rippleAlpha),
                radius = 12.dp.toPx() * rippleScale,
                center = Offset(node2X, centerY),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Node 3: Target Asset
        val node3Color = if (progress >= interventionT) Healthy else Warning
        drawCircle(color = Surface2, radius = 9.dp.toPx(), center = Offset(node3X, centerY))
        drawCircle(color = node3Color, radius = 5.dp.toPx(), center = Offset(node3X, centerY))

        // Moving Threat Particle Dot
        val particleX: Float
        val particleColor: Color
        if (progress < interventionT) {
            // Threat travels from Node 1 to Node 2
            val ratio = progress / interventionT
            particleX = node1X + (node2X - node1X) * ratio
            particleColor = Critical
        } else {
            // Stopped and contained at Node 2
            particleX = node2X
            particleColor = Healthy
        }

        drawCircle(
            color = particleColor.copy(alpha = 0.4f),
            radius = 6.dp.toPx(),
            center = Offset(particleX, centerY)
        )
        drawCircle(
            color = particleColor,
            radius = 3.5.dp.toPx(),
            center = Offset(particleX, centerY)
        )
    }
}

@Composable
private fun AnimatedRiskSummaryRow(
    res: SimulationResult,
    progress: Float
) {
    // Current interpolated risk score based on progress
    val currentRisk = if (res.timeline.isNotEmpty()) {
        val idx = ((res.timeline.size - 1) * progress).toInt().coerceIn(0, res.timeline.size - 1)
        res.timeline[idx].risk
    } else {
        if (progress < res.interventionT) {
            res.baselineRisk
        } else {
            val ratio = (progress - res.interventionT) / (1f - res.interventionT)
            res.baselineRisk + (res.residualRisk - res.baselineRisk) * ratio
        }
    }

    val riskColor = when {
        currentRisk > 0.65f -> Critical
        currentRisk > 0.35f -> Warning
        else -> Healthy
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Bg0)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radial Risk Arc Gauge
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 4.dp.toPx()
                // Track
                drawArc(
                    color = BorderColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(strokeW, cap = StrokeCap.Round)
                )
                // Active Sweep
                drawArc(
                    color = riskColor,
                    startAngle = -90f,
                    sweepAngle = 360f * currentRisk.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(strokeW, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "${(currentRisk * 100).toInt()}%",
                style = TechnicalValue.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = riskColor)
            )
        }

        // Before (Baseline)
        Column {
            Text(text = "BEFORE", style = MetadataText.copy(fontSize = 10.sp))
            Text(
                text = "${(res.baselineRisk * 100).toInt()}%",
                style = TechnicalValue.copy(fontSize = 18.sp, color = Critical, fontWeight = FontWeight.Bold)
            )
        }

        // Reduction Delta
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "REDUCTION", style = MetadataText.copy(fontSize = 10.sp))
            Text(
                text = "-${res.riskReductionPct}%",
                style = TechnicalValue.copy(fontSize = 18.sp, color = Healthy, fontWeight = FontWeight.Bold)
            )
        }

        // After (Residual)
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "AFTER", style = MetadataText.copy(fontSize = 10.sp))
            Text(
                text = "${(res.residualRisk * 100).toInt()}%",
                style = TechnicalValue.copy(fontSize = 18.sp, color = Healthy, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun LiveNarrativeFeed(
    narrative: List<NarrativeEvent>,
    progress: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE INCIDENT TIMELINE FEED",
                style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
            )
            Text(
                text = "TIMED TO REPLAY",
                style = MetadataText.copy(fontSize = 9.sp, color = TextMuted)
            )
        }

        val visibleItems = narrative.filter { it.t <= progress }
        if (visibleItems.isEmpty()) {
            Text(
                text = "Awaiting replay clock trigger...",
                style = MetadataText.copy(fontSize = 11.sp, color = TextMuted)
            )
        } else {
            visibleItems.forEach { item ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Bg0)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "T+%.1fs", item.t * 3.0f),
                            style = TechnicalValue.copy(
                                fontSize = 10.sp,
                                color = if (item.t >= 0.35f) Healthy else Info,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        item.mitre?.let { mitreCode ->
                            Box(
                                modifier = Modifier
                                    .background(WarningBg, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = mitreCode,
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = Warning)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = item.text,
                            style = MetadataText.copy(fontSize = 11.sp, color = TextPrimary),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhyThisOutcomePanel(
    factors: List<FactorAttribution>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Bg0)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Info,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WHY THIS OUTCOME? (SHAP ATTRIBUTION)",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TextSecondary,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (isExpanded) 90f else 0f)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (factors.isEmpty()) {
                    Text(
                        text = "Standard latent mitigation parameters applied.",
                        style = MetadataText.copy(fontSize = 11.sp, color = TextMuted)
                    )
                } else {
                    factors.forEach { factor ->
                        val isNegative = factor.impact < 0
                        val impactText = String.format(Locale.US, "%+d%% RISK", (factor.impact * 100).toInt())
                        val impactColor = if (isNegative) Healthy else Warning

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface0)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = factor.feature.replace("_", " "),
                                    style = TechnicalValue.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = factor.description,
                                    style = MetadataText.copy(fontSize = 10.sp, color = TextSecondary)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(if (isNegative) HealthyBg else WarningBg, RoundedCornerShape(4.dp))
                                    .border(1.dp, if (isNegative) HealthyBorder else WarningBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = impactText,
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = impactColor, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
