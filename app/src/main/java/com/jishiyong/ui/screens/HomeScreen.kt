package com.jishiyong.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.components.*
import com.jishiyong.ui.theme.*
import com.jishiyong.util.DateUtils
import com.jishiyong.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onStatsClick: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    isCheckingUpdate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activeItems by viewModel.activeItems.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val expiredCount by viewModel.expiredCount.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Item?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 顶部渐变区域 + 概览
            item {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "及时用",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "让每件物品都物尽其用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 搜索按钮
                                IconButton(
                                    onClick = { showSearch = !showSearch },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                                        contentDescription = if (showSearch) "关闭" else "搜索",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 统计按钮
                                IconButton(
                                    onClick = onStatsClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BarChart,
                                        contentDescription = "统计",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 检查更新按钮
                                IconButton(
                                    onClick = onCheckUpdate,
                                    enabled = !isCheckingUpdate,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (isCheckingUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.SystemUpdate,
                                            contentDescription = "检查更新",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 搜索栏（展开时显示）
                        AnimatedVisibility(
                            visible = showSearch,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("搜索物品名称或备注...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Rounded.Clear, contentDescription = "清除")
                                        }
                                    }
                                }
                            )
                        }

                        // 统计卡片
                        OverviewCards(
                            totalItems = totalCount,
                            activeItems = activeCount,
                            expiredItems = expiredCount
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // 分类筛选
            item {
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setCategory(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 列表标题
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的物品",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${activeItems.size} 件",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 物品列表
            if (activeItems.isEmpty()) {
                item {
                    EmptyState(
                        message = if (searchQuery.isNotEmpty()) "没有找到匹配的物品" else "还没有添加物品",
                        subtitle = if (searchQuery.isNotEmpty()) "试试其他关键词" else "点击右下角 + 添加第一件物品"
                    )
                }
            } else {
                items(
                    items = activeItems,
                    key = { it.id }
                ) { item ->
                    ModernItemCard(
                        item = item,
                        expiryStatus = viewModel.getExpiryStatus(item),
                        daysUntilExpiry = viewModel.getDaysUntilExpiry(item),
                        onClick = { onItemClick(item.id) },
                        onDelete = { showDeleteDialog = item },
                        onConsume = { viewModel.markAsConsumed(item, it) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 浮动添加按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "添加",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除\"${item.name}\"吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun OverviewCards(
    totalItems: Int,
    activeItems: Int,
    expiredItems: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 全部
        GradientCard(
            modifier = Modifier.weight(1f),
            gradient = GradientForest
        ) {
            Text(
                text = "$totalItems",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "全部物品",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // 进行中
        GradientCard(
            modifier = Modifier.weight(1f),
            gradient = GradientPrimary
        ) {
            Text(
                text = "$activeItems",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "进行中",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // 已过期
        GradientCard(
            modifier = Modifier.weight(1f),
            gradient = if (expiredItems > 0) GradientSunset else Brush.linearGradient(
                colors = listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
            )
        ) {
            Text(
                text = "$expiredItems",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "已过期",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ItemCategory?,
    onCategorySelected: (ItemCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        item {
            CategoryChip(
                emoji = "✨",
                label = "全部",
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(ItemCategory.entries.toList()) { category ->
            CategoryChip(
                emoji = category.icon,
                label = category.displayName,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun ModernItemCard(
    item: Item,
    expiryStatus: ExpiryStatus,
    daysUntilExpiry: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onConsume: (ConsumeType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConsumeMenu by remember { mutableStateOf(false) }

    ModernCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.category.icon,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 物品信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.quantity > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "×${item.quantity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(
                        status = expiryStatus,
                        daysUntilExpiry = daysUntilExpiry
                    )
                    Text(
                        text = DateUtils.formatShort(item.expirationDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 操作按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box {
                    IconButton(
                        onClick = { showConsumeMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "完成",
                            tint = StatusFresh,
                            modifier = Modifier.size(22.dp)
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
                                    onConsume(type)
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📦",
                style = MaterialTheme.typography.displaySmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
