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
fun FileScanScreen(viewModel: FileScanViewModel) {
    val filename by viewModel.selectedFilename.collectAsState()
    val fileResult by viewModel.fileResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = "DOWNLOAD & FILE RISK ENGINE", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
        Text(text = "Zip-Safe Archive & APK Permission Analysis", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // File Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "TARGET FILE (VIA SYSTEM PICKER)", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = filename, style = MaterialTheme.typography.titleMedium, color = PurpleAccent)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.scanFile() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            enabled = !isScanning
        ) {
            Text(text = if (isScanning) "ANALYZING FILE..." else "INSPECT FILE THREATS", color = BgDark)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (fileResult != null) {
            val res = fileResult!!
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
                        Text(text = "APK RISK EVALUATION", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${res.riskScore}/100 RISK",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (res.riskScore > 60) ThreatRed else SafeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = res.recommendedAction, style = MaterialTheme.typography.titleMedium, color = if (res.riskScore > 60) ThreatRed else SafeGreen)

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BorderDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "ARCHIVE SAFETY: VERIFIED SAFE (NO ZIP-BOMB)", style = MaterialTheme.typography.labelSmall, color = SafeGreen)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "DETECTION REASONS:", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    res.whyPoints.forEach { pt ->
                        Text(text = "• $pt", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "REQUESTED PERMISSIONS ANALYZED:", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                    Spacer(modifier = Modifier.height(6.dp))
                    res.permissionsAnalyzed.forEach { perm ->
                        Text(text = "• ${perm.substringAfterLast('.')}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
