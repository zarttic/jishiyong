package com.jishiyong.data.db.converter

import androidx.room.TypeConverter
import com.jishiyong.data.db.entity.ConsumeType
import com.jishiyong.data.db.entity.ItemCategory
import org.json.JSONArray
import org.json.JSONException

/**
 * Room 类型转换器
 */
class ListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return JSONArray(value ?: emptyList<String>()).toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        val text = value?.trim().orEmpty()
        if (text.isBlank() || text == "null") return emptyList()

        return try {
            val jsonArray = JSONArray(text)
            List(jsonArray.length()) { index -> jsonArray.optString(index) }
                .filter { it.isNotBlank() }
        } catch (_: JSONException) {
            text.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return JSONArray(value ?: DEFAULT_REMINDER_DAYS).toString()
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        val text = value?.trim().orEmpty()
        if (text.isBlank() || text == "null") return DEFAULT_REMINDER_DAYS

        val values = try {
            val jsonArray = JSONArray(text)
            List(jsonArray.length()) { index ->
                when (val item = jsonArray.opt(index)) {
                    is Number -> item.toInt()
                    is String -> item.toIntOrNull()
                    else -> null
                }
            }
        } catch (_: JSONException) {
            text.split(",").map { it.trim().toIntOrNull() }
        }

        return values
            .mapNotNull { it }
            .filter { it > 0 }
            .ifEmpty { DEFAULT_REMINDER_DAYS }
    }

    @TypeConverter
    fun fromItemCategory(value: ItemCategory?): String {
        return (value ?: ItemCategory.OTHER).name
    }

    @TypeConverter
    fun toItemCategory(value: String?): ItemCategory {
        val text = value?.trim().orEmpty()
        return ItemCategory.entries.firstOrNull {
            it.name.equals(text, ignoreCase = true) || it.displayName == text
        } ?: ItemCategory.OTHER
    }

    @TypeConverter
    fun fromConsumeType(value: ConsumeType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toConsumeType(value: String?): ConsumeType? {
        val text = value?.trim().orEmpty()
        if (text.isBlank() || text == "null") return null
        return ConsumeType.entries.firstOrNull {
            it.name.equals(text, ignoreCase = true) || it.displayName == text
        }
    }

    private companion object {
        val DEFAULT_REMINDER_DAYS = listOf(7, 3, 1)
    }
}
