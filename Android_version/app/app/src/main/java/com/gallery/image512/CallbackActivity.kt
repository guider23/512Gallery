package com.gallery.image512

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.gallery.image512.data.auth.AppwriteConfig
import com.gallery.image512.data.auth.AuthManager
import io.appwrite.services.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("CallbackActivity", "=== OAUTH CALLBACK RECEIVED ===")
        android.util.Log.d("CallbackActivity", "Intent data: ${intent.data}")
        android.util.Log.d("CallbackActivity", "Intent action: ${intent.action}")
        android.util.Log.d("CallbackActivity", "Intent extras: ${intent.extras}")
        
        // Log the callback URL parameters
        intent.data?.let { uri ->
            android.util.Log.d("CallbackActivity", "Callback URI: $uri")
            android.util.Log.d("CallbackActivity", "Scheme: ${uri.scheme}")
            android.util.Log.d("CallbackActivity", "Host: ${uri.host}")
            android.util.Log.d("CallbackActivity", "Query params: ${uri.query}")
            
            // Check for success or error parameters
            val userId = uri.getQueryParameter("userId")
            val secret = uri.getQueryParameter("secret")
            val error = uri.getQueryParameter("error")
            
            android.util.Log.d("CallbackActivity", "userId: $userId")
            android.util.Log.d("CallbackActivity", "secret exists: ${secret != null}")
            android.util.Log.d("CallbackActivity", "error: $error")
        }
        
        // Check current session immediately after callback
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val account = Account(AppwriteConfig.getClient(applicationContext))
                android.util.Log.d("CallbackActivity", "Checking for session immediately after callback...")
                
                val sessions = account.listSessions()
                android.util.Log.d("CallbackActivity", "Sessions found: ${sessions.sessions.size}")
                sessions.sessions.forEach { session ->
                    android.util.Log.d("CallbackActivity", "  Session ID: ${session.id}, Provider: ${session.provider}")
                }
                
                val user = account.get()
                android.util.Log.d("CallbackActivity", "Current user: ${user.id}, Email: ${user.email}")
            } catch (e: Exception) {
                android.util.Log.e("CallbackActivity", "Error checking session: ${e.message}", e)
            }
            
            withContext(Dispatchers.Main) {
                // Create an intent to launch MainActivity
                val intent = Intent(this@CallbackActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("from_oauth", true)
                }
                android.util.Log.d("CallbackActivity", "Launching MainActivity with from_oauth flag")
                startActivity(intent)
                finish()
            }
        }
    }
}
