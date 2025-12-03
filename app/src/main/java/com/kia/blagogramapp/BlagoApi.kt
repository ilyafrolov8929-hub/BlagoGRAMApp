package com.kia.blagogramapp

import com.kia.blagogramapp.api.ApiRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface BlagoApi {

    @POST("index.php")
    suspend fun call(
        @Body request: ApiRequest
    ): Response<Map<String, Any>>
}
