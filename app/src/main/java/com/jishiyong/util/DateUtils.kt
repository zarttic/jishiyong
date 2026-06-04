package com.jishiyong.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    private val shortFormatter = DateTimeFormatter.ofPattern("MM/dd")
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** 格式化为中文显示 */
    fun formatChinese(date: LocalDate): String = date.format(displayFormatter)

    /** 格式化为短日期 */
    fun formatShort(date: LocalDate): String = date.format(shortFormatter)

    /** 格式化为 ISO 格式 */
    fun formatISO(date: LocalDate): String = date.format(isoFormatter)

    /** 计算距离过期的天数 */
    fun daysUntilExpiry(expirationDate: LocalDate): Int {
        return ChronoUnit.DAYS.between(LocalDate.now(), expirationDate).toInt()
    }

    /** 获取剩余天数的文字描述 */
    fun getRemainingText(expirationDate: LocalDate): String {
        val days = daysUntilExpiry(expirationDate)
        return when {
            days < 0 -> "已过期${-days}天"
            days == 0 -> "今天过期"
            days == 1 -> "明天过期"
            days <= 7 -> "${days}天后过期"
            days <= 30 -> "${days}天后过期"
            days <= 365 -> "${days / 30}个月后过期"
            else -> "${days / 365}年后过期"
        }
    }

    /** 获取友好的相对日期描述 */
    fun getRelativeDate(date: LocalDate): String {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
        return when (days) {
            -1 -> "昨天"
            0 -> "今天"
            1 -> "明天"
            in 2..6 -> "${days}天后"
            in 7..13 -> "下周"
            in 14..29 -> "${days / 7}周后"
            else -> formatChinese(date)
        }
    }
}
