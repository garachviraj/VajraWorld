package com.vajraworld.defender.ui.screens.explainability

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import kotlin.math.abs

@Composable
fun ExplainabilityScreen(
    viewModel: ExplainabilityViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "WHY IS RISK INCREASING?",
            subtitle = "LEVEL 1-5 EXPLAINABILITY",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Level 1: Human Narrative
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LEVEL 1 • EXECUTIVE NARRATIVE",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.narrative,
                        style = Typography.bodyMedium.copy(lineHeight = 22.sp, color = TextPrimary)
                    )
                }
            }

            // Level 2: Feature Attribution Waterfall (Section 26)
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
                        Text(
                            text = "LEVEL 2 • ATTRIBUTION WATERFALL",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                        )
                        Text(
                            text = "SHAP / INT-GRAD",
                            style = MetadataText
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    state.attributions.forEach { attr ->
                        val isPositive = attr.impact > 0
                        val barColor = if (isPositive) Critical else Healthy
                        val pct = (abs(attr.impact) * 100).toInt()

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = attr.description,
                                    style = Typography.bodySmall.copy(color = TextPrimary)
                                )
                                Text(
                                    text = "${if (isPositive) "+" else "-"}$pct%",
                                    style = TechnicalValue.copy(
                                        fontSize = 11.sp,
                                        color = barColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            // Visual horizontal contribution bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Surface2)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (abs(attr.impact) * 3f).coerceIn(0.05f, 1f))
                                        .fillMaxHeight()
                                        .background(barColor)
                                )
                            }
                        }
                    }
                }
            }

            // Level 3: Temporal Change Point Timeline (Section 27)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LEVEL 3 • TEMPORAL CHANGE POINTS",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    state.temporalEvents.forEach { ev ->
                        val isHigh = ev.risk > 0.5f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ev.timeOffset,
                                style = TechnicalValue.copy(fontSize = 11.sp, color = if (isHigh) Critical else Info)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = ev.signalLevel,
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                                )
                                Text(
                                    text = ev.description,
                                    style = Typography.bodySmall.copy(color = TextPrimary)
                                )
                            }
                            Text(
                                text = "${(ev.risk * 100).toInt()}%",
                                style = TechnicalValue.copy(
                                    fontSize = 11.sp,
                                    color = if (isHigh) Critical else Healthy,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Level 4: Graph Topology Center & Level 5: Uncertainty
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HealthyBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = HealthyBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LEVEL 4 & 5 • TOPOLOGY & UNCERTAINTY BOUNDS",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TechnicalMetadataRow(label = "Primary Investigated Node", value = state.centerNode)
                    TechnicalMetadataRow(label = "Forecast Reference ID", value = state.forecastId)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.uncertaintyWarning,
                        style = MetadataText.copy(color = TextPrimary)
                    )
                }
            }
        }
    }
}
