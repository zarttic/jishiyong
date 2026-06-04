package com.jishiyong.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.ui.components.GradientButton
import com.jishiyong.ui.theme.*
import com.jishiyong.util.Constants
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
    var showCategoryMenu by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部区域
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "添加物品",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 表单内容
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 物品名称
                ModernTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = "物品名称",
                    placeholder = "例如：牛奶、洗面奶",
                    isError = nameError,
                    errorMessage = "请输入物品名称",
                    leadingIcon = Icons.Rounded.Inventory2
                )

                // 分类选择
                Box {
                    ModernTextField(
                        value = "${category.icon} ${category.displayName}",
                        onValueChange = {},
                        label = "分类",
                        readOnly = true,
                        leadingIcon = Icons.Rounded.Category,
                        trailingIcon = {
                            IconButton(onClick = { showCategoryMenu = true }) {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        ItemCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.displayName}") },
                                onClick = {
                                    category = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // 日期选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernDateField(
                        label = "购买日期",
                        date = purchaseDate,
                        onDateSelected = { purchaseDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    ModernDateField(
                        label = "过期日期",
                        date = expirationDate,
                        onDateSelected = { expirationDate = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 数量
                ModernTextField(
                    value = quantity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                    label = "数量",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = Icons.Rounded.Numbers
                )

                // 备注
                ModernTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "备注",
                    placeholder = "可选，添加备注信息",
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = Icons.AutoMirrored.Filled.Notes
                )

                // 提醒设置
                ModernTextField(
                    value = reminderDaysText,
                    onValueChange = { reminderDaysText = it },
                    label = "提前提醒天数",
                    placeholder = "用逗号分隔，如：7,3,1",
                    supportingText = "将在过期前这些天数发送提醒通知",
                    leadingIcon = Icons.Rounded.Notifications
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 保存按钮
                GradientButton(
                    text = "保存物品",
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@GradientButton
                        }

                        val reminderDays = reminderDaysText
                            .split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .filter { it > 0 }
                            .sorted()
                            .ifEmpty { Constants.DEFAULT_REMINDER_DAYS }

                        val item = Item(
                            name = name.trim(),
                            category = category,
                            purchaseDate = purchaseDate,
                            expirationDate = expirationDate,
                            note = note.trim(),
                            quantity = quantity.toIntOrNull() ?: 1,
                            reminderDays = reminderDays
                        )
                        onSave(item)
                    },
                    icon = Icons.Rounded.Save
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder) }
        } else null,
        isError = isError,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            errorBorderColor = MaterialTheme.colorScheme.error
        ),
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingIcon = trailingIcon,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else if (supportingText != null) {
            { Text(supportingText) }
        } else null
    )
}

@Composable
private fun ModernDateField(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ModernTextField(
        value = DateUtils.formatShort(date),
        onValueChange = {},
        label = label,
        readOnly = true,
        modifier = modifier,
        leadingIcon = Icons.Rounded.CalendarMonth,
        trailingIcon = {
            IconButton(onClick = {
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
            }) {
                Icon(Icons.Rounded.EditCalendar, contentDescription = "选择日期")
            }
        }
    )
}
