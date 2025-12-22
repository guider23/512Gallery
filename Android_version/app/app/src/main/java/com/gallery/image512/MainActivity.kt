package com.gallery.image512

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gallery.image512.ui.screens.LoginScreen
import com.gallery.image512.ui.screens.MainScreen
import com.gallery.image512.ui.screens.RegisterScreen
import com.gallery.image512.ui.screens.WelcomeScreen
import com.gallery.image512.ui.theme.Image512GalleryTheme
import com.gallery.image512.ui.viewmodel.AuthViewModel
import com.gallery.image512.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private var sharedImageUris: List<Uri>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle shared images
        handleSharedImages(intent)
        
        enableEdgeToEdge()
        setContent {
            Image512GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(sharedImageUris = sharedImageUris)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedImages(intent)
    }
    
    private fun handleSharedImages(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                // Single image shared
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    sharedImageUris = listOf(uri)
                    android.util.Log.d("MainActivity", "Received shared image: $uri")
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                // Multiple images shared
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                    sharedImageUris = uris
                    android.util.Log.d("MainActivity", "Received ${uris.size} shared images")
                }
            }
        }
    }
}

@Composable
fun AppNavigation(sharedImageUris: List<Uri>? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    
    // Initialize AuthViewModel with context
    LaunchedEffect(Unit) {
        authViewModel.initAuth(context)
    }
    
    // Navigate to main when authenticated
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isLoading) {
            android.util.Log.d("MainActivity", "Auth state changed - isAuthenticated: ${authState.isAuthenticated}, route: ${navController.currentDestination?.route}")
            
            if (authState.isAuthenticated && navController.currentDestination?.route in listOf("welcome", "login", "register")) {
                android.util.Log.d("MainActivity", "Navigating to main screen")
                navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                }
            } else if (!authState.isAuthenticated && navController.currentDestination?.route == "main") {
                android.util.Log.d("MainActivity", "Navigating to welcome screen")
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onUseAnotherMethod = {
                    navController.navigate("login")
                }
            )
        }
        
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("main") {
            val mainViewModel: MainViewModel = viewModel()
            
            // Handle shared images
            LaunchedEffect(sharedImageUris) {
                sharedImageUris?.let { uris ->
                    android.util.Log.d("MainActivity", "Processing ${uris.size} shared images")
                    mainViewModel.uploadMultipleImages(uris)
                }
            }
            
            MainScreen(
                viewModel = mainViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
