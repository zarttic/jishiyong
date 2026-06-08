package com.jishiyong.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.components.AssistantFace
import com.jishiyong.ui.components.CategoryStamp
import com.jishiyong.ui.components.FoldedPaperSurface
import com.jishiyong.ui.components.FreshBackdropStyle
import com.jishiyong.ui.components.FreshCornerLarge
import com.jishiyong.ui.components.FreshnessLabelCard
import com.jishiyong.ui.components.FreshnessTicks
import com.jishiyong.ui.components.FridgeDoorBackdrop
import com.jishiyong.ui.components.categoryColor
import com.jishiyong.ui.components.consumeColor
import com.jishiyong.ui.components.consumeIcon
import com.jishiyong.ui.components.freshnessTickCount
import com.jishiyong.ui.components.remainingDaysLabel
import com.jishiyong.ui.components.statusColor
import com.jishiyong.ui.components.statusLabel
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.BrandSoft
import com.jishiyong.ui.theme.FoldPaper
import com.jishiyong.ui.theme.InkMuted
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.StatusUrgent
import com.jishiyong.ui.theme.StatusWarning
import com.jishiyong.ui.theme.SurfaceClean
import com.jishiyong.util.DateUtils
import com.jishiyong.viewmodel.ItemDetailUiState
import com.jishiyong.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    backdropStyle: FreshBackdropStyle = FreshBackdropStyle.ColdMist,
    modifier: Modifier = Modifier
) {
    val detailState by remember(itemId) {
        viewModel.getItemDetailState(itemId)
    }.collectAsStateWithLifecycle(initialValue = ItemDetailUiState.Loading)
    val item = when (val state = detailState) {
        ItemDetailUiState.Loading -> {
            LoadingItem()
            return
        }
        ItemDetailUiState.Missing -> {
            MissingItem(onBack = onBack)
            return
        }
        is ItemDetailUiState.Loaded -> state.item
    }

    val expiryStatus = viewModel.getExpiryStatus(item)
    val daysUntilExpiry = viewModel.getDaysUntilExpiry(item)
    val statusColor = expiryStatus.statusColor()

    var showConsumeMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "物品档案袋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = item.name,
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
                actions = {
                    Box {
                        IconButton(onClick = { showConsumeMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "标记已处理",
                                tint = BrandPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showConsumeMenu,
                            onDismissRequest = { showConsumeMenu = false }
                        ) {
                            ConsumeType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = type.consumeIcon(),
                                            contentDescription = null,
                                            tint = type.consumeColor()
                                        )
                                    },
                                    onClick = {
                                        showConsumeMenu = false
                                        viewModel.markAsConsumed(item, type, onSuccess = onBack)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
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
                FolderHero(
                    item = item,
                    statusText = expiryStatus.statusLabel(),
                    remainingText = remainingDaysLabel(daysUntilExpiry),
                    statusColor = statusColor,
                    expiryStatus = expiryStatus,
                    daysUntilExpiry = daysUntilExpiry,
                    tickCount = freshnessTickCount(daysUntilExpiry, expiryStatus)
                )

                DetailPaperSection(title = "档案信息") {
                    DetailRow(
                        icon = Icons.Default.Category,
                        label = "分类",
                        value = item.category.displayName,
                        color = item.category.categoryColor()
                    )
                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "购买日期",
                        value = DateUtils.formatChinese(item.purchaseDate)
                    )
                    DetailRow(
                        icon = Icons.Default.EventBusy,
                        label = "到期日期",
                        value = DateUtils.formatChinese(item.expirationDate),
                        color = statusColor
                    )
                    DetailRow(
                        icon = Icons.Default.Numbers,
                        label = "数量",
                        value = "${item.quantity} 件${if (item.usedQuantity > 0) "，已用 ${item.usedQuantity}" else ""}"
                    )
                    DetailRow(
                        icon = Icons.Default.Notifications,
                        label = "提醒",
                        value = item.reminderDays.joinToString("、") { "提前${it}天" }
                    )
                    if (item.note.isNotBlank()) {
                        DetailRow(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            label = "备注",
                            value = item.note
                        )
                    }
                }

                if (item.quantity > 1) {
                    UsageTicksSection(
                        item = item,
                        onAddOne = {
                            viewModel.adjustUsedQuantity(item, 1)
                        },
                        onUndo = {
                            viewModel.adjustUsedQuantity(item, -1)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("撕下标签") },
            text = { Text("确定删除“${item.name}”？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item, onSuccess = onBack)
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = SurfaceClean,
            shape = FreshCornerLarge
        )
    }
}

@Composable
private fun FolderHero(
    item: Item,
    statusText: String,
    remainingText: String,
    statusColor: Color,
    expiryStatus: ExpiryStatus,
    daysUntilExpiry: Int,
    tickCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 26.dp,
            topEnd = 26.dp,
            bottomEnd = 26.dp,
            bottomStart = 10.dp
        ),
        color = SurfaceClean.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, OutlineSoft.copy(alpha = 0.9f))
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(142.dp)
                        .height(34.dp)
                        .background(
                            color = FoldPaper,
                            shape = RoundedCornerShape(bottomEnd = 18.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(OutlineSoft.copy(alpha = 0.62f))
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryStamp(category = item.category, size = 52.dp)
                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )

                Box(modifier = Modifier.padding(horizontal = 2.dp)) {
                    FreshnessLabelCard(
                        item = item,
                        expiryStatus = expiryStatus,
                        daysUntilExpiry = daysUntilExpiry,
                        large = true
                    )
                }

                Timeline(
                    startText = DateUtils.formatShort(item.purchaseDate),
                    endText = DateUtils.formatShort(item.expirationDate)
                )

                FreshnessTicks(
                    activeTicks = tickCount,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun Timeline(
    startText: String,
    endText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = startText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = InkMuted
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(BrandPrimary, StatusWarning, StatusUrgent)
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
        )
        Text(
            text = endText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = InkMuted
        )
    }
}

@Composable
private fun DetailPaperSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    FoldedPaperSurface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceClean.copy(alpha = 0.92f),
        borderColor = OutlineSoft.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun UsageTicksSection(
    item: Item,
    onAddOne: () -> Unit,
    onUndo: () -> Unit
) {
    DetailPaperSection(title = "数量使用刻度") {
        Text(
            text = "已用 ${item.usedQuantity} / ${item.quantity}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = BrandPrimary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(item.quantity) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                        .background(
                            color = if (index < item.usedQuantity) BrandPrimary else BrandSoft,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddOne,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("使用 1 件")
            }
            if (item.usedQuantity > 0) {
                OutlinedButton(
                    onClick = onUndo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("撤回")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color = BrandPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        CategoryStamp(
            icon = icon,
            color = color,
            size = 34.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AssistantFace(boxSize = 58.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在翻档案袋")
        }
    }
}

@Composable
private fun MissingItem(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            AssistantFace(boxSize = 58.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "这张标签不在了",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack, shape = RoundedCornerShape(16.dp)) {
                Text("返回保鲜墙")
            }
        }
    }
}
