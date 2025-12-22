package com.gallery.image512.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gallery.image512.ui.theme.BrillantFontFamily
import com.gallery.image512.ui.theme.Image512GalleryTheme
import com.gallery.image512.ui.theme.RobotoFontFamily
import com.gallery.image512.ui.theme.AccentGreen
import com.gallery.image512.ui.theme.PrimaryDark
import com.gallery.image512.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AnimatedWave(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF7FFF00)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val waveHeight = height * 0.4f
        
        val path = Path().apply {
            moveTo(0f, height)
            
            // Create wave effect
            for (x in 0..width.toInt()) {
                val normalizedX = x / width
                val y = height - waveHeight * 0.5f - 
                    (sin((normalizedX * 4 * PI + animatedOffset * PI / 180).toFloat()) * waveHeight * 0.3f).toFloat() -
                    (sin((normalizedX * 2 * PI - animatedOffset * PI / 180).toFloat()) * waveHeight * 0.2f).toFloat()
                lineTo(x.toFloat(), y)
            }
            
            lineTo(width, height)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.8f),
                    color.copy(alpha = 0.6f)
                )
            ),
            style = Fill
        )
    }
}

@Composable
fun WelcomeScreen(
    onLoginSuccess: () -> Unit,
    onUseAnotherMethod: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsState()
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.initAuth(context)
    }
    
    Image512GalleryTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryDark)
        ) {
            val screenHeight = maxHeight
            val topSectionHeight = screenHeight * 0.25f
            val bottomSectionTop = screenHeight * 0.75f
            
            // Animated wave at bottom
            AnimatedWave(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter),
                color = AccentGreen
            )
            
            // Show loading overlay when checking auth
            if (authState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Checking authentication...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontFamily = RobotoFontFamily
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Section - 512Gallery Title at 25% height
                Spacer(modifier = Modifier.height(topSectionHeight - 60.dp))
                
                // Logo/Title with Brillant font
                Text(
                    text = "512Gallery",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BrillantFontFamily,
                    color = Color.White,
                    letterSpacing = 0.sp
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom Section - Starting at 75% height
                // "A built by Kaniska" text with fancy font
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(
                            fontSize = 18.sp,
                            color = Color(0xFFCCCCCC),
                            fontWeight = FontWeight.Normal,
                            fontFamily = RobotoFontFamily
                        )) {
                            append("Built by ")
                        }
                        withStyle(style = SpanStyle(
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Cursive
                        )) {
                            append("𝒦𝒶𝓃𝒾𝓈𝓀𝒶")
                        }
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Login Button
                Button(
                    onClick = {
                        onUseAnotherMethod()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = PrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Login with mail",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RobotoFontFamily
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // "Check authentication details" button/text
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.recheckAuthWithDelay()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Check authentication details",
                        fontSize = 14.sp,
                        fontFamily = RobotoFontFamily,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Terms and Privacy at bottom
                Text(
                    text = "By continuing, you agree to our Terms and Privacy Policy",
                    fontSize = 12.sp,
                    fontFamily = RobotoFontFamily,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error message if any
                if (authState.error != null && !authState.isLoading) {
                    Text(
                        text = authState.error!!,
                        color = Color(0xFFFF4444),
                        fontSize = 14.sp,
                        fontFamily = RobotoFontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(300.dp)) // Space for wave
            }
        }
    }
}
