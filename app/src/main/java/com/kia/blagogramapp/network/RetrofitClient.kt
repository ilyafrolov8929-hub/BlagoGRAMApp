package com.kia.blagogramapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.kia.blagogramapp.BlagoApi

object RetrofitClient {

    private const val BASE_URL = "https://blagogram.ru/api/v1/"

    val api: BlagoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BlagoApi::class.java)
    }
}
