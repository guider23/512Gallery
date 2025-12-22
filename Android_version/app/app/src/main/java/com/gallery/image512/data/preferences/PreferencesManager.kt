package com.gallery.image512.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gallery.image512.data.model.CompressionQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val COMPRESS_LARGE_IMAGES = booleanPreferencesKey("compress_large_images")
        private val COMPRESSION_QUALITY = stringPreferencesKey("compression_quality")
        private val SHOW_COMPRESSION_DIALOG = booleanPreferencesKey("show_compression_dialog")
    }
    
    val compressLargeImages: Flow<Boolean> = context.preferencesDataStore.data
        .map { preferences ->
            preferences[COMPRESS_LARGE_IMAGES] ?: true
        }
    
    val compressionQuality: Flow<CompressionQuality> = context.preferencesDataStore.data
        .map { preferences ->
            val qualityString = preferences[COMPRESSION_QUALITY] ?: CompressionQuality.BALANCED.name
            try {
                CompressionQuality.valueOf(qualityString)
            } catch (e: IllegalArgumentException) {
                CompressionQuality.BALANCED
            }
        }
    
    val showCompressionDialog: Flow<Boolean> = context.preferencesDataStore.data
        .map { preferences ->
            preferences[SHOW_COMPRESSION_DIALOG] ?: true
        }
    
    suspend fun setCompressLargeImages(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[COMPRESS_LARGE_IMAGES] = enabled
        }
    }
    
    suspend fun setCompressionQuality(quality: CompressionQuality) {
        context.preferencesDataStore.edit { preferences ->
            preferences[COMPRESSION_QUALITY] = quality.name
        }
    }
    
    suspend fun setShowCompressionDialog(show: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[SHOW_COMPRESSION_DIALOG] = show
        }
    }
    
    suspend fun getCompressionQualityValue(): CompressionQuality {
        var quality = CompressionQuality.BALANCED
        context.preferencesDataStore.data.map { preferences ->
            val qualityString = preferences[COMPRESSION_QUALITY] ?: CompressionQuality.BALANCED.name
            quality = try {
                CompressionQuality.valueOf(qualityString)
            } catch (e: IllegalArgumentException) {
                CompressionQuality.BALANCED
            }
        }
        return quality
    }
    
    suspend fun shouldCompress(): Boolean {
        var compress = true
        context.preferencesDataStore.data.map { preferences ->
            compress = preferences[COMPRESS_LARGE_IMAGES] ?: true
        }
        return compress
    }
}
