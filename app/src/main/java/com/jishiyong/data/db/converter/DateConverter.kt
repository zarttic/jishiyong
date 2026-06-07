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
        if (text.isBlank() || text == "null") return INVALID_DATE_FALLBACK

        return try {
            LocalDate.parse(text)
        } catch (_: DateTimeParseException) {
            INVALID_DATE_FALLBACK
        }
    }

    private companion object {
        val INVALID_DATE_FALLBACK: LocalDate = LocalDate.of(1970, 1, 1)
    }
}
