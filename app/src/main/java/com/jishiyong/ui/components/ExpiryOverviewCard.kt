package com.jishiyong.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.StatusCritical
import com.jishiyong.ui.theme.StatusFresh
import com.jishiyong.ui.theme.StatusWarning
import com.jishiyong.ui.theme.SurfaceClean

@Composable
fun ExpiryOverviewCard(
    totalItems: Int,
    activeItems: Int,
    expiredItems: Int,
    warningItems: Int,
    modifier: Modifier = Modifier
) {
    val stableItems = (activeItems - warningItems).coerceAtLeast(0)

    FoldedPaperSurface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceClean.copy(alpha = 0.92f),
        borderColor = OutlineSoft.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "今日保鲜看板",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = BrandPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (warningItems > 0) "优先处理 $warningItems 件" else "今天不用急",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "保鲜墙上一共有 $totalItems 件物品。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$activeItems",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandPrimary
                )
            }
            FreshnessHeatBar(
                stableCount = stableItems,
                warningCount = warningItems,
                expiredCount = expiredItems
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniMetric("稳定", stableItems, StatusFresh)
                MiniMetric("临期", warningItems, StatusWarning)
                MiniMetric("过期", expiredItems, StatusCritical)
            }
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    count: Int,
    color: Color
) {
    Text(
        text = "$label $count",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}
