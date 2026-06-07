package com.jishiyong.data.db.dao

import androidx.room.*
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.db.entity.ItemCategory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ItemDao {

    // ======================== 查询 ========================

    /** 获取所有未消费物品，按过期日期升序 */
    @Query("SELECT * FROM items WHERE isConsumed = 0 ORDER BY expirationDate ASC")
    fun getActiveItems(): Flow<List<Item>>

    /** 获取所有未消费物品快照，按过期日期升序 */
    @Query("SELECT * FROM items WHERE isConsumed = 0 ORDER BY expirationDate ASC")
    suspend fun getActiveItemsSnapshot(): List<Item>

    /** 获取所有已消费物品 */
    @Query("SELECT * FROM items WHERE isConsumed = 1 ORDER BY updatedAt DESC")
    fun getConsumedItems(): Flow<List<Item>>

    /** 按分类筛选未消费物品 */
    @Query("SELECT * FROM items WHERE isConsumed = 0 AND category = :category ORDER BY expirationDate ASC")
    fun getActiveItemsByCategory(category: ItemCategory): Flow<List<Item>>

    /** 获取指定日期前过期的未消费物品（用于提醒） */
    @Query("SELECT * FROM items WHERE isConsumed = 0 AND expirationDate <= :date ORDER BY expirationDate ASC")
    suspend fun getItemsExpiringBefore(date: LocalDate): List<Item>

    /** 获取即将过期的物品（今天到指定天数内） */
    @Query("""
        SELECT * FROM items
        WHERE isConsumed = 0
        AND expirationDate >= :today
        AND expirationDate <= :deadline
        ORDER BY expirationDate ASC
    """)
    suspend fun getItemsExpiringSoon(today: LocalDate, deadline: LocalDate): List<Item>

    /** 获取已过期的物品 */
    @Query("SELECT * FROM items WHERE isConsumed = 0 AND expirationDate < :today ORDER BY expirationDate ASC")
    suspend fun getExpiredItems(today: LocalDate): List<Item>

    /** 根据 ID 获取物品 */
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    /** 根据 ID 获取物品（Flow） */
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemByIdFlow(id: Long): Flow<Item?>

    /** 搜索物品 */
    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY expirationDate ASC")
    fun searchItems(query: String): Flow<List<Item>>

    /** 获取当前未消费库存的分类统计 */
    @Query("SELECT category, COUNT(*) as count FROM items WHERE isConsumed = 0 GROUP BY category ORDER BY count DESC")
    suspend fun getCategoryStats(): List<CategoryStat>

    /** 获取指定创建时间范围内的分类统计 */
    @Query("""
        SELECT category, COUNT(*) as count FROM items
        WHERE createdAt >= :startInclusive
        AND createdAt < :endExclusive
        GROUP BY category
        ORDER BY count DESC
    """)
    suspend fun getCategoryStatsCreatedBetween(
        startInclusive: Long,
        endExclusive: Long
    ): List<CategoryStat>

    /** 获取总物品数 */
    @Query("SELECT COUNT(*) FROM items")
    fun getTotalCount(): Flow<Int>

    /** 获取指定创建时间范围内的物品总数 */
    @Query("""
        SELECT COUNT(*) FROM items
        WHERE createdAt >= :startInclusive
        AND createdAt < :endExclusive
    """)
    suspend fun getCreatedCountBetween(startInclusive: Long, endExclusive: Long): Int

    /** 获取活跃物品数 */
    @Query("SELECT COUNT(*) FROM items WHERE isConsumed = 0")
    fun getActiveCount(): Flow<Int>

    /** 获取活跃物品数快照 */
    @Query("SELECT COUNT(*) FROM items WHERE isConsumed = 0")
    suspend fun getActiveCountSnapshot(): Int

    /** 获取已过期物品数 */
    @Query("SELECT COUNT(*) FROM items WHERE isConsumed = 0 AND expirationDate < :today")
    fun getExpiredCount(today: LocalDate): Flow<Int>

    /** 获取已过期物品数快照 */
    @Query("SELECT COUNT(*) FROM items WHERE isConsumed = 0 AND expirationDate < :today")
    suspend fun getExpiredCountSnapshot(today: LocalDate): Int

    /** 获取本月消费统计 */
    @Query("""
        SELECT consumeType, COUNT(*) as count FROM items
        WHERE isConsumed = 1
        AND updatedAt >= :startInclusive
        AND updatedAt < :endExclusive
        GROUP BY consumeType
    """)
    suspend fun getMonthlyConsumeStats(
        startInclusive: Long,
        endExclusive: Long
    ): List<ConsumeStat>

    /** 搜索活跃物品 */
    @Query("SELECT * FROM items WHERE isConsumed = 0 AND name LIKE '%' || :query || '%' ORDER BY expirationDate ASC")
    fun searchActiveItems(query: String): Flow<List<Item>>

    // ======================== 写入 ========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM items WHERE isConsumed = 1")
    suspend fun deleteAllConsumed()

    /** 标记为已消费 */
    @Query("UPDATE items SET isConsumed = 1, consumeType = :type, updatedAt = :timestamp WHERE id = :id")
    suspend fun markAsConsumed(id: Long, type: ConsumeType, timestamp: Long = System.currentTimeMillis())

    /** 更新已使用数量 */
    @Query("UPDATE items SET usedQuantity = :quantity, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateUsedQuantity(id: Long, quantity: Int, timestamp: Long = System.currentTimeMillis())
}

/** 分类统计结果 */
data class CategoryStat(
    val category: ItemCategory,
    val count: Int
)

/** 消费统计结果 */
data class ConsumeStat(
    val consumeType: ConsumeType?,
    val count: Int
)
