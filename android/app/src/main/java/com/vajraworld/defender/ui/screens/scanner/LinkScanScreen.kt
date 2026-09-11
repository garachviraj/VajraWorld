package com.vajraworld.defender.ui.screens.scanner

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
            .background(BgLight)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "GUARDIAN LINK ENGINE",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "3-Layer URL & Social-Engineering Inspection",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Preset Samples
        Text(text = "SAMPLE SCENARIO PRESETS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
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
                        viewModel.updateUrl("http://192.168.1.50/secure-bank-login.xyz/update.apk")
                        viewModel.updateContext("URGENT: Your bank account is suspended! Verify now.")
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Phishing APK Lure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThreatRed)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(SafeGreenBg, RoundedCornerShape(8.dp))
                    .border(1.dp, SafeGreenBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.updateUrl("https://auth.google.com/oauth2/v1/certs")
                        viewModel.updateContext("Standard OAuth security key certificate")
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Benign HTTPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Input URL Field
        OutlinedTextField(
            value = inputUrl,
            onValueChange = { viewModel.updateUrl(it) },
            label = { Text("Target URL to Inspect") },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Context Text Field
        OutlinedTextField(
            value = contextText,
            onValueChange = { viewModel.updateContext(it) },
            label = { Text("Surrounding Message Context (Optional)") },
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.scanUrl() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            enabled = !isScanning && inputUrl.isNotBlank()
        ) {
            Text(
                text = if (isScanning) "INSPECTING 3-LAYER URL THREATS..." else "ANALYZE LINK THREAT",
                color = SurfaceWhite,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scan Results Breakdown
        if (scanResult != null) {
            val res = scanResult!!
            val isHighRisk = res.riskScore >= 60

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, if (isHighRisk) ThreatRedBorder else SafeGreenBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURITY VERDICT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isHighRisk) ThreatRedBg else SafeGreenBg,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${res.riskScore}/100 RISK",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isHighRisk) ThreatRed else SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = res.recommendedAction,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isHighRisk) ThreatRed else SafeGreen
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "WHY THIS SCORE (LAYERED ATTRIBUTION):",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    res.whyPoints.forEach { pt ->
                        Text(
                            text = "• $pt",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "LINK PROGRESSION TRAJECTORY:",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    res.progressionTrajectory.forEach { stage ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stage["step"].toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = stage["status"].toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (stage["status"] == "OBSERVED") ThreatRed else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
