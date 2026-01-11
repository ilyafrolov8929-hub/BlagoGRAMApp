package com.kia.blagogramapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kia.blagogramapp.ChatsActivity
import com.kia.blagogramapp.R
import com.kia.blagogramapp.model.Chat

object NotificationHelper {
    const val MESSAGES_CHANNEL_ID = "new_messages_channel"
    const val SERVICE_CHANNEL_ID = "service_channel"
    private const val MESSAGES_CHANNEL_NAME = "Новые сообщения"
    private const val SERVICE_CHANNEL_NAME = "Работа в фоне"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val msgChannel = NotificationChannel(
                MESSAGES_CHANNEL_ID,
                MESSAGES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых сообщениях"
                enableLights(true)
                enableVibration(true)
            }
            
            val svcChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Обеспечивает работу приложения в фоновом режиме"
            }
            
            notificationManager.createNotificationChannel(msgChannel)
            notificationManager.createNotificationChannel(svcChannel)
        }
    }

    fun getServiceNotification(context: Context): Notification {
        val intent = Intent(context, ChatsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("Lume")
            .setContentText("\\(★ω★)/")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showMessageNotification(context: Context, chat: Chat) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Интент ведет на ChatsActivity, которая решит, открывать ли конкретный чат
        val intent = Intent(context, ChatsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_chat_id", chat.id)
            putExtra("open_chat_name", chat.name)
            putExtra("open_chat_avatar", chat.avatar)
            putExtra("open_chat_user_id", chat.user_id ?: 0)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, chat.id + 10000, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(chat.name ?: "Lume")
            .setContentText(chat.last_msg ?: "Новое сообщение")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(chat.id, builder.build())
    }
}
