package com.gallery.image512.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gallery.image512.data.auth.AuthManager
import com.gallery.image512.data.model.AppState
import com.gallery.image512.data.model.UploadQueueItem
import com.gallery.image512.data.model.UploadStatus
import com.gallery.image512.data.model.UserImage
import com.gallery.image512.data.preferences.PreferencesManager
import com.gallery.image512.data.repository.ImageRepository
import com.gallery.image512.util.ConnectivityObserver
import com.gallery.image512.util.ImageCompressor
import com.gallery.image512.util.NetworkConnectivityObserver
import com.gallery.image512.worker.ImageUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ImageRepository(application)
    private val authManager = AuthManager(application)
    private val preferencesManager = PreferencesManager(application)
    private val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(application)
    private val workManager = WorkManager.getInstance(application)
    
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    private var userId: String = "anonymous"
    
    init {
        loadUserId()
        loadStats()
        loadUserImages()
        observeConnectivity()
    }
    
    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                val isOnline = status == ConnectivityObserver.Status.Available
                _state.update { it.copy(isOnline = isOnline) }
                
                if (isOnline && _state.value.uploadQueue.any { it.status == UploadStatus.FAILED }) {
                    // Auto-retry failed uploads when connection is restored
                    retryFailedUploads()
                }
            }
        }
    }
    
    private fun loadUserId() {
        viewModelScope.launch {
            userId = authManager.getUserId() ?: "anonymous"
            loadUserImages()
        }
    }
    
    fun loadUserImages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingGallery = true) }
            
            repository.getUserImages(userId).fold(
                onSuccess = { images ->
                    _state.update {
                        it.copy(
                            userImages = images.sortedByDescending { img -> img.uploadedAt },
                            isLoadingGallery = false
                        )
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("MainViewModel", "Failed to load user images: ${error.message}")
                    _state.update { it.copy(isLoadingGallery = false) }
                }
            )
        }
    }
    
    fun loadStats() {
        viewModelScope.launch {
            repository.getIndexStats().onSuccess { count ->
                _state.update { it.copy(totalImages = count) }
            }
        }
    }
    
    fun uploadImage(imageUri: Uri, filename: String) {
        viewModelScope.launch {
            // Get file size to determine if we should show compression dialog
            val fileSizeMB = ImageCompressor.getFileSizeMB(getApplication(), imageUri)
            
            // Create queue item
            val queueItem = UploadQueueItem(
                uri = imageUri.toString(),
                filename = filename,
                userId = userId,
                status = UploadStatus.PENDING,
                fileSizeMB = fileSizeMB
            )
            
            // Add to queue
            _state.update { 
                it.copy(
                    uploadQueue = it.uploadQueue + queueItem,
                    uploadMessage = "Added to upload queue"
                )
            }
            
            // Start upload worker
            enqueueUploadWork(queueItem, fileSizeMB)
            
            // Clear message after 3 seconds
            kotlinx.coroutines.delay(3000)
            _state.update { it.copy(uploadMessage = "") }
        }
    }
    
    fun uploadMultipleImages(imageUris: List<Uri>) {
        viewModelScope.launch {
            _state.update { 
                it.copy(uploadMessage = "Adding ${imageUris.size} image(s) to queue...")
            }
            
            val newQueueItems = imageUris.mapIndexed { index, uri ->
                val filename = "image_${System.currentTimeMillis()}_${index}.jpg"
                val fileSizeMB = ImageCompressor.getFileSizeMB(getApplication(), uri)
                
                UploadQueueItem(
                    uri = uri.toString(),
                    filename = filename,
                    userId = userId,
                    status = UploadStatus.PENDING,
                    fileSizeMB = fileSizeMB
                )
            }
            
            // Add all to queue
            _state.update { 
                it.copy(
                    uploadQueue = it.uploadQueue + newQueueItems,
                    uploadMessage = "Added ${newQueueItems.size} image(s) to upload queue"
                )
            }
            
            // Start upload workers for each
            newQueueItems.forEach { queueItem ->
                enqueueUploadWork(queueItem, queueItem.fileSizeMB)
            }
            
            // Clear message after 3 seconds
            kotlinx.coroutines.delay(3000)
            _state.update { it.copy(uploadMessage = "") }
        }
    }
    
    private fun enqueueUploadWork(queueItem: UploadQueueItem, fileSizeMB: Double) {
        viewModelScope.launch {
            // Check if we should compress (only for images > 2MB)
            val shouldCompress = fileSizeMB > 2.0
            
            // Build work request
            val uploadWorkRequest = OneTimeWorkRequestBuilder<ImageUploadWorker>()
                .setInputData(
                    workDataOf(
                        ImageUploadWorker.KEY_IMAGE_URI to queueItem.uri,
                        ImageUploadWorker.KEY_FILENAME to queueItem.filename,
                        ImageUploadWorker.KEY_USER_ID to queueItem.userId,
                        ImageUploadWorker.KEY_COMPRESS to shouldCompress,
                        ImageUploadWorker.KEY_QUALITY_HIGH to false // Default to balanced quality
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, // Initial backoff: 30 seconds
                    TimeUnit.SECONDS
                )
                .addTag(ImageUploadWorker.WORK_TAG_UPLOAD)
                .addTag(queueItem.id)
                .build()
            
            // Enqueue work
            workManager.enqueue(uploadWorkRequest)
            
            // Observe work progress
            workManager.getWorkInfoByIdLiveData(uploadWorkRequest.id).observeForever { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(ImageUploadWorker.KEY_PROGRESS, 0)
                        updateQueueItemStatus(queueItem.id, UploadStatus.UPLOADING, progress)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        updateQueueItemStatus(queueItem.id, UploadStatus.COMPLETED, 100)
                        // Reload user images and stats
                        loadUserImages()
                        loadStats()
                        // Remove from queue after delay
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(2000)
                            removeFromQueue(queueItem.id)
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(ImageUploadWorker.KEY_ERROR)
                        updateQueueItemStatus(queueItem.id, UploadStatus.FAILED, 0, error)
                    }
                    WorkInfo.State.CANCELLED -> {
                        updateQueueItemStatus(queueItem.id, UploadStatus.CANCELLED, 0)
                    }
                    else -> {
                        // ENQUEUED or BLOCKED
                    }
                }
            }
        }
    }
    
    private fun updateQueueItemStatus(
        id: String,
        status: UploadStatus,
        progress: Int = 0,
        error: String? = null
    ) {
        _state.update { currentState ->
            currentState.copy(
                uploadQueue = currentState.uploadQueue.map { item ->
                    if (item.id == id) {
                        item.copy(status = status, progress = progress, error = error)
                    } else {
                        item
                    }
                }
            )
        }
    }
    
    private fun removeFromQueue(id: String) {
        _state.update { currentState ->
            currentState.copy(
                uploadQueue = currentState.uploadQueue.filter { it.id != id }
            )
        }
    }
    
    fun retryUpload(queueItem: UploadQueueItem) {
        // Update status to pending
        updateQueueItemStatus(queueItem.id, UploadStatus.PENDING, 0, null)
        // Enqueue again
        enqueueUploadWork(queueItem, queueItem.fileSizeMB)
    }
    
    fun retryFailedUploads() {
        val failedItems = _state.value.uploadQueue.filter { it.status == UploadStatus.FAILED }
        failedItems.forEach { retryUpload(it) }
    }
    
    fun cancelUpload(queueItem: UploadQueueItem) {
        // Cancel work
        workManager.cancelAllWorkByTag(queueItem.id)
        // Update status
        updateQueueItemStatus(queueItem.id, UploadStatus.CANCELLED, 0)
        // Remove after delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            removeFromQueue(queueItem.id)
        }
    }
    
    fun clearCompletedUploads() {
        _state.update { currentState ->
            currentState.copy(
                uploadQueue = currentState.uploadQueue.filter { 
                    it.status != UploadStatus.COMPLETED 
                }
            )
        }
    }
    
    fun toggleUploadQueueSheet() {
        _state.update { it.copy(showUploadQueue = !it.showUploadQueue) }
    }
    
    fun searchImages(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchMessage = "Please enter a search query") }
            return
        }
        
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSearching = true,
                    searchMessage = "Searching...",
                    showResults = false
                )
            }
            
            repository.searchImages(query, userId).fold(
                onSuccess = { results ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            searchMessage = "Found ${results.size} result(s)",
                            searchResults = results,
                            showResults = true
                        )
                    }
                    // Clear message after 5 seconds
                    kotlinx.coroutines.delay(5000)
                    _state.update { it.copy(searchMessage = "") }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            searchMessage = error.message ?: "Search failed",
                            searchResults = emptyList(),
                            showResults = false
                        )
                    }
                    // Clear message after 5 seconds
                    kotlinx.coroutines.delay(5000)
                    _state.update { it.copy(searchMessage = "") }
                }
            )
        }
    }
    
    fun clearSearchResults() {
        _state.update { 
            it.copy(
                showResults = false,
                searchResults = emptyList(),
                searchMessage = ""
            )
        }
    }
    
    fun selectImage(image: UserImage?) {
        _state.update { it.copy(selectedImage = image) }
    }
    
    fun deleteImage(image: UserImage) {
        viewModelScope.launch {
            repository.deleteImage(image.id, userId).fold(
                onSuccess = { message ->
                    // Remove from local state
                    _state.update {
                        it.copy(
                            userImages = it.userImages.filter { img -> img.id != image.id },
                            selectedImage = null,
                            uploadMessage = message
                        )
                    }
                    // Reload stats
                    loadStats()
                    // Clear message after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    _state.update { it.copy(uploadMessage = "") }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            uploadMessage = "Delete failed: ${error.message}"
                        )
                    }
                    // Clear message after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    _state.update { it.copy(uploadMessage = "") }
                }
            )
        }
    }
}
