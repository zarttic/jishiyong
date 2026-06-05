package com.jishiyong.data.db.converter

import androidx.room.TypeConverter
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.ItemCategory
import org.json.JSONArray

/**
 * Room 类型转换器
 */
class ListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val jsonArray = JSONArray(value)
        return List(jsonArray.length()) { index -> jsonArray.getString(index) }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val jsonArray = JSONArray(value)
        return List(jsonArray.length()) { index -> jsonArray.getInt(index) }
    }

    @TypeConverter
    fun fromItemCategory(value: ItemCategory): String {
        return value.name
    }

    @TypeConverter
    fun toItemCategory(value: String): ItemCategory {
        return ItemCategory.valueOf(value)
    }

    @TypeConverter
    fun fromConsumeType(value: ConsumeType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toConsumeType(value: String?): ConsumeType? {
        return value?.let { ConsumeType.valueOf(it) }
    }
}
