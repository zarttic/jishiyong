package com.jishiyong.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jishiyong.speech.BaiduAsrSettings
import com.jishiyong.ui.components.FoldedPaperSurface
import com.jishiyong.ui.components.FreshBackdropStyle
import com.jishiyong.ui.components.FridgeDoorBackdrop
import com.jishiyong.ui.components.StatusPill
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.BrandPrimaryDark
import com.jishiyong.ui.theme.BrandSoft
import com.jishiyong.ui.theme.FoldPaper
import com.jishiyong.ui.theme.InkMuted
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.StatusCritical
import com.jishiyong.ui.theme.StatusFresh
import com.jishiyong.ui.theme.SurfaceClean
import com.jishiyong.ui.theme.SurfaceSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedBackdropStyle: FreshBackdropStyle,
    onBackdropStyleSelected: (FreshBackdropStyle) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baiduAsrConfigured = remember(context) {
        BaiduAsrSettings.load(context).isComplete
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "界面和语音配置",
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
                .padding(paddingValues),
            style = selectedBackdropStyle
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FoldedPaperSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceClean.copy(alpha = 0.9f),
                    borderColor = BrandPrimary.copy(alpha = 0.18f),
                    foldColor = BrandSoft
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "保鲜墙背景",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "首页保持清爽，背景方向放在这里调。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkMuted
                                )
                            }
                            StatusPill(
                                text = selectedBackdropStyle.displayName,
                                color = BrandPrimary
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FreshBackdropStyle.entries.forEach { style ->
                                BackdropSettingRow(
                                    style = style,
                                    selected = selectedBackdropStyle == style,
                                    onClick = { onBackdropStyleSelected(style) }
                                )
                            }
                        }
                    }
                }

                FoldedPaperSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceClean.copy(alpha = 0.86f),
                    borderColor = OutlineSoft.copy(alpha = 0.9f),
                    foldColor = FoldPaper
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "语音",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        AsrStatusRow(configured = baiduAsrConfigured)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun BackdropSettingRow(
    style: FreshBackdropStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) BrandPrimary.copy(alpha = 0.1f) else SurfaceSoft.copy(alpha = 0.72f),
        border = BorderStroke(
            1.dp,
            if (selected) BrandPrimary.copy(alpha = 0.42f) else OutlineSoft.copy(alpha = 0.72f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackdropSwatch(
                style = style,
                modifier = Modifier.size(width = 46.dp, height = 34.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = style.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) BrandPrimaryDark else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = style.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "当前背景",
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = InkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BackdropSwatch(
    style: FreshBackdropStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = 10.dp.toPx()
        val brush = when (style) {
            FreshBackdropStyle.ColdMist -> Brush.linearGradient(
                colors = listOf(Color(0xFFEFF6F2), Color.White, Color(0xFFEFE6D4))
            )
            FreshBackdropStyle.MagneticWall -> Brush.linearGradient(
                colors = listOf(Color(0xFFF3EEE2), Color(0xFFEDF4EF), Color(0xFFFFFDF7))
            )
            FreshBackdropStyle.PaperBoard -> Brush.linearGradient(
                colors = listOf(Color(0xFFF8F1DF), Color(0xFFFFF8E8), Color(0xFFF7F3E9))
            )
            FreshBackdropStyle.KitchenTile -> Brush.linearGradient(
                colors = listOf(Color(0xFFF7F5ED), Color(0xFFEDF4EF))
            )
            FreshBackdropStyle.ShelfBoard -> Brush.verticalGradient(
                colors = listOf(Color(0xFFF4EEE0), Color(0xFFF8F3E7), Color(0xFFEBE1CD))
            )
        }
        drawRoundRect(
            brush = brush,
            cornerRadius = CornerRadius(radius, radius)
        )

        when (style) {
            FreshBackdropStyle.ColdMist -> {
                drawCircle(
                    color = Color.White.copy(alpha = 0.72f),
                    radius = size.width * 0.26f,
                    center = Offset(size.width * 0.26f, size.height * 0.28f)
                )
            }
            FreshBackdropStyle.PaperBoard -> {
                val step = 8.dp.toPx()
                var x = step
                while (x < size.width) {
                    drawLine(
                        color = OutlineSoft.copy(alpha = 0.32f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += step
                }
            }
            FreshBackdropStyle.KitchenTile -> {
                val step = 15.dp.toPx()
                var x = step
                while (x < size.width) {
                    drawLine(
                        color = InkMuted.copy(alpha = 0.2f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(
                        color = InkMuted.copy(alpha = 0.16f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += step
                }
            }
            FreshBackdropStyle.ShelfBoard -> {
                listOf(size.height * 0.36f, size.height * 0.68f).forEach { y ->
                    drawLine(
                        color = OutlineSoft.copy(alpha = 0.72f),
                        start = Offset(size.width * 0.1f, y),
                        end = Offset(size.width * 0.9f, y),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            FreshBackdropStyle.MagneticWall -> {
                val path = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.78f)
                    quadraticBezierTo(size.width * 0.52f, size.height * 0.56f, size.width * 0.92f, size.height * 0.72f)
                }
                drawPath(
                    path = path,
                    color = BrandPrimary.copy(alpha = 0.14f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        drawRoundRect(
            color = Color(0xFF18231F).copy(alpha = 0.08f),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun AsrStatusRow(configured: Boolean) {
    val statusColor = if (configured) StatusFresh else StatusCritical
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = statusColor.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(13.dp),
                color = statusColor.copy(alpha = 0.13f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "百度云语音",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (configured) "已随安装包配置，可作为系统语音不可用时的兜底。" else "未随安装包配置，发布流程会阻止这种包继续分发。",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
            }
            StatusPill(
                text = if (configured) "已配置" else "缺失",
                color = statusColor
            )
        }
    }
}
