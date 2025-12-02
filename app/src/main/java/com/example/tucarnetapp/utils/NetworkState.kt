package com.example.tucarnetapp.utils

/**
 * Representa los diferentes estados de la conexión a internet
 */
sealed class NetworkState {
    object Available : NetworkState()      // Hay internet
    object Unavailable : NetworkState()    // No hay internet
    object Lost : NetworkState()           // Se perdió la conexión
    object Connecting : NetworkState()     // Conectando...
}