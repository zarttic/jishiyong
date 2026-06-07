package com.jishiyong.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.ui.components.categoryColor
import com.jishiyong.ui.components.categoryIcon
import com.jishiyong.util.DateUtils
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onSave: (Item) -> Unit,
    onCancel: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "新增物品",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "记录保质期与提醒",
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormSection(title = "基础信息") {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("物品名称") },
                    placeholder = { Text("例如：牛奶、洗面奶") },
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text("请输入物品名称")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors()
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "分类",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CategoryPicker(
                        selectedCategory = category,
                        onCategorySelected = { category = it }
                    )
                }

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { text: String ->
                        if (text.all { it.isDigit() }) {
                            quantity = text
                            quantityError = false
                        }
                    },
                    label = { Text("数量") },
                    isError = quantityError,
                    supportingText = {
                        if (quantityError) {
                            Text("请输入大于 0 的数量")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Numbers, contentDescription = null)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors()
                )
            }

            FormSection(title = "日期") {
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

            FormSection(title = "提醒与备注") {
                OutlinedTextField(
                    value = reminderDaysText,
                    onValueChange = {
                        reminderDaysText = it
                        reminderDaysError = false
                    },
                    label = { Text("提前提醒天数") },
                    placeholder = { Text("7,3,1") },
                    isError = reminderDaysError,
                    supportingText = {
                        Text(if (reminderDaysError) "请输入用逗号分隔的正整数" else "用逗号分隔，例如 7,3,1")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    placeholder = { Text("可选，记录开封、存放位置等") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors()
                )
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }

                    val parsedQuantity = quantity.toIntOrNull()
                    if (parsedQuantity == null || parsedQuantity <= 0) {
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
                            quantity = parsedQuantity,
                            reminderDays = reminderDays
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "保存物品",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
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
            val categoryColor = category.categoryColor()
            Surface(
                modifier = Modifier
                    .width(88.dp)
                    .clickable { onCategorySelected(category) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) categoryColor else categoryColor.copy(alpha = 0.1f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = category.categoryIcon(),
                        contentDescription = null,
                        tint = if (selected) Color.White else categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
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

    OutlinedTextField(
        value = DateUtils.formatChinese(date),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        isError = isError,
        supportingText = supportingText?.let { message ->
            { Text(message) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker() },
        leadingIcon = {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = { showDatePicker() }) {
                Icon(Icons.Default.EditCalendar, contentDescription = "选择日期")
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
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
