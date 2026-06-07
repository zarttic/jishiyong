package com.jishiyong.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jishiyong.JiShiYongApp
import com.jishiyong.agent.InventoryActionExecutor
import com.jishiyong.agent.InventoryActionStore
import com.jishiyong.agent.InventoryAgent
import com.jishiyong.agent.InventoryAgentFactory
import com.jishiyong.agent.VoiceInputState
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.data.repository.ItemRepository
import com.jishiyong.update.AppUpdateChecker
import com.jishiyong.update.UpdateCheckState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ItemDetailUiState {
    data object Loading : ItemDetailUiState()
    data object Missing : ItemDetailUiState()
    data class Loaded(val item: Item) : ItemDetailUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository =
        (application as JiShiYongApp).repository
    private val updateChecker = AppUpdateChecker()
    private val inventoryAgent: InventoryAgent = InventoryAgentFactory.createDefault(application)
    private val actionExecutor = InventoryActionExecutor()
    private val actionStore = object : InventoryActionStore {
        override suspend fun insert(item: Item): Long = repository.insert(item)
        override suspend fun getItemById(id: Long): Item? = repository.getItemById(id)
        override suspend fun markAsConsumed(id: Long, type: ConsumeType) {
            repository.markAsConsumed(id, type)
        }

        override suspend fun updateUsedQuantity(id: Long, quantity: Int) {
            repository.updateUsedQuantity(id, quantity)
        }
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
    val expiredCount: StateFlow<Int> = repository.getExpiredCount()
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
            if (quantity >= item.quantity) {
                repository.markAsConsumed(item.id, ConsumeType.USED_UP)
            } else {
                repository.updateUsedQuantity(item.id, quantity.coerceAtLeast(0))
            }
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
}
