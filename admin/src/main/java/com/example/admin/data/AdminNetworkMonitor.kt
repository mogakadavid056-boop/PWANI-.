package com.example.admin.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdminConnectivityCheck {
  fun isOnlineNow(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
  }
}

class AdminNetworkMonitor(context: Context) {
  private val appContext = context.applicationContext
  private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
  private val _isOnline = MutableStateFlow(AdminConnectivityCheck.isOnlineNow(appContext))
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private var registered = false

  private val callback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      _isOnline.value = AdminConnectivityCheck.isOnlineNow(appContext)
    }

    override fun onLost(network: Network) {
      _isOnline.value = AdminConnectivityCheck.isOnlineNow(appContext)
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
      _isOnline.value = AdminConnectivityCheck.isOnlineNow(appContext)
    }
  }

  init {
    runCatching {
      cm?.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
      registered = true
    }
  }

  fun refresh() {
    _isOnline.value = AdminConnectivityCheck.isOnlineNow(appContext)
  }

  fun stop() {
    if (registered) {
      runCatching { cm?.unregisterNetworkCallback(callback) }
      registered = false
    }
  }
}
