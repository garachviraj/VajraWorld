package com.vajraworld.defender.ui.screens.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import java.util.Locale

@Composable
fun LinkScanScreen(
    viewModel: LinkScanViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val inputUrl by viewModel.inputUrl.collectAsState()
    val contextText by viewModel.contextText.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val urlHistory by viewModel.urlHistory.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "LINK GUARDIAN",
            subtitle = "ENTROPY & BRAND DECEPTION ENGINE",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {


            // URL Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PASTE OR SHARE A LINK",
                            style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Surface2)
                                .clickable {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) viewModel.updateUrl(clip)
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
                        value = inputUrl,
                        onValueChange = { viewModel.updateUrl(it) },
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

                    OutlinedTextField(
                        value = contextText,
                        onValueChange = { viewModel.updateContext(it) },
                        label = { Text("Surrounding Message Context (Optional)", style = MetadataText) },
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
                        textStyle = Typography.bodySmall.copy(color = TextPrimary)
                    )
                }
            }

            // Inspect Button
            Button(
                onClick = { viewModel.scanUrl() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Info),
                enabled = !isScanning && inputUrl.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Scan",
                    tint = Bg0,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "EVALUATING SHANNON ENTROPY & HOMOGLYPHS..." else "ANALYZE LINK SECURITY",
                    color = Bg0,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Results Section
            AnimatedVisibility(
                visible = scanResult != null && !isScanning,
                enter = fadeIn()
            ) {
                scanResult?.let { res ->
                    val isHighRisk = res.riskScore >= 50
                    val accentColor = if (isHighRisk) Critical else Healthy
                    val accentBorder = if (isHighRisk) CriticalBorder else HealthyBorder

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
                                    text = "URL THREAT EVALUATION",
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

                            // Technical metrics
                            TechnicalMetadataRow(
                                label = "Shannon Entropy",
                                value = "${String.format(Locale.US, "%.2f", res.entropy)} bits/char"
                            )
                            TechnicalMetadataRow(
                                label = "Confidence Index",
                                value = "${(res.confidence * 100).toInt()}%"
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Detection reasons
                            Text(
                                text = "DETECTION REASONS & ATTRIBUTION:",
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
                        }
                    }
                }
            }

            // URL Scan History & Risk Tracker Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "URL AUDIT HISTORY (${urlHistory.size})",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        if (urlHistory.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Surface2)
                                    .clickable { viewModel.clearHistory() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CLEAR",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = Critical)
                                )
                            }
                        }
                    }

                    if (urlHistory.isEmpty()) {
                        Text(
                            text = "No URLs scanned or logged yet. Scan links above to record forensic evaluations.",
                            style = MetadataText
                        )
                    } else {
                        val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            urlHistory.take(15).forEach { item ->
                                val isHighRisk = item.riskScore >= 50
                                val itemColor = if (isHighRisk) Critical else Healthy
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Surface1)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.selectHistoryItem(item) }
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.target,
                                                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(itemColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .border(1.dp, itemColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "RISK ${item.riskScore}%",
                                                    style = TechnicalValue.copy(fontSize = 8.sp, color = itemColor)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isHighRisk) "Flagged: Deceptive Pattern / High Abuse" else "Nominal URL structure",
                                                style = MetadataText.copy(fontSize = 9.sp, color = if (isHighRisk) Warning else TextMuted)
                                            )
                                            Text(
                                                text = dateFormat.format(java.util.Date(item.createdAt)),
                                                style = MetadataText.copy(fontSize = 9.sp)
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
    }
}
