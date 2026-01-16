package com.kia.blagogramapp.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    val gson = GsonBuilder()
        .registerTypeAdapter(Boolean::class.java, FlexibleBooleanAdapter())
        .registerTypeAdapter(Boolean::class.javaPrimitiveType, FlexibleBooleanAdapter())
        .create()

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val longPollingClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val downloadClient = OkHttpClient.Builder()
        .addInterceptor(logging) 
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://blagogram.ru/api/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val longPollingRetrofit = Retrofit.Builder()
        .baseUrl("https://blagogram.ru/api/v1/")
        .client(longPollingClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
    val longPollingApi: ApiService = longPollingRetrofit.create(ApiService::class.java)

    val jamendoRetrofit = Retrofit.Builder()
        .baseUrl("https://api.jamendo.com/v3.0/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val jamendoApi: JamendoApi = jamendoRetrofit.create(JamendoApi::class.java)
}
