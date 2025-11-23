package com.example.tucarnetapp.utils

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object FirebaseTokenHelper {

    /**
     * Obtiene el token de Firebase SIEMPRE actualizado
     * Firebase lo renueva automáticamente si está por expirar
     */
    suspend fun getValidToken(): String? {
        return try {
            val user = FirebaseAuth.getInstance().currentUser

            // false = usar token cacheado si aún es válido
            // true = forzar renovación (úsalo solo si ves errores de token expirado)
            val tokenResult = user?.getIdToken(false)?.await()

            tokenResult?.token
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Forzar renovación del token (rara vez necesario)
     */
    suspend fun forceRefreshToken(): String? {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            val tokenResult = user?.getIdToken(true)?.await() // true = forzar refresh
            tokenResult?.token
        } catch (e: Exception) {
            null
        }
    }
}