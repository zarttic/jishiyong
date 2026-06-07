package com.jishiyong.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jishiyong.data.db.dao.CategoryStat
import com.jishiyong.data.db.dao.ConsumeStat
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.ui.components.AssistantFace
import com.jishiyong.ui.components.CategoryStamp
import com.jishiyong.ui.components.FoldedPaperSurface
import com.jishiyong.ui.components.FreshBackdropStyle
import com.jishiyong.ui.components.FridgeDoorBackdrop
import com.jishiyong.ui.components.categoryColor
import com.jishiyong.ui.components.nullableConsumeColor
import com.jishiyong.ui.components.nullableConsumeIcon
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.FoldPaper
import com.jishiyong.ui.theme.InkMuted
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.StatusCritical
import com.jishiyong.ui.theme.StatusFresh
import com.jishiyong.ui.theme.SurfaceClean
import com.jishiyong.ui.theme.SurfaceSoft
import com.jishiyong.viewmodel.StatisticsViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit,
    backdropStyle: FreshBackdropStyle = FreshBackdropStyle.ColdMist,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val wasteRate = if (uiState.consumedThisMonth > 0) {
        (uiState.wastedThisMonth.toFloat() / uiState.consumedThisMonth * 100).toInt()
    } else {
        0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "少浪费报告",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "像月度小收据一样看用掉和浪费",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        FridgeDoorBackdrop(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            style = backdropStyle
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MonthReceipt(
                    selectedMonth = uiState.selectedMonth,
                    wasteRate = wasteRate,
                    added = uiState.totalItems,
                    consumed = uiState.consumedThisMonth,
                    wasted = uiState.wastedThisMonth,
                    active = uiState.activeItems,
                    expired = uiState.expiredItems,
                    categoryStats = uiState.categoryStats,
                    consumeStats = uiState.monthlyConsumeStats,
                    onMonthSelected = viewModel::selectMonth
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MonthReceipt(
    selectedMonth: YearMonth,
    wasteRate: Int,
    added: Int,
    consumed: Int,
    wasted: Int,
    active: Int,
    expired: Int,
    categoryStats: List<CategoryStat>,
    consumeStats: List<ConsumeStat>,
    onMonthSelected: (YearMonth) -> Unit
) {
    val statusColor = if (wasteRate > 30) StatusCritical else BrandPrimary
    FoldedPaperSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
        color = SurfaceClean.copy(alpha = 0.92f),
        borderColor = OutlineSoft.copy(alpha = 0.9f),
        foldColor = FoldPaper
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            ReceiptTearEdge()
            MonthSelector(
                selectedMonth = selectedMonth,
                onMonthSelected = onMonthSelected
            )

            ReceiptDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (wasteRate > 30) "本月需要收紧" else "本月情况稳定",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when {
                            wasteRate > 30 -> "临期处理偏晚，下次可以少量多次补货。"
                            expired > 0 -> "当前还有 $expired 件已过期，先处理这批。"
                            else -> "用掉和入库节奏还算稳。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$wasteRate%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReceiptMetric(
                    label = "入库",
                    count = added,
                    icon = Icons.Default.AddCircle,
                    color = BrandPrimary,
                    modifier = Modifier.weight(1f)
                )
                ReceiptMetric(
                    label = "用掉",
                    count = consumed,
                    icon = Icons.Default.Verified,
                    color = StatusFresh,
                    modifier = Modifier.weight(1f)
                )
                ReceiptMetric(
                    label = "浪费",
                    count = wasted,
                    icon = Icons.Default.TrendingDown,
                    color = StatusCritical,
                    modifier = Modifier.weight(1f)
                )
            }
            InventorySnapshotLine(active = active, expired = expired)

            ReceiptDivider()

            ReceiptSectionTitle("入库分类")
            if (categoryStats.isEmpty()) {
                EmptyReceiptLine("这个月还没有新增物品")
            } else {
                val maxCount = categoryStats.maxOfOrNull { it.count } ?: 1
                categoryStats.forEach { stat ->
                    CategoryReceiptLine(
                        category = stat.category,
                        count = stat.count,
                        maxCount = maxCount
                    )
                }
            }

            ReceiptDivider()

            ReceiptSectionTitle("处理方式")
            if (consumeStats.isEmpty()) {
                EmptyReceiptLine("这个月还没有处理记录")
            } else {
                consumeStats.forEach { stat ->
                    ConsumeReceiptLine(
                        consumeType = stat.consumeType,
                        count = stat.count
                    )
                }
            }

            ReceiptDivider()

            ReceiptSuggestion(
                message = reportSuggestion(wasteRate, categoryStats.firstOrNull()?.category)
            )
        }
    }
}

@Composable
private fun ReceiptTearEdge() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(12) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .background(FoldPaper.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun InventorySnapshotLine(
    active: Int,
    expired: Int
) {
    val text = if (expired > 0) {
        "当前库存 $active 件，其中 $expired 件已过期"
    } else {
        "当前库存 $active 件，没有未处理过期物品"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthSelected(selectedMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "月度收据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = {
                val nextMonth = selectedMonth.plusMonths(1)
                if (!nextMonth.isAfter(YearMonth.now())) {
                    onMonthSelected(nextMonth)
                }
            },
            enabled = !selectedMonth.plusMonths(1).isAfter(YearMonth.now())
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
        }
    }
}

@Composable
private fun ReceiptMetric(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSoft,
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CategoryStamp(
                icon = icon,
                color = color,
                size = 34.dp
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = InkMuted
            )
        }
    }
}

@Composable
private fun ReceiptDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(18) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(OutlineSoft.copy(alpha = 0.78f))
            )
        }
    }
}

@Composable
private fun ReceiptSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun EmptyReceiptLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReceiptSuggestion(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AssistantFace(boxSize = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "小用建议",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = BrandPrimary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryReceiptLine(
    category: ItemCategory,
    count: Int,
    maxCount: Int
) {
    val color = category.categoryColor()
    ReceiptLine(
        label = category.displayName,
        countText = "$count",
        color = color,
        fraction = count.toFloat() / maxCount.coerceAtLeast(1),
        leading = {
            CategoryStamp(category = category, size = 34.dp)
        }
    )
}

@Composable
private fun ConsumeReceiptLine(
    consumeType: ConsumeType?,
    count: Int
) {
    val color = nullableConsumeColor(consumeType)
    ReceiptLine(
        label = consumeType?.displayName ?: "未知",
        countText = "$count 件",
        color = color,
        fraction = 1f,
        leading = {
            CategoryStamp(
                icon = nullableConsumeIcon(consumeType),
                color = color,
                size = 34.dp
            )
        }
    )
}

@Composable
private fun ReceiptLine(
    label: String,
    countText: String,
    color: Color,
    fraction: Float,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(color.copy(alpha = 0.13f), RoundedCornerShape(999.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                        .background(color, RoundedCornerShape(999.dp))
                )
            }
        }
    }
}

private fun reportSuggestion(
    wasteRate: Int,
    topCategory: ItemCategory?
): String {
    return if (wasteRate > 30) {
        "${topCategory?.displayName ?: "临期"}类处理有点集中，下次少量多次补货。"
    } else {
        "${topCategory?.displayName ?: "库存"}节奏稳定，继续先看临期标签。"
    }
}
