package com.jishiyong.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jishiyong.JiShiYongApp
import com.jishiyong.MainActivity
import com.jishiyong.R
import com.jishiyong.data.db.entity.Item
import com.jishiyong.data.repository.ExpiryStatus
import com.jishiyong.util.Constants
import com.jishiyong.util.DateUtils
import java.time.LocalDate

class ExpirationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? JiShiYongApp ?: return Result.failure()
        val repository = app.repository

        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success() // 没有权限，静默跳过
            }
        }

        val today = LocalDate.now()

        // 1. 检查已过期物品
        val expiredItems = repository.getExpiredItems()
        if (expiredItems.isNotEmpty()) {
            sendExpiredNotification(expiredItems)
        }

        // 2. 检查今天需要提醒的物品
        val itemsNeedingReminders = repository.getItemsNeedingReminders()
        for ((item, levels) in itemsNeedingReminders) {
            sendReminderNotification(item, levels)
        }

        return Result.success()
    }

    private fun sendExpiredNotification(expiredItems: List<Item>) {
        val text = if (expiredItems.size == 1) {
            "\"${expiredItems[0].name}\" 已过期，请及时处理！"
        } else {
            "您有 ${expiredItems.size} 件物品已过期，请及时处理！"
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            Constants.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ 物品已过期")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(Constants.NOTIFICATION_ID_BASE, notification)
    }

    private fun sendReminderNotification(item: Item, levels: List<Int>) {
        val daysLeft = DateUtils.daysUntilExpiry(item.expirationDate)
        val level = levels.minOrNull() ?: return
        val levelText = Constants.getReminderLevelText(level)

        val emoji = when {
            daysLeft <= 1 -> "🔴"
            daysLeft <= 3 -> "🟠"
            daysLeft <= 7 -> "🟡"
            else -> "🟢"
        }

        val text = if (daysLeft == 0) {
            "\"${item.name}\" 今天就要过期了！"
        } else if (daysLeft == 1) {
            "\"${item.name}\" 明天就要过期了！"
        } else {
            "\"${item.name}\" 还有 ${daysLeft} 天过期"
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            Constants.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$emoji $levelText")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()

        // 使用 item.id 作为 notification ID，避免重复
        NotificationManagerCompat.from(applicationContext)
            .notify(Constants.NOTIFICATION_ID_BASE + item.id.toInt(), notification)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
