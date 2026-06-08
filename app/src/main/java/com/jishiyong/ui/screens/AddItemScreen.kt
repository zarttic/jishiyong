package com.jishiyong.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.ui.components.AssistantNote
import com.jishiyong.ui.components.CategoryStamp
import com.jishiyong.ui.components.FoldedPaperSurface
import com.jishiyong.ui.components.FreshBackdropStyle
import com.jishiyong.ui.components.FreshnessLabelCard
import com.jishiyong.ui.components.FridgeDoorBackdrop
import com.jishiyong.ui.components.PaperSection
import com.jishiyong.ui.components.StatusPill
import com.jishiyong.ui.components.categoryColor
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.BrandPrimaryDark
import com.jishiyong.ui.theme.BrandSoft
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.SurfaceClean
import com.jishiyong.ui.theme.SurfaceSoft
import com.jishiyong.util.DateUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onSave: (Item) -> Unit,
    onCancel: () -> Unit,
    backdropStyle: FreshBackdropStyle = FreshBackdropStyle.ColdMist,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCategory.FOOD) }
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
    var expirationDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var note by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var reminderDaysText by remember { mutableStateOf("7,3,1") }
    var nameError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    var reminderDaysError by remember { mutableStateOf(false) }

    val parsedQuantity = quantity.toIntOrNull()?.takeIf { it > 0 } ?: 1
    val previewItem = Item(
        name = name.ifBlank { "新的保鲜标签" },
        category = category,
        purchaseDate = purchaseDate,
        expirationDate = expirationDate,
        note = note.trim(),
        quantity = parsedQuantity,
        reminderDays = parseReminderDays(reminderDaysText) ?: listOf(7, 3, 1)
    )
    val previewDays = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate).toInt()
    val previewStatus = expiryStatusFor(previewDays)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "贴一张保鲜标签",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "像标签打印机一样先预览再贴上",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
            style = backdropStyle
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabelPrinterPreview(
                    item = previewItem,
                    expiryStatus = previewStatus,
                    daysUntilExpiry = previewDays
                )

                AssistantNote(
                    title = "小用便签",
                    message = reminderHint(reminderDaysText, expirationDate)
                )

                PaperSection(title = "标签内容") {
                    PaperTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = false
                        },
                        label = "物品名称",
                        placeholder = "例如：牛奶、洗面奶",
                        isError = nameError,
                        errorText = "请输入物品名称"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "分类印章",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CategoryPicker(
                            selectedCategory = category,
                            onCategorySelected = { category = it }
                        )
                    }

                    PaperTextField(
                        value = quantity,
                        onValueChange = { text ->
                            if (text.all { it.isDigit() }) {
                                quantity = text
                                quantityError = false
                            }
                        },
                        label = "数量",
                        isError = quantityError,
                        errorText = "请输入大于 0 的数量",
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                PaperSection(title = "保鲜时间") {
                    DateField(
                        label = "购买日期",
                        date = purchaseDate,
                        onDateSelected = {
                            purchaseDate = it
                            dateError = false
                        }
                    )
                    DateField(
                        label = "到期日期",
                        date = expirationDate,
                        onDateSelected = {
                            expirationDate = it
                            dateError = false
                        },
                        isError = dateError,
                        supportingText = if (dateError) "到期日期不能早于购买日期" else null
                    )
                }

                PaperSection(title = "提醒与小备注") {
                    PaperTextField(
                        value = reminderDaysText,
                        onValueChange = {
                            reminderDaysText = it
                            reminderDaysError = false
                        },
                        label = "提前提醒天数",
                        placeholder = "7,3,1",
                        isError = reminderDaysError,
                        errorText = "请输入用逗号分隔的正整数",
                        supportText = "用逗号分隔，例如 7,3,1"
                    )

                    PaperTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "备注",
                        placeholder = "可选，记录开封、存放位置等",
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                        },
                        minLines = 2,
                        maxLines = 4
                    )
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }

                        val finalQuantity = quantity.toIntOrNull()
                        if (finalQuantity == null || finalQuantity <= 0) {
                            quantityError = true
                            return@Button
                        }

                        if (expirationDate.isBefore(purchaseDate)) {
                            dateError = true
                            return@Button
                        }

                        val reminderDays = parseReminderDays(reminderDaysText)
                        if (reminderDays == null) {
                            reminderDaysError = true
                            return@Button
                        }

                        onSave(
                            Item(
                                name = name.trim(),
                                category = category,
                                purchaseDate = purchaseDate,
                                expirationDate = expirationDate,
                                note = note.trim(),
                                quantity = finalQuantity,
                                reminderDays = reminderDays
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "贴上标签",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LabelPrinterPreview(
    item: Item,
    expiryStatus: ExpiryStatus,
    daysUntilExpiry: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        PrinterMachineHead()
        FoldedPaperSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(
                topStart = 8.dp,
                topEnd = 8.dp,
                bottomEnd = 26.dp,
                bottomStart = 10.dp
            ),
            color = SurfaceClean.copy(alpha = 0.9f),
            borderColor = BrandPrimary.copy(alpha = 0.16f),
            foldColor = BrandSoft
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "标签预览",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary
                    )
                    StatusPill(
                        text = "打印预览",
                        color = BrandPrimary
                    )
                }
                Box(modifier = Modifier.padding(horizontal = 6.dp)) {
                    FreshnessLabelCard(
                        item = item,
                        expiryStatus = expiryStatus,
                        daysUntilExpiry = daysUntilExpiry,
                        large = true
                    )
                }
                Text(
                    text = "确认内容后再贴上墙，小用不会直接改库存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrinterMachineHead() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomEnd = 18.dp,
            bottomStart = 18.dp
        ),
        color = BrandPrimaryDark
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "标签打印机",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(5.dp)
                        .background(BrandSoft.copy(alpha = 0.62f), RoundedCornerShape(999.dp))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(13.dp)
                    .background(Color(0xFF0B3C34), RoundedCornerShape(999.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(13.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                        .background(SurfaceSoft.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
                )
            }
        }
    }
}

@Composable
private fun CategoryPicker(
    selectedCategory: ItemCategory,
    onCategorySelected: (ItemCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        items(ItemCategory.entries.toList()) { category ->
            val selected = selectedCategory == category
            val color = category.categoryColor()
            FoldedPaperSurface(
                modifier = Modifier
                    .width(92.dp)
                    .clickable { onCategorySelected(category) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) color else SurfaceClean.copy(alpha = 0.82f),
                borderColor = color.copy(alpha = if (selected) 0.8f else 0.24f),
                foldColor = if (selected) color.copy(alpha = 0.82f) else BrandSoft
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryStamp(
                        category = category,
                        selected = selected,
                        size = 38.dp
                    )
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) Color.White else color
                    )
                }
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    val context = LocalContext.current
    val showDatePicker = {
        val calendar = Calendar.getInstance()
        calendar.set(date.year, date.monthValue - 1, date.dayOfMonth)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = DateUtils.formatChinese(date),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            isError = isError,
            supportingText = supportingText?.let { message ->
                { Text(message) }
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { showDatePicker() }) {
                    Icon(Icons.Default.EditCalendar, contentDescription = "选择日期")
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .semantics {
                    role = Role.Button
                    contentDescription = "选择$label"
                }
                .clickable { showDatePicker() }
        )
    }
}

@Composable
private fun PaperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    supportText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = {
            val text = if (isError) errorText else supportText
            if (text != null) Text(text)
        },
        singleLine = maxLines == 1,
        minLines = minLines,
        maxLines = maxLines,
        leadingIcon = leadingIcon,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceClean.copy(alpha = 0.92f),
    unfocusedContainerColor = SurfaceClean.copy(alpha = 0.82f),
    focusedBorderColor = BrandPrimary.copy(alpha = 0.46f),
    unfocusedBorderColor = OutlineSoft,
    cursorColor = BrandPrimary
)

private fun parseReminderDays(text: String): List<Int>? {
    val tokens = text
        .split(',', '，')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return null

    val days = tokens.map { token ->
        val value = token.toIntOrNull() ?: return null
        if (value <= 0) return null
        value
    }
    return days.distinct().sorted()
}

private fun expiryStatusFor(daysUntilExpiry: Int): ExpiryStatus {
    return when {
        daysUntilExpiry < 0 -> ExpiryStatus.EXPIRED
        daysUntilExpiry == 0 -> ExpiryStatus.EXPIRING_CRITICAL
        daysUntilExpiry <= 3 -> ExpiryStatus.EXPIRING_SOON
        daysUntilExpiry <= 7 -> ExpiryStatus.EXPIRING_WARNING
        else -> ExpiryStatus.FRESH
    }
}

private fun reminderHint(
    reminderDaysText: String,
    expirationDate: LocalDate
): String {
    val days = parseReminderDays(reminderDaysText)
    val expiryText = DateUtils.formatShort(expirationDate)
    return if (days == null) {
        "提醒节奏还没写对，先用 7,3,1 这样的格式。"
    } else {
        "我会在 ${days.joinToString("、") { "提前${it}天" }} 这几次提醒你，标签到 $expiryText。"
    }
}
