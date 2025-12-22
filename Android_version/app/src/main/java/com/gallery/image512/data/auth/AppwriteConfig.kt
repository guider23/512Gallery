package com.gallery.image512.data.auth

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases

object AppwriteConfig {
    const val ENDPOINT = "https://nyc.cloud.appwrite.io/v1"
    const val PROJECT_ID = "6919d044000ebf7aad07"
    const val DATABASE_ID = "691ac3620010400be8f9"
    const val COLLECTION_ID = "images"
    
    private var client: Client? = null
    
    fun getClient(context: Context): Client {
        if (client == null) {
            android.util.Log.d("AppwriteConfig", "Creating new Appwrite Client instance")
            client = Client(context.applicationContext)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)
            android.util.Log.d("AppwriteConfig", "Client created with endpoint: $ENDPOINT, project: $PROJECT_ID")
        } else {
            android.util.Log.d("AppwriteConfig", "Reusing existing Appwrite Client instance")
        }
        return client!!
    }
    
    fun getAccount(context: Context): Account {
        return Account(getClient(context))
    }
    
    fun getDatabase(context: Context): Databases {
        return Databases(getClient(context))
    }
}
