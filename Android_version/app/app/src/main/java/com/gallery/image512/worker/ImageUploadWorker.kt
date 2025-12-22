package com.gallery.image512.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gallery.image512.data.repository.ImageRepository
import com.gallery.image512.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageUploadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val KEY_IMAGE_URI = "image_uri"
        const val KEY_FILENAME = "filename"
        const val KEY_USER_ID = "user_id"
        const val KEY_COMPRESS = "compress"
        const val KEY_QUALITY_HIGH = "quality_high"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val WORK_TAG_UPLOAD = "image_upload"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val imageUriString = inputData.getString(KEY_IMAGE_URI) 
                ?: return@withContext Result.failure()
            val filename = inputData.getString(KEY_FILENAME) 
                ?: return@withContext Result.failure()
            val userId = inputData.getString(KEY_USER_ID) 
                ?: return@withContext Result.failure()
            val shouldCompress = inputData.getBoolean(KEY_COMPRESS, true)
            val qualityHigh = inputData.getBoolean(KEY_QUALITY_HIGH, false)
            
            val imageUri = Uri.parse(imageUriString)
            
            android.util.Log.d("ImageUploadWorker", "Starting upload for $filename")
            
            // Update progress: Compressing
            setProgress(workDataOf(KEY_PROGRESS to 10))
            
            // Compress if needed
            val fileToUpload = if (shouldCompress) {
                val sizeMB = ImageCompressor.getFileSizeMB(context, imageUri)
                if (sizeMB > 2.0) {
                    android.util.Log.d("ImageUploadWorker", "Compressing image (${"%.2f".format(sizeMB)}MB)")
                    ImageCompressor.compressIfNeeded(context, imageUri, filename, qualityHigh)
                } else {
                    android.util.Log.d("ImageUploadWorker", "Skipping compression (small file)")
                    // Copy to temp file without compression
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                        ?: return@withContext Result.failure()
                    val tempFile = java.io.File(context.cacheDir, filename)
                    java.io.FileOutputStream(tempFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                    tempFile
                }
            } else {
                android.util.Log.d("ImageUploadWorker", "Compression disabled by user")
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure()
                val tempFile = java.io.File(context.cacheDir, filename)
                java.io.FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                tempFile
            }
            
            // Update progress: Uploading
            setProgress(workDataOf(KEY_PROGRESS to 50))
            
            // Upload
            android.util.Log.d("ImageUploadWorker", "Uploading file (${fileToUpload.length() / 1024}KB)")
            val repository = ImageRepository(context)
            val uploadUri = Uri.fromFile(fileToUpload)
            
            repository.uploadImage(uploadUri, filename, userId).fold(
                onSuccess = { result ->
                    // Clean up
                    fileToUpload.delete()
                    
                    android.util.Log.d("ImageUploadWorker", "Upload successful: ${result.message}")
                    
                    // Update progress: Complete
                    setProgress(workDataOf(KEY_PROGRESS to 100))
                    Result.success()
                },
                onFailure = { error ->
                    // Clean up
                    fileToUpload.delete()
                    
                    android.util.Log.e("ImageUploadWorker", "Upload failed: ${error.message}")
                    
                    // Set error data
                    val errorData = workDataOf(KEY_ERROR to (error.message ?: "Upload failed"))
                    
                    // Retry on failure (WorkManager will handle retry logic with exponential backoff)
                    if (runAttemptCount < 3) {
                        android.util.Log.d("ImageUploadWorker", "Scheduling retry (attempt ${runAttemptCount + 1}/3)")
                        Result.retry()
                    } else {
                        android.util.Log.e("ImageUploadWorker", "Max retries reached, marking as failed")
                        Result.failure(errorData)
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("ImageUploadWorker", "Worker exception", e)
            val errorData = workDataOf(KEY_ERROR to (e.message ?: "Unknown error"))
            
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(errorData)
            }
        }
    }
}
