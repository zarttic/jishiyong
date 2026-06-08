package com.jishiyong.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.speech.BaiduCloudSpeechRecognizer
import com.jishiyong.ui.components.AssistantFace
import com.jishiyong.ui.components.AssistantNote
import com.jishiyong.ui.components.CategoryFilterChips
import com.jishiyong.ui.components.FoldedPaperSurface
import com.jishiyong.ui.components.FreshBackdropStyle
import com.jishiyong.ui.components.FreshCornerLarge
import com.jishiyong.ui.components.FreshnessHeatBar
import com.jishiyong.ui.components.FreshnessLabelCard
import com.jishiyong.ui.components.FridgeDoorBackdrop
import com.jishiyong.ui.components.SearchPaperField
import com.jishiyong.ui.components.ShelfHeader
import com.jishiyong.ui.components.StatusPill
import com.jishiyong.ui.components.VoiceHandle
import com.jishiyong.ui.components.consumeColor
import com.jishiyong.ui.components.consumeIcon
import com.jishiyong.ui.theme.BrandPrimary
import com.jishiyong.ui.theme.BrandPrimaryDark
import com.jishiyong.ui.theme.BrandSoft
import com.jishiyong.ui.theme.InkMuted
import com.jishiyong.ui.theme.OutlineSoft
import com.jishiyong.ui.theme.StatusCritical
import com.jishiyong.ui.theme.StatusFresh
import com.jishiyong.ui.theme.StatusUrgent
import com.jishiyong.ui.theme.StatusWarning
import com.jishiyong.ui.theme.SurfaceClean
import com.jishiyong.ui.theme.SurfaceSoft
import com.jishiyong.update.UpdateCheckState
import com.jishiyong.util.DateUtils
import com.jishiyong.viewmodel.MainViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onStatsClick: () -> Unit,
    onInspectClick: () -> Unit,
    onSettingsClick: () -> Unit,
    backdropStyle: FreshBackdropStyle = FreshBackdropStyle.ColdMist,
    modifier: Modifier = Modifier
) {
    val activeItems by viewModel.activeItems.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()
    val voiceInputState by viewModel.voiceInputState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf<Item?>(null) }
    var showConsumeMenuFor by remember { mutableStateOf<Item?>(null) }
    var cloudSpeechJob by remember { mutableStateOf<Job?>(null) }
    val ignoreNextSpeechError = remember { mutableStateOf(false) }
    val baiduCloudSpeechRecognizer = remember(context) {
        BaiduCloudSpeechRecognizer(context)
    }
    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }
    val externalSpeechRecognizerAvailable = remember(context) {
        buildSpeechRecognitionIntent().resolveActivity(context.packageManager) != null
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

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            viewModel.handleVoiceText(text)
        } else {
            viewModel.failVoiceInput("没有识别到可用语音内容，请重试")
        }
    }

    val startBaiduCloudVoiceRecognition = {
        if (!baiduCloudSpeechRecognizer.isConfigured()) {
            viewModel.failVoiceInput("当前设备没有可用的系统语音识别，且未配置百度云语音识别")
        } else {
            viewModel.startVoiceInput()
            cloudSpeechJob?.cancel()
            cloudSpeechJob = coroutineScope.launch {
                val text = try {
                    baiduCloudSpeechRecognizer.recognizeOnce(
                        onRecordingFinished = { viewModel.markVoiceRecognizing() }
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    viewModel.failVoiceInput("百度云语音识别失败，请稍后再试")
                    return@launch
                }
                viewModel.handleVoiceText(text)
                cloudSpeechJob = null
            }
        }
    }

    val startExternalVoiceRecognition = {
        if (!externalSpeechRecognizerAvailable) {
            startBaiduCloudVoiceRecognition()
        } else {
            viewModel.startVoiceInput()
            try {
                speechRecognitionLauncher.launch(buildSpeechRecognitionIntent())
            } catch (_: Exception) {
                startBaiduCloudVoiceRecognition()
            }
        }
    }

    val startVoiceRecognition = {
        if (speechRecognizer == null) {
            startExternalVoiceRecognition()
        } else {
            ignoreNextSpeechError.value = false
            viewModel.startVoiceInput()
            try {
                speechRecognizer.cancel()
                speechRecognizer.startListening(buildSpeechRecognitionIntent())
            } catch (_: Exception) {
                startExternalVoiceRecognition()
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
        val requiresRecordAudioPermission = speechRecognizer != null ||
                (!externalSpeechRecognizerAvailable && baiduCloudSpeechRecognizer.isConfigured())
        if (!requiresRecordAudioPermission || ContextCompat.checkSelfPermission(
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
        cloudSpeechJob?.cancel()
        cloudSpeechJob = null
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
    val itemStatuses = prioritizedItems.associateWith { viewModel.getExpiryStatus(it) }
    val itemDays = prioritizedItems.associateWith { viewModel.getDaysUntilExpiry(it) }
    val warningItems = prioritizedItems.count {
        when (itemStatuses[it]) {
            ExpiryStatus.FRESH,
            ExpiryStatus.EXPIRED -> false
            else -> true
        }
    }
    val expiredItems = prioritizedItems.count { itemStatuses[it] == ExpiryStatus.EXPIRED }
    val urgentCount = prioritizedItems.count {
        val status = itemStatuses[it]
        status == ExpiryStatus.EXPIRED ||
                status == ExpiryStatus.EXPIRING_SOON ||
                status == ExpiryStatus.EXPIRING_CRITICAL
    }
    val freshCount = prioritizedItems.count { itemStatuses[it] == ExpiryStatus.FRESH }
    val shelves = buildTimeShelves(prioritizedItems, itemDays)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        FridgeDoorBackdrop(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            style = backdropStyle
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, top = 24.dp, end = 18.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HomeTopBar(
                        activeCount = activeCount,
                        updateCheckState = updateCheckState,
                        onCheckUpdates = { viewModel.checkForUpdates(manual = true) },
                        onStatsClick = onStatsClick,
                        onInspectClick = onInspectClick,
                        onSettingsClick = onSettingsClick
                    )
                }

                item {
                    TodayFreshnessBoard(
                        urgentCount = urgentCount,
                        warningCount = warningItems,
                        freshCount = freshCount,
                        expiredCount = expiredItems,
                        topItems = prioritizedItems.take(3),
                        itemDays = itemDays,
                        totalCount = totalCount
                    )
                }

                item {
                    AssistantNote(
                        title = "小用便签",
                        message = assistantHomeMessage(prioritizedItems.firstOrNull(), itemDays),
                        trailing = {
                            StatusPill(
                                text = if (urgentCount > 0 || warningItems > 0) "提醒中" else "看着呢",
                                color = if (urgentCount > 0 || warningItems > 0) StatusWarning else BrandPrimary
                            )
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchPaperField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            modifier = Modifier.weight(1f),
                            trailing = {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        FoldedPaperSurface(
                            modifier = Modifier
                                .size(54.dp)
                                .clickable(onClick = onAddClick),
                            shape = RoundedCornerShape(16.dp),
                            color = BrandPrimary,
                            borderColor = BrandPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "贴一张保鲜标签",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    CategoryFilterChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = viewModel::setCategory
                    )
                }

                if (prioritizedItems.isEmpty()) {
                    item {
                        EmptyFreshnessWall(
                            filtered = selectedCategory != null || searchQuery.isNotBlank(),
                            onAddClick = onAddClick,
                            onVoiceClick = onVoiceClick
                        )
                    }
                } else {
                    shelves.forEach { shelf ->
                        if (shelf.items.isNotEmpty()) {
                            item(key = "shelf-${shelf.title}") {
                                TimeShelfSection(
                                    shelf = shelf,
                                    itemStatuses = itemStatuses,
                                    itemDays = itemDays,
                                    onItemClick = { onItemClick(it.id) },
                                    onConsumeClick = { showConsumeMenuFor = it },
                                    onDeleteClick = { showDeleteDialog = it },
                                    reminderMessage = shelfReminderMessage(shelf, itemStatuses, itemDays)
                                )
                            }
                        }
                    }
                }
            }

            val voiceBusy = voiceInputState.isBusy()
            VoiceHandle(
                busy = voiceBusy,
                onClick = onVoiceClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp)
            )
        }
    }

    showConsumeMenuFor?.let { item ->
        ConsumeActionSheet(
            item = item,
            onDismiss = { showConsumeMenuFor = null },
            onConsume = { type ->
                viewModel.markAsConsumed(item, type) {
                    showConsumeMenuFor = null
                }
            }
        )
    }

    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("撕下标签") },
            text = { Text("确定删除“${item.name}”？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item) {
                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            },
            containerColor = SurfaceClean,
            shape = FreshCornerLarge
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
                            val url = state.update.downloadUrl
                            if (url.startsWith("https://", ignoreCase = true)) {
                                try {
                                    uriHandler.openUri(url)
                                } catch (_: Exception) {
                                    viewModel.reportOperationError("打开下载链接失败，请稍后再试")
                                }
                            } else {
                                viewModel.reportOperationError("下载链接不是安全的 HTTPS 地址")
                            }
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
                },
                containerColor = SurfaceClean,
                shape = FreshCornerLarge
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
                },
                containerColor = SurfaceClean,
                shape = FreshCornerLarge
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
                },
                containerColor = SurfaceClean,
                shape = FreshCornerLarge
            )
        }
        UpdateCheckState.Checking,
        UpdateCheckState.Idle -> Unit
    }

    VoiceInputSheet(
        voiceInputState = voiceInputState,
        onConfirm = viewModel::confirmVoiceAction,
        onCancel = cancelVoiceRecognition,
        onDismiss = viewModel::cancelVoiceInput,
        onCandidateSelected = viewModel::selectVoiceCandidate
    )
}

@Composable
private fun HomeTopBar(
    activeCount: Int,
    updateCheckState: UpdateCheckState,
    onCheckUpdates: () -> Unit,
    onStatsClick: () -> Unit,
    onInspectClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "及时用",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (activeCount > 0) "$activeCount 件物品贴在保鲜墙上" else "从第一张保鲜标签开始",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 10.dp)
        ) {
            PaperIconButton(
                onClick = onCheckUpdates,
                enabled = updateCheckState !is UpdateCheckState.Checking
            ) {
                if (updateCheckState is UpdateCheckState.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = BrandPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SystemUpdateAlt,
                        contentDescription = "检查更新",
                        tint = BrandPrimary
                    )
                }
            }
            PaperIconButton(onClick = onInspectClick) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "小用巡视",
                    tint = BrandPrimary
                )
            }
            PaperIconButton(onClick = onStatsClick) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "少浪费报告",
                    tint = BrandPrimary
                )
            }
            PaperIconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = BrandPrimary
                )
            }
        }
    }
}

@Composable
private fun PaperIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceClean.copy(alpha = 0.74f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.16f))
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            content = { content() }
        )
    }
}

@Composable
private fun TodayFreshnessBoard(
    urgentCount: Int,
    warningCount: Int,
    freshCount: Int,
    expiredCount: Int,
    topItems: List<Item>,
    itemDays: Map<Item, Int>,
    totalCount: Int
) {
    FoldedPaperSurface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceClean.copy(alpha = 0.9f),
        borderColor = BrandPrimary.copy(alpha = 0.18f),
        foldColor = BrandSoft
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "今日保鲜看板",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary
                    )
                    Text(
                        text = if (urgentCount > 0) "优先处理 $urgentCount 件" else "今天不用急",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                StatusPill(
                    text = "$totalCount 张标签",
                    color = BrandPrimary
                )
            }
            Text(
                text = todayBoardCopy(topItems, itemDays, totalCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BoardMetric(
                    label = "稳定",
                    count = freshCount,
                    color = StatusFresh,
                    modifier = Modifier.weight(1f)
                )
                BoardMetric(
                    label = "临期",
                    count = warningCount,
                    color = StatusWarning,
                    modifier = Modifier.weight(1f)
                )
                BoardMetric(
                    label = "过期",
                    count = expiredCount,
                    color = StatusCritical,
                    modifier = Modifier.weight(1f)
                )
            }
            FreshnessHeatBar(
                stableCount = freshCount,
                warningCount = warningCount,
                expiredCount = expiredCount,
                modifier = Modifier.padding(top = 3.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeatLegend("稳定", StatusFresh)
                HeatLegend("临期", StatusWarning)
                HeatLegend("过期", StatusCritical)
            }
            if (topItems.isNotEmpty()) {
                PriorityLabelStrip(
                    topItems = topItems,
                    itemDays = itemDays
                )
            }
        }
    }
}

@Composable
private fun BoardMetric(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.09f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityLabelStrip(
    topItems: List<Item>,
    itemDays: Map<Item, Int>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        items(topItems, key = { it.id }) { item ->
            val days = itemDays[item] ?: 0
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = SurfaceSoft.copy(alpha = 0.72f),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft.copy(alpha = 0.62f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(4.dp)
                            .background(priorityColor(days), RoundedCornerShape(999.dp))
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = compactDaysLabel(days),
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor(days),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatLegend(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, RoundedCornerShape(999.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun priorityColor(daysUntilExpiry: Int): Color {
    return when {
        daysUntilExpiry < 0 -> StatusCritical
        daysUntilExpiry == 0 -> StatusCritical
        daysUntilExpiry <= 3 -> StatusWarning
        else -> StatusFresh
    }
}

private fun compactDaysLabel(daysUntilExpiry: Int): String {
    return when {
        daysUntilExpiry < 0 -> "过期${-daysUntilExpiry}天"
        daysUntilExpiry == 0 -> "今天"
        daysUntilExpiry == 1 -> "明天"
        else -> "${daysUntilExpiry}天"
    }
}

@Composable
private fun TimeShelfSection(
    shelf: TimeShelfGroup,
    itemStatuses: Map<Item, ExpiryStatus>,
    itemDays: Map<Item, Int>,
    onItemClick: (Item) -> Unit,
    onConsumeClick: (Item) -> Unit,
    onDeleteClick: (Item) -> Unit,
    reminderMessage: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ShelfHeader(
            title = shelf.title,
            countText = "${shelf.items.size} 件",
            color = shelf.color,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        reminderMessage?.let { message ->
            ShelfAssistantNudge(
                message = message,
                color = shelf.color
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(
                items = shelf.items,
                key = { it.id }
            ) { item ->
                FreshnessLabelCard(
                    item = item,
                    expiryStatus = itemStatuses[item] ?: ExpiryStatus.FRESH,
                    daysUntilExpiry = itemDays[item] ?: 0,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.width(214.dp),
                    actions = {
                        LabelActionButton(
                            icon = Icons.Default.CheckCircle,
                            text = "处理",
                            color = BrandPrimary,
                            onClick = { onConsumeClick(item) },
                            modifier = Modifier.weight(1f)
                        )
                        LabelActionButton(
                            icon = Icons.Default.DeleteOutline,
                            text = "删除",
                            color = MaterialTheme.colorScheme.error,
                            onClick = { onDeleteClick(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ShelfAssistantNudge(
    message: String,
    color: Color
) {
    FoldedPaperSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 7.dp
        ),
        color = SurfaceClean.copy(alpha = 0.78f),
        borderColor = color.copy(alpha = 0.2f),
        foldColor = BrandSoft
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AssistantFace(boxSize = 30.dp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = BrandPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.LabelActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
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

@Composable
private fun EmptyFreshnessWall(
    filtered: Boolean,
    onAddClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FoldedPaperSurface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceClean.copy(alpha = if (filtered) 0.72f else 0.58f),
            borderColor = OutlineSoft.copy(alpha = 0.7f),
            foldColor = BrandSoft.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 8.dp),
                    color = BrandSoft.copy(alpha = 0.52f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.16f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AssistantFace(boxSize = 54.dp)
                        Text(
                            text = if (filtered) "没有匹配标签" else "第一张保鲜标签",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (filtered) "换个关键词或分类看看。" else "贴上以后，小用会按到期时间帮你摆上货架。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                FreshnessHeatBar(
                    stableCount = 1,
                    warningCount = 1,
                    expiredCount = 0,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (filtered) "贴新标签" else "贴第一张标签")
            }
            OutlinedButton(
                onClick = onVoiceClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("对小用说一句")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsumeActionSheet(
    item: Item,
    onDismiss: () -> Unit,
    onConsume: (ConsumeType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceClean,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AssistantNote(
                title = "小用确认",
                message = "要把“${item.name}”标记成哪种处理结果？"
            )
            ConsumeType.entries.forEach { type ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConsume(type) },
                    shape = RoundedCornerShape(18.dp),
                    color = type.consumeColor().copy(alpha = 0.09f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, type.consumeColor().copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = type.consumeIcon(),
                            contentDescription = null,
                            tint = type.consumeColor(),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = InkMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceInputSheet(
    voiceInputState: VoiceInputState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onCandidateSelected: (Item) -> Unit
) {
    if (voiceInputState == VoiceInputState.Idle) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismiss: () -> Unit = {
        if (voiceInputState.isExecuting()) {
            Unit
        } else {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        containerColor = SurfaceClean,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AssistantNote(
                title = "小用助手",
                message = voiceSheetMessage(voiceInputState)
            )

            when (voiceInputState) {
                VoiceInputState.Listening,
                VoiceInputState.Recognizing,
                is VoiceInputState.Parsing,
                is VoiceInputState.Executing -> {
                    VoiceWave()
                    if (!voiceInputState.isExecuting()) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("取消")
                        }
                    }
                }
                is VoiceInputState.PendingConfirmation -> {
                    UnderstandCard(
                        rows = listOf(
                            "识别到" to voiceInputState.recognizedText,
                            "动作" to voiceActionTitle(voiceInputState.action),
                            "结果" to voiceActionPreview(voiceInputState.action, voiceInputState.matchedItem)
                        ),
                        diagnostics = voiceInputState.diagnostics
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("重说")
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("确认再改")
                        }
                    }
                }
                is VoiceInputState.NeedsSelection -> {
                    UnderstandCard(
                        rows = listOf(
                            "识别到" to voiceInputState.recognizedText,
                            "需要" to voiceInputState.message
                        ),
                        diagnostics = voiceInputState.diagnostics
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = voiceInputState.candidates,
                            key = { it.id }
                        ) { item ->
                            FoldedPaperSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCandidateSelected(item) },
                                shape = RoundedCornerShape(18.dp),
                                color = SurfaceSoft
                            ) {
                                Text(
                                    text = "${item.name} · 剩余 ${(item.quantity - item.usedQuantity).coerceAtLeast(0)} · ${DateUtils.formatShort(item.expirationDate)}过期",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("取消")
                    }
                }
                is VoiceInputState.Success -> {
                    UnderstandCard(rows = listOf("结果" to voiceInputState.message))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("知道了")
                    }
                }
                is VoiceInputState.Error -> {
                    val rows = buildList {
                        voiceInputState.recognizedText?.let { add("识别到" to it) }
                        add("结果" to voiceInputState.message)
                    }
                    UnderstandCard(rows = rows)
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("知道了")
                    }
                }
                VoiceInputState.Idle -> Unit
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VoiceWave() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(18.dp, 36.dp, 52.dp, 31.dp, 44.dp, 24.dp).forEach { height ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(9.dp)
                    .height(height)
                    .background(BrandPrimary, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun UnderstandCard(
    rows: List<Pair<String, String>>,
    diagnostics: List<InventoryPlanningDiagnostic> = emptyList()
) {
    FoldedPaperSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        color = BrandSoft.copy(alpha = 0.62f),
        borderColor = BrandPrimary.copy(alpha = 0.16f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BrandPrimary.copy(alpha = 0.12f))
                    )
                }
                Row(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(58.dp)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            diagnostics
                .filter { it.message.isNotBlank() }
                .forEach { diagnostic ->
                    Text(
                        text = diagnostic.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
        }
    }
}

private data class TimeShelfGroup(
    val title: String,
    val color: Color,
    val items: List<Item>
)

private fun buildTimeShelves(
    items: List<Item>,
    itemDays: Map<Item, Int>
): List<TimeShelfGroup> {
    val today = items.filter { (itemDays[it] ?: 0) <= 0 }
    val tomorrow = items.filter { (itemDays[it] ?: 0) == 1 }
    val threeDays = items.filter { (itemDays[it] ?: 0) in 2..3 }
    val thisWeek = items.filter { (itemDays[it] ?: 0) in 4..7 }
    val later = items.filter { (itemDays[it] ?: 0) > 7 }
    return listOf(
        TimeShelfGroup("今天", StatusCritical, today),
        TimeShelfGroup("明天", StatusUrgent, tomorrow),
        TimeShelfGroup("3 天内", StatusWarning, threeDays),
        TimeShelfGroup("本周", StatusFresh, thisWeek),
        TimeShelfGroup("更晚", BrandPrimary, later)
    )
}

private fun todayBoardCopy(
    topItems: List<Item>,
    itemDays: Map<Item, Int>,
    totalCount: Int
): String {
    if (totalCount == 0) return "保鲜墙还空着，贴一张标签就能开始提醒。"
    if (topItems.isEmpty()) return "当前筛选下没有需要看的物品。"
    val names = topItems.take(3).joinToString("、") { it.name }
    val first = topItems.first()
    val firstDays = itemDays[first] ?: 0
    return when {
        firstDays < 0 -> "$names 已经过期，先确认还能不能用。"
        firstDays == 0 -> "${first.name} 今天到期，$names 可以先安排。"
        firstDays <= 3 -> "$names 快到提醒线了，少量处理最稳。"
        else -> "$names 状态稳定，今天可以慢慢看。"
    }
}

private fun shelfReminderMessage(
    shelf: TimeShelfGroup,
    itemStatuses: Map<Item, ExpiryStatus>,
    itemDays: Map<Item, Int>
): String? {
    val first = shelf.items.firstOrNull() ?: return null
    val firstDays = itemDays[first] ?: 0
    val hasExpired = shelf.items.any { itemStatuses[it] == ExpiryStatus.EXPIRED }
    val hasUrgent = shelf.items.any {
        val status = itemStatuses[it]
        status == ExpiryStatus.EXPIRING_CRITICAL || status == ExpiryStatus.EXPIRING_SOON
    }
    return when {
        hasExpired -> "小用看过了，这组有过期标签，先确认 ${first.name}。"
        shelf.title == "今天" -> "今天这组先看 ${first.name}，处理前我会帮你确认。"
        hasUrgent -> "${first.name} 快到提醒线了，这组可以提前安排。"
        firstDays == 1 -> "明天到期的先摆在这里，明早我再提醒你。"
        else -> null
    }
}

private fun assistantHomeMessage(
    firstItem: Item?,
    itemDays: Map<Item, Int>
): String {
    if (firstItem == null) return "我在这儿，等你贴第一张保鲜标签。"
    val days = itemDays[firstItem] ?: 0
    return when {
        days < 0 -> "我看过了，${firstItem.name} 已过期，先确认一下最稳。"
        days == 0 -> "我看过了，今天先处理 ${firstItem.name} 最稳。"
        days <= 3 -> "${firstItem.name} 快到提醒线了，可以提前安排。"
        else -> "这面墙今天挺稳，我会继续帮你看着。"
    }
}

private fun VoiceInputState.isBusy(): Boolean {
    return this is VoiceInputState.Listening ||
            this is VoiceInputState.Recognizing ||
            this is VoiceInputState.Parsing ||
            this is VoiceInputState.Executing
}

private fun VoiceInputState.isExecuting(): Boolean = this is VoiceInputState.Executing

private fun voiceSheetMessage(state: VoiceInputState): String {
    return when (state) {
        VoiceInputState.Listening -> "我在听，可以直接说库存变化。"
        VoiceInputState.Recognizing -> "正在把语音转成文字。"
        is VoiceInputState.Parsing -> "${state.messagePrefix}：${state.recognizedText}"
        is VoiceInputState.Executing -> "正在执行确认过的库存操作。"
        is VoiceInputState.PendingConfirmation -> "我整理好了，确认后再改库存。"
        is VoiceInputState.NeedsSelection -> "这句有几个可能的库存，请选一个。"
        is VoiceInputState.Success -> "操作完成。"
        is VoiceInputState.Error -> "这句没听清，没有改库存。"
        VoiceInputState.Idle -> ""
    }
}

private fun voiceActionTitle(action: InventoryAction): String {
    return when (action) {
        is InventoryAction.AddItem -> "新增标签"
        is InventoryAction.ConsumeItem -> "使用物品"
        is InventoryAction.DiscardItem -> "丢弃物品"
        is InventoryAction.AskClarification -> "需要确认"
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
            val before = matchedItem?.let { (it.quantity - it.usedQuantity).coerceAtLeast(0) }
            val after = before?.let { (it - action.quantity).coerceAtLeast(0) }
            if (before != null && after != null) {
                "$itemName 剩余 $before -> $after"
            } else {
                "消耗 $itemName x${action.quantity}"
            }
        }
        is InventoryAction.DiscardItem -> {
            val itemName = matchedItem?.name ?: action.itemName
            "丢弃 $itemName x${action.quantity}"
        }
        is InventoryAction.AskClarification -> action.message
    }
}

private fun buildSpeechRecognitionIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出库存操作")
    }
}

private fun speechErrorText(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "录音失败，请重试"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别服务不可用或已取消"
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
