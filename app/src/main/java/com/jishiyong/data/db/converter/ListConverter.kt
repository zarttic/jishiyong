package com.jishiyong.data.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.ItemCategory

/**
 * Room 类型转换器
 */
class ListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
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
