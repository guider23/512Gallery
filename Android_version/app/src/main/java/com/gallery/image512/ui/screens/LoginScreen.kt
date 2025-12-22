package com.gallery.image512.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
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
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Section - 512Gallery Title at 25% height
                Spacer(modifier = Modifier.height(topSectionHeight - 60.dp))
                
                Text(
                    text = "512Gallery",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BrillantFontFamily,
                    color = Color.White,
                    letterSpacing = 0.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Sign in to continue",
                    fontSize = 16.sp,
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF999999),
                    letterSpacing = 0.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    label = { 
                        Text(
                            "Email", 
                            color = Color(0xFF999999), 
                            fontSize = 14.sp,
                            fontFamily = RobotoFontFamily
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color(0xFF999999)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7FFF00),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF7FFF00)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = RobotoFontFamily,
                        fontSize = 16.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    label = { 
                        Text(
                            "Password", 
                            color = Color(0xFF999999), 
                            fontSize = 14.sp,
                            fontFamily = RobotoFontFamily
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = Color(0xFF999999)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF999999)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7FFF00),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF7FFF00)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = RobotoFontFamily,
                        fontSize = 16.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
                
                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF4444),
                        fontSize = 14.sp,
                        fontFamily = RobotoFontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }
                
                // Spacer to push bottom content to 75% height
                Spacer(modifier = Modifier.height((bottomSectionTop - topSectionHeight - 400.dp).coerceAtLeast(40.dp)))
                
                // Bottom Section - Starting at 75% height
                // "Built by Kaniska" text
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
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all fields"
                            return@Button
                        }
                        
                        isLoading = true
                        scope.launch {
                            val result = viewModel.login(email, password)
                            isLoading = false
                            
                            result.onSuccess {
                                onLoginSuccess()
                            }.onFailure { error ->
                                errorMessage = error.message ?: "Login failed"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = PrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoFontFamily
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Check Authentication Details Button
                TextButton(
                    onClick = { /* Add check auth logic */ },
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
                
                // Privacy Policy Text
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
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Register Link
                Row(
                    modifier = Modifier.padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = Color(0xFF999999),
                        fontSize = 14.sp,
                        fontFamily = RobotoFontFamily
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Sign Up",
                            color = Color(0xFF7FFF00),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RobotoFontFamily
                        )
                    }
                }
            }
        }
    }
}
