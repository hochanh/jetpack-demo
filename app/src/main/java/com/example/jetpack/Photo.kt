package com.example.jetpack

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

data class Photo(
    val url: String,
)


interface PhotoApiService {
    @GET
    fun getPhotos(@Url fullUrl: String): Call<List<Photo>>
}


object PhotoApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://example.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: PhotoApiService = retrofit.create(PhotoApiService::class.java)
}