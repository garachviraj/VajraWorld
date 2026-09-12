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
            .border(width = 1.dp, color = BorderColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            } else {
                VajraAppLogo(size = 32.dp)
            }

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = subtitle,
                    color = Info,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timeState.value.isNotEmpty()) {
                Text(
                    text = timeState.value,
                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextMuted)
                )
            }
            ModeBadge(isSynthetic = isSynthetic)

            if (onHubClick != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surface2)
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .clickable { onHubClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HUB",
                        style = TechnicalValue.copy(fontSize = 9.sp, color = Info, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
