package com.jishiyong.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.SurfaceClean

@Composable
fun CategoryFilterChips(
    selectedCategory: ItemCategory?,
    onCategorySelected: (ItemCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        item {
            CategoryPaperChip(
                label = "全部",
                color = BrandPrimary,
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AllInbox,
                        contentDescription = null,
                        tint = if (selectedCategory == null) Color.White else BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        items(ItemCategory.entries.toList()) { category ->
            val selected = selectedCategory == category
            val color = category.categoryColor()
            CategoryPaperChip(
                label = category.displayName,
                color = color,
                selected = selected,
                onClick = { onCategorySelected(category) },
                icon = {
                    CategoryStamp(
                        category = category,
                        selected = selected,
                        size = 24.dp
                    )
                }
            )
        }
    }
}

@Composable
private fun CategoryPaperChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) color else SurfaceClean.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, if (selected) color else OutlineSoft.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else color
            )
        }
    }
}
