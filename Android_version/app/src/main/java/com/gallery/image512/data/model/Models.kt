package com.gallery.image512.data.model

import com.google.gson.annotations.SerializedName

// Backend API Models
data class BackendUploadResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("total_images")
    val totalImages: Int?,
    val error: String?
)

data class BackendSearchRequest(
    val query: String,
    val userId: String
)

data class BackendSearchResponse(
    val results: List<BackendSearchResult>?,
    @SerializedName("total_images")
    val totalImages: Int?,
    val error: String?
)

data class UserImagesRequest(
    val userId: String
)

data class BackendUserImagesResponse(
    val images: List<BackendUserImage>,
    val count: Int,
    val error: String?
)

data class DeleteImageRequest(
    val imageId: String,
    val userId: String
)

data class DeleteImageResponse(
    val success: Boolean,
    val message: String?,
    val error: String?
)

data class BackendUserImage(
    val id: String,
    val filename: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("uploaded_at")
    val uploadedAt: Long? = null
)

data class BackendSearchResult(
    val rank: Int,
    val filename: String,
    val score: Float,
    val similarity: String,
    @SerializedName("image_url")
    val imageUrl: String
)

// ImgBB Models
data class ImgBBResponse(
    val success: Boolean,
    val data: ImgBBData?,
    val error: ImgBBError?
)

data class ImgBBData(
    val id: String,
    val url: String,
    @SerializedName("display_url")
    val displayUrl: String,
    @SerializedName("delete_url")
    val deleteUrl: String
)

data class ImgBBError(
    val message: String
)

// Pinecone Models
data class UpsertRequest(
    val vectors: List<Vector>
)

data class Vector(
    val id: String,
    val values: List<Float>,
    val metadata: Map<String, String>
)

data class UpsertResponse(
    @SerializedName("upsertedCount")
    val upsertedCount: Int
)

data class QueryRequest(
    val vector: List<Float>,
    @SerializedName("topK")
    val topK: Int = 1,
    @SerializedName("includeMetadata")
    val includeMetadata: Boolean = true
)

data class QueryResponse(
    val matches: List<Match>
)

data class Match(
    val id: String,
    val score: Float,
    val metadata: Map<String, String>
)

data class IndexStatsResponse(
    @SerializedName("totalRecordCount")
    val totalRecordCount: Int = 0,
    val namespaces: Map<String, NamespaceStats>? = null
)

data class NamespaceStats(
    @SerializedName("recordCount")
    val recordCount: Int = 0
)

// UI Models
data class SearchResult(
    val rank: Int,
    val filename: String,
    val score: Float,
    val similarity: String,
    val imageUrl: String
)

data class UserImage(
    val id: String,
    val filename: String,
    val imageUrl: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

data class AppState(
    val totalImages: Int = 0,
    val isUploading: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingGallery: Boolean = false,
    val uploadMessage: String = "",
    val searchMessage: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val userImages: List<UserImage> = emptyList(),
    val showResults: Boolean = false,
    val selectedImage: UserImage? = null,
    val uploadQueue: List<UploadQueueItem> = emptyList(),
    val isOnline: Boolean = true,
    val showUploadQueue: Boolean = false
)

data class UploadResult(
    val success: Boolean,
    val message: String,
    val imageUrl: String,
    val totalImages: Int
)

// Upload Queue Models
data class UploadQueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: String,
    val filename: String,
    val userId: String,
    val status: UploadStatus = UploadStatus.PENDING,
    val progress: Int = 0,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val fileSizeMB: Double = 0.0
)

enum class UploadStatus {
    PENDING,
    COMPRESSING,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

// User Preferences
data class UserPreferences(
    val compressLargeImages: Boolean = true,  // Auto-compress images > 2MB
    val compressionQuality: CompressionQuality = CompressionQuality.BALANCED
)

enum class CompressionQuality {
    HIGH,      // 90% quality, larger file
    BALANCED,  // 80% quality, good balance
    SMALL      // 70% quality, smallest file
}
