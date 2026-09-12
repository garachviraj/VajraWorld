package com.vajraworld.defender.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Bg1
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Bg1)
                .verticalScroll(scrollState)
        ) {
            VajraTopBar(
                title = "SETTINGS & GUARDS",
                subtitle = "LIVE CYBER-DEFENSE CONTROLS",
                onBack = onBack,
                onHubClick = onHubClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                                // SECTION 0: 24/7 BACKGROUND SENTINEL & STORAGE WATCHDOG
                Text(
                    text = "24/7 BACKGROUND SENTINEL & STORAGE WATCHDOG",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (state.backgroundSentinelEnabled) InfoBorder else BorderColor,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Master Sentinel On/Off Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (state.backgroundSentinelEnabled) Info.copy(alpha = 0.15f) else Critical.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (state.backgroundSentinelEnabled) Info else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "24/7 Background Sentinel",
                                            style = Typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (state.backgroundSentinelEnabled) HealthyBg else WarningBg)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (state.backgroundSentinelEnabled) "ACTIVE" else "PAUSED",
                                                style = TechnicalValue.copy(
                                                    fontSize = 8.5.sp,
                                                    color = if (state.backgroundSentinelEnabled) Healthy else Warning,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Download watchdog, app install interceptor & screen cast shield",
                                        style = MetadataText.copy(fontSize = 9.sp)
                                    )
                                }
                            }
                            Switch(
                                checked = state.backgroundSentinelEnabled,
                                onCheckedChange = { viewModel.toggleBackgroundSentinel(context, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Bg0,
                                    checkedTrackColor = Info,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = Surface2
                                )
                            )
                        }

                        HorizontalDivider(color = BorderSubtle)

                        // Master Scan Trigger Button & Live Metrics
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ON-DEMAND MASTER STORAGE & APP SCAN",
                                style = TechnicalValue.copy(fontSize = 10.5.sp, color = Info, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Recursively audits all storage directories (Downloads, Documents, DCIM, WhatsApp, SD) & APK manifests with 16-byte magic header dissection.",
                                style = MetadataText.copy(fontSize = 9.sp)
                            )

                            Button(
                                onClick = { viewModel.runMasterStorageScan(context) },
                                enabled = !state.isMasterScanning,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isMasterScanning) Surface2 else Info,
                                    disabledContainerColor = Surface2
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (state.isMasterScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Info
                                        )
                                        Text(
                                            text = "SCANNING IN PROGRESS...",
                                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Bg0,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "RUN FULL MASTER STORAGE SCAN NOW",
                                            style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            state.masterScanProgress?.let { prog ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Surface1)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (prog.isComplete) "SCAN COMPLETE • NOTIFICATION DISPATCHED" else "SCANNING RECURSIVE STORAGE...",
                                            style = TechnicalValue.copy(
                                                fontSize = 9.5.sp,
                                                color = if (prog.isComplete) Healthy else Info,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "${prog.percent}%",
                                            style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { prog.percent / 100f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = if (prog.threatsFound > 0) Critical else Healthy,
                                        trackColor = Surface2
                                    )

                                    Text(
                                        text = prog.currentPath,
                                        style = MetadataText.copy(fontSize = 8.5.sp, color = TextSecondary),
                                        maxLines = 1
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Audited: ${prog.totalFilesAudited + prog.totalAppsAudited} items",
                                            style = MetadataText.copy(fontSize = 8.5.sp, color = TextSecondary)
                                        )
                                        Text(
                                            text = "Clean: ${prog.cleanFilesCount} | Threats: ${prog.threatsFound}",
                                            style = TechnicalValue.copy(
                                                fontSize = 8.5.sp,
                                                color = if (prog.threatsFound > 0) Critical else Healthy,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)

                        // Architectural Breakdown: How Our Background Defense Operates
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Info, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "HOW OUR BACKGROUND DEFENSE OPERATES",
                                    style = TechnicalValue.copy(fontSize = 10.5.sp, color = Info, fontWeight = FontWeight.Bold)
                                )
                            }

                            BackgroundMechanismItem(
                                title = "1. Recursive Storage & Download Watchdog",
                                desc = "Monitors Downloads, Documents, DCIM, WhatsApp, and media volumes with real-time FileObserver hooks. Dissects 16-byte magic headers to catch disguised ELF/DEX binaries (.jpg/.pdf), deceptive double extensions (.pdf.apk), and ransomware encryption signatures (.locked, .crypto)."
                            )

                            BackgroundMechanismItem(
                                title = "2. Autonomous App Install Interceptor",
                                desc = "AppInstallReceiver catches PACKAGE_ADDED events the moment any APK is installed, decompresses the AndroidManifest, audits for toxic permissions (Accessibility + Screen Overlay banking trojans), and alerts you immediately."
                            )

                            BackgroundMechanismItem(
                                title = "3. Zero-Lapse OTP & Screen Share Privacy Shield",
                                desc = "Monitors display subsystem states. During Zoom, Teams, AnyDesk, or Google Cast streaming, Vajra Guardian automatically suppresses incoming OTP SMS notifications to prevent remote screen watchers from viewing your 2FA codes."
                            )

                            BackgroundMechanismItem(
                                title = "4. Automated Forensic Notifications",
                                desc = "Upon finishing full storage scans or detecting anomalous file headers, Vajra pushes high-priority forensic breakdown notifications with clean vs threat tallies and actionable guidance directly to your status bar."
                            )

                            BackgroundMechanismItem(
                                title = "5. 100% Offline Edge Architecture",
                                desc = "Zero file contents, photos, or personal data ever leave your device. All magic byte decoders, SHA-256 calculators, and heuristic engines execute entirely on local CPU."
                            )
                        }
                    }
                }

// SECTION 1: App Storage & Cache Footprint (Item 6)
                Text(
                    text = "APPLICATION STORAGE & DISK FOOTPRINT",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

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
                                Text(text = "SPACE ACQUIRED BY VAJRA DEFENDER", style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold))
                                Text(text = "Real-time calculation of APK, SQLite DB & cache buffers", style = MetadataText.copy(fontSize = 9.sp))
                            }
                            Text(
                                text = "${String.format("%.2f", state.totalAppSizeMb)} MB",
                                style = TechnicalValue.copy(fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(8.dp))

                        TechnicalMetadataRow(label = "Application Binary (APK)", value = "${String.format("%.2f", state.appCodeSizeMb)} MB")
                        TechnicalMetadataRow(label = "Security Room DB & Logs", value = "${String.format("%.2f", state.userDataSizeMb)} MB")
                        TechnicalMetadataRow(label = "Temporary Analysis Cache", value = "${String.format("%.2f", state.cacheSizeMb)} MB")

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.clearAppCache(context) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Info, modifier = Modifier.size(15.dp))
                                    Text(text = "CLEAR CACHE", style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary))
                                }
                            }

                            Button(
                                onClick = { viewModel.clearAllScansAndEvents(context) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CriticalBg)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Critical, modifier = Modifier.size(15.dp))
                                    Text(text = "RESET AUDIT DATA", style = TechnicalValue.copy(fontSize = 10.sp, color = Critical))
                                }
                            }
                        }
                    }
                }

                // SECTION 2: Screen Sharing & Cast Shield (Item 15)
                Text(
                    text = "SCREEN SHARING & CAST SHIELD",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (state.isScreenSharingActive) CriticalBorder else InfoBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isScreenSharingActive) Critical else Healthy)
                                )
                                Text(
                                    text = if (state.isScreenSharingActive) "SCREEN SHARING / CAST ACTIVE!" else "NO SCREEN SHARING DETECTED",
                                    style = TechnicalValue.copy(
                                        fontSize = 11.sp,
                                        color = if (state.isScreenSharingActive) Critical else Healthy,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Text(
                            text = "WHAT IS THIS FOR? During Zoom, Google Meet, AnyDesk, or Google Cast sessions, incoming SMS or 2FA push notifications display sensitive OTPs on screen to all remote spectators. This shield automatically suppresses OTP notifications while sharing is active.",
                            style = MetadataText.copy(fontSize = 9.5.sp, color = TextSecondary, lineHeight = 14.sp)
                        )

                        HorizontalDivider(color = BorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Mask OTP Notifications During Cast", style = Typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                                Text(text = "Suppresses incoming 2FA verification codes from the screen", style = MetadataText.copy(fontSize = 9.sp))
                            }
                            Switch(
                                checked = state.maskOtpDuringScreenShare,
                                onCheckedChange = { viewModel.toggleMaskOtpScreenShare(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Bg0, checkedTrackColor = Info)
                            )
                        }
                    }
                }

                // SECTION 3: Log Retention Policy (Item 5)
                Text(
                    text = "SECURITY LOG RETENTION POLICY",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "AUTOMATIC LOG PRUNING SCHEDULE", style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextSecondary))
                        Text(text = "Determines how many days of network packets, URL scans, and clipboard audits are preserved in local storage.", style = MetadataText.copy(fontSize = 9.sp))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1 to "1 DAY (Default)", 7 to "7 DAYS", 30 to "30 DAYS").forEach { (days, label) ->
                                val isSel = state.logRetentionDays == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Info else Surface1)
                                        .border(1.dp, if (isSel) Info else BorderColor, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setLogRetentionPolicy(context, days) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = TechnicalValue.copy(fontSize = 9.sp, color = if (isSel) Bg0 else TextPrimary, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 4: Real-Time Protection Guards (Item 14)
                Text(
                    text = "REAL-TIME PROTECTION GUARDS",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GuardToggleRow(
                            title = "New App Install Monitor",
                            subtitle = "Autonomously audits newly installed APKs for toxic permissions",
                            isEnabled = state.installGuardEnabled,
                            onToggle = { viewModel.toggleInstallGuard(it) },
                            icon = Icons.Default.InstallMobile,
                            accentColor = Info
                        )
                        HorizontalDivider(color = BorderSubtle)
                        GuardToggleRow(
                            title = "Screen Sharing & Cast Shield",
                            subtitle = "Monitors virtual displays and unauthorized screen recording",
                            isEnabled = state.screenShareShieldEnabled,
                            onToggle = { viewModel.toggleScreenShareShield(it) },
                            icon = Icons.AutoMirrored.Filled.ScreenShare,
                            accentColor = Critical
                        )
                        HorizontalDivider(color = BorderSubtle)
                        GuardToggleRow(
                            title = "Telephony Call Fraud Alert",
                            subtitle = "Protects against vishing and OTP social engineering during calls",
                            isEnabled = state.callGuardEnabled,
                            onToggle = { viewModel.toggleCallGuard(it) },
                            icon = Icons.Default.PhoneInTalk,
                            accentColor = Warning
                        )
                        HorizontalDivider(color = BorderSubtle)
                        GuardToggleRow(
                            title = "Clipboard Secret Shield",
                            subtitle = "Detects keys, credentials & seed phrases with 0-plaintext storage",
                            isEnabled = state.clipboardGuardEnabled,
                            onToggle = { viewModel.toggleClipboardGuard(it) },
                            icon = Icons.Default.ContentPaste,
                            accentColor = Healthy
                        )
                        HorizontalDivider(color = BorderSubtle)
                        GuardToggleRow(
                            title = "Phishing & Deceptive Link Interceptor",
                            subtitle = "Shannon entropy and normalized Levenshtein brand similarity",
                            isEnabled = state.linkGuardEnabled,
                            onToggle = { viewModel.toggleLinkGuard(it) },
                            icon = Icons.Default.Link,
                            accentColor = Info
                        )
                    }
                }

                // SECTION 5: Android System Permissions & Accessibility Shortcuts
                Text(
                    text = "SYSTEM SECURITY SHORTCUTS",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemPermissionRow(
                            title = "Browser Phishing Guard (Accessibility)",
                            subtitle = "Required to inspect browser URL address bars in real time",
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        )
                        HorizontalDivider(color = BorderSubtle)
                        SystemPermissionRow(
                            title = "Notification Privacy Listener",
                            subtitle = "Required to detect SMS lures and shield OTPs during screen sharing",
                            onClick = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HorizontalDivider(color = BorderSubtle)
                            SystemPermissionRow(
                                title = "All Files Storage Access",
                                subtitle = "Required for deep recursive external storage auditing",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuardToggleRow(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = title, style = Typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                Text(text = subtitle, style = MetadataText.copy(fontSize = 9.sp))
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Bg0, checkedTrackColor = accentColor)
        )
    }
}

@Composable
fun SystemPermissionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold))
            Text(text = subtitle, style = MetadataText.copy(fontSize = 9.sp))
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
    }
}

@Composable
fun BackgroundMechanismItem(
    title: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Surface1)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        )
        Text(
            text = desc,
            style = MetadataText.copy(fontSize = 8.5.sp, color = TextSecondary, lineHeight = 12.5.sp)
        )
    }
}
