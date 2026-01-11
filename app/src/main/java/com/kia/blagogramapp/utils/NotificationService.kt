package com.kia.blagogramapp.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.*

class NotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var checkJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        startForeground(888, NotificationHelper.getServiceNotification(this))
        startChecking()
    }

    private fun startChecking() {
        checkJob?.cancel()
        checkJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkMessages()
                } catch (e: Exception) {
                    Log.e("NotificationService", "Error checking messages", e)
                }
                delay(60_00) // Проверяем раз в минуту
            }
        }
    }

    private suspend fun checkMessages() {
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        if (token.isEmpty()) return

        val repo = ApiRepository(RetrofitClient.api)
        val chats = repo.getChats(token)

        val currentUnreadCount = chats.sumOf { it.unread }
        val lastUnreadCount = prefs.getInt("last_unread_total", 0)

        if (currentUnreadCount > lastUnreadCount) {
            val newMessagesChats = chats.filter { it.unread > 0 }
            if (newMessagesChats.isNotEmpty()) {
                // Шлем уведомление для первого чата с новыми сообщениями
                NotificationHelper.showMessageNotification(this, newMessagesChats[0])
            }
        }
        
        prefs.edit().putInt("last_unread_total", currentUnreadCount).apply()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, NotificationService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
