package com.jishiyong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.ui.components.*
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(60.dp))

                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "统计分析",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 月份选择器
                MonthSelector(
                    selectedMonth = uiState.selectedMonth,
                    onMonthSelected = { viewModel.selectMonth(it) }
                )

                // 本月概览卡片
                GradientCard(
                    gradient = GradientPrimary,
                    cornerRadius = 24.dp
                ) {
                    Text(
                        text = "本月概览",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            count = uiState.consumedThisMonth,
                            label = "已消费",
                            color = Color.White
                        )
                        StatItem(
                            count = uiState.wastedThisMonth,
                            label = "已浪费",
                            color = Color.White
                        )
                        StatItem(
                            count = uiState.activeItems,
                            label = "进行中",
                            color = Color.White
                        )
                    }
                }

                // 分类统计
                if (uiState.categoryStats.isNotEmpty()) {
                    GlassCard(cornerRadius = 20.dp) {
                        Text(
                            text = "分类统计",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val maxCount = uiState.categoryStats.maxOfOrNull { it.count } ?: 1

                        uiState.categoryStats.forEach { stat ->
                            CategoryStatRow(
                                category = stat.category,
                                count = stat.count,
                                maxCount = maxCount
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // 消费方式统计
                if (uiState.monthlyConsumeStats.isNotEmpty()) {
                    GlassCard(cornerRadius = 20.dp) {
                        Text(
                            text = "消费方式",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        uiState.monthlyConsumeStats.forEach { stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val emoji = when (stat.consumeType) {
                                        ConsumeType.USED_UP -> "✅"
                                        ConsumeType.DISCARDED -> "🗑️"
                                        ConsumeType.EXPIRED -> "❌"
                                        ConsumeType.GIFTED -> "🎁"
                                        null -> "❓"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(
                                        text = stat.consumeType?.displayName ?: "未知",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${stat.count} 件",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 浪费率
                if (uiState.consumedThisMonth > 0) {
                    val wasteRate = (uiState.wastedThisMonth.toFloat() / uiState.consumedThisMonth * 100).toInt()
                    val gradient = if (wasteRate > 30) GradientSunset else GradientForest

                    GradientCard(
                        gradient = gradient,
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "本月浪费率",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (wasteRate > 30) "浪费较多，建议优化" else "做得不错！",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = "$wasteRate%",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = NumberFontFamily,
                                    letterSpacing = (-2).sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
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
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "上个月")
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
            Icon(Icons.Rounded.ChevronRight, contentDescription = "下个月")
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = NumberFontFamily,
                letterSpacing = (-1).sp
            ),
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = category.icon, style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            text = category.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(count.toFloat() / maxCount)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GradientPrimary)
            )
        }

        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = NumberFontFamily
            ),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
    }
}
