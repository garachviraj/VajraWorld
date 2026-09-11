package com.vajraworld.defender.ui.screens.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.data.repository.VajraRepository
import com.vajraworld.defender.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ClipboardGuardianScreen(repository: VajraRepository? = null) {
    var timerOption by remember { mutableStateOf(30) }
    var remainingSeconds by remember { mutableStateOf(30) }
    var isTimerActive by remember { mutableStateOf(false) }

    var inputClipboardText by remember { mutableStateOf("AKIAIOSFODNN7EXAMPLE secret_access_key") }
    var detectedLeak by remember { mutableStateOf<String?>("AWS_ACCESS_KEY (AKIA...)") }
    var isSensitive by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Active Countdown Timer Loop
    LaunchedEffect(isTimerActive, remainingSeconds) {
        if (isTimerActive && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                // Clear clipboard
                inputClipboardText = ""
                detectedLeak = null
                isSensitive = false
                isTimerActive = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CLIPBOARD GUARDIAN",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Foreground Secret Leak Detection & Auto-Clear",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(SafeGreenBg, RoundedCornerShape(8.dp))
                    .border(1.dp, SafeGreenBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ZERO RETENTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = SafeGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Guarantee Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "STRICT PRIVACY ASSURANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SafeGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "VajraWorld never uploads or stores copied clipboard strings. Scans occur strictly on foreground intent and plaintexts are purged upon analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Clipboard Input / Inspection Box
        Text(
            text = "CLIPBOARD CONTENT TO INSPECT",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = inputClipboardText,
            onValueChange = {
                inputClipboardText = it
                if (it.contains("AKIA")) {
                    detectedLeak = "AWS_ACCESS_KEY (AKIA...)"
                    isSensitive = true
                } else if (it.contains("ghp_")) {
                    detectedLeak = "GITHUB_PERSONAL_ACCESS_TOKEN"
                    isSensitive = true
                } else {
                    detectedLeak = null
                    isSensitive = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = BorderLight,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Sample Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(ThreatRedBg, RoundedCornerShape(8.dp))
                    .border(1.dp, ThreatRedBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        inputClipboardText = "AKIAIOSFODNN7EXAMPLE secret_access_key"
                        detectedLeak = "AWS_ACCESS_KEY (AKIA...)"
                        isSensitive = true
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Paste AWS Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThreatRed)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(SafeGreenBg, RoundedCornerShape(8.dp))
                    .border(1.dp, SafeGreenBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        inputClipboardText = "Meeting link: https://meet.google.com/abc-defg-hij"
                        detectedLeak = null
                        isSensitive = false
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Paste Benign Text", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Auto-Clear Timer Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAFETY AUTO-CLEAR TIMER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (isTimerActive) {
                        Text(
                            text = "${remainingSeconds}s remaining",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ThreatRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 60).forEach { sec ->
                        val isSelected = timerOption == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) BrandBlueLight else SurfaceWhite,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) BrandBlue else BorderLight,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    timerOption = sec
                                    remainingSeconds = sec
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec}s Timer",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BrandBlue else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isTimerActive = !isTimerActive
                        if (isTimerActive) remainingSeconds = timerOption
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerActive) WarningAmber else BrandBlue)
                ) {
                    Text(
                        text = if (isTimerActive) "PAUSE AUTO-CLEAR TIMER" else "START AUTO-CLEAR TIMER",
                        style = MaterialTheme.typography.labelMedium,
                        color = SurfaceWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Detection Result Notice
        if (detectedLeak != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, ThreatRedBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = ThreatRedBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SENSITIVE SECRET DETECTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThreatRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type: $detectedLeak",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThreatRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recommended: Clear clipboard immediately and rotate exposed key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            inputClipboardText = ""
                            detectedLeak = null
                            isSensitive = false
                            isTimerActive = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CLEAR CLIPBOARD NOW",
                            color = SurfaceWhite,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SafeGreenBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SafeGreenBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓ Clipboard is clean. No sensitive credentials detected.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SafeGreen
                    )
                }
            }
        }
    }
}
