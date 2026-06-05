package com.jishiyong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.update.UpdateCheckState
import com.jishiyong.ui.components.CategoryFilterChips
import com.jishiyong.ui.components.ExpiryOverviewCard
import com.jishiyong.ui.components.ItemCard
import com.jishiyong.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeItems by viewModel.activeItems.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val expiredCount by viewModel.expiredCount.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()
    val uriHandler = LocalUriHandler.current

    var showDeleteDialog by remember { mutableStateOf<Item?>(null) }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
    }

    val prioritizedItems = activeItems.sortedWith(
        compareBy<Item> { viewModel.getDaysUntilExpiry(it) }
            .thenBy { it.expirationDate }
            .thenBy { it.name }
    )
    val warningItems = activeItems.count {
        when (viewModel.getExpiryStatus(it)) {
            ExpiryStatus.FRESH, ExpiryStatus.EXPIRED -> false
            else -> true
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新增") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                HomeHeader(
                    activeCount = activeCount,
                    searchQuery = searchQuery,
                    onSearchChange = viewModel::setSearchQuery,
                    updateCheckState = updateCheckState,
                    onCheckUpdates = { viewModel.checkForUpdates(manual = true) },
                    onStatsClick = onStatsClick
                )
            }

            item {
                ExpiryOverviewCard(
                    totalItems = totalCount,
                    activeItems = activeCount,
                    expiredItems = expiredCount,
                    warningItems = warningItems
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                CategoryFilterChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = viewModel::setCategory
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                InventorySectionHeader(
                    count = prioritizedItems.size,
                    hasFilters = selectedCategory != null || searchQuery.isNotBlank()
                )
            }

            if (prioritizedItems.isEmpty()) {
                item {
                    EmptyState(
                        message = if (searchQuery.isNotBlank()) "没有匹配的物品" else "库存还是空的",
                        subtitle = if (searchQuery.isNotBlank()) "换个关键词或清除筛选条件" else "添加第一件物品后，这里会按到期时间排序"
                    )
                }
            } else {
                items(
                    items = prioritizedItems,
                    key = { it.id }
                ) { item ->
                    ItemCard(
                        item = item,
                        expiryStatus = viewModel.getExpiryStatus(item),
                        daysUntilExpiry = viewModel.getDaysUntilExpiry(item),
                        onClick = { onItemClick(item.id) },
                        onDelete = { showDeleteDialog = item },
                        onConsume = { viewModel.markAsConsumed(item, it) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除物品") },
            text = { Text("确定删除“${item.name}”？此操作不可撤销。") },
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

    when (val state = updateCheckState) {
        is UpdateCheckState.Available -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateCheckState,
                title = { Text("发现新版本") },
                text = {
                    Text("${state.update.releaseName} 已发布，可前往 GitHub 下载 APK。")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            uriHandler.openUri(state.update.downloadUrl)
                            viewModel.dismissUpdateCheckState()
                        }
                    ) {
                        Text("去下载")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissUpdateCheckState) {
                        Text("稍后")
                    }
                }
            )
        }
        is UpdateCheckState.UpToDate -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateCheckState,
                title = { Text("已是最新版本") },
                text = { Text("当前安装的版本已经是最新发布版本。") },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissUpdateCheckState) {
                        Text("知道了")
                    }
                }
            )
        }
        is UpdateCheckState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateCheckState,
                title = { Text("检查更新失败") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissUpdateCheckState) {
                        Text("知道了")
                    }
                }
            )
        }
        UpdateCheckState.Checking,
        UpdateCheckState.Idle -> Unit
    }
}

@Composable
private fun HomeHeader(
    activeCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    updateCheckState: UpdateCheckState,
    onCheckUpdates: () -> Unit,
    onStatsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "及时用",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (activeCount > 0) "$activeCount 件物品正在管理" else "从第一件物品开始管理",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(
                        onClick = onCheckUpdates,
                        enabled = updateCheckState !is UpdateCheckState.Checking
                    ) {
                        if (updateCheckState is UpdateCheckState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.SystemUpdateAlt,
                                contentDescription = "检查更新",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(onClick = onStatsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "统计",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("搜索名称、备注") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun InventorySectionHeader(
    count: Int,
    hasFilters: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (hasFilters) "筛选结果" else "待处理清单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "按到期时间从近到远排列",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "$count 件",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun EmptyState(
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
