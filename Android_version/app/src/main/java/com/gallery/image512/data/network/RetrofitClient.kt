package com.gallery.image512.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Heroku backend URL (deployed)
    private const val BACKEND_BASE_URL = "https://gallery512-backend-0af060311aab.herokuapp.com/"
    
    // For local testing, uncomment one of these:
    // private const val BACKEND_BASE_URL = "http://10.0.2.2:5000/" // Android emulator
    // private const val BACKEND_BASE_URL = "http://YOUR_COMPUTER_IP:5000/" // Real device
    
    private const val IMGBB_BASE_URL = "https://api.imgbb.com/"
    private const val PINECONE_BASE_URL = "https://image-retrieval-5ehrb9l.svc.aped-4627-b74a.pinecone.io/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    val backendApi: BackendApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApiService::class.java)
    }
    
    val imgBBApi: ImgBBApiService by lazy {
        Retrofit.Builder()
            .baseUrl(IMGBB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImgBBApiService::class.java)
    }
    
    val pineconeApi: PineconeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PINECONE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PineconeApiService::class.java)
    }
}
