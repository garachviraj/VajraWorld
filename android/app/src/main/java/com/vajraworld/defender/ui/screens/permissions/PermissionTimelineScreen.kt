package com.vajraworld.defender.ui.screens.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.engine.AppPermissionUsageRecord
import com.vajraworld.defender.domain.engine.SensitivePermissionEvent
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PermissionTimelineScreen(
    viewModel: PermissionTimelineViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val filterScrollState = rememberScrollState()

    val filterOptions = listOf("ALL", "TOXIC", "CAMERA", "AUDIO", "LOCATION", "SMS", "OVERLAY")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
    ) {
        VajraTopBar(
            title = "PERMISSION TIMELINE",
            subtitle = "SENSITIVE OP ACCESS AUDITOR",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "APP PERMISSION AUDIT",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${state.totalAppsAudited} Applications Audited via AppOps / PackageManager",
                            style = MetadataText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (state.toxicCombinationCount > 0) CriticalBg else HealthyBg,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (state.toxicCombinationCount > 0) CriticalBorder else HealthyBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${state.toxicCombinationCount} TOXIC SIGNATURES",
                            style = TechnicalValue.copy(
                                fontSize = 9.sp,
                                color = if (state.toxicCombinationCount > 0) Critical else Healthy
                            )
                        )
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(filterScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterOptions.forEach { filter ->
                    val isSelected = state.selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Info else Surface0)
                            .border(1.dp, if (isSelected) Info else BorderColor, RoundedCornerShape(6.dp))
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            style = TechnicalValue.copy(
                                fontSize = 10.sp,
                                color = if (isSelected) TextWhite else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // List of Applications & Permission Timelines
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Info)
                }
            } else if (state.filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No applications match the '${state.selectedFilter}' filter.",
                        style = MetadataText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredRecords, key = { it.packageName }) { record ->
                        AppPermissionCard(
                            record = record,
                            onOpenAppSettings = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", record.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPermissionCard(
    record: AppPermissionUsageRecord,
    onOpenAppSettings: () -> Unit
) {
    val badgeColor = when (record.overallRiskLevel) {
        "CRITICAL" -> Critical
        "HIGH" -> Warning
        "ELEVATED" -> Warning
        else -> Healthy
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (record.toxicCombinations.isNotEmpty()) CriticalBorder else BorderColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (record.toxicCombinations.isNotEmpty()) CriticalBg.copy(alpha = 0.2f) else Surface0
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.appName,
                        style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = record.packageName,
                        style = MetadataText.copy(fontSize = 9.sp),
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${record.overallRiskLevel} (${record.riskScore})",
                            style = TechnicalValue.copy(fontSize = 8.sp, color = badgeColor)
                        )
                    }

                    IconButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Manage Permissions",
                            tint = Info,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Toxic Combination Warning Alert
            if (record.toxicCombinations.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CriticalBg)
                        .border(1.dp, CriticalBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "CRITICAL TOXIC PERMISSION COMBINATION",
                            style = TechnicalValue.copy(fontSize = 9.sp, color = Critical, fontWeight = FontWeight.Bold)
                        )
                        record.toxicCombinations.forEach { toxic ->
                            Text(
                                text = "• $toxic",
                                style = MetadataText.copy(fontSize = 9.sp, color = TextPrimary)
                            )
                        }
                    }
                }
            }

            // Sensitive Permissions List with timestamps
            if (record.sensitivePermissions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SENSITIVE PERMISSIONS ACCESSED / GRANTED (${record.sensitivePermissions.size}):",
                        style = TechnicalValue.copy(fontSize = 9.sp, color = TextSecondary)
                    )

                    record.sensitivePermissions.forEach { event ->
                        PermissionEventRow(event)
                    }
                }
            } else {
                Text(
                    text = "No high-risk sensitive permissions granted.",
                    style = MetadataText.copy(fontSize = 9.sp, color = Healthy)
                )
            }
        }
    }
}

@Composable
private fun PermissionEventRow(event: SensitivePermissionEvent) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    val timeFormatted = if (event.lastAccessTimeMs > 0) dateFormat.format(Date(event.lastAccessTimeMs)) else "Recent"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Surface1)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when (event.riskLevel) {
                            "CRITICAL" -> Critical
                            "HIGH" -> Warning
                            else -> Info
                        }
                    )
            )
            Text(
                text = event.opLabel,
                style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary)
            )
        }

        Text(
            text = "Active: $timeFormatted",
            style = MetadataText.copy(fontSize = 9.sp)
        )
    }
}
