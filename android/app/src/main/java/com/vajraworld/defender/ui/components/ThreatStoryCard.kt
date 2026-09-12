package com.vajraworld.defender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun ThreatStoryCard(
    title: String,
    stage: String,
    correlatedEventsCount: Int,
    predictedNextStage: String? = null,
    riskScore: Int = 68,
    onClick: () -> Unit,
    onSimulate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCritical = riskScore >= 70
    val accentColor = if (isCritical) Critical else Warning
    val accentBg = if (isCritical) CriticalBg else WarningBg
    val accentBorder = if (isCritical) CriticalBorder else WarningBorder

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface0)
            .border(1.dp, accentBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Threat Icon",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "CORRELATED THREAT STORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            SecurityStatusPill(riskScore = riskScore)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Entity path progression
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Surface1)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STAGE: $stage",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
            )
            Text(
                text = "$correlatedEventsCount events linked",
                style = MetadataText
            )
        }

        if (predictedNextStage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FORECAST PIVOT: ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Info
                )
                Text(
                    text = predictedNextStage,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Warning
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tap to inspect bipartite graph & evidence",
                style = MetadataText
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "INSPECT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Info
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Inspect",
                    tint = Info,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
