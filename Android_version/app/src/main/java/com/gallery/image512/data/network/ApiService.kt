package com.gallery.image512.data.network

import com.gallery.image512.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// Backend Flask API Service
interface BackendApiService {
    @Multipart
    @POST("upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("userId") userId: RequestBody
    ): Response<BackendUploadResponse>
    
    @POST("search")
    suspend fun searchImages(
        @Body request: BackendSearchRequest
    ): Response<BackendSearchResponse>
    
    @POST("user_images")
    suspend fun getUserImages(
        @Body request: UserImagesRequest
    ): Response<BackendUserImagesResponse>
    
    @POST("delete_image")
    suspend fun deleteImage(
        @Body request: DeleteImageRequest
    ): Response<DeleteImageResponse>
}

interface ImgBBApiService {
    @Multipart
    @POST("1/upload")
    suspend fun uploadImage(
        @Part("key") apiKey: RequestBody,
        @Part("image") imageBase64: RequestBody
    ): Response<ImgBBResponse>
}

interface PineconeApiService {
    @POST("vectors/upsert")
    suspend fun upsertVector(
        @Header("Api-Key") apiKey: String,
        @Body request: UpsertRequest
    ): Response<UpsertResponse>
    
    @POST("query")
    suspend fun queryVectors(
        @Header("Api-Key") apiKey: String,
        @Body request: QueryRequest
    ): Response<QueryResponse>
    
    @GET("describe_index_stats")
    suspend fun getIndexStats(
        @Header("Api-Key") apiKey: String
    ): Response<IndexStatsResponse>
}
