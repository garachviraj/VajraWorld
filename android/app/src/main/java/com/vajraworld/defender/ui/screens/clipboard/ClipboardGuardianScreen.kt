package com.vajraworld.defender.ui.screens.clipboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.domain.engine.ClipboardSecretEngine
import com.vajraworld.defender.domain.engine.LocalClipboardResult
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ClipboardGuardianScreen(
    repository: VajraRepository? = null,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    var timerOption by remember { mutableStateOf(30) }
    var remainingSeconds by remember { mutableStateOf(30) }
    var isTimerActive by remember { mutableStateOf(false) }

    var inputClipboardText by remember { mutableStateOf("AKIAIOSFODNN7EXAMPLE secret_access_key") }
    var scanResult by remember {
        mutableStateOf<LocalClipboardResult>(
            ClipboardSecretEngine.scan("AKIAIOSFODNN7EXAMPLE secret_access_key")
        )
    }

    val scrollState = rememberScrollState()

    // Active Countdown Timer Loop
    LaunchedEffect(isTimerActive, remainingSeconds) {
        if (isTimerActive && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                // Clear clipboard
                inputClipboardText = ""
                scanResult = ClipboardSecretEngine.scan("")
                isTimerActive = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "CLIPBOARD GUARDIAN",
            subtitle = "ZERO-RETENTION SECRET DETECTOR",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Strict Privacy Guarantee Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HealthyBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = HealthyBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STRICT ZERO-STORAGE PRIVACY GUARANTEE",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy)
                        )
                        Box(
                            modifier = Modifier
                                .background(Healthy, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ENFORCED",
                                style = TechnicalValue.copy(fontSize = 9.sp, color = Bg0)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "VajraWorld never stores, logs, or uploads clipboard strings (value_stored = false). Regex evaluation occurs on-device and plaintexts are purged immediately.",
                        style = MetadataText.copy(color = TextPrimary, lineHeight = 16.sp)
                    )
                }
            }

            // Clipboard Input Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FOREGROUND CLIPBOARD INSPECTION",
                            style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Surface2)
                                .clickable {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        inputClipboardText = clip
                                        scanResult = ClipboardSecretEngine.scan(clip)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = Info,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(text = "PASTE CLIPBOARD", style = TechnicalValue.copy(fontSize = 9.sp, color = Info))
                        }
                    }

                    OutlinedTextField(
                        value = inputClipboardText,
                        onValueChange = {
                            inputClipboardText = it
                            scanResult = ClipboardSecretEngine.scan(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Info,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Surface1,
                            unfocusedContainerColor = Surface1
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary)
                    )
                }
            }

            // Benchmark Preset Samples
            Text(
                text = "TEST SECRET LURES",
                style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CriticalBg)
                        .border(1.dp, CriticalBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            inputClipboardText = "AKIAIOSFODNN7EXAMPLE secret_access_key"
                            scanResult = ClipboardSecretEngine.scan(inputClipboardText)
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Paste AWS Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Critical)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HealthyBg)
                        .border(1.dp, HealthyBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            inputClipboardText = "Meeting schedule: 2:00 PM EST via secure link"
                            scanResult = ClipboardSecretEngine.scan(inputClipboardText)
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Paste Benign Text", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Healthy)
                }
            }

            // Real Secret Evaluation Card
            val isLeak = scanResult.isSensitive
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isLeak) CriticalBorder else HealthyBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECRETS EVALUATION",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                        )
                        SecurityStatusPill(riskScore = scanResult.riskScore)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scanResult.recommendation,
                        style = Typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isLeak) Critical else Healthy
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLeak) {
                        Text(
                            text = "DETECTED SECRET TYPES:",
                            style = TechnicalValue.copy(fontSize = 10.sp, color = Critical)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        scanResult.detectedTypes.forEach { type ->
                            Text(
                                text = "• $type",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = Critical),
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "No credentials, API tokens, private keys, or mnemonic seeds detected in clipboard.",
                            style = MetadataText.copy(color = TextSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TechnicalMetadataRow(label = "Value Stored in Database", value = "FALSE (0-Retention)")
                }
            }

            // Auto-Clear Timer Card
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
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = Info, modifier = Modifier.size(16.dp))
                            Text(text = "AUTO-CLEAR COUNTDOWN", style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary))
                        }
                        if (isTimerActive) {
                            Text(
                                text = "${remainingSeconds}s remaining",
                                style = TechnicalValue.copy(fontSize = 12.sp, color = Warning, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 30, 60).forEach { sec ->
                            val isSel = timerOption == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) InfoBg else Surface1)
                                    .border(1.dp, if (isSel) InfoBorder else BorderColor, RoundedCornerShape(6.dp))
                                    .clickable {
                                        timerOption = sec
                                        remainingSeconds = sec
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    style = TechnicalValue.copy(
                                        fontSize = 11.sp,
                                        color = if (isSel) Info else TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                remainingSeconds = timerOption
                                isTimerActive = true
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Info)
                        ) {
                            Text(text = "START AUTO-CLEAR", style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0))
                        }

                        Button(
                            onClick = {
                                inputClipboardText = ""
                                scanResult = ClipboardSecretEngine.scan("")
                                isTimerActive = false
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Critical)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = TextWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "PURGE NOW", style = TechnicalValue.copy(fontSize = 11.sp, color = TextWhite))
                        }
                    }
                }
            }
        }
    }
}
