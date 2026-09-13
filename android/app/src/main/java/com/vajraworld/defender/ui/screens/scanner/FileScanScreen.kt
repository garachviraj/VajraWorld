package com.vajraworld.defender.ui.screens.scanner

import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.engine.ScannedFileRecord
import com.vajraworld.defender.ui.components.ConfidenceBadge
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import java.io.File

@Composable
fun FileScanScreen(
    viewModel: FileScanViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val selectedFilename by viewModel.selectedFilename.collectAsState()
    val result by viewModel.fileResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isStorageScanning by viewModel.isStorageScanning.collectAsState()
    val storageProgress by viewModel.storageScanProgress.collectAsState()
    val scanHistory by viewModel.fileScanHistory.collectAsState()

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanUri(context, uri)
        }
    }

    var selectedAuditFile by remember { mutableStateOf<ScannedFileRecord?>(null) }
    var inspectedRecord by remember { mutableStateOf<ScannedFileRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "FILE & STORAGE AUDITOR",
            subtitle = "ON-DEVICE RECURSIVE ARCHIVE INSPECTOR",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Action: Run Full Storage Deep Audit
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isStorageScanning) InfoBorder else BorderColor, RoundedCornerShape(12.dp)),
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
                                text = "DEEP STORAGE & INSTALLED APK AUDITOR",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Recursively audits all device partitions, Downloads, Documents & installed APKs",
                                style = MetadataText.copy(fontSize = 9.5.sp)
                            )
                        }
                        if (isStorageScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Info,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.startStorageDeepScan(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isStorageScanning) Surface2 else Info),
                        enabled = !isStorageScanning
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isStorageScanning) Icons.Default.Sync else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isStorageScanning) TextSecondary else Bg0,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isStorageScanning) "AUDITING PHYSICAL STORAGE..." else "RUN FULL STORAGE DEEP AUDIT",
                                style = TechnicalValue.copy(
                                    fontSize = 11.sp,
                                    color = if (isStorageScanning) TextSecondary else Bg0,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Progress Gauge & Stats (Section 29)
                    storageProgress?.let { prog ->
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Surface1)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "SCANNED FILES", style = MetadataText.copy(fontSize = 8.5.sp))
                                    Text(
                                        text = "${prog.scannedCount} Files",
                                        style = TechnicalValue.copy(fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (prog.suspiciousCount > 0) CriticalBg else Surface1)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "THREATS FLAGGED", style = MetadataText.copy(fontSize = 8.5.sp, color = if (prog.suspiciousCount > 0) Critical else TextSecondary))
                                    Text(
                                        text = "${prog.suspiciousCount} Suspicious",
                                        style = TechnicalValue.copy(fontSize = 13.sp, color = if (prog.suspiciousCount > 0) Critical else Healthy, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (prog.isComplete) "Audit Finished: ${prog.scannedCount} files verified" else "Inspecting: ${prog.currentFilePath.takeLast(50)}",
                            style = MetadataText.copy(fontSize = 9.sp, color = if (prog.isComplete) Healthy else TextSecondary)
                        )
                    }
                }
            }

            // Single File Pick via SAF
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
                            Text(
                                text = "AUDIT SPECIFIC DEVICE FILE / APK",
                                style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                            Text(
                                text = "Inspect any APK, script, or binary via Storage Access Framework",
                                style = MetadataText.copy(fontSize = 9.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Info, modifier = Modifier.size(16.dp))
                            Text(
                                text = "SELECT DEVICE FILE (SAF STREAM)",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                            )
                        }
                    }

                    selectedFilename?.let { fname ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Selected: $fname", style = MetadataText.copy(color = Info))
                    }
                }
            }

            // Single File Analysis Result Card
            result?.let { r ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (r.riskScore >= 60) CriticalBorder else HealthyBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FILE AUDIT FINDINGS",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = if (r.riskScore >= 60) Critical else Healthy)
                            )
                            SecurityStatusPill(riskScore = r.riskScore)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = r.filename,
                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SHA-256: ${r.sha256}",
                            style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "THREAT REASONS & ATTRIBUTION",
                            style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                        )
                        r.whyPoints.forEach { point ->
                            Text(
                                text = "• $point",
                                style = Typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        if (r.permissionsAnalyzed.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "PERMISSIONS ANALYZED (${r.permissionsAnalyzed.size})",
                                style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                            )
                            Text(
                                text = r.permissionsAnalyzed.joinToString(", ") { it.substringAfterLast(".") },
                                style = MetadataText.copy(fontSize = 9.sp, color = TextPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (r.riskScore >= 60) CriticalBg else HealthyBg)
                                .border(1.dp, if (r.riskScore >= 60) CriticalBorder else HealthyBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED ACTION: ${r.recommendedAction}",
                                style = TechnicalValue.copy(
                                    fontSize = 10.5.sp,
                                    color = if (r.riskScore >= 60) Critical else Healthy,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Audited Files Stream List & Priority Threats
            val scannedResults = storageProgress?.results ?: emptyList()
            val threats = scannedResults.filter { it.isSuspicious }

            // 1. PRIORITY THREATS SECTION (Requirement 3)
            if (threats.isNotEmpty()) {
                Text(
                    text = " FLAGGED THREATS & HIGH-RISK ANOMALIES (${threats.size})",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = Critical, fontWeight = FontWeight.Bold)
                )

                threats.forEach { threatRec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, CriticalBorder, RoundedCornerShape(10.dp))
                            .clickable { inspectedRecord = threatRec },
                        colors = CardDefaults.cardColors(containerColor = Surface0)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CriticalBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Critical,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = threatRec.filename,
                                            style = TechnicalValue.copy(
                                                fontSize = 11.5.sp,
                                                color = Critical,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "${threatRec.sizeBytes / 1024} KB • ${threatRec.fileCategory} • TAP TO INSPECT & DELETE",
                                            style = MetadataText.copy(fontSize = 8.5.sp, color = TextSecondary)
                                        )
                                    }
                                }
                                SecurityStatusPill(riskScore = threatRec.riskScore)
                            }

                            if (threatRec.threatReasons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                threatRec.threatReasons.take(2).forEach { reason ->
                                    Text(
                                        text = "• $reason",
                                        style = MetadataText.copy(fontSize = 9.sp, color = Critical),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. ALL AUDITED ASSETS
            if (scannedResults.isNotEmpty()) {
                Text(
                    text = "AUDITED STORAGE FILES & PACKAGES (${scannedResults.size})",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                )

                scannedResults.takeLast(40).reversed().forEach { fileRec ->
                    val isThreat = fileRec.isSuspicious
                    val borderCol = if (isThreat) CriticalBorder else BorderColor

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                            .clickable { inspectedRecord = fileRec },
                        colors = CardDefaults.cardColors(containerColor = Surface0)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (fileRec.isApk) Icons.Default.Android else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isThreat) Critical else Info,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = fileRec.filename,
                                        style = TechnicalValue.copy(
                                            fontSize = 11.sp,
                                            color = if (isThreat) Critical else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${fileRec.sizeBytes / 1024} KB • ${if (fileRec.isApk) "APK Package" else "Storage File"}",
                                        style = MetadataText.copy(fontSize = 9.sp)
                                    )
                                }
                            }

                            SecurityStatusPill(riskScore = fileRec.riskScore)
                        }
                    }
                }
            }
        }

        if (inspectedRecord != null) {
            ScannedFileDetailDialog(
                record = inspectedRecord!!,
                onDismiss = { inspectedRecord = null },
                onDelete = {
                    val target = inspectedRecord!!
                    val ok = viewModel.deleteScannedFile(target)
                    inspectedRecord = null
                    android.widget.Toast.makeText(context, if (ok) "Deleted '${target.filename}'" else "Delete failed", android.widget.Toast.LENGTH_SHORT).show()
                },
                onQuarantine = {
                    val target = inspectedRecord!!
                    val ok = viewModel.quarantineScannedFile(target)
                    inspectedRecord = null
                    android.widget.Toast.makeText(context, if (ok) "Quarantined '${target.filename}'" else "Quarantine failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}


@Composable
fun ScannedFileDetailDialog(
    record: ScannedFileRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onQuarantine: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FILE ARTIFACT DISSECTION",
                        style = TechnicalValue.copy(fontSize = 12.sp, color = if (record.isSuspicious) Critical else Info, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = record.fileCategory,
                        style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary)
                    )
                }
                SecurityStatusPill(riskScore = record.riskScore)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = record.filename,
                    style = Typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                )

                HorizontalDivider(color = BorderSubtle)

                TechnicalMetadataRow(label = "Category", value = record.fileCategory)
                TechnicalMetadataRow(label = "Size", value = "${record.sizeBytes / 1024} KB (${record.sizeBytes} bytes)")
                TechnicalMetadataRow(label = "Is Package", value = if (record.isApk) "Android APK" else "Non-Package File")
                TechnicalMetadataRow(label = "Storage Path", value = record.path)
                TechnicalMetadataRow(label = "SHA-256", value = record.sha256.take(28) + "...")

                if (record.threatReasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FORENSIC ATTRIBUTION REASONS:",
                        style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                    record.threatReasons.forEach { r ->
                        Text(
                            text = "• $r",
                            style = MetadataText.copy(fontSize = 9.sp, color = if (record.isSuspicious) Critical else TextPrimary)
                        )
                    }
                }

                if (record.permissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EXTRACTED PERMISSIONS (${record.permissions.size}):",
                        style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = record.permissions.joinToString(", ") { it.substringAfterLast(".") },
                        style = MetadataText.copy(fontSize = 8.5.sp, color = TextSecondary)
                    )
                }

                if (record.dexReport != null) {
                    val dex = record.dexReport
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (dex.detectedLoops.isNotEmpty() || dex.malwareSignatures.isNotEmpty()) CriticalBorder else InfoBorder, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Surface1)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DEX BYTECODE & LOOP DISSECTION",
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = if (dex.riskScore >= 50) Critical else Info, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${dex.classesCount} Classes • ${dex.methodsCount} Methods",
                                    style = MetadataText.copy(fontSize = 8.sp, color = TextSecondary)
                                )
                            }

                            HorizontalDivider(color = BorderSubtle)

                            if (dex.detectedLoops.isNotEmpty()) {
                                Text(
                                    text = "MALICIOUS BYTECODE LOOPS (${dex.detectedLoops.size}):",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = Critical, fontWeight = FontWeight.Bold)
                                )
                                dex.detectedLoops.forEach { loop ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CriticalBg, RoundedCornerShape(4.dp))
                                            .border(1.dp, CriticalBorder, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "[${loop.loopType}] ${loop.className}.${loop.methodName}()",
                                                style = TechnicalValue.copy(fontSize = 8.5.sp, color = Critical, fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Cycle: ${loop.cycleInstructionCount} insns (branch offset: ${loop.branchOffset}) • ${loop.explanation}",
                                                style = MetadataText.copy(fontSize = 8.sp, color = TextPrimary)
                                            )
                                        }
                                    }
                                }
                            }

                            if (dex.malwareSignatures.isNotEmpty()) {
                                Text(
                                    text = "TROJAN & SPYWARE SIGNATURES (${dex.malwareSignatures.size}):",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = Warning, fontWeight = FontWeight.Bold)
                                )
                                dex.malwareSignatures.forEach { sig ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(WarningBg, RoundedCornerShape(4.dp))
                                            .border(1.dp, WarningBorder, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "[${sig.category}] ${sig.matchedPattern}",
                                                style = TechnicalValue.copy(fontSize = 8.5.sp, color = Warning, fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = sig.description,
                                                style = MetadataText.copy(fontSize = 8.sp, color = TextPrimary)
                                            )
                                        }
                                    }
                                }
                            }

                            if (dex.codeSnippets.isNotEmpty()) {
                                Text(
                                    text = "DISASSEMBLED CODE SNIPPETS:",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = Info, fontWeight = FontWeight.Bold)
                                )
                                dex.codeSnippets.take(2).forEach { snip ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Bg0, RoundedCornerShape(4.dp))
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "// ${snip.className}.${snip.methodName} - ${snip.riskWarning}",
                                                style = MetadataText.copy(fontSize = 7.5.sp, color = Warning)
                                            )
                                            Text(
                                                text = snip.pseudocode,
                                                style = TechnicalValue.copy(fontSize = 8.sp, color = Info)
                                            )
                                        }
                                    }
                                }
                            }

                            if (dex.detectedLoops.isEmpty() && dex.malwareSignatures.isEmpty()) {
                                Text(
                                    text = " Verified clean Dalvik instruction stream. Zero infinite loops, fork bombs, droppers, or banking trojan signatures found.",
                                    style = MetadataText.copy(fontSize = 8.5.sp, color = Healthy)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (record.isSuspicious) {
                    Button(
                        onClick = onQuarantine,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Warning)
                    ) {
                        Text(text = "QUARANTINE", style = TechnicalValue.copy(fontSize = 10.sp, color = Bg0, fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Critical)
                    ) {
                        Text(text = "DELETE FILE", style = TechnicalValue.copy(fontSize = 10.sp, color = Bg0, fontWeight = FontWeight.Bold))
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(text = "CLOSE", style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary))
                }
            }
        },
        containerColor = Surface0
    )
}
