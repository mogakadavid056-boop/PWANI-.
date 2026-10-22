package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkMonitor {
  private val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private var isInitialized = false
  private var onReconnectListener: (() -> Unit)? = null

  fun initialize(context: Context) {
    if (isInitialized) return
    isInitialized = true

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (connectivityManager == null) {
      _isOnline.value = true
      return
    }

    // Initial check
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    val currentlyOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    _isOnline.value = currentlyOnline

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    connectivityManager.registerNetworkCallback(
      request,
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          val wasOffline = !_isOnline.value
          _isOnline.value = true
          if (wasOffline) {
            onReconnectListener?.invoke()
          }
        }

        override fun onLost(network: Network) {
          // Verify if another network is available
          val active = connectivityManager.activeNetwork
          val cap = connectivityManager.getNetworkCapabilities(active)
          val stillOnline = cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
          _isOnline.value = stillOnline
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
          val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          val wasOffline = !_isOnline.value
          _isOnline.value = hasInternet
          if (hasInternet && wasOffline) {
            onReconnectListener?.invoke()
          }
        }
      }
    )
  }

  fun setOnReconnectListener(listener: (() -> Unit)?) {
    onReconnectListener = listener
  }

  fun checkOnlineNow(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val active = cm.activeNetwork ?: return false
    val cap = cm.getNetworkCapabilities(active) ?: return false
    return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
