package com.jishiyong.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jishiyong.JiShiYongApp
import com.jishiyong.data.db.dao.CategoryStat
import com.jishiyong.data.db.dao.ConsumeStat
import com.jishiyong.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class StatisticsUiState(
    val categoryStats: List<CategoryStat> = emptyList(),
    val monthlyConsumeStats: List<ConsumeStat> = emptyList(),
    val totalItems: Int = 0,
    val activeItems: Int = 0,
    val expiredItems: Int = 0,
    val consumedThisMonth: Int = 0,
    val wastedThisMonth: Int = 0,
    val selectedMonth: YearMonth = YearMonth.now()
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository =
        (application as JiShiYongApp).repository

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            val categoryStats = repository.getCategoryStats()
            val activeItems = repository.getActiveItems()
            val consumedItems = repository.getConsumedItems()

            // 计算本月统计
            val selectedMonth = _uiState.value.selectedMonth
            val startOfMonth = selectedMonth.atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val endOfMonth = selectedMonth.atEndOfMonth()
                .atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val monthlyStats = repository.getMonthlyConsumeStats(startOfMonth, endOfMonth)

            val consumedThisMonth = monthlyStats.sumOf { it.count }
            val wastedThisMonth = monthlyStats
                .filter { it.consumeType == com.jishiyong.data.db.entity.ConsumeType.EXPIRED }
                .sumOf { it.count }

            _uiState.value = StatisticsUiState(
                categoryStats = categoryStats,
                monthlyConsumeStats = monthlyStats,
                activeItems = categoryStats.sumOf { it.count },
                expiredItems = 0, // 需要单独查询
                consumedThisMonth = consumedThisMonth,
                wastedThisMonth = wastedThisMonth,
                selectedMonth = selectedMonth
            )
        }
    }

    fun selectMonth(month: YearMonth) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        loadStatistics()
    }
}
