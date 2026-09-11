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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun ClipboardGuardianScreen() {
    var timerOption by remember { mutableStateOf(30) }
    var detectedLeak by remember { mutableStateOf<String?>("AWS_ACCESS_KEY (AKIA...)") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "CLIPBOARD GUARDIAN", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                Text(text = "Sensitive Secret Leak Detection", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen)
            ) {
                Text(
                    text = "ZERO RETENTION ACTIVE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = SafeGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Guarantee Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "STRICT PRIVACY ASSURANCE", style = MaterialTheme.typography.labelSmall, color = SafeGreen)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "VajraWorld never uploads or stores copied clipboard strings. Only security pattern classifications (e.g. TYPE=API_KEY) are logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auto-Clear Timer Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "CLIPBOARD SAFETY AUTO-CLEAR TIMER", style = MaterialTheme.typography.labelSmall)
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
                                .background(if (isSelected) AccentCyan else CardDark, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) AccentCyan else BorderDark, RoundedCornerShape(6.dp))
                                .clickable { timerOption = sec }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sec}s Timer",
                                fontSize = 12.sp,
                                color = if (isSelected) BgDark else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Detection Notice
        if (detectedLeak != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ThreatRed, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "SENSITIVE SECRET EXPOSED ON CLIPBOARD", style = MaterialTheme.typography.labelSmall, color = ThreatRed)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Detected pattern: $detectedLeak", style = MaterialTheme.typography.titleMedium, color = WarningAmber)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Value stored: False | Recommended: Wipe clipboard immediately and rotate credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { detectedLeak = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThreatRed),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(text = "CLEAR CLIPBOARD NOW", color = TextPrimary)
                    }
                }
            }
        }
    }
}
