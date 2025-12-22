package com.gallery.image512.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class AuthManager(private val context: Context) {
    private val account = AppwriteConfig.getAccount(context)
    
    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }
    
    val userIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }
    
    val userEmailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }
    
    val isLoggedInFlow: Flow<Boolean> = userIdFlow.map { it != null }
    
    suspend fun login(email: String, password: String): Result<User<Map<String, Any>>> {
        return try {
            val session = account.createEmailPasswordSession(
                email = email,
                password = password
            )
            
            val user = account.get()
            saveUserData(user.id, user.email, user.name)
            
            Result.success(user)
        } catch (e: AppwriteException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun register(email: String, password: String, name: String): Result<User<Map<String, Any>>> {
        return try {
            // Create account
            val user = account.create(
                userId = "unique()",
                email = email,
                password = password,
                name = name
            )
            
            // Auto-login after registration
            login(email, password)
            
            Result.success(user)
        } catch (e: AppwriteException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            account.deleteSession("current")
            clearUserData()
            Result.success(Unit)
        } catch (e: AppwriteException) {
            clearUserData() // Clear local data even if server call fails
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(): User<Map<String, Any>>? {
        return try {
            android.util.Log.d("AuthManager", "Attempting to get current user...")
            val user = account.get()
            android.util.Log.d("AuthManager", "User found: ${user.id}, Email: ${user.email}, Name: ${user.name}")
            user
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "No current user session: ${e.message}")
            null
        }
    }
    
    suspend fun getUserId(): String? {
        return context.dataStore.data.first()[USER_ID_KEY]
    }
    
    suspend fun getUserEmail(): String? {
        return context.dataStore.data.first()[USER_EMAIL_KEY]
    }
    
    suspend fun getUserName(): String? {
        return context.dataStore.data.first()[USER_NAME_KEY]
    }
    
    suspend fun saveUserDataFromUser(user: User<Map<String, Any>>) {
        saveUserData(user.id, user.email, user.name)
    }
    
    private suspend fun saveUserData(userId: String, email: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_NAME_KEY] = name
        }
    }
    
    private suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
