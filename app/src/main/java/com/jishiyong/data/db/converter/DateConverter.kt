package com.jishiyong.data.db.converter

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Room 日期类型转换器
 */
class DateConverter {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        val text = dateString?.trim().orEmpty()
        if (text.isBlank() || text == "null") return LocalDate.now()

        return try {
            LocalDate.parse(text)
        } catch (_: DateTimeParseException) {
            LocalDate.now()
        }
    }
}
