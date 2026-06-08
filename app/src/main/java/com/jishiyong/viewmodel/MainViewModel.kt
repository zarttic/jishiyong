package com.jishiyong.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jishiyong.AppContainerProvider
import com.jishiyong.agent.InventoryActionExecutor
import com.jishiyong.agent.InventoryActionStore
import com.jishiyong.agent.InventoryAgent
import com.jishiyong.agent.VoiceInputState
import com.jishiyong.data.db.InventoryChangeResult
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.data.repository.ItemRepository
import com.jishiyong.update.AppUpdateChecker
import com.jishiyong.update.UpdateCheckState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

sealed class ItemDetailUiState {
    data object Loading : ItemDetailUiState()
    data object Missing : ItemDetailUiState()
    data class Loaded(val item: Item) : ItemDetailUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AppContainerProvider).container
    private val repository: ItemRepository =
        container.repository
    private val updateChecker: AppUpdateChecker =
        container.updateChecker
    private val inventoryAgent: InventoryAgent =
        container.inventoryAgent
    private val actionExecutor: InventoryActionExecutor =
        container.actionExecutor
    private val actionStore = object : InventoryActionStore {
        override suspend fun insert(item: Item): Long = repository.insert(item)
        override suspend fun applyInventoryChange(
            id: Long,
            quantity: Int,
            consumeType: ConsumeType
        ): InventoryChangeResult = repository.applyInventoryChange(id, quantity, consumeType)
    }
    private var hasCheckedForUpdates = false
    private var voiceParseJob: Job? = null

    // ======================== UI 状态 ========================

    private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
    val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private val _voiceInputState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState.asStateFlow()

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    private val _today = MutableStateFlow(repository.today())

    // ======================== 数据流 ========================

    private val allActiveItems: StateFlow<List<Item>> = repository.getActiveItems()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 活跃物品列表 */
    val activeItems: StateFlow<List<Item>> = combine(
        allActiveItems,
        _selectedCategory,
        _searchQuery
    ) { items, category, query ->
        items.filter { item ->
            (category == null || item.category == category) &&
                    (query.isEmpty() || item.name.contains(query, ignoreCase = true) ||
                            item.note.contains(query, ignoreCase = true))
        }
    }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 已消费物品列表 */
    val consumedItems: StateFlow<List<Item>> = repository.getConsumedItems()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 总物品数 */
    val totalCount: StateFlow<Int> = repository.getTotalCount()
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 活跃物品数 */
    val activeCount: StateFlow<Int> = repository.getActiveCount()
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 已过期物品数 */
    val expiredCount: StateFlow<Int> = combine(allActiveItems, _today) { items, today ->
        items.count { item -> item.expirationDate < today }
    }
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        refreshTodayAtMidnight()
    }

    // ======================== 操作 ========================

    fun setCategory(category: ItemCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun checkForUpdates(manual: Boolean = false) {
        if (_updateCheckState.value is UpdateCheckState.Checking) return
        if (!manual && hasCheckedForUpdates) return

        if (!manual) {
            hasCheckedForUpdates = true
        }

        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            _updateCheckState.value = try {
                val update = updateChecker.checkLatestRelease()
                when {
                    update != null -> UpdateCheckState.Available(update)
                    manual -> UpdateCheckState.UpToDate
                    else -> UpdateCheckState.Idle
                }
            } catch (_: Exception) {
                if (manual) {
                    UpdateCheckState.Error("检查更新失败，请稍后再试")
                } else {
                    UpdateCheckState.Idle
                }
            }
        }
    }

    fun dismissUpdateCheckState() {
        _updateCheckState.value = UpdateCheckState.Idle
    }

    fun dismissOperationError() {
        _operationError.value = null
    }

    fun reportOperationError(message: String) {
        _operationError.value = message
    }

    fun startVoiceInput() {
        voiceParseJob?.cancel()
        _voiceInputState.value = VoiceInputState.Listening
    }

    fun markVoiceRecognizing() {
        _voiceInputState.value = VoiceInputState.Recognizing
    }

    fun handleVoiceText(recognizedText: String) {
        val text = recognizedText.trim()
        if (text.isBlank()) {
            _voiceInputState.value = VoiceInputState.Error("没有识别到语音内容，请重试")
            return
        }

        voiceParseJob?.cancel()
        _voiceInputState.value = VoiceInputState.Parsing(
            recognizedText = text,
            parserLabel = inventoryAgent.mode.displayName,
            messagePrefix = inventoryAgent.mode.parsingMessagePrefix
        )
        voiceParseJob = viewModelScope.launch {
            val itemsSnapshot = try {
                repository.getActiveItems().first()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                allActiveItems.value
            }
            val preview = try {
                inventoryAgent.previewWithPlanning(text, itemsSnapshot)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                VoiceInputState.Error("${inventoryAgent.mode.displayName}解析失败，请稍后再试", text)
            }
            val current = _voiceInputState.value
            if (current is VoiceInputState.Parsing && current.recognizedText == text) {
                _voiceInputState.value = preview
            }
        }
    }

    fun failVoiceInput(message: String, recognizedText: String? = null) {
        voiceParseJob?.cancel()
        _voiceInputState.value = VoiceInputState.Error(message, recognizedText)
    }

    fun cancelVoiceInput() {
        voiceParseJob?.cancel()
        _voiceInputState.value = VoiceInputState.Idle
    }

    fun selectVoiceCandidate(item: Item) {
        val currentState = _voiceInputState.value as? VoiceInputState.NeedsSelection ?: return
        if (currentState.candidates.none { it.id == item.id }) return

        _voiceInputState.value = VoiceInputState.PendingConfirmation(
            recognizedText = currentState.recognizedText,
            action = currentState.action,
            matchedItem = item,
            diagnostics = currentState.diagnostics
        )
    }

    fun confirmVoiceAction() {
        val pending = _voiceInputState.value as? VoiceInputState.PendingConfirmation ?: return
        _voiceInputState.value = VoiceInputState.Executing(pending.recognizedText)
        viewModelScope.launch {
            val result = try {
                val executionState = actionExecutor.execute(pending, actionStore)
                if (executionState is VoiceInputState.Success) {
                    inventoryAgent.rememberSuccessfulAction(pending)
                }
                executionState
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                VoiceInputState.Error("语音操作执行失败，请稍后再试", pending.recognizedText)
            }
            _voiceInputState.value = result
        }
    }

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun addItem(item: Item, onSuccess: () -> Unit = {}) {
        launchWriteOperation("添加失败，请稍后再试") {
            repository.insert(item)
            _showAddDialog.value = false
            onSuccess()
        }
    }

    fun deleteItem(item: Item, onSuccess: () -> Unit = {}) {
        launchWriteOperation("删除失败，请稍后再试") {
            repository.delete(item)
            onSuccess()
        }
    }

    fun markAsConsumed(item: Item, type: ConsumeType, onSuccess: () -> Unit = {}) {
        launchWriteOperation("标记失败，请稍后再试") {
            repository.markAsConsumed(item.id, type)
            onSuccess()
        }
    }

    fun updateUsedQuantity(item: Item, quantity: Int) {
        launchWriteOperation("数量更新失败，请稍后再试") {
            val delta = quantity.coerceAtLeast(0) - item.usedQuantity
            if (delta != 0) {
                handleInventoryChangeResult(repository.adjustUsedQuantity(item.id, delta))
            }
        }
    }

    fun adjustUsedQuantity(item: Item, delta: Int) {
        launchWriteOperation("数量更新失败，请稍后再试") {
            handleInventoryChangeResult(repository.adjustUsedQuantity(item.id, delta))
        }
    }

    fun deleteAllConsumed() {
        launchWriteOperation("清理失败，请稍后再试") {
            repository.deleteAllConsumed()
        }
    }

    fun getItemDetailState(itemId: Long): Flow<ItemDetailUiState> {
        return repository.getItemByIdFlow(itemId)
            .map<Item?, ItemDetailUiState> { item ->
                if (item == null) ItemDetailUiState.Missing else ItemDetailUiState.Loaded(item)
            }
            .onStart { emit(ItemDetailUiState.Loading) }
            .catch { emit(ItemDetailUiState.Missing) }
    }

    fun getExpiryStatus(item: Item): ExpiryStatus = repository.getExpiryStatus(item)
    fun getDaysUntilExpiry(item: Item): Int = repository.getDaysUntilExpiry(item)

    private fun handleInventoryChangeResult(result: InventoryChangeResult) {
        when (result) {
            is InventoryChangeResult.Applied -> Unit
            InventoryChangeResult.Missing -> _operationError.value = "库存不存在或已被删除"
            InventoryChangeResult.AlreadyConsumed -> _operationError.value = "该库存已经处理完成"
            is InventoryChangeResult.InsufficientQuantity -> _operationError.value = "库存数量不足，请刷新后重试"
            InventoryChangeResult.InvalidQuantity -> _operationError.value = "数量更新失败，请稍后再试"
            InventoryChangeResult.Conflict -> _operationError.value = "库存刚刚发生变化，请刷新后重试"
        }
    }

    private fun launchWriteOperation(
        errorMessage: String,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _operationError.value = errorMessage
            }
        }
    }

    private fun refreshTodayAtMidnight() {
        viewModelScope.launch {
            while (isActive) {
                _today.value = repository.today()
                delay(millisUntilNextDay())
            }
        }
    }

    private fun millisUntilNextDay(): Long {
        val now = ZonedDateTime.now()
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        return (Duration.between(now, nextDay).toMillis() + DATE_REFRESH_GRACE_MILLIS)
            .coerceAtLeast(MIN_DATE_REFRESH_DELAY_MILLIS)
    }

    private companion object {
        private const val DATE_REFRESH_GRACE_MILLIS = 1_000L
        private const val MIN_DATE_REFRESH_DELAY_MILLIS = 60_000L
    }
}
