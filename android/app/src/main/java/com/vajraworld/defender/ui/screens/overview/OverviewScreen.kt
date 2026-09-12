package com.vajraworld.defender.ui.screens.overview

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.engine.ScannedAppReport
import com.vajraworld.defender.ui.components.*
import com.vajraworld.defender.ui.theme.*
import java.util.Locale

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onNavigateToSimulation: () -> Unit,
    onNavigateToRadar: () -> Unit = {},
    onNavigateToLinkScan: () -> Unit = {},
    onNavigateToFileScan: () -> Unit = {},
    onNavigateToClipboard: () -> Unit = {},
    onNavigateToExplainability: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToTrajectory: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToIncidents: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenSurfacesHub: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val infiniteTransition = rememberInfiniteTransition(label = "OverviewFlowPulse")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

    // Forensic Report Dialog
    if (state.generatedReport != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReport() },
            containerColor = Bg0,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FORENSIC SECURITY REPORT",
                        style = TechnicalValue.copy(fontSize = 13.sp, color = Info, fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { viewModel.dismissReport() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Surface0, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = state.generatedReport!!,
                        style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = com.vajraworld.defender.domain.engine.SecurityReportGenerator.exportAndShareReport(
                            context,
                            state.generatedReport!!
                        )
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Info),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Bg0, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "SHARE / EXPORT", style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.generatedReport!!))
                        android.widget.Toast.makeText(context, "Report copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(text = "COPY", style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        // Cockpit Top Bar (Section 9 & 50)
        VajraTopBar(
            title = "VAJRAWORLD",
            subtitle = "GUARDIAN COCKPIT",
            isSynthetic = state.isSynthetic,
            onHubClick = onOpenSurfacesHub
        )

        // Hero Branding Header Card (Issue 1 & 8)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Surface0)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.vajraworld.defender.R.drawable.vajra_logo),
                        contentDescription = "VajraWorld Logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Info.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "VAJRAWORLD GUARDIAN",
                                style = TechnicalValue.copy(fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Black)
                            )
                            Box(
                                modifier = Modifier
                                    .background(HealthyBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(text = "LIVE SHIELD", style = TechnicalValue.copy(fontSize = 8.sp, color = Healthy))
                            }
                        }
                        Text(
                            text = "Autonomous Physical Mobile Cyber Defense Engine",
                            style = MetadataText.copy(fontSize = 9.5.sp, color = TextSecondary)
                        )
                        Text(
                            text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
                            style = TechnicalValue.copy(fontSize = 9.5.sp, color = Info)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateReport(context) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Info)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Bg0, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isGeneratingReport) "GENERATING..." else "FORENSIC REPORT",
                            style = TechnicalValue.copy(fontSize = 10.5.sp, color = Bg0, fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = { com.vajraworld.defender.service.VajraNotificationManager.sendTestNotification(context) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TEST ALERT",
                            style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Real-Time Screen Sharing / Recording Warning Alert
        if (state.screenShareStatus?.isScreenSharingOrRecording == true) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.dp, CriticalBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CriticalBg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ScreenShare, contentDescription = "Screen Share Warning", tint = Critical, modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = "SCREEN SHARING / RECORDING ACTIVE",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Critical, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = state.screenShareStatus?.details ?: "External display or screen capture service active.",
                            style = MetadataText.copy(fontSize = 10.sp, color = TextPrimary)
                        )
                    }
                }
            }
        }

        // Real-Time Active Voice Call Warning Alert
        if (state.callSecurityStatus?.isCallActive == true && state.callSecurityStatus?.riskWarning != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.dp, Warning.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = "Active Call Warning", tint = Warning, modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = "ACTIVE VOICE CALL IN PROGRESS",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Warning, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = state.callSecurityStatus?.riskWarning ?: "Beware of urgent verification codes over voice calls.",
                            style = MetadataText.copy(fontSize = 10.sp, color = TextPrimary)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Flow Status Bar
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .alpha(liveDotAlpha)
                            .background(if (state.isLiveConnected) Healthy else Warning)
                    )
                    Text(
                        text = "${state.activeFlowsCount} ACTIVE FLOWS",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                    )
                    Text(
                        text = "• ${String.format(java.util.Locale.US, "%.1f", state.eventsPerSec)} evt/s",
                        style = MetadataText
                    )
                }

                SecurityStatusPill(riskScore = state.forecastRisk)
            }

            // Real On-Device Hardware & Deep Threat Scanner
            RealDeviceScannerCard(
                state = state,
                onStartScan = { viewModel.startDeviceScan() }
            )

            // Hero Security State Orb (Section 10)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURITY WORLD STATE",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                        )
                        Text(
                            text = "COVERAGE: ${state.coverage}",
                            style = MetadataText
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SecurityStatusOrb(
                        currentRisk = state.forecastRisk / 100f,
                        forecastRisk = (state.forecastRisk + 12).coerceAtMost(100) / 100f,
                        uncertainty = state.uncertainty,
                        size = 180.dp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time threat landscape continuously evaluated by World Model",
                        style = MetadataText,
                        fontSize = 11.sp
                    )
                }
            }

            // Live Risk Graph (Section 11)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEMPORAL RISK TRAJECTORY",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                    )
                    Text(
                        text = "OBSERVED → FORECAST",
                        style = MetadataText.copy(color = Info)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LiveRiskGraph(
                    observedPoints = state.observedHistory,
                    forecastPoints = state.forecastTrajectory,
                    uncertainty = state.uncertainty,
                    height = 130.dp
                )
            }

            // Correlated Real Threat Story Card
            val dynamicStoryTitle = when {
                state.scannedAudit?.highRiskApps?.isNotEmpty() == true ->
                    "High-Risk App: ${state.scannedAudit!!.highRiskApps.first().appName}"
                state.deviceTelemetry?.integrity?.isRooted == true ->
                    "Root Binary Compromise Detected"
                state.deviceTelemetry?.integrity?.isAdbEnabled == true ->
                    "USB Debugging Active (Bridge Exposed)"
                state.forecastRisk > 30 ->
                    "Elevated System Vulnerability Detected"
                else ->
                    "On-Device Real-Time Surveillance"
            }
            val dynamicNextStage = when {
                state.scannedAudit?.highRiskApps?.isNotEmpty() == true ->
                    "Permission Misuse -> Sensitive Data Vector"
                state.deviceTelemetry?.integrity?.isRooted == true ->
                    "Kernel Access -> Defense Evasion"
                state.forecastRisk > 30 ->
                    "Continuous Sandbox Monitoring"
                else ->
                    "Nominal Posture Maintained"
            }
            val dynamicCorrelatedCount = state.scannedAudit?.let { it.highRiskApps.size + it.mediumRiskApps.size } ?: 0

            ThreatStoryCard(
                title = dynamicStoryTitle,
                stage = state.predictedStage,
                correlatedEventsCount = dynamicCorrelatedCount,
                predictedNextStage = dynamicNextStage,
                riskScore = state.forecastRisk,
                onClick = onNavigateToRadar,
                onSimulate = onNavigateToSimulation
            )

            // Predictive Horizon & Critical Asset
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
                        Text(
                            text = "PREDICTED ATTACK HORIZON",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                        )
                        Box(
                            modifier = Modifier
                                .background(CriticalBg, RoundedCornerShape(4.dp))
                                .border(1.dp, CriticalBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ETA ~${state.etaSeconds}s",
                                style = TechnicalValue.copy(fontSize = 10.sp, color = Critical)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        state.horizonBars.forEachIndexed { idx, barVal ->
                            val barHeight = (barVal * 42).dp.coerceAtLeast(6.dp)
                            val barColor = if (barVal > 0.6f) Critical else if (barVal > 0.35f) Warning else Healthy
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(barVal * 100).toInt()}%",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = barColor)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${(idx + 1) * 30}s",
                                    style = MetadataText.copy(fontSize = 9.sp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "CURRENT STAGE", style = MetadataText)
                            Text(
                                text = state.predictedStage,
                                style = TechnicalValue.copy(fontSize = 12.sp, color = Warning)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "AT RISK ASSET", style = MetadataText)
                            Text(
                                text = state.criticalAsset,
                                style = TechnicalValue.copy(fontSize = 12.sp, color = Info)
                            )
                        }
                    }
                }
            }

            // OTP Privacy Vault Guarantee Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HealthyBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = HealthyBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OTP PRIVACY VAULT GUARANTEE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Healthy
                        )
                        Text(
                            text = "Zero Plaintext OTPs or Raw Messages Stored (Verified In-Flight SHA-256)",
                            style = MetadataText.copy(color = TextPrimary)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Healthy, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "VERIFIED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Bg0
                        )
                    }
                }
            }

            // Comprehensive Cockpit Defence Surfaces Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GUARDIAN DEFENCE SURFACES & MODULES",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                )
                Text(
                    text = "12 ACTIVE SURFACES",
                    style = MetadataText.copy(color = Info)
                )
            }

            val surfaces = listOf(
                Pair(
                    "RADAR DEFENCE",
                    Triple("4-Ring Spatial Scope", Icons.Default.Radar, Info)
                ) to onNavigateToRadar,
                Pair(
                    "LINK SCANNER",
                    Triple("Entropy & Deception", Icons.Default.Link, AccentCyan)
                ) to onNavigateToLinkScan,
                Pair(
                    "FILE / APK",
                    Triple("SAF Stream & Perms", Icons.Default.Folder, PurpleAccent)
                ) to onNavigateToFileScan,
                Pair(
                    "CLIPBOARD VAULT",
                    Triple("0-Retention Secrets", Icons.Default.ContentPaste, Healthy)
                ) to onNavigateToClipboard,
                Pair(
                    "PERMISSIONS",
                    Triple("AppOps Op Auditor", Icons.Default.AppRegistration, Warning)
                ) to onNavigateToPermissions,
                Pair(
                    "SETTINGS",
                    Triple("Live Security Controls", Icons.Default.Tune, Info)
                ) to onNavigateToSettings,
                Pair(
                    "EXPLAINABILITY",
                    Triple("SHAP Feature Attrib", Icons.Default.Info, Info)
                ) to onNavigateToExplainability,
                Pair(
                    "MODEL HEALTH",
                    Triple("Dynamic Benchmark", Icons.Default.Settings, Warning)
                ) to onNavigateToHealth,
                Pair(
                    "TRAJECTORY",
                    Triple("K-Step ATT&CK Futures", Icons.Default.Timeline, Info)
                ) to onNavigateToTrajectory,
                Pair(
                    "NETWORK SOC",
                    Triple("Host Topology Graph", Icons.Default.Hub, AccentCyan)
                ) to onNavigateToNetwork,
                Pair(
                    "INCIDENTS",
                    Triple("ATT&CK Detections", Icons.Default.Warning, Critical)
                ) to onNavigateToIncidents,
                Pair(
                    "SIMULATOR",
                    Triple("Counterfactual Replay", Icons.Default.Security, Healthy)
                ) to onNavigateToSimulation
            )

            // Render in 2-column rows
            for (i in surfaces.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val item1 = surfaces[i]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface0)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable { item1.second() }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item1.first.second.second,
                                    contentDescription = item1.first.first,
                                    tint = item1.first.second.third,
                                    modifier = Modifier.size(18.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Surface2, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "READY",
                                        style = TechnicalValue.copy(fontSize = 8.sp, color = item1.first.second.third)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item1.first.first,
                                style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = item1.first.second.first,
                                style = MetadataText.copy(fontSize = 10.sp),
                                maxLines = 1
                            )
                        }
                    }

                    if (i + 1 < surfaces.size) {
                        val item2 = surfaces[i + 1]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface0)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable { item2.second() }
                            .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item2.first.second.second,
                                        contentDescription = item2.first.first,
                                        tint = item2.first.second.third,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Surface2, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "READY",
                                            style = TechnicalValue.copy(fontSize = 8.sp, color = item2.first.second.third)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item2.first.first,
                                    style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = item2.first.second.first,
                                    style = MetadataText.copy(fontSize = 10.sp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Counterfactual Defence Simulator Action Button
            Button(
                onClick = onNavigateToSimulation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Info)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Simulate",
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUN COUNTERFACTUAL DEFENCE SIMULATION",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun RealDeviceScannerCard(
    state: OverviewUiState,
    onStartScan: () -> Unit
) {
    var expandedAppsList by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Surface0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Real Hardware Identification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Physical Device",
                        tint = Info,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "${Build.MANUFACTURER.uppercase(Locale.US)} ${Build.MODEL}",
                            style = TechnicalValue.copy(fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • Live Sensor Node",
                            style = MetadataText.copy(fontSize = 10.sp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(Surface2, RoundedCornerShape(6.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.isScanning) "SCANNING..." else "REAL DEVICE",
                        style = TechnicalValue.copy(
                            fontSize = 9.sp,
                            color = if (state.isScanning) Warning else Healthy
                        )
                    )
                }
            }

            // Real Hardware Telemetry Grid (RAM, Storage, Battery, Wi-Fi)
            val telemetry = state.deviceTelemetry
            if (telemetry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // RAM
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Surface1, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(text = "RAM", style = MetadataText.copy(fontSize = 9.sp))
                            Text(
                                text = "${telemetry.hardware.ramUsagePct}%",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                            )
                        }
                    }

                    // Storage
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Surface1, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(text = "FREE DISK", style = MetadataText.copy(fontSize = 9.sp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", telemetry.hardware.freeStorageGb)}GB",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                            )
                        }
                    }

                    // Battery
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Surface1, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(text = "BATTERY", style = MetadataText.copy(fontSize = 9.sp))
                            Text(
                                text = "${String.format(Locale.US, "%.0f", telemetry.hardware.batteryTemperatureC)}°C",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                            )
                        }
                    }

                    // Network
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Surface1, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(text = "NETWORK", style = MetadataText.copy(fontSize = 9.sp))
                            Text(
                                text = (telemetry.network.wifiSsid ?: telemetry.network.activeTransport).take(8),
                                style = TechnicalValue.copy(fontSize = 11.sp, color = Info),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Integrity Status Chips (Root, Lock Screen, USB Debugging)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rootText = if (telemetry.integrity.isRooted) "ROOT: DETECTED" else "ROOT: CLEAN"
                    val rootColor = if (telemetry.integrity.isRooted) Critical else Healthy
                    Box(
                        modifier = Modifier
                            .background(rootColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .border(1.dp, rootColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(text = rootText, style = TechnicalValue.copy(fontSize = 9.sp, color = rootColor))
                    }

                    val lockText = if (telemetry.integrity.isDeviceSecure) "LOCK: ENCRYPTED" else "LOCK: UNSECURED"
                    val lockColor = if (telemetry.integrity.isDeviceSecure) Healthy else Warning
                    Box(
                        modifier = Modifier
                            .background(lockColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .border(1.dp, lockColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(text = lockText, style = TechnicalValue.copy(fontSize = 9.sp, color = lockColor))
                    }

                    val adbText = if (telemetry.integrity.isAdbEnabled) "ADB: ACTIVE" else "ADB: SECURED"
                    val adbColor = if (telemetry.integrity.isAdbEnabled) Warning else Healthy
                    Box(
                        modifier = Modifier
                            .background(adbColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .border(1.dp, adbColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(text = adbText, style = TechnicalValue.copy(fontSize = 9.sp, color = adbColor))
                    }
                }
            }

            // Scanning progress or Scan Button
            if (state.isScanning) {
                val scanProg = state.scanProgress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface1, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = scanProg?.phase ?: "System Audit",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${scanProg?.progressPct ?: 0}%",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (scanProg?.progressPct ?: 0) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Info,
                        trackColor = Surface2
                    )

                    Text(
                        text = scanProg?.currentTarget ?: "Auditing device sensors...",
                        style = MetadataText.copy(fontSize = 10.sp),
                        maxLines = 1
                    )
                }
            } else {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Info)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Scan",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.scanProgress?.isComplete == true) "RE-SCAN PHYSICAL DEVICE" else "START FULL DEVICE SECURITY SCAN",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Scan Results Summary & Expandable App List
            val audit = state.scannedAudit
            if (audit != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (audit.highRiskApps.isEmpty()) HealthyBg else CriticalBg)
                        .border(1.dp, if (audit.highRiskApps.isEmpty()) HealthyBorder else CriticalBorder, RoundedCornerShape(8.dp))
                        .clickable { expandedAppsList = !expandedAppsList }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${audit.userAppsCount} USER APPS AUDITED • ${audit.highRiskApps.size} HIGH RISK",
                            style = TechnicalValue.copy(
                                fontSize = 11.sp,
                                color = if (audit.highRiskApps.isEmpty()) Healthy else Critical,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (expandedAppsList) "Tap to collapse installed packages" else "Tap to inspect all installed packages & permissions",
                            style = MetadataText.copy(fontSize = 10.sp)
                        )
                    }

                    Icon(
                        imageVector = if (expandedAppsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = if (audit.highRiskApps.isEmpty()) Healthy else Critical
                    )
                }

                AnimatedVisibility(visible = expandedAppsList) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allApps = audit.highRiskApps + audit.mediumRiskApps + audit.safeApps
                        allApps.take(25).forEach { app ->
                            AppAuditRowItem(app)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppAuditRowItem(app: ScannedAppReport) {
    val badgeColor = when (app.riskLevel) {
        "CRITICAL" -> Critical
        "HIGH" -> Warning
        "ELEVATED" -> Warning
        else -> Healthy
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Surface1)
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${app.riskLevel} (${app.riskScore})",
                        style = TechnicalValue.copy(fontSize = 8.sp, color = badgeColor)
                    )
                }
            }

            Text(
                text = app.packageName,
                style = MetadataText.copy(fontSize = 9.sp),
                maxLines = 1
            )

            if (app.riskReasons.isNotEmpty()) {
                Text(
                    text = "Signals: " + app.riskReasons.joinToString("; "),
                    style = MetadataText.copy(fontSize = 9.sp, color = Warning),
                    maxLines = 2
                )
            }
        }
    }
}
