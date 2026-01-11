package com.kia.blagogramapp

import android.app.Application
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.utils.NotificationHelper
import com.kia.blagogramapp.utils.NotificationService
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DesignEngine.init(this)
        EmojiManager.install(IosEmojiProvider())

        NotificationHelper.createNotificationChannels(this)
        
        refreshSubscriptionStatus()
        
        // ЗАПУСКАЕМ ПОСТОЯННЫЙ СЕРВИС
        NotificationService.start(this)
    }

    private fun refreshSubscriptionStatus() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        if (token.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))
                val sub = repo.checkSubscription(token)
                val isActive = when (val a = sub?.active) {
                    is Boolean -> a
                    is Number -> a.toInt() == 1
                    is String -> a == "1" || a == "true"
                    else -> false
                }
                prefs.edit().putBoolean("is_premium", isActive).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}