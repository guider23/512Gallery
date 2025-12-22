package com.gallery.image512.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.image512.data.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val userEmail: String? = null,
    val userName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private lateinit var authManager: AuthManager
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    fun initAuth(context: Context) {
        authManager = AuthManager(context)
        checkAuthState()
    }
    
    fun recheckAuthWithDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            checkAuthState()
        }
    }
    
    private fun checkAuthState() {
        android.util.Log.d("AuthViewModel", "=== checkAuthState() START ===")
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            android.util.Log.d("AuthViewModel", "Auth state set to loading")
            
            try {
                // Try to get current user session from Appwrite
                android.util.Log.d("AuthViewModel", "Calling authManager.getCurrentUser()...")
                val user = authManager.getCurrentUser()
                
                if (user != null && user.email.isNotEmpty()) {
                    android.util.Log.d("AuthViewModel", "✅ USER FOUND! ID: ${user.id}, Email: ${user.email}, Name: ${user.name}")
                    // User is logged in, save their data
                    authManager.saveUserDataFromUser(user)
                    val userId = authManager.getUserId()
                    val userEmail = authManager.getUserEmail()
                    val userName = authManager.getUserName()
                    
                    android.util.Log.d("AuthViewModel", "Saved user data - ID: $userId, Email: $userEmail, Name: $userName")
                    
                    _authState.value = AuthState(
                        isAuthenticated = true,
                        userId = userId,
                        userEmail = userEmail,
                        userName = userName,
                        isLoading = false
                    )
                    android.util.Log.d("AuthViewModel", "✅ Auth state updated - isAuthenticated: true")
                } else {
                    // No active session or user has no email (guest/anonymous)
                    android.util.Log.w("AuthViewModel", "❌ NO VALID AUTHENTICATED USER FOUND")
                    _authState.value = AuthState(
                        isAuthenticated = false,
                        userId = null,
                        userEmail = null,
                        userName = null,
                        isLoading = false
                    )
                    android.util.Log.d("AuthViewModel", "Auth state updated - isAuthenticated: false")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ ERROR checking auth state: ${e.message}", e)
                _authState.value = AuthState(
                    isAuthenticated = false,
                    userId = null,
                    userEmail = null,
                    userName = null,
                    isLoading = false,
                    error = e.message
                )
            }
            android.util.Log.d("AuthViewModel", "=== checkAuthState() END ===")
        }
    }
    
    suspend fun login(email: String, password: String): Result<Unit> {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        return try {
            val result = authManager.login(email, password)
            
            result.onSuccess {
                // Refresh auth state after successful login
                checkAuthState()
            }.onFailure { error ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
            
            result.map { Unit }
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    suspend fun register(email: String, password: String, name: String): Result<Unit> {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        return try {
            val result = authManager.register(email, password, name)
            
            result.onSuccess {
                // Refresh auth state after successful registration
                checkAuthState()
            }.onFailure { error ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
            
            result.map { Unit }
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            
            try {
                authManager.logout()
                _authState.value = AuthState(
                    isAuthenticated = false,
                    userId = null,
                    userEmail = null,
                    userName = null,
                    isLoading = false
                )
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}
