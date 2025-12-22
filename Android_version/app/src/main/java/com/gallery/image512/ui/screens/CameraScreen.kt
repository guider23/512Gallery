package com.gallery.image512.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gallery.image512.ui.theme.AccentGreen
import com.gallery.image512.ui.theme.PrimaryDark
import com.gallery.image512.ui.theme.RobotoFontFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFrontCamera by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onClose()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    if (hasCameraPermission) {
        // Show preview after capture
        if (capturedImageUri != null && capturedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryDark)
            ) {
                // Image preview
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Top bar with title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Review Photo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoFontFamily,
                        color = Color.White
                    )
                }
                
                // Bottom action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Retake button
                    Button(
                        onClick = {
                            capturedImageUri = null
                            capturedBitmap = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retake",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retake",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoFontFamily
                        )
                    }
                    
                    // Accept button
                    Button(
                        onClick = {
                            onImageCaptured(capturedImageUri!!)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = PrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accept",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accept",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoFontFamily
                        )
                    }
                }
            }
        } else {
            // Camera preview mode
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
            var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
            var previewView: PreviewView? by remember { mutableStateOf(null) }
            
            LaunchedEffect(isFrontCamera) {
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        previewView?.let { view ->
                            it.setSurfaceProvider(view.surfaceProvider)
                        }
                    }
                
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                
                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Camera binding failed", e)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Camera preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Close button at top-left with better visibility
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(56.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Bottom controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spacer for balance
                    Spacer(modifier = Modifier.width(56.dp))
                    
                    // Capture button (center)
                    FloatingActionButton(
                        onClick = {
                            val photoFile = createImageFile(context)
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            
                            imageCapture?.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val savedUri = Uri.fromFile(photoFile)
                                        capturedImageUri = savedUri
                                        capturedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    }
                                    
                                    override fun onError(exc: ImageCaptureException) {
                                        Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                                    }
                                }
                            )
                        },
                        containerColor = AccentGreen,
                        contentColor = PrimaryDark,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Capture Photo",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    // Flip camera button (right)
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}
