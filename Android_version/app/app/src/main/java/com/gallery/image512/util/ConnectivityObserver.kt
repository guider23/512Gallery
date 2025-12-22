package com.gallery.image512.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface ConnectivityObserver {
    fun observe(): Flow<Status>
    fun isConnected(): Boolean
    
    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}

class NetworkConnectivityObserver(
    private val context: Context
) : ConnectivityObserver {
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    override fun observe(): Flow<ConnectivityObserver.Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(ConnectivityObserver.Status.Available)
            }
            
            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                trySend(ConnectivityObserver.Status.Losing)
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(ConnectivityObserver.Status.Lost)
            }
            
            override fun onUnavailable() {
                super.onUnavailable()
                trySend(ConnectivityObserver.Status.Unavailable)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Send initial state
        trySend(
            if (isConnected()) ConnectivityObserver.Status.Available 
            else ConnectivityObserver.Status.Unavailable
        )
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    override fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
