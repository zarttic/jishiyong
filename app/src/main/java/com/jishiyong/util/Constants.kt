package com.jishiyong.util

object Constants {
    // 通知相关
    const val NOTIFICATION_CHANNEL_ID = "jishiyong_reminders"
    const val NOTIFICATION_CHANNEL_NAME = "过期提醒"
    const val NOTIFICATION_CHANNEL_DESC = "物品过期到期提醒通知"

    // WorkManager
    const val REMINDER_WORK_NAME = "expiration_reminder_work"
    const val REMINDER_WORK_TAG = "reminder"

    // 通知 ID
    const val NOTIFICATION_ID_BASE = 1000

    // 默认提醒天数
    val DEFAULT_REMINDER_DAYS = listOf(7, 3, 1)

    // 提醒级别名称
    fun getReminderLevelText(days: Int): String = when {
        days <= 1 -> "紧急提醒"
        days <= 3 -> "临近提醒"
        days <= 7 -> "提前提醒"
        else -> "预提醒"
    }

    // DataStore
    const val SETTINGS_DATASTORE = "settings"
    const val KEY_REMINDER_ENABLED = "reminder_enabled"
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"
    const val KEY_DEFAULT_REMINDER_DAYS = "default_reminder_days"
}
