package com.vajraworld.defender.ui.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun LinkScanScreen(viewModel: LinkScanViewModel) {
    val inputUrl by viewModel.inputUrl.collectAsState()
    val contextText by viewModel.contextText.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = "GUARDIAN LINK ENGINE", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
        Text(text = "3-Layer URL & Social-Engineering Inspection", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Input URL Field
        OutlinedTextField(
            value = inputUrl,
            onValueChange = { viewModel.updateUrl(it) },
            label = { Text("Target URL to Analyze", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Context Text Field
        OutlinedTextField(
            value = contextText,
            onValueChange = { viewModel.updateContext(it) },
            label = { Text("Surrounding Message Context (Optional)", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.scanUrl() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            enabled = !isScanning && inputUrl.isNotBlank()
        ) {
            Text(text = if (isScanning) "INSPECTING URL..." else "ANALYZE LINK THREAT", color = BgDark)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scan Results Breakdown
        if (scanResult != null) {
            val res = scanResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (res.riskScore > 60) ThreatRed else SafeGreen, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "VERDICT", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${res.riskScore}/100 RISK",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (res.riskScore > 60) ThreatRed else SafeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = res.recommendedAction, style = MaterialTheme.typography.titleMedium, color = if (res.riskScore > 60) ThreatRed else SafeGreen)

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BorderDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "WHY THIS SCORE (LAYERED ATTRIBUTION):", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    res.whyPoints.forEach { pt ->
                        Text(text = "• $pt", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "LINK PROGRESSION TRAJECTORY:", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                    Spacer(modifier = Modifier.height(6.dp))
                    res.progressionTrajectory.forEach { stage ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stage["step"].toString(), style = MaterialTheme.typography.bodySmall)
                            Text(text = stage["status"].toString(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
