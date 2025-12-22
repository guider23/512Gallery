package com.gallery.image512.ui.screens

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.image512.data.model.UserImage

@Composable
fun ImageViewerScreen(
    image: UserImage,
    onDismiss: () -> Unit,
    onDelete: (UserImage) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(image.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = image.filename,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp),
                contentScale = ContentScale.Fit
            )
            
            // Bottom action bar
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1A)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = image.filename,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Copy Link
                        ActionButton(
                            icon = Icons.Default.Link,
                            label = "Copy Link",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Image URL", image.imageUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                        
                        // Download/Save
                        ActionButton(
                            icon = Icons.Default.Download,
                            label = "Save",
                            onClick = {
                                try {
                                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    val request = DownloadManager.Request(image.imageUrl.toUri())
                                        .setTitle(image.filename)
                                        .setDescription("Downloading image")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "512Gallery/${image.filename}")
                                        .setAllowedOverMetered(true)
                                        .setAllowedOverRoaming(true)
                                    
                                    downloadManager.enqueue(request)
                                    Toast.makeText(context, "Downloading ${image.filename}...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        
                        // Delete
                        ActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = {
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Image?", color = Color.White) },
            text = { 
                Text(
                    "This will permanently delete this image from your gallery and make it inaccessible.",
                    color = Color(0xFFCCCCCC)
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(image)
                        showDeleteConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = Color(0xFF7FFF00))
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF7FFF00),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
