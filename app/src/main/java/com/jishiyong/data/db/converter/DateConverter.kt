package com.jishiyong.data.db.converter

import androidx.room.TypeConverter
import java.time.LocalDate

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
        return dateString?.let { LocalDate.parse(it) }
    }
}
