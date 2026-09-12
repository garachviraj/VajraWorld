package com.vajraworld.defender.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
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
                // Section 1: Live On-Device Security Guards
                Text(
                    text = "REAL-TIME PROTECTION GUARDS",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. App Install Guard
                        GuardToggleRow(
                            title = "New App Install Monitor",
                            subtitle = "Autonomously audits newly installed APKs for toxic permissions (Overlay + Accessibility trojans).",
                            isEnabled = state.installGuardEnabled,
                            onToggle = { viewModel.toggleInstallGuard(it) },
                            icon = Icons.Default.InstallMobile,
                            accentColor = Info
                        )

                        Divider(color = BorderSubtle, thickness = 1.dp)

                        // 2. Screen Sharing Shield
                        GuardToggleRow(
                            title = "Screen Sharing & Cast Shield",
                            subtitle = "Detects active virtual displays, screen mirroring, or unauthorized recording during secure sessions.",
                            isEnabled = state.screenShareShieldEnabled,
                            onToggle = { viewModel.toggleScreenShareShield(it) },
                            icon = Icons.Default.ScreenShare,
                            accentColor = Critical
                        )

                        Divider(color = BorderSubtle, thickness = 1.dp)

                        // 3. Call Fraud & Vishing Guard
                        GuardToggleRow(
                            title = "Telephony Call Fraud Alert",
                            subtitle = "Alerts against social engineering / OTP forwarding lures during active voice calls.",
                            isEnabled = state.callGuardEnabled,
                            onToggle = { viewModel.toggleCallGuard(it) },
                            icon = Icons.Default.PhoneInTalk,
                            accentColor = Warning
                        )

                        Divider(color = BorderSubtle, thickness = 1.dp)

                        // 4. Clipboard Secret Shield
                        GuardToggleRow(
                            title = "Clipboard Secret Shield",
                            subtitle = "Detects credentials, AWS keys, credit cards & seeds in memory with zero plaintext storage.",
                            isEnabled = state.clipboardGuardEnabled,
                            onToggle = { viewModel.toggleClipboardGuard(it) },
                            icon = Icons.Default.ContentPaste,
                            accentColor = Healthy
                        )

                        Divider(color = BorderSubtle, thickness = 1.dp)

                        // 5. Phishing Link Interceptor
                        GuardToggleRow(
                            title = "Phishing & Deceptive Link Interceptor",
                            subtitle = "Computes Shannon entropy and normalized Levenshtein brand similarity for URLs.",
                            isEnabled = state.linkGuardEnabled,
                            onToggle = { viewModel.toggleLinkGuard(it) },
                            icon = Icons.Default.Link,
                            accentColor = Info
                        )

                        Divider(color = BorderSubtle, thickness = 1.dp)

                        // 6. Notification OTP Vault
                        GuardToggleRow(
                            title = "Notification OTP Privacy Vault",
                            subtitle = "In-flight notification triage with verified SHA-256 and zero plaintext OTP retention.",
                            isEnabled = state.notificationGuardEnabled,
                            onToggle = { viewModel.toggleNotificationGuard(it) },
                            icon = Icons.Default.NotificationsActive,
                            accentColor = Healthy
                        )
                    }
                }

                // Section 2: Timer & Privacy Parameters
                Text(
                    text = "PRIVACY & AUTO-PURGE PARAMETERS",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sensitive Clipboard Auto-Purge Countdown",
                            style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Duration before sensitive credentials in clipboard are automatically scrubbed from memory.",
                            style = MetadataText
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 60).forEach { seconds ->
                                val isSelected = state.autoClearTimerSec == seconds
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Info else Surface2)
                                        .clickable { viewModel.setAutoClearTimer(seconds) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${seconds}s",
                                        style = TechnicalValue.copy(
                                            fontSize = 12.sp,
                                            color = if (isSelected) TextWhite else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Data & Storage Control Center
                Text(
                    text = "LOCAL DATA & PRIVACY CONTROL",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearUrlHistory() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear URL History", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "CLEAR URL SCAN HISTORY", style = TechnicalValue.copy(fontSize = 11.sp))
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearClipboardLogs() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Clipboard Log", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "PURGE DAILY CLIPBOARD AUDIT LOG", style = TechnicalValue.copy(fontSize = 11.sp))
                        }

                        Button(
                            onClick = { viewModel.clearAllScansAndEvents() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Critical)
                        ) {
                            Icon(Icons.Default.SecurityUpdateWarning, contentDescription = "Purge All", tint = TextWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "PURGE ALL LOCAL SECURITY RECORDS", style = TechnicalValue.copy(fontSize = 11.sp, color = TextWhite))
                        }
                    }
                }

                // Section 4: Live Protection Privileges & Diagnostics
                Text(
                    text = "REAL-TIME PROTECTION & DIAGNOSTICS",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Info)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Bg0, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "ENABLE BROWSER PHISHING GUARD (ACCESSIBILITY)", style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold))
                        }

                        Button(
                            onClick = {
                                com.vajraworld.defender.service.VajraNotificationManager.sendTestNotification(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "TEST THREAT ALERT WITH BLOCK/UNBLOCK ACTIONS", style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Engine Attestation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface1)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VAJRAWORLD GUARDIAN ON-DEVICE ENGINE v2.0",
                            style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Pure Kotlin Autonomous Defense Plane • Zero-Knowledge Architecture",
                            style = MetadataText.copy(fontSize = 9.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardToggleRow(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isEnabled) accentColor else TextMuted,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = TechnicalValue.copy(
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (isEnabled) Healthy.copy(alpha = 0.12f) else Surface2,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "ACTIVE" else "DISABLED",
                            style = TechnicalValue.copy(
                                fontSize = 8.sp,
                                color = if (isEnabled) Healthy else TextMuted
                            )
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MetadataText.copy(fontSize = 10.sp, lineHeight = 14.sp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = Info,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Surface2
            )
        )
    }
}
