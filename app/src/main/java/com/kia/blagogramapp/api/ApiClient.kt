package com.kia.blagogramapp.api

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Клиент для Long Polling (длительное ожидание ответа)
    val longPollingClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // Ждем ответа от сервера до 90 секунд
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Специальный клиент для скачивания: добавляем логи только для заголовков
    val downloadClient = OkHttpClient.Builder()
        .addInterceptor(logging) 
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://blagogram.ru/api/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val longPollingRetrofit = Retrofit.Builder()
        .baseUrl("https://blagogram.ru/api/v1/")
        .client(longPollingClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
    val longPollingApi: ApiService = longPollingRetrofit.create(ApiService::class.java)

    val jamendoRetrofit = Retrofit.Builder()
        .baseUrl("https://api.jamendo.com/v3.0/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val jamendoApi: JamendoApi = jamendoRetrofit.create(JamendoApi::class.java)
}
