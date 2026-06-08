package com.jishiyong.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jishiyong.AppContainerProvider
import com.jishiyong.data.db.dao.CategoryStat
import com.jishiyong.data.db.dao.ConsumeStat
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.repository.ItemRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val container = (application as AppContainerProvider).container
    private val repository: ItemRepository =
        container.repository

    private val _uiState = MutableStateFlow(
        StatisticsUiState(selectedMonth = YearMonth.from(repository.today()))
    )
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                // 计算本月统计
                val selectedMonth = _uiState.value.selectedMonth
                val startInclusive = selectedMonth.atDay(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val endExclusive = selectedMonth.plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val categoryStats = repository.getCategoryStatsCreatedBetween(
                    startInclusive = startInclusive,
                    endExclusive = endExclusive
                )
                val monthlyStats = repository.getMonthlyConsumeStats(startInclusive, endExclusive)
                val createdThisMonth = repository.getCreatedCountBetween(startInclusive, endExclusive)

                val consumedThisMonth = monthlyStats.sumOf { it.count }
                val wastedThisMonth = monthlyStats
                    .filter { it.consumeType == ConsumeType.EXPIRED }
                    .sumOf { it.count }

                _uiState.value = StatisticsUiState(
                    categoryStats = categoryStats,
                    monthlyConsumeStats = monthlyStats,
                    totalItems = createdThisMonth,
                    activeItems = repository.getActiveCountSnapshot(),
                    expiredItems = repository.getExpiredCountSnapshot(),
                    consumedThisMonth = consumedThisMonth,
                    wastedThisMonth = wastedThisMonth,
                    selectedMonth = selectedMonth
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Keep the current screen state if old local data cannot be parsed.
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        loadStatistics()
    }
}
