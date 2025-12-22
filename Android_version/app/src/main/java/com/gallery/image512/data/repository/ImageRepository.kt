package com.gallery.image512.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.gallery.image512.data.model.*
import com.gallery.image512.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ImageRepository(private val context: Context) {
    
    private val backendApi = RetrofitClient.backendApi
    private val pineconeApi = RetrofitClient.pineconeApi
    
    companion object {
        private const val TAG = "ImageRepository"
        const val PINECONE_API_KEY = "pcsk_3Wqijz_HjXLvshEn4NQrKKj39ovjf6db6696kZnBqcBnDiwwe67j8aBT9Kb91U7iwT9ak5"
    }
    
    suspend fun getIndexStats(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting index stats from Pinecone...")
                val response = pineconeApi.getIndexStats(PINECONE_API_KEY)
                
                if (response.isSuccessful) {
                    val stats = response.body()
                    val count = stats?.totalRecordCount ?: 0
                    Log.d(TAG, "Total images in index: $count")
                    Result.success(count)
                } else {
                    Log.e(TAG, "Failed to get stats: ${response.code()}")
                    Result.success(0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting index stats", e)
                Result.success(0)
            }
        }
    }
    
    suspend fun uploadImage(imageUri: Uri, filename: String, userId: String): Result<UploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Uploading image to backend ===")
                Log.d(TAG, "Image URI: $imageUri")
                Log.d(TAG, "Filename: $filename")
                Log.d(TAG, "UserId: $userId")
                
                // Copy URI to temp file
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure(Exception("Failed to open image"))
                
                val tempFile = File(context.cacheDir, filename)
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                
                Log.d(TAG, "Temp file created: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
                
                // Create multipart request
                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", filename, requestFile)
                val userIdPart = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), userId)
                
                Log.d(TAG, "Calling backend /upload endpoint...")
                val response = backendApi.uploadImage(filePart, userIdPart)
                
                // Clean up temp file
                tempFile.delete()
                
                Log.d(TAG, "Backend response code: ${response.code()}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Backend error: $errorBody")
                    return@withContext Result.failure(
                        Exception("Backend upload failed: HTTP ${response.code()}")
                    )
                }
                
                val body = response.body()
                Log.d(TAG, "Response: success=${body?.success}, message=${body?.message}")
                
                if (body?.success == true) {
                    Result.success(
                        UploadResult(
                            success = true,
                            message = body.message ?: "Upload successful",
                            imageUrl = body.imageUrl ?: "",
                            totalImages = body.totalImages ?: 0
                        )
                    )
                } else {
                    Result.failure(Exception(body?.error ?: "Upload failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload exception", e)
                Result.failure(Exception("Upload failed: ${e.message}", e))
            }
        }
    }
    
    suspend fun searchImages(query: String, userId: String): Result<List<SearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Searching images via backend ===")
                Log.d(TAG, "Query: $query")
                Log.d(TAG, "UserId: $userId")
                
                val request = BackendSearchRequest(query, userId)
                val response = backendApi.searchImages(request)
                
                Log.d(TAG, "Search response code: ${response.code()}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Search error: $errorBody")
                    return@withContext Result.failure(
                        Exception("Search failed: HTTP ${response.code()}")
                    )
                }
                
                val body = response.body()
                
                if (body?.error != null) {
                    return@withContext Result.failure(Exception(body.error))
                }
                
                val backendResults = body?.results ?: emptyList()
                
                if (backendResults.isEmpty()) {
                    return@withContext Result.failure(Exception("No matches found"))
                }
                
                // Convert backend results to app results
                val results = backendResults.map { backendResult ->
                    SearchResult(
                        rank = backendResult.rank,
                        filename = backendResult.filename,
                        score = backendResult.score,
                        similarity = backendResult.similarity,
                        imageUrl = backendResult.imageUrl
                    )
                }
                
                Log.d(TAG, "Found ${results.size} results")
                Result.success(results)
            } catch (e: Exception) {
                Log.e(TAG, "Search exception", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getUserImages(userId: String): Result<List<UserImage>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Fetching user images from backend ===")
                Log.d(TAG, "UserId: $userId")
                
                val request = UserImagesRequest(userId)
                val response = backendApi.getUserImages(request)
                
                Log.d(TAG, "User images response code: ${response.code()}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Fetch user images error: $errorBody")
                    return@withContext Result.failure(
                        Exception("Failed to fetch images: HTTP ${response.code()}")
                    )
                }
                
                val body = response.body()
                
                if (body?.error != null) {
                    return@withContext Result.failure(Exception(body.error))
                }
                
                val backendImages = body?.images ?: emptyList()
                
                val images = backendImages.map { backendImage ->
                    UserImage(
                        id = backendImage.id,
                        filename = backendImage.filename,
                        imageUrl = backendImage.imageUrl,
                        uploadedAt = backendImage.uploadedAt ?: System.currentTimeMillis()
                    )
                }
                
                Log.d(TAG, "Fetched ${images.size} user images")
                Result.success(images)
            } catch (e: Exception) {
                Log.e(TAG, "Fetch user images exception", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteImage(imageId: String, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Deleting image ===")
                Log.d(TAG, "ImageId: $imageId")
                Log.d(TAG, "UserId: $userId")
                
                val request = DeleteImageRequest(imageId, userId)
                val response = backendApi.deleteImage(request)
                
                Log.d(TAG, "Delete response code: ${response.code()}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Delete error: $errorBody")
                    return@withContext Result.failure(
                        Exception("Failed to delete image: HTTP ${response.code()}")
                    )
                }
                
                val body = response.body()
                
                if (body?.error != null) {
                    return@withContext Result.failure(Exception(body.error))
                }
                
                Log.d(TAG, "Image deleted successfully")
                Result.success(body?.message ?: "Image deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Delete image exception", e)
                Result.failure(e)
            }
        }
    }
}
