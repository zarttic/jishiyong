package com.jishiyong.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.theme.CategoryClothing
import com.jishiyong.ui.theme.CategoryCosmetics
import com.jishiyong.ui.theme.CategoryDaily
import com.jishiyong.ui.theme.CategoryDrink
import com.jishiyong.ui.theme.CategoryElectronics
import com.jishiyong.ui.theme.CategoryFood
import com.jishiyong.ui.theme.CategoryMedicine
import com.jishiyong.ui.theme.CategoryOther
import com.jishiyong.ui.theme.StatusCritical
import com.jishiyong.ui.theme.StatusExpired
import com.jishiyong.ui.theme.StatusFresh
import com.jishiyong.ui.theme.StatusUrgent
import com.jishiyong.ui.theme.StatusWarning

fun ExpiryStatus.statusColor(): Color = when (this) {
    ExpiryStatus.FRESH -> StatusFresh
    ExpiryStatus.EXPIRING_WARNING -> StatusWarning
    ExpiryStatus.EXPIRING_SOON -> StatusUrgent
    ExpiryStatus.EXPIRING_CRITICAL -> StatusCritical
    ExpiryStatus.EXPIRED -> StatusExpired
}

fun ExpiryStatus.statusLabel(): String = when (this) {
    ExpiryStatus.FRESH -> "状态稳定"
    ExpiryStatus.EXPIRING_WARNING -> "需要关注"
    ExpiryStatus.EXPIRING_SOON -> "尽快安排"
    ExpiryStatus.EXPIRING_CRITICAL -> "优先处理"
    ExpiryStatus.EXPIRED -> "已经过期"
}

fun remainingDaysLabel(daysUntilExpiry: Int): String = when {
    daysUntilExpiry < 0 -> "过期 ${-daysUntilExpiry} 天"
    daysUntilExpiry == 0 -> "今天到期"
    daysUntilExpiry == 1 -> "明天到期"
    else -> "剩余 $daysUntilExpiry 天"
}

fun ItemCategory.categoryColor(): Color = when (this) {
    ItemCategory.FOOD -> CategoryFood
    ItemCategory.DRINK -> CategoryDrink
    ItemCategory.DAILY -> CategoryDaily
    ItemCategory.MEDICINE -> CategoryMedicine
    ItemCategory.COSMETICS -> CategoryCosmetics
    ItemCategory.ELECTRONICS -> CategoryElectronics
    ItemCategory.CLOTHING -> CategoryClothing
    ItemCategory.OTHER -> CategoryOther
}

fun ItemCategory.categoryIcon(): ImageVector = when (this) {
    ItemCategory.FOOD -> Icons.Default.Restaurant
    ItemCategory.DRINK -> Icons.Default.LocalDrink
    ItemCategory.DAILY -> Icons.Default.CleaningServices
    ItemCategory.MEDICINE -> Icons.Default.Medication
    ItemCategory.COSMETICS -> Icons.Default.Face
    ItemCategory.ELECTRONICS -> Icons.Default.Devices
    ItemCategory.CLOTHING -> Icons.Default.Checkroom
    ItemCategory.OTHER -> Icons.Default.Inventory2
}

fun ConsumeType.consumeIcon(): ImageVector = when (this) {
    ConsumeType.USED_UP -> Icons.Default.CheckCircle
    ConsumeType.DISCARDED -> Icons.Default.DeleteSweep
    ConsumeType.EXPIRED -> Icons.Default.Report
    ConsumeType.GIFTED -> Icons.Default.Redeem
}

fun ConsumeType.consumeColor(): Color = when (this) {
    ConsumeType.USED_UP -> StatusFresh
    ConsumeType.DISCARDED -> StatusWarning
    ConsumeType.EXPIRED -> StatusCritical
    ConsumeType.GIFTED -> CategoryDrink
}

fun nullableConsumeIcon(type: ConsumeType?): ImageVector = type?.consumeIcon() ?: Icons.Default.Close

fun nullableConsumeColor(type: ConsumeType?): Color = type?.consumeColor() ?: StatusExpired
