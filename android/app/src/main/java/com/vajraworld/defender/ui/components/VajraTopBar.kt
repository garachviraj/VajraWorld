package com.vajraworld.defender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VajraTopBar(
    title: String = "VAJRAWORLD",
    subtitle: String = "GUARDIAN",
    isSynthetic: Boolean = false,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeState = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            timeState.value = sdf.format(Date())
            delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Bg0)
            .border(width = 1.dp, color = BorderSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Surface1, RoundedCornerShape(6.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Always render the official logo asset
            VajraAppLogo(size = 32.dp)

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Info,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (timeState.value.isNotEmpty()) {
                Text(
                    text = timeState.value,
                    style = TechnicalValue.copy(fontSize = 10.sp, color = TextMuted),
                    maxLines = 1,
                    softWrap = false
                )
            }
            ModeBadge(isSynthetic = isSynthetic, compact = true)

            if (onHubClick != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surface1)
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .clickable { onHubClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HUB",
                        style = TechnicalValue.copy(fontSize = 9.sp, color = Info, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
