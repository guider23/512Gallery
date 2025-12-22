package com.gallery.image512.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {
    
    private const val MAX_WIDTH = 2048
    private const val MAX_HEIGHT = 2048
    private const val DEFAULT_QUALITY = 80
    private const val HIGH_QUALITY = 90
    private const val SIZE_THRESHOLD_MB = 2.0 // Only compress images > 2MB
    
    /**
     * Compress image if it's larger than threshold
     * @param context Application context
     * @param imageUri URI of the image
     * @param filename Output filename
     * @param userPreference User's quality preference (null = auto, true = high quality)
     * @return Compressed file or original file if already small
     */
    suspend fun compressIfNeeded(
        context: Context,
        imageUri: Uri,
        filename: String,
        userPreference: Boolean? = null
    ): File = withContext(Dispatchers.IO) {
        
        // Copy to temp file first
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("Failed to open image")
        
        val tempFile = File(context.cacheDir, "temp_$filename")
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        
        val fileSizeMB = tempFile.length() / (1024.0 * 1024.0)
        
        // If file is small enough, return as-is
        if (fileSizeMB <= SIZE_THRESHOLD_MB) {
            android.util.Log.d("ImageCompressor", "Image is small (${"%.2f".format(fileSizeMB)}MB), no compression needed")
            return@withContext tempFile
        }
        
        android.util.Log.d("ImageCompressor", "Image is large (${"%.2f".format(fileSizeMB)}MB), compressing...")
        
        // Determine compression quality based on user preference
        val quality = when (userPreference) {
            true -> HIGH_QUALITY  // User wants high quality
            false -> DEFAULT_QUALITY  // User wants smaller size
            null -> DEFAULT_QUALITY  // Auto-compress for big images
        }
        
        return@withContext try {
            val compressed = Compressor.compress(context, tempFile) {
                resolution(MAX_WIDTH, MAX_HEIGHT)
                quality(quality)
                default()
            }
            val compressedSizeMB = compressed.length() / (1024.0 * 1024.0)
            android.util.Log.d("ImageCompressor", "Compressed to ${"%.2f".format(compressedSizeMB)}MB (${((1 - compressedSizeMB/fileSizeMB) * 100).toInt()}% reduction)")
            compressed
        } catch (e: Exception) {
            // If compression fails, return original
            android.util.Log.e("ImageCompressor", "Compression failed", e)
            tempFile
        }
    }
    
    /**
     * Get image dimensions without loading full bitmap
     */
    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get file size in MB
     */
    fun getFileSizeMB(context: Context, uri: Uri): Double {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val size = inputStream?.available() ?: 0
            inputStream?.close()
            size / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }
}
