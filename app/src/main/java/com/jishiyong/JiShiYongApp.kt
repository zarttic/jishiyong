package com.jishiyong

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import com.jishiyong.data.db.AppDatabase
import com.jishiyong.data.repository.ItemRepository
import com.jishiyong.notification.ExpirationReminderWorker
import com.jishiyong.util.Constants
import java.util.concurrent.TimeUnit

class JiShiYongApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: ItemRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化数据库和仓库
        database = AppDatabase.getDatabase(this)
        repository = ItemRepository(database.itemDao())

        // 创建通知渠道
        createNotificationChannel()

        // 调度过期提醒任务
        try {
            scheduleExpirationReminder()
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to schedule expiration reminders", exception)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = Constants.NOTIFICATION_CHANNEL_DESC
            enableVibration(true)
            enableLights(true)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleExpirationReminder() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // 每天早上 9 点检查一次过期物品
        val reminderWork = PeriodicWorkRequestBuilder<ExpirationReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .addTag(Constants.REMINDER_WORK_TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )
    }

    /**
     * 计算距离下一个早上 9 点的延迟时间
     */
    private fun calculateInitialDelay(): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        private const val TAG = "JiShiYongApp"

        lateinit var instance: JiShiYongApp
            private set
    }
}
