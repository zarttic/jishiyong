package com.jishiyong.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加物品") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 物品名称
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("物品名称 *") },
                placeholder = { Text("例如：牛奶、洗面奶") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("请输入物品名称") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Inventory2, contentDescription = null)
                }
            )

            // 分类选择
            Box {
                OutlinedTextField(
                    value = "${category.icon} ${category.displayName}",
                    onValueChange = {},
                    label = { Text("分类") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Category, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择分类")
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

            // 购买日期
            DateField(
                label = "购买日期",
                date = purchaseDate,
                onDateSelected = { purchaseDate = it }
            )

            // 过期日期
            DateField(
                label = "过期日期 *",
                date = expirationDate,
                onDateSelected = { expirationDate = it }
            )

            // 数量
            OutlinedTextField(
                value = quantity,
                onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                label = { Text("数量") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Numbers, contentDescription = null)
                }
            )

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                placeholder = { Text("可选，添加备注信息") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null)
                }
            )

            // 提醒设置
            OutlinedTextField(
                value = reminderDaysText,
                onValueChange = { reminderDaysText = it },
                label = { Text("提前提醒天数") },
                placeholder = { Text("用逗号分隔，如：7,3,1") },
                supportingText = {
                    Text("将在过期前这些天数发送提醒通知")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "保存",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    OutlinedTextField(
        value = DateUtils.formatChinese(date),
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
        },
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
                Icon(Icons.Default.EditCalendar, contentDescription = "选择日期")
            }
        }
    )
}
