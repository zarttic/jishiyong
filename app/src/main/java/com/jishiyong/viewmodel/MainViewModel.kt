package com.jishiyong.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jishiyong.JiShiYongApp
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.data.repository.ItemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository =
        (application as JiShiYongApp).repository

    // ======================== UI 状态 ========================

    private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
    val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    // ======================== 数据流 ========================

    /** 活跃物品列表 */
    val activeItems: StateFlow<List<Item>> = combine(
        repository.getActiveItems(),
        _selectedCategory,
        _searchQuery
    ) { items, category, query ->
        items.filter { item ->
            (category == null || item.category == category) &&
                    (query.isEmpty() || item.name.contains(query, ignoreCase = true) ||
                            item.note.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 已消费物品列表 */
    val consumedItems: StateFlow<List<Item>> = repository.getConsumedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 总物品数 */
    val totalCount: StateFlow<Int> = repository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 活跃物品数 */
    val activeCount: StateFlow<Int> = repository.getActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 已过期物品数 */
    val expiredCount: StateFlow<Int> = repository.getExpiredCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ======================== 操作 ========================

    fun setCategory(category: ItemCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun addItem(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
            _showAddDialog.value = false
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun markAsConsumed(item: Item, type: ConsumeType) {
        viewModelScope.launch {
            repository.markAsConsumed(item.id, type)
        }
    }

    fun updateUsedQuantity(item: Item, quantity: Int) {
        viewModelScope.launch {
            if (quantity >= item.quantity) {
                repository.markAsConsumed(item.id, ConsumeType.USED_UP)
            } else {
                repository.updateUsedQuantity(item.id, quantity)
            }
        }
    }

    fun deleteAllConsumed() {
        viewModelScope.launch {
            repository.deleteAllConsumed()
        }
    }

    fun getExpiryStatus(item: Item): ExpiryStatus = repository.getExpiryStatus(item)
    fun getDaysUntilExpiry(item: Item): Int = repository.getDaysUntilExpiry(item)
}
