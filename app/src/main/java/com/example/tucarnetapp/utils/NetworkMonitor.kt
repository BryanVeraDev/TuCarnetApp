package com.example.tucarnetapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitor de conectividad de red
 * Detecta cambios en la conexión a internet en tiempo real
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    companion object {
        private const val TAG = "NetworkMonitor"
    }

    /**
     * Verifica si hay conexión a internet en este momento
     * @return true si hay conexión válida, false si no
     */
    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.run {
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    /**
     * Verifica el tipo de conexión actual
     * @return "WiFi", "Móvil", "Ethernet" o "Sin conexión"
     */
    fun getConnectionType(): String {
        val network = connectivityManager.activeNetwork ?: return "Sin conexión"
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return "Sin conexión"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Móvil"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Desconocido"
        }
    }

    /**
     * Flow que emite cambios en la conectividad
     * Puedes observarlo en Activities/Fragments con lifecycleScope
     */
    fun observeConnectivity(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                Log.d(TAG, "✅ Red disponible: $network")
                trySend(NetworkState.Available)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "❌ Red perdida: $network")
                trySend(NetworkState.Lost)
            }

            override fun onUnavailable() {
                Log.d(TAG, "⚠️ Red no disponible")
                trySend(NetworkState.Unavailable)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                Log.d(TAG, "🔄 Capacidades cambiadas - Internet: $hasInternet, Validado: $isValidated")

                if (hasInternet && isValidated) {
                    trySend(NetworkState.Available)
                } else {
                    trySend(NetworkState.Unavailable)
                }
            }
        }

        // Registrar el callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emitir estado inicial
        if (isConnected()) {
            trySend(NetworkState.Available)
        } else {
            trySend(NetworkState.Unavailable)
        }

        // Cleanup cuando se cierra el flow
        awaitClose {
            Log.d(TAG, "🛑 Desregistrando NetworkCallback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() // Solo emite cuando el estado cambia
}