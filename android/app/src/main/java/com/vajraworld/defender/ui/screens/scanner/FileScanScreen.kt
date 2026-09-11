package com.vajraworld.defender.ui.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
fun FileScanScreen(viewModel: FileScanViewModel) {
    val filename by viewModel.selectedFilename.collectAsState()
    val fileResult by viewModel.fileResult.collectAsState()
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
            text = "DOWNLOAD & FILE RISK ENGINE",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "Zip-Safe Archive & APK Permission Analysis",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Test Samples
        Text(text = "SELECT SAMPLE FOR INSPECTION", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (filename == "secure_banking_update.apk") ThreatRedBg else SurfaceWhite, RoundedCornerShape(8.dp))
                    .border(1.dp, if (filename == "secure_banking_update.apk") ThreatRedBorder else BorderLight, RoundedCornerShape(8.dp))
                    .clickable { viewModel.selectFile("secure_banking_update.apk") }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Malicious APK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (filename == "secure_banking_update.apk") ThreatRed else TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (filename == "camera_utility.apk") SafeGreenBg else SurfaceWhite, RoundedCornerShape(8.dp))
                    .border(1.dp, if (filename == "camera_utility.apk") SafeGreenBorder else BorderLight, RoundedCornerShape(8.dp))
                    .clickable { viewModel.selectFile("camera_utility.apk") }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Benign APK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (filename == "camera_utility.apk") SafeGreen else TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // File Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SELECTED PACKAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = filename,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.scanFile() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            enabled = !isScanning
        ) {
            Text(
                text = if (isScanning) "DECOMPRESSING & ANALYZING MANIFEST..." else "INSPECT FILE THREATS",
                color = SurfaceWhite,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (fileResult != null) {
            val res = fileResult!!
            val isThreat = res.riskScore >= 60

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, if (isThreat) ThreatRedBorder else SafeGreenBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "APK RISK EVALUATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isThreat) ThreatRedBg else SafeGreenBg,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${res.riskScore}/100 RISK",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isThreat) ThreatRed else SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = res.recommendedAction,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isThreat) ThreatRed else SafeGreen
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(SafeGreenBg, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "ZIP-SAFE", style = MaterialTheme.typography.labelSmall, color = SafeGreen)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Decompression verified nominal (No Zip-Bomb)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SafeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DETECTION REASONS:",
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

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "REQUESTED PERMISSIONS ANALYZED:",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    res.permissionsAnalyzed.forEach { perm ->
                        Text(
                            text = "• ${perm.substringAfterLast('.')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
