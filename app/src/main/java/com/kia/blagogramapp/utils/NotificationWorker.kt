package com.kia.blagogramapp.utils

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.delay

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val NOTIFICATION_ID = 888
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationHelper.getServiceNotification(applicationContext)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Failed to set foreground", e)
        }

        val prefs = applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        if (token.isEmpty()) return Result.success()

        return try {
            val repo = ApiRepository(RetrofitClient.api)
            val chats = repo.getChats(token)

            val currentUnreadCount = chats.sumOf { it.unread }
            val lastUnreadCount = prefs.getInt("last_unread_total", 0)

            if (currentUnreadCount > lastUnreadCount) {
                val newMessagesChats = chats.filter { it.unread > 0 }
                if (newMessagesChats.isNotEmpty()) {
                    // Передаем весь объект Chat в обновленный метод
                    NotificationHelper.showMessageNotification(
                        applicationContext, 
                        newMessagesChats[0]
                    )
                }
            }

            prefs.edit().putInt("last_unread_total", currentUnreadCount).apply()
            
            delay(1000) 
            
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error", e)
            Result.retry()
        }
    }
}
