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
import com.jishiyong.MainActivity
import com.jishiyong.R
import com.jishiyong.data.db.entity.Item
import com.jishiyong.util.Constants
import com.jishiyong.util.DateUtils

/**
 * 通知辅助工具类
 */
object NotificationHelper {

    /**
     * 发送即时提醒通知（用于测试或手动触发）
     */
    fun sendImmediateReminder(context: Context, item: Item) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val daysLeft = DateUtils.daysUntilExpiry(item.expirationDate)
        val emoji = when {
            daysLeft <= 1 -> "🔴"
            daysLeft <= 3 -> "🟠"
            else -> "🟡"
        }

        val text = if (daysLeft <= 0) {
            "\"${item.name}\" 已过期，请及时处理！"
        } else {
            "\"${item.name}\" 还有 ${daysLeft} 天过期"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$emoji 过期提醒")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(Constants.NOTIFICATION_ID_BASE + item.id.toInt(), notification)
    }
}
