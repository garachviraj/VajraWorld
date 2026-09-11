package com.vajraworld.defender.ui.screens.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.Incident
import com.vajraworld.defender.ui.theme.*

@Composable
fun IncidentDetailScreen(
    incident: Incident,
    onBack: () -> Unit,
    onTestDefence: () -> Unit,
    onAcknowledge: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Back Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandBlue)
            }
            Text(
                text = incident.incidentId,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Risk & Confidence Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, ThreatRedBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = ThreatRedBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PREDICTED INTRUSION PROGRESSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThreatRed
                    )
                    Box(
                        modifier = Modifier
                            .background(SurfaceWhite, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CONFIDENCE: ${(incident.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = BrandBlue
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = incident.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Risk Level: ${(incident.risk * 100).toInt()}% • Estimated Time to Progression: ${incident.etaSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThreatRed
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. What is happening
        SectionCard(title = "1. WHAT IS HAPPENING NOW") {
            Text(
                text = "Host-17 (Workstation) has initiated rapid internal port probing across subnet 192.168.1.0/24 and requested 14 Kerberos TGT tickets in 35 seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }

        // 2. What the model expects next
        SectionCard(title = "2. WHAT THE WORLD MODEL EXPECTS NEXT") {
            Text(
                text = "Multi-step rollout forecasts credential abuse targeting AD-01 (Domain Controller) followed by lateral pivot to Finance-DB-02 on port 445 (SMB) within 74s.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ThreatRed
            )
        }

        // 3. Why the model thinks so
        SectionCard(title = "3. WHY THE MODEL THINKS SO") {
            Text(text = "• Destination port entropy surged to 3.42 (+0.19 impact)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(text = "• East-west lateral fan-out reached 31 destinations (+0.13 impact)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(text = "• Inter-arrival time variance dropped on periodic Kerberos probe (+0.11 impact)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }

        // 4. Affected assets
        SectionCard(title = "4. AFFECTED ASSETS") {
            Text(text = "• Source: Host-17 (192.168.1.17) - Workstation", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(text = "• Pivot Target: AD-01 (192.168.1.10) - Critical Domain Controller", style = MaterialTheme.typography.bodySmall, color = BrandBlue)
            Text(text = "• High-Value Target: Finance-DB-02 (192.168.2.50) - Tier-1 Database", style = MaterialTheme.typography.bodySmall, color = ThreatRed)
        }

        // 5. ATT&CK mapping
        SectionCard(title = "5. MITRE ATT&CK PROGRESSION MAPPING") {
            Text(text = "Current: Reconnaissance (T1046 - Network Service Discovery)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(text = "Upcoming: Lateral Movement (T1021.002 - SMB/Windows Admin Shares)", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
        }

        // 6. Test a defence
        SectionCard(title = "6. COUNTERFACTUAL SIMULATION") {
            Text(text = "Simulate isolating Host-17 before executing production quarantine.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onTestDefence,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text(text = "TEST DEFENCE ON THIS INCIDENT", color = SurfaceWhite, style = MaterialTheme.typography.labelMedium)
            }
        }

        // 7. Recommended next step
        SectionCard(title = "7. RECOMMENDED NEXT STEP") {
            Text(text = incident.recommendedAction, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = SafeGreen)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Policy Permission: Allowed (Reversible action)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }

        // 8. Audit History & Acknowledge
        SectionCard(title = "8. AUDIT HISTORY & RESPONSE") {
            Text(text = "Logged to immutable audit ledger at ${incident.createdAt}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onAcknowledge(incident.incidentId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Text(
                    text = if (incident.acknowledged) "ALREADY ACKNOWLEDGED" else "ACKNOWLEDGE & LOG ACTION",
                    color = BrandBlue,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = BrandBlue)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
