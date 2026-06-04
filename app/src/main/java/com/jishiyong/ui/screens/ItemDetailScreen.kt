package com.jishiyong.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
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
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.components.*
import com.jishiyong.ui.theme.*
import com.jishiyong.util.DateUtils
import com.jishiyong.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeItems by viewModel.activeItems.collectAsState()
    val item = activeItems.find { it.id == itemId }

    if (item == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📦", style = MaterialTheme.typography.displaySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("物品不存在或已处理", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                GradientButton(text = "返回", onClick = onBack, modifier = Modifier.width(120.dp))
            }
        }
        return
    }

    val expiryStatus = viewModel.getExpiryStatus(item)
    val daysUntilExpiry = viewModel.getDaysUntilExpiry(item)

    val statusGradient = when (expiryStatus) {
        ExpiryStatus.FRESH -> GradientForest
        ExpiryStatus.EXPIRING_WARNING -> Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFFD54F)))
        ExpiryStatus.EXPIRING_SOON -> Brush.linearGradient(listOf(Color(0xFFFF6D00), Color(0xFFFFAB40)))
        ExpiryStatus.EXPIRING_CRITICAL -> GradientSunset
        ExpiryStatus.EXPIRED -> Brush.linearGradient(listOf(Color(0xFF757575), Color(0xFF9E9E9E)))
    }

    var showConsumeMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            // 顶部渐变区域
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
                        Spacer(modifier = Modifier.weight(1f))

                        // 消费按钮
                        Box {
                            IconButton(
                                onClick = { showConsumeMenu = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(StatusFresh.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "完成",
                                    tint = StatusFresh
                                )
                            }
                            DropdownMenu(
                                expanded = showConsumeMenu,
                                onDismissRequest = { showConsumeMenu = false }
                            ) {
                                ConsumeType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.displayName) },
                                        onClick = {
                                            showConsumeMenu = false
                                            viewModel.markAsConsumed(item, type)
                                            onBack()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 删除按钮
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 状态卡片
                    GradientCard(
                        gradient = statusGradient,
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.category.icon,
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val statusText = when (expiryStatus) {
                                ExpiryStatus.FRESH -> "✅ 新鲜"
                                ExpiryStatus.EXPIRING_WARNING -> "⚠️ 即将过期"
                                ExpiryStatus.EXPIRING_SOON -> "🟠 临近过期"
                                ExpiryStatus.EXPIRING_CRITICAL -> "🔴 紧急"
                                ExpiryStatus.EXPIRED -> "❌ 已过期"
                            }

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (daysUntilExpiry < 0) "已过期 ${-daysUntilExpiry} 天"
                                else if (daysUntilExpiry == 0) "今天过期"
                                else "还有 $daysUntilExpiry 天过期",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 详细信息
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 信息卡片
                GlassCard(cornerRadius = 20.dp) {
                    Text(
                        text = "详细信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow(
                        icon = Icons.Rounded.Category,
                        label = "分类",
                        value = "${item.category.icon} ${item.category.displayName}"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "购买日期",
                        value = DateUtils.formatChinese(item.purchaseDate)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow(
                        icon = Icons.Rounded.EventBusy,
                        label = "过期日期",
                        value = DateUtils.formatChinese(item.expirationDate)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow(
                        icon = Icons.Rounded.Numbers,
                        label = "数量",
                        value = "${item.quantity} 件${if (item.usedQuantity > 0) " (已用 ${item.usedQuantity})" else ""}"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow(
                        icon = Icons.Rounded.Notifications,
                        label = "提醒设置",
                        value = item.reminderDays.joinToString(", ") { "提前${it}天" }
                    )
                    if (item.note.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            label = "备注",
                            value = item.note
                        )
                    }
                }

                // 使用进度（多数量物品）
                if (item.quantity > 1) {
                    GlassCard(cornerRadius = 20.dp) {
                        Text(
                            text = "使用进度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val progress = if (item.quantity > 0) {
                            item.usedQuantity.toFloat() / item.quantity
                        } else 0f

                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(500),
                            label = "progress"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "已用 ${item.usedQuantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "剩余 ${item.quantity - item.usedQuantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlineGradientButton(
                                text = "使用 1 件",
                                onClick = {
                                    viewModel.updateUsedQuantity(item, item.usedQuantity + 1)
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Add
                            )
                            if (item.usedQuantity > 0) {
                                OutlineGradientButton(
                                    text = "撤回",
                                    onClick = {
                                        viewModel.updateUsedQuantity(item, item.usedQuantity - 1)
                                    },
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Rounded.Remove
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除\"${item.name}\"吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
