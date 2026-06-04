package com.jishiyong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.ui.theme.*
import com.jishiyong.viewmodel.StatisticsViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计分析") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 月份选择器
            MonthSelector(
                selectedMonth = uiState.selectedMonth,
                onMonthSelected = { viewModel.selectMonth(it) }
            )

            // 本月概览
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "本月概览",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            count = uiState.consumedThisMonth,
                            label = "已消费",
                            color = StatusFresh
                        )
                        StatItem(
                            count = uiState.wastedThisMonth,
                            label = "已浪费",
                            color = StatusCritical
                        )
                        StatItem(
                            count = uiState.activeItems,
                            label = "进行中",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 分类统计
            if (uiState.categoryStats.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "分类统计",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val maxCount = uiState.categoryStats.maxOfOrNull { it.count } ?: 1

                        uiState.categoryStats.forEach { stat ->
                            CategoryStatRow(
                                category = stat.category,
                                count = stat.count,
                                maxCount = maxCount
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 消费方式统计
            if (uiState.monthlyConsumeStats.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "消费方式统计",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        uiState.monthlyConsumeStats.forEach { stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (emoji, color) = when (stat.consumeType) {
                                        ConsumeType.USED_UP -> "✅" to StatusFresh
                                        ConsumeType.DISCARDED -> "🗑️" to StatusWarning
                                        ConsumeType.EXPIRED -> "❌" to StatusCritical
                                        ConsumeType.GIFTED -> "🎁" to MaterialTheme.colorScheme.primary
                                        null -> "❓" to StatusExpired
                                    }
                                    Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stat.consumeType?.displayName ?: "未知",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = "${stat.count} 件",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 浪费率
            if (uiState.consumedThisMonth > 0) {
                val wasteRate = (uiState.wastedThisMonth.toFloat() / uiState.consumedThisMonth * 100).toInt()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (wasteRate > 30) StatusCritical.copy(alpha = 0.1f)
                        else StatusFresh.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "本月浪费率",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (wasteRate > 30) "浪费较多，建议优化购买计划"
                                else "做得不错，继续保持！",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$wasteRate%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (wasteRate > 30) StatusCritical else StatusFresh
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthSelected(selectedMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = {
            val nextMonth = selectedMonth.plusMonths(1)
            if (!nextMonth.isAfter(YearMonth.now())) {
                onMonthSelected(nextMonth)
            }
        }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryStatRow(
    category: ItemCategory,
    count: Int,
    maxCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.icon,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(StatusFresh.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(count.toFloat() / maxCount)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatusFresh)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
    }
}
