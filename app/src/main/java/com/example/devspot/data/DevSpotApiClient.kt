package com.example.devspot.data

import com.example.devspot.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DevSpotApiClient {
    val service: DevSpotApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DevSpotApi::class.java)
    }
}
