package com.jishiyong.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.jishiyong.data.db.converter.DateConverter
import com.jishiyong.data.db.converter.ListConverter
import java.time.LocalDate

/**
 * 物品实体类
 */
@Entity(tableName = "items")
@TypeConverters(DateConverter::class, ListConverter::class)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 物品名称 */
    val name: String,

    /** 分类 */
    val category: ItemCategory,

    /** 购买日期 */
    val purchaseDate: LocalDate,

    /** 过期日期 */
    val expirationDate: LocalDate,

    /** 备注 */
    val note: String = "",

    /** 图片路径列表 */
    val imagePaths: List<String> = emptyList(),

    /** 数量 */
    val quantity: Int = 1,

    /** 已使用数量 */
    val usedQuantity: Int = 0,

    /** 提前提醒天数列表（多级提醒） */
    val reminderDays: List<Int> = listOf(7, 3, 1),

    /** 是否已标记为已处理（用完/丢弃） */
    val isConsumed: Boolean = false,

    /** 消费方式 */
    val consumeType: ConsumeType? = null,

    /** 创建时间戳 */
    val createdAt: Long = System.currentTimeMillis(),

    /** 更新时间戳 */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 物品分类
 */
enum class ItemCategory(val displayName: String, val icon: String) {
    FOOD("食品", "🍽️"),
    DRINK("饮品", "🥤"),
    DAILY("日用品", "🧴"),
    MEDICINE("药品", "💊"),
    COSMETICS("化妆品", "💄"),
    ELECTRONICS("电子产品", "📱"),
    CLOTHING("服饰", "👕"),
    OTHER("其他", "📦");

    companion object {
        fun fromDisplayName(name: String): ItemCategory {
            return entries.find { it.displayName == name } ?: OTHER
        }
    }
}

/**
 * 消费方式
 */
enum class ConsumeType(val displayName: String) {
    USED_UP("已用完"),
    DISCARDED("已丢弃"),
    EXPIRED("已过期"),
    GIFTED("已送人")
}
