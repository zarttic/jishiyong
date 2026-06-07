package com.jishiyong.data.repository

import com.jishiyong.data.db.dao.CategoryStat
import com.jishiyong.data.db.dao.ConsumeStat
import com.jishiyong.data.db.dao.ItemDao
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 物品数据仓库
 */
class ItemRepository(private val itemDao: ItemDao) {

    // ======================== 查询 ========================

    fun getActiveItems(): Flow<List<Item>> = itemDao.getActiveItems()

    fun getConsumedItems(): Flow<List<Item>> = itemDao.getConsumedItems()

    fun getActiveItemsByCategory(category: ItemCategory): Flow<List<Item>> =
        itemDao.getActiveItemsByCategory(category)

    suspend fun getItemsExpiringBefore(date: LocalDate): List<Item> =
        itemDao.getItemsExpiringBefore(date)

    suspend fun getItemsExpiringSoon(today: LocalDate, daysAhead: Int): List<Item> {
        val deadline = today.plusDays(daysAhead.toLong())
        return itemDao.getItemsExpiringSoon(today, deadline)
    }

    suspend fun getExpiredItems(): List<Item> =
        itemDao.getExpiredItems(LocalDate.now())

    suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)

    fun getItemByIdFlow(id: Long): Flow<Item?> = itemDao.getItemByIdFlow(id)

    fun searchItems(query: String): Flow<List<Item>> = itemDao.searchItems(query)

    fun searchActiveItems(query: String): Flow<List<Item>> = itemDao.searchActiveItems(query)

    fun getTotalCount(): Flow<Int> = itemDao.getTotalCount()

    fun getActiveCount(): Flow<Int> = itemDao.getActiveCount()

    fun getExpiredCount(): Flow<Int> = itemDao.getExpiredCount(LocalDate.now())

    suspend fun getCategoryStats(): List<CategoryStat> = itemDao.getCategoryStats()

    suspend fun getMonthlyConsumeStats(startOfMonth: Long, endOfMonth: Long): List<ConsumeStat> =
        itemDao.getMonthlyConsumeStats(startOfMonth, endOfMonth)

    // ======================== 写入 ========================

    suspend fun insert(item: Item): Long = itemDao.insert(item)

    suspend fun update(item: Item) = itemDao.update(item.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(item: Item) = itemDao.delete(item)

    suspend fun deleteById(id: Long) = itemDao.deleteById(id)

    suspend fun markAsConsumed(id: Long, type: ConsumeType) =
        itemDao.markAsConsumed(id, type)

    suspend fun updateUsedQuantity(id: Long, quantity: Int) =
        itemDao.updateUsedQuantity(id, quantity)

    suspend fun deleteAllConsumed() = itemDao.deleteAllConsumed()

    // ======================== 业务逻辑 ========================

    /**
     * 获取需要提醒的物品及对应提醒级别
     */
    suspend fun getItemsNeedingReminders(): Map<Item, List<Int>> {
        val today = LocalDate.now()
        val items = itemDao.getItemsExpiringSoon(today, today.plusDays(31))
        val result = mutableMapOf<Item, List<Int>>()

        for (item in items) {
            val triggeredLevels = getTriggeredReminderLevels(item, today)
            if (triggeredLevels.isNotEmpty()) {
                result[item] = triggeredLevels
            }
        }
        return result
    }

    /**
     * 获取物品剩余天数
     */
    fun getDaysUntilExpiry(item: Item): Int {
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, item.expirationDate).toInt()
    }

    /**
     * 获取物品状态描述
     */
    fun getExpiryStatus(item: Item): ExpiryStatus {
        val days = getDaysUntilExpiry(item)
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= 3 -> ExpiryStatus.EXPIRING_CRITICAL
            days <= 7 -> ExpiryStatus.EXPIRING_SOON
            days <= 30 -> ExpiryStatus.EXPIRING_WARNING
            else -> ExpiryStatus.FRESH
        }
    }
}

/**
 * 过期状态枚举
 */
enum class ExpiryStatus(val displayName: String) {
    FRESH("新鲜"),
    EXPIRING_WARNING("即将过期"),
    EXPIRING_SOON("临近过期"),
    EXPIRING_CRITICAL("紧急"),
    EXPIRED("已过期")
}

fun getTriggeredReminderLevels(item: Item, today: LocalDate): List<Int> {
    val daysUntilExpiry = ChronoUnit.DAYS.between(today, item.expirationDate).toInt()
    if (daysUntilExpiry < 0) return emptyList()

    return item.reminderDays
        .filter { it == daysUntilExpiry }
        .distinct()
        .sorted()
}
