package com.jishiyong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.components.AssistantFace
import com.jishiyong.ui.components.AssistantNote
import com.jishiyong.ui.components.FoldedPaperSurface
import com.jishiyong.ui.components.FreshnessLabelCard
import com.jishiyong.ui.components.FridgeDoorBackdrop
import com.jishiyong.ui.components.remainingDaysLabel
import com.jishiyong.ui.components.statusColor
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.BrandSoft
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.StatusCritical
import com.jishiyong.ui.theme.SurfaceClean
import com.jishiyong.util.DateUtils
import com.jishiyong.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeItems by viewModel.activeItems.collectAsStateWithLifecycle()
    var inspectedItemIds by remember { mutableStateOf(emptySet<Long>()) }
    var roundTotal by remember { mutableStateOf(0) }

    val allInspectionItems = activeItems
        .sortedWith(
            compareBy<Item> { viewModel.getDaysUntilExpiry(it) }
                .thenBy { it.expirationDate }
                .thenBy { it.name }
        )
        .filter { viewModel.getDaysUntilExpiry(it) <= 7 }
    val allInspectionItemKey = allInspectionItems.joinToString(separator = ",") { it.id.toString() }

    LaunchedEffect(allInspectionItemKey) {
        val activeIds = allInspectionItems.map { it.id }.toSet()
        inspectedItemIds = inspectedItemIds.intersect(activeIds)
        if (inspectedItemIds.isEmpty()) {
            roundTotal = allInspectionItems.size
        }
    }

    val inspectionItems = allInspectionItems
        .filterNot { it.id in inspectedItemIds }

    val currentItem = inspectionItems.firstOrNull()
    val inspectedCount = inspectedItemIds.size
    val totalInRound = roundTotal.coerceAtLeast(inspectedCount + inspectionItems.size)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "小用巡视",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "逐项盘点，不做聊天页",
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
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    inspectionItems.isEmpty() && inspectedCount == 0 -> EmptyInspection(
                        title = "今天没有需要巡视的标签",
                        message = "7 天内到期或已经过期的物品会出现在这里。"
                    )
                    currentItem == null -> EmptyInspection(
                        title = "这轮巡视完成",
                        message = "刚刚这批临期标签已经看过了。",
                        actionText = "再巡视一遍",
                        onAction = {
                            inspectedItemIds = emptySet()
                            roundTotal = allInspectionItems.size
                        }
                    )
                    else -> InspectionCard(
                        item = currentItem,
                        index = inspectedCount,
                        total = totalInRound,
                        expiryStatus = viewModel.getExpiryStatus(currentItem),
                        daysUntilExpiry = viewModel.getDaysUntilExpiry(currentItem),
                        onUsed = {
                            viewModel.updateUsedQuantity(currentItem, currentItem.usedQuantity + 1)
                            inspectedItemIds = inspectedItemIds + currentItem.id
                        },
                        onKept = {
                            inspectedItemIds = inspectedItemIds + currentItem.id
                        },
                        onDiscarded = {
                            viewModel.markAsConsumed(currentItem, ConsumeType.DISCARDED)
                            inspectedItemIds = inspectedItemIds + currentItem.id
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InspectionCard(
    item: Item,
    index: Int,
    total: Int,
    expiryStatus: ExpiryStatus,
    daysUntilExpiry: Int,
    onUsed: () -> Unit,
    onKept: () -> Unit,
    onDiscarded: () -> Unit
) {
    val statusColor = expiryStatus.statusColor()
    val remaining = (item.quantity - item.usedQuantity).coerceAtLeast(0)

    FoldedPaperSurface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceClean.copy(alpha = 0.92f),
        borderColor = statusColor.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AssistantNote(
                title = "小用巡视",
                message = "第 ${index + 1} / $total 件，我只问这一张标签。"
            )

            InspectionProgress(
                progress = ((index + 1).toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f),
                color = statusColor
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${item.name} 还在吗？",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = listOf(
                        item.category.displayName,
                        "当前剩 $remaining 件",
                        "${DateUtils.formatShort(item.expirationDate)}到期"
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FreshnessLabelCard(
                item = item,
                expiryStatus = expiryStatus,
                daysUntilExpiry = daysUntilExpiry,
                large = true
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InspectionButton(
                    text = "用掉了",
                    icon = Icons.Default.CheckCircle,
                    color = BrandPrimary,
                    onClick = onUsed,
                    modifier = Modifier.fillMaxWidth()
                )
                InspectionButton(
                    text = "还在",
                    icon = Icons.Default.Inventory2,
                    color = statusColor,
                    onClick = onKept,
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true
                )
                InspectionButton(
                    text = "丢掉了",
                    icon = Icons.Default.DeleteOutline,
                    color = StatusCritical,
                    onClick = onDiscarded,
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true
                )
            }

            Text(
                text = "状态：${remainingDaysLabel(daysUntilExpiry)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun InspectionProgress(
    progress: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .background(BrandSoft, RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(9.dp)
                .background(color, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun InspectionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text, color = color, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyInspection(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    FoldedPaperSurface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceClean.copy(alpha = 0.78f),
        borderColor = OutlineSoft.copy(alpha = 0.86f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AssistantFace(boxSize = 54.dp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Button(onClick = onAction, shape = RoundedCornerShape(16.dp)) {
                    Text(actionText)
                }
            }
        }
    }
}
