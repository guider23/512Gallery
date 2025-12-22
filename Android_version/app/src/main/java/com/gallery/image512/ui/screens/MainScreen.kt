package com.gallery.image512.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.image512.data.model.SearchResult
import com.gallery.image512.data.model.UploadQueueItem
import com.gallery.image512.data.model.UploadStatus
import com.gallery.image512.data.model.UserImage
import com.gallery.image512.ui.theme.*
import com.gallery.image512.ui.viewmodel.MainViewModel
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showGallery by remember { mutableStateOf(true) }
    var showCameraScreen by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadMultipleImages(uris)
        }
    }
    
    // Show camera screen
    if (showCameraScreen) {
        CameraScreen(
            onImageCaptured = { uri ->
                showCameraScreen = false
                val filename = "camera_${System.currentTimeMillis()}.jpg"
                viewModel.uploadImage(uri, filename)
            },
            onClose = { showCameraScreen = false }
        )
        return
    }
    
    // Show image viewer when an image is selected
    state.selectedImage?.let { image ->
        ImageViewerScreen(
            image = image,
            onDismiss = { viewModel.selectImage(null) },
            onDelete = { img ->
                viewModel.deleteImage(img)
            }
        )
    }
    
    Image512GalleryTheme {
        Scaffold(
            containerColor = PrimaryDark,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "512Gallery",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = BrillantFontFamily,
                            color = TextPrimary
                        )
                    },
                    actions = {
                        // Upload Queue Badge
                        if (state.uploadQueue.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = AccentGreen,
                                        contentColor = PrimaryDark
                                    ) {
                                        Text(
                                            "${state.uploadQueue.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                IconButton(onClick = { viewModel.toggleUploadQueueSheet() }) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload Queue",
                                        tint = AccentGreen
                                    )
                                }
                            }
                        }
                        
                        IconButton(onClick = { showCameraScreen = true }) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Open Camera",
                                tint = AccentGreen
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = AccentGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryDark
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = AccentGreen,
                    contentColor = PrimaryDark,
                    modifier = Modifier.size(64.dp)
                ) {
                    if (state.isUploading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Upload Image",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(PrimaryDark)
            ) {
                // Offline Banner
                if (!state.isOnline) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4A1A1A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "You're offline. Uploads will resume when connected.",
                                color = ErrorRed,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        if (it.isBlank()) {
                            viewModel.clearSearchResults()
                            showGallery = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    placeholder = {
                        Text(
                            text = "Search images by description...",
                            color = Color(0xFF666666)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF666666)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            Row {
                                if (state.showResults) {
                                    IconButton(onClick = { 
                                        searchQuery = ""
                                        viewModel.clearSearchResults()
                                        showGallery = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF666666)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { 
                                        viewModel.searchImages(searchQuery)
                                        showGallery = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Search",
                                            tint = AccentGreen
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF7FFF00)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // Status Messages
                if (state.uploadMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = state.uploadMessage,
                            color = Color(0xFF7FFF00),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                if (state.searchMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = state.searchMessage,
                            color = Color(0xFF7FFF00),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Content Section Header
                Text(
                    text = if (state.showResults) "Search Results" else "Your Gallery",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 12.dp)
                )
                
                // Image Grid
                when {
                    state.isSearching -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7FFF00))
                        }
                    }
                    state.showResults && state.searchResults.isNotEmpty() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.searchResults) { result ->
                                SearchResultCard(
                                    result = result,
                                    onClick = {
                                        // Convert SearchResult to UserImage for viewer
                                        val userImage = UserImage(
                                            id = result.filename,
                                            filename = result.filename,
                                            imageUrl = result.imageUrl
                                        )
                                        viewModel.selectImage(userImage)
                                    }
                                )
                            }
                        }
                    }
                    state.isLoadingGallery -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7FFF00))
                        }
                    }
                    state.userImages.isNotEmpty() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.userImages) { image ->
                                GalleryImageCard(
                                    image = image,
                                    onClick = { viewModel.selectImage(image) }
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "No images",
                                    tint = Color(0xFF333333),
                                    modifier = Modifier.size(80.dp)
                                )
                                Text(
                                    text = if (state.showResults) "No results found" else "No images yet",
                                    color = Color(0xFF666666),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (state.showResults) "Try a different search" else "Tap + to upload your first image",
                                    color = Color(0xFF444444),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Upload Queue Bottom Sheet
        if (state.showUploadQueue) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleUploadQueueSheet() },
                containerColor = SecondaryDark,
                tonalElevation = 0.dp
            ) {
                UploadQueueSheet(
                    queue = state.uploadQueue,
                    onRetry = { viewModel.retryUpload(it) },
                    onCancel = { viewModel.cancelUpload(it) },
                    onClearCompleted = { viewModel.clearCompletedUploads() }
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(result.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = result.filename,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Similarity Score Badge
        val similarityPercent = (result.score * 100).toInt()
        if (similarityPercent > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = Color(0xCC000000),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$similarityPercent%",
                    color = Color(0xFF7FFF00),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        
        // Rank Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            color = Color(0xCC000000),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "#${result.rank}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun GalleryImageCard(
    image: UserImage,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = image.filename,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun UploadQueueSheet(
    queue: List<UploadQueueItem>,
    onRetry: (UploadQueueItem) -> Unit,
    onCancel: (UploadQueueItem) -> Unit,
    onClearCompleted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upload Queue",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            if (queue.any { it.status == UploadStatus.COMPLETED }) {
                TextButton(onClick = onClearCompleted) {
                    Text(
                        "Clear Completed",
                        color = AccentGreen,
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No uploads in queue",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(queue) { item ->
                    UploadQueueItemCard(
                        item = item,
                        onRetry = { onRetry(item) },
                        onCancel = { onCancel(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun UploadQueueItemCard(
    item: UploadQueueItem,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TertiaryDark
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with filename and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.filename,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "${"%.2f".format(item.fileSizeMB)} MB",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                
                // Status Badge
                Surface(
                    color = when (item.status) {
                        UploadStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.2f)
                        UploadStatus.FAILED -> ErrorRed.copy(alpha = 0.2f)
                        UploadStatus.UPLOADING, UploadStatus.COMPRESSING -> AccentBlue.copy(alpha = 0.2f)
                        else -> TextSecondary.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (item.status) {
                            UploadStatus.PENDING -> "Pending"
                            UploadStatus.COMPRESSING -> "Compressing"
                            UploadStatus.UPLOADING -> "Uploading"
                            UploadStatus.COMPLETED -> "Completed"
                            UploadStatus.FAILED -> "Failed"
                            UploadStatus.CANCELLED -> "Cancelled"
                        },
                        color = when (item.status) {
                            UploadStatus.COMPLETED -> SuccessGreen
                            UploadStatus.FAILED -> ErrorRed
                            UploadStatus.UPLOADING, UploadStatus.COMPRESSING -> AccentBlue
                            else -> TextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            // Progress bar for uploading items
            if (item.status == UploadStatus.UPLOADING || item.status == UploadStatus.COMPRESSING) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AccentGreen,
                    trackColor = Color(0xFF2A2A2A)
                )
                Text(
                    text = "${item.progress}%",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Error message
            if (item.status == UploadStatus.FAILED && item.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.error,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
            
            // Action buttons for failed uploads
            if (item.status == UploadStatus.FAILED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 13.sp)
                    }
                    
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
