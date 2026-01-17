package com.kia.blagogramapp.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

data class AppUpdate(val version: String, val downloadUrl: String)

object UpdateManager {
    private const val DOWNLOAD_PAGE_URL = "https://blagogram.ru/download"

    suspend fun checkUpdates(): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(DOWNLOAD_PAGE_URL).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext null

            // Ищем ссылки вида Lume-1.2.3.apk
            val pattern = Pattern.compile("Lume-(\\d+\\.\\d+\\.\\d+)\\.apk")
            val matcher = pattern.matcher(html)

            val updates = mutableListOf<AppUpdate>()
            while (matcher.find()) {
                val version = matcher.group(1) ?: ""
                val fileName = matcher.group(0) ?: ""
                updates.add(AppUpdate(version, "https://blagogram.ru/download/$fileName"))
            }

            // Сортируем по версии (наивная сортировка, для продакшена лучше сложнее)
            updates.maxByOrNull { it.version }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error checking updates", e)
            null
        }
    }

    fun isNewerVersion(current: String, remote: String): Boolean {
        // Простейшее сравнение строк, если формат всегда X.X.X
        return remote > current
    }
}