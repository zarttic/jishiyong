package com.jishiyong.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jishiyong.agent.InventoryAction
import com.jishiyong.agent.InventoryPlanningDiagnostic
import com.jishiyong.agent.VoiceInputState
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.update.UpdateCheckState
import com.jishiyong.ui.components.CategoryFilterChips
import com.jishiyong.ui.components.ExpiryOverviewCard
import com.jishiyong.ui.components.ItemCard
import com.jishiyong.util.DateUtils
import com.jishiyong.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeItems by viewModel.activeItems.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val expiredCount by viewModel.expiredCount.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()
    val voiceInputState by viewModel.voiceInputState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var showDeleteDialog by remember { mutableStateOf<Item?>(null) }
    val ignoreNextSpeechError = remember { mutableStateOf(false) }
    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                viewModel.startVoiceInput()
            }

            override fun onBeginningOfSpeech() {
                viewModel.startVoiceInput()
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                viewModel.markVoiceRecognizing()
            }

            override fun onError(error: Int) {
                if (ignoreNextSpeechError.value) {
                    ignoreNextSpeechError.value = false
                    viewModel.cancelVoiceInput()
                    return
                }
                viewModel.failVoiceInput(speechErrorText(error))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                viewModel.handleVoiceText(text)
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val startVoiceRecognition = {
        if (speechRecognizer == null) {
            viewModel.failVoiceInput("当前设备不支持系统语音识别")
        } else {
            ignoreNextSpeechError.value = false
            viewModel.startVoiceInput()
            try {
                speechRecognizer.cancel()
                speechRecognizer.startListening(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出库存操作")
                    }
                )
            } catch (_: Exception) {
                viewModel.failVoiceInput("启动语音识别失败，请稍后再试")
            }
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecognition()
        } else {
            viewModel.failVoiceInput("需要录音权限才能使用语音库存操作")
        }
    }

    val onVoiceClick = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceRecognition()
        } else {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val cancelVoiceRecognition = {
        ignoreNextSpeechError.value = true
        speechRecognizer?.cancel()
        viewModel.cancelVoiceInput()
    }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
    }

    val prioritizedItems = activeItems.sortedWith(
        compareBy<Item> { viewModel.getDaysUntilExpiry(it) }
            .thenBy { it.expirationDate }
            .thenBy { it.name }
    )
    val warningItems = activeItems.count {
        when (viewModel.getExpiryStatus(it)) {
            ExpiryStatus.FRESH, ExpiryStatus.EXPIRED -> false
            else -> true
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新增") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                HomeHeader(
                    activeCount = activeCount,
                    searchQuery = searchQuery,
                    onSearchChange = viewModel::setSearchQuery,
                    updateCheckState = updateCheckState,
                    onCheckUpdates = { viewModel.checkForUpdates(manual = true) },
                    voiceInputState = voiceInputState,
                    onVoiceClick = onVoiceClick,
                    onStatsClick = onStatsClick
                )
            }

            item {
                ExpiryOverviewCard(
                    totalItems = totalCount,
                    activeItems = activeCount,
                    expiredItems = expiredCount,
                    warningItems = warningItems
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                CategoryFilterChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = viewModel::setCategory
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                InventorySectionHeader(
                    count = prioritizedItems.size,
                    hasFilters = selectedCategory != null || searchQuery.isNotBlank()
                )
            }

            if (prioritizedItems.isEmpty()) {
                item {
                    EmptyState(
                        message = if (searchQuery.isNotBlank()) "没有匹配的物品" else "库存还是空的",
                        subtitle = if (searchQuery.isNotBlank()) "换个关键词或清除筛选条件" else "添加第一件物品后，这里会按到期时间排序"
                    )
                }
            } else {
                items(
                    items = prioritizedItems,
                    key = { it.id }
                ) { item ->
                    ItemCard(
                        item = item,
                        expiryStatus = viewModel.getExpiryStatus(item),
                        daysUntilExpiry = viewModel.getDaysUntilExpiry(item),
                        onClick = { onItemClick(item.id) },
                        onDelete = { showDeleteDialog = item },
                        onConsume = { viewModel.markAsConsumed(item, it) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除物品") },
            text = { Text("确定删除“${item.name}”？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    when (val state = updateCheckState) {
        is UpdateCheckState.Available -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateCheckState,
                title = { Text("发现新版本") },
                text = {
                    Text("${state.update.releaseName} 已发布，可前往 GitHub 下载 APK。")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            uriHandler.openUri(state.update.downloadUrl)
                            viewModel.dismissUpdateCheckState()
                        }
                    ) {
                        Text("去下载")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissUpdateCheckState) {
                        Text("稍后")
                    }
                }
            )
        }
        is UpdateCheckState.UpToDate -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateCheckState,
                title = { Text("已是最新版本") },
                text = { Text("当前安装的版本已经是最新发布版本。") },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissUpdateCheckState) {
                        Text("知道了")
                    }
                }
            )
        }
        is UpdateCheckState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateCheckState,
                title = { Text("检查更新失败") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissUpdateCheckState) {
                        Text("知道了")
                    }
                }
            )
        }
        UpdateCheckState.Checking,
        UpdateCheckState.Idle -> Unit
    }

    VoiceInputDialog(
        voiceInputState = voiceInputState,
        onConfirm = viewModel::confirmVoiceAction,
        onCancel = cancelVoiceRecognition,
        onDismiss = viewModel::cancelVoiceInput,
        onCandidateSelected = viewModel::selectVoiceCandidate
    )
}

@Composable
private fun HomeHeader(
    activeCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    updateCheckState: UpdateCheckState,
    onCheckUpdates: () -> Unit,
    voiceInputState: VoiceInputState,
    onVoiceClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "及时用",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (activeCount > 0) "$activeCount 件物品正在管理" else "从第一件物品开始管理",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(
                        onClick = onCheckUpdates,
                        enabled = updateCheckState !is UpdateCheckState.Checking
                    ) {
                        if (updateCheckState is UpdateCheckState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.SystemUpdateAlt,
                                contentDescription = "检查更新",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(onClick = onStatsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "统计",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("搜索名称、备注") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            val voiceBusy = voiceInputState is VoiceInputState.Listening ||
                    voiceInputState is VoiceInputState.Recognizing ||
                    voiceInputState is VoiceInputState.Parsing ||
                    voiceInputState is VoiceInputState.Executing
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                IconButton(
                    onClick = onVoiceClick,
                    enabled = !voiceBusy
                ) {
                    if (voiceBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "语音库存操作",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceInputDialog(
    voiceInputState: VoiceInputState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onCandidateSelected: (Item) -> Unit
) {
    when (voiceInputState) {
        VoiceInputState.Idle -> Unit
        VoiceInputState.Listening,
        VoiceInputState.Recognizing,
        is VoiceInputState.Parsing,
        is VoiceInputState.Executing -> {
            val isExecuting = voiceInputState is VoiceInputState.Executing
            AlertDialog(
                onDismissRequest = {
                    if (!isExecuting) {
                        onCancel()
                    }
                },
                title = { Text(voiceStateTitle(voiceInputState)) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(voiceStateMessage(voiceInputState))
                    }
                },
                confirmButton = {
                    if (!isExecuting) {
                        TextButton(onClick = onCancel) {
                            Text("取消")
                        }
                    }
                }
            )
        }
        is VoiceInputState.PendingConfirmation -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("确认语音操作") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "识别文本：${voiceInputState.recognizedText}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "将执行：\n${voiceActionPreview(voiceInputState.action, voiceInputState.matchedItem)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        PlanningDiagnosticsText(voiceInputState.diagnostics)
                    }
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text("确认执行")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
        is VoiceInputState.NeedsSelection -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("选择库存") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(voiceInputState.message)
                        Text(
                            text = "识别文本：${voiceInputState.recognizedText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PlanningDiagnosticsText(voiceInputState.diagnostics)
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            items(
                                items = voiceInputState.candidates,
                                key = { it.id }
                            ) { item ->
                                TextButton(
                                    onClick = { onCandidateSelected(item) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${item.name} · 剩余 ${(item.quantity - item.usedQuantity).coerceAtLeast(0)} · ${DateUtils.formatShort(item.expirationDate)}过期",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
        is VoiceInputState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("操作完成") },
                text = { Text(voiceInputState.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("知道了")
                    }
                }
            )
        }
        is VoiceInputState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("语音操作失败") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        voiceInputState.recognizedText?.let {
                            Text(
                                text = "识别文本：$it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(voiceInputState.message)
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("知道了")
                    }
                }
            )
        }
    }
}

@Composable
private fun PlanningDiagnosticsText(diagnostics: List<InventoryPlanningDiagnostic>) {
    diagnostics
        .filter { it.message.isNotBlank() }
        .forEach { diagnostic ->
            Text(
                text = diagnostic.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
}

private fun voiceStateTitle(state: VoiceInputState): String {
    return when (state) {
        VoiceInputState.Listening -> "待说话"
        VoiceInputState.Recognizing -> "识别中"
        is VoiceInputState.Parsing -> "解析中"
        is VoiceInputState.Executing -> "执行中"
        else -> ""
    }
}

private fun voiceStateMessage(state: VoiceInputState): String {
    return when (state) {
        VoiceInputState.Listening -> "请说出要新增、消耗或丢弃的库存"
        VoiceInputState.Recognizing -> "正在识别语音内容"
        is VoiceInputState.Parsing -> "${state.messagePrefix}：${state.recognizedText}"
        is VoiceInputState.Executing -> "正在执行语音操作"
        else -> ""
    }
}

private fun voiceActionPreview(action: InventoryAction, matchedItem: Item?): String {
    return when (action) {
        is InventoryAction.AddItem -> {
            val draft = action.draft
            "新增 ${draft.name} x${draft.quantity}\n分类：${draft.category.displayName}\n购买：${DateUtils.formatChinese(draft.purchaseDate)}\n过期：${DateUtils.formatChinese(draft.expirationDate)}"
        }
        is InventoryAction.ConsumeItem -> {
            val itemName = matchedItem?.name ?: action.itemName
            "消耗 $itemName x${action.quantity}"
        }
        is InventoryAction.DiscardItem -> {
            val itemName = matchedItem?.name ?: action.itemName
            "丢弃 $itemName x${action.quantity}"
        }
        is InventoryAction.AskClarification -> action.message
    }
}

private fun speechErrorText(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "录音失败，请重试"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别已取消"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "语音识别网络异常，请稍后再试"
        SpeechRecognizer.ERROR_NO_MATCH -> "没有识别到可用语音内容，请重试"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别服务正忙，请稍后再试"
        SpeechRecognizer.ERROR_SERVER -> "语音识别服务异常，请稍后再试"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有听到语音，请重试"
        else -> "语音识别失败，请重试"
    }
}

@Composable
private fun InventorySectionHeader(
    count: Int,
    hasFilters: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (hasFilters) "筛选结果" else "待处理清单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "按到期时间从近到远排列",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "$count 件",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun EmptyState(
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
