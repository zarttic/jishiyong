package com.jishiyong.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.theme.*

@Composable
fun StatusBadge(
    status: ExpiryStatus,
    daysUntilExpiry: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (status) {
        ExpiryStatus.FRESH -> Triple(StatusFreshLight, StatusFresh, StatusFresh)
        ExpiryStatus.EXPIRING_WARNING -> Triple(StatusWarningLight, StatusWarning, StatusWarning)
        ExpiryStatus.EXPIRING_SOON -> Triple(StatusUrgentLight, StatusUrgent, StatusUrgent)
        ExpiryStatus.EXPIRING_CRITICAL -> Triple(StatusCriticalLight, StatusCritical, StatusCritical)
        ExpiryStatus.EXPIRED -> Triple(StatusExpiredLight, StatusExpired, StatusExpired)
    }

    val animatedDotColor by animateColorAsState(
        targetValue = dotColor,
        animationSpec = tween(500),
        label = "dot_color"
    )

    val text = when {
        daysUntilExpiry < 0 -> "已过期${-daysUntilExpiry}天"
        daysUntilExpiry == 0 -> "今天过期"
        daysUntilExpiry == 1 -> "明天过期"
        else -> "${daysUntilExpiry}天后过期"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 动态圆点指示器
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedDotColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CategoryChip(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
