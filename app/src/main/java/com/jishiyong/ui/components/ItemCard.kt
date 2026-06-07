package com.jishiyong.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.theme.BrandPrimary

@Composable
fun ItemCard(
    item: Item,
    expiryStatus: ExpiryStatus,
    daysUntilExpiry: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onConsume: (ConsumeType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConsumeMenu by remember { mutableStateOf(false) }

    FreshnessLabelCard(
        item = item,
        expiryStatus = expiryStatus,
        daysUntilExpiry = daysUntilExpiry,
        onClick = onClick,
        modifier = modifier,
        actions = {
            Box(modifier = Modifier.weight(1f)) {
                TagAction(
                    text = "处理",
                    icon = Icons.Default.CheckCircle,
                    color = BrandPrimary,
                    onClick = { showConsumeMenu = true },
                    modifier = Modifier.fillMaxWidth()
                )
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
                                onConsume(type)
                            }
                        )
                    }
                }
            }
            TagAction(
                text = "删除",
                icon = Icons.Default.DeleteOutline,
                color = MaterialTheme.colorScheme.error,
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    )
}

@Composable
private fun TagAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
