package com.vajraworld.defender.ui.screens.scanner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun FileScanScreen(
    viewModel: FileScanViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val filename by viewModel.selectedFilename.collectAsState()
    val fileResult by viewModel.fileResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isRealDeviceFile by viewModel.isRealDeviceFile.collectAsState()
    val storageProgress by viewModel.storageScanProgress.collectAsState()
    val isStorageScanning by viewModel.isStorageScanning.collectAsState()
    val fileHistory by viewModel.fileScanHistory.collectAsState()
    val scrollState = rememberScrollState()

    // Storage Access Framework (SAF) File Picker Launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.scanUri(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "FILE & STORAGE INSPECTOR",
            subtitle = "STREAMING SHA-256 & STORAGE AUDITOR",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Full Storage & Downloads Deep Scan Card (Requirement 5)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AUTONOMOUS STORAGE SCANNER",
                                style = TechnicalValue.copy(fontSize = 12.sp, color = Info, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Deep inspection of Downloads, Documents & Sideloads",
                                style = MetadataText.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isStorageScanning) WarningBg else Surface2, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isStorageScanning) "AUDITING" else "IDLE",
                                style = TechnicalValue.copy(fontSize = 9.sp, color = if (isStorageScanning) Warning else Healthy)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (storageProgress != null) {
                        val prog = storageProgress!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Files Scanned: ${prog.scannedCount}",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                            )
                            Text(
                                text = "Threats Found: ${prog.suspiciousCount}",
                                style = TechnicalValue.copy(
                                    fontSize = 11.sp,
                                    color = if (prog.suspiciousCount > 0) Critical else Healthy,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (isStorageScanning) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Info,
                                trackColor = Surface2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prog.currentFilePath.takeLast(40),
                                style = TechnicalValue.copy(fontSize = 9.sp, color = TextMuted),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = { viewModel.startStorageDeepScan(context) },
                        enabled = !isStorageScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Info)
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Bg0, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStorageScanning) "SCANNING STORAGE IN BACKGROUND..." else "RUN FULL STORAGE DEEP AUDIT",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // SAF Pick Single File Button
            Button(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2)
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "Pick File",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OR CHOOSE SINGLE APK / FILE (SAF)",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Preset Test Samples Row
            Text(
                text = "OR SELECT BENCHMARK SAMPLE PACKAGE",
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
                        .background(if (filename == "secure_banking_update.apk") CriticalBg else Surface0)
                        .border(1.dp, if (filename == "secure_banking_update.apk") CriticalBorder else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectFile("secure_banking_update.apk") }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Malicious APK Sample",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (filename == "secure_banking_update.apk") Critical else TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (filename == "camera_utility.apk") HealthyBg else Surface0)
                        .border(1.dp, if (filename == "camera_utility.apk") HealthyBorder else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectFile("camera_utility.apk") }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Benign Utility APK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (filename == "camera_utility.apk") Healthy else TextPrimary
                    )
                }
            }

            // Selected Package Status Card
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
                            text = "TARGET ARCHIVE / PACKAGE",
                            style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                        )
                        Box(
                            modifier = Modifier
                                .background(if (isRealDeviceFile) HealthyBg else WarningBg, RoundedCornerShape(4.dp))
                                .border(1.dp, if (isRealDeviceFile) HealthyBorder else WarningBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isRealDeviceFile) "DEVICE FILE (SAF STREAM)" else "PRESET SAMPLE",
                                style = TechnicalValue.copy(
                                    fontSize = 9.sp,
                                    color = if (isRealDeviceFile) Healthy else Warning
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = filename,
                        style = TechnicalValue.copy(fontSize = 14.sp, color = Info)
                    )
                }
            }

            // Trigger Inspection Button (if preset selected)
            if (!isRealDeviceFile) {
                Button(
                    onClick = { viewModel.scanFile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2),
                    enabled = !isScanning
                ) {
                    Text(
                        text = if (isScanning) "DECOMPRESSING & PARSING MANIFEST..." else "INSPECT SAMPLE THREATS",
                        style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary)
                    )
                }
            }

            // Scan Progress Indicator (Section 29)
            if (isScanning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, InfoBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface1)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Info, strokeWidth = 2.dp)
                            Text(text = "INSPECTION PIPELINE RUNNING", style = TechnicalValue.copy(fontSize = 11.sp, color = Info))
                        }
                        Text(text = "1. Reading stream metadata & envelope", style = MetadataText)
                        Text(text = "2. Calculating streaming SHA-256 (8KB buffer)", style = MetadataText)
                        Text(text = "3. Checking Zip-bomb decompression ratio", style = MetadataText)
                        Text(text = "4. Parsing binary AndroidManifest.xml permissions", style = MetadataText)
                    }
                }
            }

            // Inspection Results (Section 30)
            AnimatedVisibility(
                visible = fileResult != null && !isScanning,
                enter = fadeIn()
            ) {
                fileResult?.let { res ->
                    val isThreat = res.riskScore >= 60
                    val accentColor = if (isThreat) Critical else Healthy
                    val accentBorder = if (isThreat) CriticalBorder else HealthyBorder

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accentBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Surface0)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "APK RISK EVALUATION",
                                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                                )
                                SecurityStatusPill(riskScore = res.riskScore)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = res.recommendedAction,
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Zip-Safe Badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Surface1)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (res.archiveSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Safe",
                                    tint = if (res.archiveSafe) Healthy else Critical,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (res.archiveSafe) "ZIP-SAFE: Archive decompression nominal (No Zip-Bomb)" else "UNSAFE: Potential Zip-Bomb or malformed archive",
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = if (res.archiveSafe) Healthy else Critical)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Streaming SHA-256 Hash with Copy
                            TechnicalMetadataRow(
                                label = "Streaming SHA-256",
                                value = if (res.sha256.length > 20) "${res.sha256.take(16)}...${res.sha256.takeLast(8)}" else res.sha256,
                                allowCopy = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Detection Reasons
                            Text(
                                text = "DETECTION SIGNALS & REASONS:",
                                style = TechnicalValue.copy(fontSize = 10.sp, color = Info)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            res.whyPoints.forEach { pt ->
                                Text(
                                    text = "• $pt",
                                    style = Typography.bodySmall.copy(color = TextPrimary),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Requested Permissions Analyzed
                            if (res.permissionsAnalyzed.isNotEmpty()) {
                                Text(
                                    text = "MANIFEST PERMISSIONS ANALYZED:",
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = Warning)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                res.permissionsAnalyzed.forEach { perm ->
                                    val isToxic = perm.contains("ACCESSIBILITY") || perm.contains("ALERT_WINDOW") || perm.contains("SMS")
                                    Text(
                                        text = "• ${perm.substringAfterLast('.')}",
                                        style = TechnicalValue.copy(
                                            fontSize = 11.sp,
                                            color = if (isToxic) Critical else TextSecondary
                                        ),
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scanned Files from Current Deep Storage Audit
            if (storageProgress != null && storageProgress!!.results.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "DEEP STORAGE AUDIT FINDINGS (${storageProgress!!.results.size})",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        storageProgress!!.results.take(15).forEach { item ->
                            val isRisk = item.riskScore >= 40
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isRisk) CriticalBg else Surface1)
                                    .border(1.dp, if (isRisk) CriticalBorder else BorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.filename,
                                        style = TechnicalValue.copy(
                                            fontSize = 11.sp,
                                            color = if (isRisk) Critical else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (item.threatReasons.isNotEmpty()) item.threatReasons.first() else "Clean storage asset",
                                        style = MetadataText.copy(fontSize = 9.sp, color = if (isRisk) Critical else TextSecondary),
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (isRisk) Critical else Healthy, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${item.riskScore}%",
                                        style = TechnicalValue.copy(fontSize = 9.sp, color = Bg0, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Storage Audit History from Database
            if (fileHistory.isNotEmpty()) {
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
                                text = "PERSISTENT FILE AUDIT LOG (${fileHistory.size})",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Surface2)
                                    .clickable { viewModel.clearHistory() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "CLEAR LOG", style = TechnicalValue.copy(fontSize = 9.sp, color = Critical))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        fileHistory.take(10).forEach { item ->
                            val isRisk = item.riskScore >= 40
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Surface1)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.target,
                                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(item.createdAt)),
                                        style = MetadataText.copy(fontSize = 9.sp, color = TextMuted)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (isRisk) Critical else Healthy, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${item.riskScore}%",
                                        style = TechnicalValue.copy(fontSize = 9.sp, color = Bg0, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
