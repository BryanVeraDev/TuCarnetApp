package com.example.tucarnetapp.ui.home

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tucarnetapp.R
import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.AuthRequest
import com.example.tucarnetapp.session.SessionManager
import com.example.tucarnetapp.session.UserSession
import com.example.tucarnetapp.ui.home.HomeScreenActivity
import com.example.tucarnetapp.ui.home.LoadingActivity
import com.example.tucarnetapp.ui.terms.TermsConditionsActivity
import com.example.tucarnetapp.utils.FirebaseTokenHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 0L // 0 segundos
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sessionManager = SessionManager.getInstance(this)

        lifecycleScope.launch {
            delay(SPLASH_DELAY) // Mostrar splash 2 segundos
            checkSession()
        }
    }

    /**
     * 🎯 NÚCLEO DEL SISTEMA
     * Verifica si hay sesión y decide a dónde redirigir
     */
    private suspend fun checkSession() {
        val firebaseUser = auth.currentUser

        Log.d(TAG, "🔍 Verificando sesión...")
        Log.d(TAG, "Firebase User: ${firebaseUser?.email}")
        Log.d(TAG, "SessionManager: ${sessionManager.isLoggedIn()}")

        when {
            // Caso 1: No hay usuario en Firebase → Login
            firebaseUser == null -> {
                Log.d(TAG, "❌ No hay usuario Firebase → Login")
                goToLogin()
            }

            // Caso 2: Sesión expirada (más de 30 días) → Limpiar y reautenticar
            sessionManager.isSessionExpired() -> {
                Log.d(TAG, "⏰ Sesión expirada → Reautenticar")
                sessionManager.clearSession()
                reAuthenticate()
            }

            // Caso 3: Hay sesión guardada → Restaurar
            sessionManager.isLoggedIn() && sessionManager.hasCompleteData() -> {
                Log.d(TAG, "✅ Sesión válida → Restaurando")
                restoreSession()
            }

            // Caso 4: Usuario Firebase pero sin sesión local → Reautenticar
            else -> {
                Log.d(TAG, "🔄 Usuario Firebase sin sesión local → Reautenticar")
                reAuthenticate()
            }
        }
    }

    /**
     * Restaura la sesión desde SharedPreferences
     */
    private suspend fun restoreSession() {
        val student = sessionManager.getStudent()

        if (student != null) {
            Log.d(TAG, "📦 Sesión restaurada: ${student.email}")

            // Actualizar último acceso
            sessionManager.updateLastAccess()

            // Restaurar en memoria
            UserSession.setUser(student)

            // Redirigir según estado biométrico
            redirectBasedOnStatus(student.biometric_profile?.status)
        } else {
            Log.e(TAG, "❌ Error al restaurar sesión")
            reAuthenticate()
        }
    }

    /**
     * Re-autentica con el backend usando Firebase token
     */
    private suspend fun reAuthenticate() {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            goToLogin()
            return
        }

        try {
            Log.d(TAG, "🔐 Obteniendo token Firebase...")
            val firebaseToken = FirebaseTokenHelper.getValidToken()

            if (firebaseToken == null) {
                Log.e(TAG, "❌ No se pudo obtener token")
                goToLogin()
                return
            }

            Log.d(TAG, "📡 Llamando al backend...")
            val request = AuthRequest(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: ""
            )

            val response = ApiClient.authApi.login(
                authHeader = "Bearer $firebaseToken",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val student = response.body()!!

                Log.d(TAG, "✅ Reautenticación exitosa")

                // Guardar sesión
                sessionManager.saveStudent(student)

                // Redirigir
                redirectBasedOnStatus(student.biometric_profile?.status)
            } else {
                Log.e(TAG, "❌ Error backend: ${response.message()}")
                goToLogin()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            goToLogin()
        }
    }

    /**
     * Redirige según el estado biométrico
     */
    private fun redirectBasedOnStatus(status: String?) {
        val intent = when (status) {
            "APROBADO" -> {
                Log.d(TAG, "✅ Usuario aprobado → MainActivity")
                Intent(this, LoadingActivity::class.java)
            }
            "PENDIENTE", "RECHAZADO" -> {
                Log.d(TAG, "⏳ Verificación pendiente/rechazada → Terms")
                Intent(this, TermsConditionsActivity::class.java)
            }
            else -> {
                Log.d(TAG, "❓ Estado desconocido → Terms")
                Intent(this, TermsConditionsActivity::class.java)
            }
        }

        // Limpiar stack de navegación
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Va a la pantalla de login
     */
    private fun goToLogin() {
        Log.d(TAG, "🔑 Redirigiendo a Login")
        val intent = Intent(this, HomeScreenActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}