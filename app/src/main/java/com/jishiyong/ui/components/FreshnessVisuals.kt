package com.jishiyong.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.BrandPrimaryDark
import com.jishiyong.ui.theme.BrandSoft
import com.jishiyong.ui.theme.Door
import com.jishiyong.ui.theme.FoldPaper
import com.jishiyong.ui.theme.InkMuted
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.Paper
import com.jishiyong.ui.theme.StatusCritical
import com.jishiyong.ui.theme.StatusExpired
import com.jishiyong.ui.theme.StatusFresh
import com.jishiyong.ui.theme.StatusWarning
import com.jishiyong.ui.theme.SurfaceClean
import com.jishiyong.ui.theme.SurfaceSoft
import com.jishiyong.util.DateUtils

val FreshCornerLarge = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomEnd = 24.dp,
    bottomStart = 8.dp
)

val FreshCornerMedium = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp,
    bottomStart = 7.dp
)

enum class FreshBackdropStyle(
    val displayName: String,
    val description: String
) {
    ColdMist("冷雾面", "更轻、更干净"),
    MagneticWall("磁吸墙", "保留生活感"),
    PaperBoard("纸贴板", "手账便签感"),
    KitchenTile("厨房砖", "生活场景强"),
    ShelfBoard("层板", "强调时间货架")
}

fun Modifier.fridgeDoorBackground(style: FreshBackdropStyle = FreshBackdropStyle.ColdMist): Modifier = this.background(
    brush = when (style) {
        FreshBackdropStyle.ColdMist -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEFF6F2),
                Color(0xFFF8F5EC),
                Color(0xFFEFE6D4)
            )
        )
        FreshBackdropStyle.MagneticWall -> Brush.verticalGradient(
            colors = listOf(Door, Paper)
        )
        FreshBackdropStyle.PaperBoard -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF8F1DF), Color(0xFFFFF8E8), Paper)
        )
        FreshBackdropStyle.KitchenTile -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF7F5ED), Color(0xFFEDF4EF))
        )
        FreshBackdropStyle.ShelfBoard -> Brush.verticalGradient(
            colors = listOf(Color(0xFFF4EEE0), Color(0xFFF8F3E7), Color(0xFFEBE1CD))
        )
    }
)

@Composable
fun FridgeDoorBackdrop(
    modifier: Modifier = Modifier,
    style: FreshBackdropStyle = FreshBackdropStyle.ColdMist,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fridgeDoorBackground(style)
            .drawWithContent {
                when (style) {
                    FreshBackdropStyle.ColdMist -> {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.52f),
                            radius = minOf(size.width, size.height) * 0.36f,
                            center = Offset(size.width * 0.22f, size.height * 0.04f)
                        )
                        drawCircle(
                            color = BrandPrimary.copy(alpha = 0.08f),
                            radius = minOf(size.width, size.height) * 0.34f,
                            center = Offset(size.width * 0.9f, size.height * 0.08f)
                        )
                    }
                    FreshBackdropStyle.PaperBoard -> {
                        val step = 22.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = OutlineSoft.copy(alpha = 0.16f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = OutlineSoft.copy(alpha = 0.12f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += step
                        }
                    }
                    FreshBackdropStyle.KitchenTile -> {
                        val step = 72.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = InkMuted.copy(alpha = 0.12f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = InkMuted.copy(alpha = 0.1f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += step
                        }
                    }
                    FreshBackdropStyle.ShelfBoard -> {
                        listOf(
                            size.height * 0.22f,
                            size.height * 0.47f,
                            size.height * 0.73f
                        ).forEach { y ->
                            drawLine(
                                color = OutlineSoft.copy(alpha = 0.34f),
                                start = Offset(18.dp.toPx(), y),
                                end = Offset(size.width - 18.dp.toPx(), y),
                                strokeWidth = 1.2.dp.toPx()
                            )
                        }
                    }
                    FreshBackdropStyle.MagneticWall -> Unit
                }
                if (style != FreshBackdropStyle.ColdMist) {
                    val lineAlpha = when (style) {
                        FreshBackdropStyle.ColdMist -> 0f
                        FreshBackdropStyle.MagneticWall -> 0.16f
                        FreshBackdropStyle.PaperBoard -> 0.14f
                        FreshBackdropStyle.KitchenTile -> 0.08f
                        FreshBackdropStyle.ShelfBoard -> 0.26f
                    }
                    val lineColor = OutlineSoft.copy(alpha = lineAlpha)
                    listOf(188.dp.toPx(), size.height - 184.dp.toPx()).forEach { y ->
                        if (y in 0f..size.height) {
                            drawLine(
                                color = lineColor,
                                start = Offset(22.dp.toPx(), y),
                                end = Offset(size.width - 22.dp.toPx(), y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
                drawContent()
            },
        content = content
    )
}

@Composable
fun BackdropStyleSelector(
    selectedStyle: FreshBackdropStyle,
    onStyleSelected: (FreshBackdropStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FreshBackdropStyle.entries.toList()) { style ->
            val selected = selectedStyle == style
            Surface(
                modifier = Modifier.clickable { onStyleSelected(style) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) BrandPrimary else SurfaceClean.copy(alpha = 0.74f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) BrandPrimary else OutlineSoft.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = style.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) Color.White else BrandPrimaryDark
                    )
                    Text(
                        text = style.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White.copy(alpha = 0.78f) else InkMuted
                    )
                }
            }
        }
    }
}

@Composable
fun FoldedPaperSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = FreshCornerLarge,
    color: Color = SurfaceClean,
    borderColor: Color = OutlineSoft.copy(alpha = 0.92f),
    foldColor: Color = FoldPaper,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box {
            content()
            FoldCorner(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp),
                color = foldColor,
                lineColor = borderColor
            )
        }
    }
}

@Composable
fun FoldCorner(
    modifier: Modifier = Modifier,
    color: Color = FoldPaper,
    lineColor: Color = OutlineSoft
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width, 0f)
            lineTo(size.width, size.height)
            quadraticBezierTo(size.width * 0.58f, size.height, 0f, 0f)
            close()
        }
        drawPath(path = path, color = color)
        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.8f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun CategoryStamp(
    category: ItemCategory,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    selected: Boolean = false,
    muted: Boolean = false
) {
    val categoryColor = category.categoryColor()
    val symbolColor = when {
        selected -> Color.White
        muted -> StatusExpired
        else -> categoryColor
    }
    val surfaceColor = when {
        selected -> categoryColor
        muted -> StatusExpired.copy(alpha = 0.08f)
        else -> categoryColor.copy(alpha = 0.12f)
    }
    val borderColor = when {
        selected -> categoryColor
        muted -> StatusExpired.copy(alpha = 0.18f)
        else -> categoryColor.copy(alpha = 0.22f)
    }

    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 3f),
        color = surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CategorySymbol(
                category = category,
                color = symbolColor,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
private fun CategorySymbol(
    category: ItemCategory,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.7.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val fill = color.copy(alpha = 0.14f)

        when (category) {
            ItemCategory.FOOD -> {
                val bowl = Path().apply {
                    moveTo(w * 0.2f, h * 0.55f)
                    cubicTo(w * 0.28f, h * 0.82f, w * 0.72f, h * 0.82f, w * 0.8f, h * 0.55f)
                    close()
                }
                drawPath(bowl, fill)
                drawPath(bowl, color, style = stroke)
                drawLine(color, Offset(w * 0.38f, h * 0.18f), Offset(w * 0.32f, h * 0.36f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.52f, h * 0.14f), Offset(w * 0.52f, h * 0.34f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.66f, h * 0.18f), Offset(w * 0.72f, h * 0.36f), strokeWidth, cap = StrokeCap.Round)
            }
            ItemCategory.DRINK -> {
                val carton = Path().apply {
                    moveTo(w * 0.3f, h * 0.32f)
                    lineTo(w * 0.42f, h * 0.18f)
                    lineTo(w * 0.68f, h * 0.18f)
                    lineTo(w * 0.8f, h * 0.32f)
                    lineTo(w * 0.8f, h * 0.82f)
                    lineTo(w * 0.3f, h * 0.82f)
                    close()
                }
                drawPath(carton, fill)
                drawPath(carton, color, style = stroke)
                drawLine(color, Offset(w * 0.42f, h * 0.18f), Offset(w * 0.54f, h * 0.32f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.32f, h * 0.45f), Offset(w * 0.78f, h * 0.45f), strokeWidth, cap = StrokeCap.Round)
            }
            ItemCategory.DAILY -> {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(w * 0.34f, h * 0.4f),
                    size = Size(w * 0.4f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.34f, h * 0.4f),
                    size = Size(w * 0.4f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.11f, w * 0.11f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.43f, h * 0.3f), Offset(w * 0.72f, h * 0.3f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.68f, h * 0.3f), Offset(w * 0.68f, h * 0.4f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.24f, h * 0.32f), Offset(w * 0.13f, h * 0.28f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.24f, h * 0.4f), Offset(w * 0.12f, h * 0.43f), strokeWidth, cap = StrokeCap.Round)
            }
            ItemCategory.MEDICINE -> {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(w * 0.18f, h * 0.39f),
                    size = Size(w * 0.64f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.39f),
                    size = Size(w * 0.64f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.5f, h * 0.4f), Offset(w * 0.5f, h * 0.66f), strokeWidth, cap = StrokeCap.Round)
            }
            ItemCategory.COSMETICS -> {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(w * 0.34f, h * 0.31f),
                    size = Size(w * 0.32f, h * 0.54f),
                    cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.34f, h * 0.31f),
                    size = Size(w * 0.32f, h * 0.54f),
                    cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
                    style = stroke
                )
                drawRoundRect(
                    color = color.copy(alpha = 0.2f),
                    topLeft = Offset(w * 0.4f, h * 0.16f),
                    size = Size(w * 0.2f, h * 0.16f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                )
                drawLine(color, Offset(w * 0.42f, h * 0.55f), Offset(w * 0.58f, h * 0.55f), strokeWidth, cap = StrokeCap.Round)
            }
            ItemCategory.ELECTRONICS -> {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(w * 0.3f, h * 0.15f),
                    size = Size(w * 0.4f, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.3f, h * 0.15f),
                    size = Size(w * 0.4f, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
                    style = stroke
                )
                drawCircle(color, radius = w * 0.025f, center = Offset(w * 0.5f, h * 0.77f))
            }
            ItemCategory.CLOTHING -> {
                val shirt = Path().apply {
                    moveTo(w * 0.28f, h * 0.33f)
                    lineTo(w * 0.4f, h * 0.22f)
                    lineTo(w * 0.46f, h * 0.31f)
                    lineTo(w * 0.54f, h * 0.31f)
                    lineTo(w * 0.6f, h * 0.22f)
                    lineTo(w * 0.72f, h * 0.33f)
                    lineTo(w * 0.64f, h * 0.46f)
                    lineTo(w * 0.61f, h * 0.43f)
                    lineTo(w * 0.61f, h * 0.82f)
                    lineTo(w * 0.39f, h * 0.82f)
                    lineTo(w * 0.39f, h * 0.43f)
                    lineTo(w * 0.36f, h * 0.46f)
                    close()
                }
                drawPath(shirt, fill)
                drawPath(shirt, color, style = stroke)
            }
            ItemCategory.OTHER -> {
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(w * 0.22f, h * 0.36f),
                    size = Size(w * 0.56f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.22f, h * 0.36f),
                    size = Size(w * 0.56f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.28f, h * 0.36f), Offset(w * 0.38f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.72f, h * 0.36f), Offset(w * 0.62f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.38f, h * 0.22f), Offset(w * 0.62f, h * 0.22f), strokeWidth, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun CategoryStamp(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 3f),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.13f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun FreshnessTicks(
    activeTicks: Int,
    color: Color,
    modifier: Modifier = Modifier,
    totalTicks: Int = 5
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalTicks) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(
                        color = if (index < activeTicks) color else color.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}

@Composable
fun FreshnessHeatBar(
    stableCount: Int,
    warningCount: Int,
    expiredCount: Int,
    modifier: Modifier = Modifier
) {
    val segments = listOf(
        stableCount to StatusFresh,
        warningCount to StatusWarning,
        expiredCount to StatusCritical
    ).filter { (count, _) -> count > 0 }

    if (segments.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(OutlineSoft.copy(alpha = 0.48f), RoundedCornerShape(999.dp))
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEach { (count, color) ->
                HeatSegment(
                    weight = count.toFloat(),
                    color = color
                )
            }
        }
    }
}

@Composable
private fun RowScope.HeatSegment(
    weight: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(weight)
            .background(color, RoundedCornerShape(999.dp))
    )
}

@Composable
fun FreshnessLabelCard(
    item: Item,
    expiryStatus: ExpiryStatus,
    daysUntilExpiry: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    large: Boolean = false
) {
    val statusColor = expiryStatus.statusColor()
    val isExpired = expiryStatus == ExpiryStatus.EXPIRED
    val contentAlpha = if (isExpired) 0.72f else 1f
    val labelColor = when (expiryStatus) {
        ExpiryStatus.FRESH -> SurfaceClean
        ExpiryStatus.EXPIRING_WARNING -> Color(0xFFFFFAEF)
        ExpiryStatus.EXPIRING_SOON,
        ExpiryStatus.EXPIRING_CRITICAL -> Color(0xFFFFF6EE)
        ExpiryStatus.EXPIRED -> Color(0xFFF0EBDD)
    }
    val activeTicks = freshnessTickCount(daysUntilExpiry, expiryStatus)
    val clickableModifier = onClick?.let { modifier.clickable(onClick = it) } ?: modifier
    val labelShape = RoundedCornerShape(
        topStart = if (large) 24.dp else 22.dp,
        topEnd = if (large) 24.dp else 22.dp,
        bottomEnd = if (large) 24.dp else 22.dp,
        bottomStart = if (large) 10.dp else 8.dp
    )
    val labelRotation = when {
        large -> -1.1f
        item.id % 3L == 1L -> -0.8f
        item.id % 3L == 2L -> 0.7f
        else -> 0.2f
    }

    FoldedPaperSurface(
        modifier = clickableModifier
            .rotate(labelRotation)
            .shadow(
                elevation = if (large) 10.dp else 6.dp,
                shape = labelShape,
                ambientColor = Color(0xFF372A12).copy(alpha = 0.08f),
                spotColor = Color(0xFF372A12).copy(alpha = 0.12f)
            ),
        shape = labelShape,
        color = labelColor,
        borderColor = statusColor.copy(alpha = if (expiryStatus == ExpiryStatus.FRESH) 0.22f else 0.36f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                .padding(if (large) 18.dp else 13.dp),
            verticalArrangement = Arrangement.spacedBy(if (large) 14.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryStamp(
                    category = item.category,
                    size = if (large) 52.dp else 42.dp,
                    muted = isExpired
                )
                StatusPill(
                    text = remainingDaysLabel(daysUntilExpiry),
                    color = statusColor
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name.ifBlank { "未命名标签" },
                    style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (large) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = labelMetaText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (large) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FreshnessTicks(
                activeTicks = activeTicks,
                color = statusColor,
                modifier = Modifier.fillMaxWidth()
            )

            actions?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = it
                )
            }
        }
    }
}

@Composable
fun AssistantNote(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    FoldedPaperSurface(
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomEnd = 22.dp,
            bottomStart = 8.dp
        ),
        color = Color(0xFFFDF8EA),
        borderColor = BrandPrimary.copy(alpha = 0.24f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistantFace()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandPrimaryDark
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun AssistantFace(
    modifier: Modifier = Modifier,
    boxSize: Dp = 38.dp
) {
    Surface(
        modifier = modifier.size(boxSize),
        shape = RoundedCornerShape(
            topStart = boxSize / 2.5f,
            topEnd = boxSize / 2.5f,
            bottomEnd = boxSize / 2.5f,
            bottomStart = boxSize / 6f
        ),
        color = BrandSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.24f))
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val eyeRadius = size.width * 0.065f
            drawCircle(
                color = BrandPrimary,
                radius = eyeRadius,
                center = Offset(size.width * 0.36f, size.height * 0.45f)
            )
            drawCircle(
                color = BrandPrimary,
                radius = eyeRadius,
                center = Offset(size.width * 0.64f, size.height * 0.45f)
            )
            drawArc(
                color = BrandPrimary.copy(alpha = 0.52f),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(size.width * 0.37f, size.height * 0.46f),
                size = Size(size.width * 0.26f, size.height * 0.22f),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
    }
}

@Composable
fun ShelfHeader(
    title: String,
    countText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(25.dp)
                    .height(5.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = countText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SearchPaperField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索名称、备注",
    trailing: @Composable (() -> Unit)? = null
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = trailing,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceClean.copy(alpha = 0.9f),
            unfocusedContainerColor = SurfaceClean.copy(alpha = 0.76f),
            focusedBorderColor = BrandPrimary.copy(alpha = 0.42f),
            unfocusedBorderColor = BrandPrimary.copy(alpha = 0.14f),
            cursorColor = BrandPrimary
        )
    )
}

@Composable
fun VoiceHandle(
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FoldedPaperSurface(
        modifier = modifier.clickable(enabled = !busy, onClick = onClick),
        shape = RoundedCornerShape(23.dp),
        color = SurfaceClean.copy(alpha = 0.96f),
        borderColor = BrandPrimary.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(14.dp),
                color = BrandPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (busy) "小用正在整理" else "说一句库存操作",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandPrimaryDark
                )
                Text(
                    text = "小用会先整理，再让你确认",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .background(BrandPrimary.copy(alpha = 0.34f), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
fun PaperSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    FoldedPaperSurface(
        modifier = modifier.fillMaxWidth(),
        shape = FreshCornerMedium,
        color = SurfaceClean.copy(alpha = 0.9f),
        borderColor = OutlineSoft.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            content()
        }
    }
}

fun freshnessTickCount(
    daysUntilExpiry: Int,
    expiryStatus: ExpiryStatus
): Int {
    return when {
        expiryStatus == ExpiryStatus.EXPIRED -> 1
        daysUntilExpiry <= 0 -> 1
        daysUntilExpiry == 1 -> 2
        daysUntilExpiry <= 3 -> 3
        daysUntilExpiry <= 7 -> 4
        else -> 5
    }
}

fun labelMetaText(item: Item): String {
    val available = (item.quantity - item.usedQuantity).coerceAtLeast(0)
    val noteText = item.note.trim().takeIf { it.isNotBlank() }
    val quantityText = if (item.quantity > 1) "剩余 $available / ${item.quantity}" else "数量 ${item.quantity}"
    return listOfNotNull(
        item.category.displayName,
        quantityText,
        noteText,
        "${DateUtils.formatShort(item.expirationDate)}到期"
    ).joinToString(" · ")
}
