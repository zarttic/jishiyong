package com.jishiyong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.data.repository.ItemRepository
import com.jishiyong.ui.theme.*
import com.jishiyong.util.DateUtils
import com.jishiyong.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        // 物品不存在或已消费
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📦", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("物品不存在或已处理")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("返回")
                }
            }
        }
        return
    }

    val expiryStatus = viewModel.getExpiryStatus(item)
    val daysUntilExpiry = viewModel.getDaysUntilExpiry(item)

    val statusColor = when (expiryStatus) {
        ExpiryStatus.FRESH -> StatusFresh
        ExpiryStatus.EXPIRING_WARNING -> StatusWarning
        ExpiryStatus.EXPIRING_SOON -> StatusUrgent
        ExpiryStatus.EXPIRING_CRITICAL -> StatusCritical
        ExpiryStatus.EXPIRED -> StatusExpired
    }

    var showConsumeMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("物品详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showConsumeMenu = true }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "标记已消费")
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
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
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
            // 状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = statusColor.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.category.icon,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 状态标签
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor
                    ) {
                        Text(
                            text = when (expiryStatus) {
                                ExpiryStatus.FRESH -> "✅ 新鲜"
                                ExpiryStatus.EXPIRING_WARNING -> "⚠️ 即将过期"
                                ExpiryStatus.EXPIRING_SOON -> "🟠 临近过期"
                                ExpiryStatus.EXPIRING_CRITICAL -> "🔴 紧急"
                                ExpiryStatus.EXPIRED -> "❌ 已过期"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (daysUntilExpiry < 0) "已过期 ${-daysUntilExpiry} 天"
                        else if (daysUntilExpiry == 0) "今天过期"
                        else "还有 $daysUntilExpiry 天过期",
                        style = MaterialTheme.typography.titleLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 详细信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(
                        icon = Icons.Default.Category,
                        label = "分类",
                        value = "${item.category.icon} ${item.category.displayName}"
                    )
                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "购买日期",
                        value = DateUtils.formatChinese(item.purchaseDate)
                    )
                    DetailRow(
                        icon = Icons.Default.EventBusy,
                        label = "过期日期",
                        value = DateUtils.formatChinese(item.expirationDate)
                    )
                    DetailRow(
                        icon = Icons.Default.Numbers,
                        label = "数量",
                        value = "${item.quantity} 件${if (item.usedQuantity > 0) " (已用 ${item.usedQuantity})" else ""}"
                    )
                    DetailRow(
                        icon = Icons.Default.Notifications,
                        label = "提醒设置",
                        value = item.reminderDays.joinToString(", ") { "提前${it}天" }
                    )
                    if (item.note.isNotBlank()) {
                        DetailRow(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            label = "备注",
                            value = item.note
                        )
                    }
                }
            }

            // 使用进度
            if (item.quantity > 1) {
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
                            text = "使用进度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val progress = if (item.quantity > 0) {
                            item.usedQuantity.toFloat() / item.quantity
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = StatusFresh,
                            trackColor = StatusFresh.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

                        // 使用+1按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateUsedQuantity(item, item.usedQuantity + 1)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("使用 1 件")
                            }
                            if (item.usedQuantity > 0) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateUsedQuantity(item, item.usedQuantity - 1)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("撤回")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除\"${item.name}\"吗？此操作不可撤销。") },
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
