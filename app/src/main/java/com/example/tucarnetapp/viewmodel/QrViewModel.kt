package com.example.tucarnetapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.session.QRPreferences
import com.example.tucarnetapp.utils.FirebaseTokenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class QRState {
    object Initial : QRState()
    object Loading : QRState()
    data class Success(
        val qrBase64: String,
        val jwt: String,
        val expiresIn: Int,
        val isNew: Boolean
    ) : QRState()
    data class Error(val message: String) : QRState()
}

class QRViewModel(application: Application) : AndroidViewModel(application) {

    private val qrPrefs = QRPreferences.getInstance(application)

    private val _qrState = MutableStateFlow<QRState>(QRState.Initial)
    val qrState: StateFlow<QRState> = _qrState.asStateFlow()

    private val _timeRemaining = MutableStateFlow(0L)
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()

    /**
     * Carga el QR (desde cache o generando uno nuevo)
     */
    fun loadQR(studentCode: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Si no es refresh forzado y hay QR válido, usar ese
            if (!forceRefresh && qrPrefs.isQRValid()) {
                loadFromCache()

                // Si necesita renovación, hacerlo silenciosamente
                if (qrPrefs.needsRenewal()) {
                    generateNewQR(studentCode, silent = true)
                }
                return@launch
            }

            // Generar nuevo QR
            generateNewQR(studentCode, silent = false)
        }
    }

    /**
     * Carga el QR desde cache
     */
    private fun loadFromCache() {
        val qrBase64 = qrPrefs.getQRBase64()
        val jwt = qrPrefs.getJWT()
        val remaining = qrPrefs.getTimeRemaining().toInt()

        if (qrBase64 != null && jwt != null) {
            _qrState.value = QRState.Success(
                qrBase64 = qrBase64,
                jwt = jwt,
                expiresIn = remaining,
                isNew = false
            )
            _timeRemaining.value = remaining.toLong()
        }
    }

    /**
     * Genera un nuevo QR desde la API
     */
    private suspend fun generateNewQR(studentCode: String, silent: Boolean) {
        if (!silent) {
            _qrState.value = QRState.Loading
        }

        try {
            // Obtener token de Firebase
            val firebaseToken = FirebaseTokenHelper.getValidToken()

            if (firebaseToken == null) {
                _qrState.value = QRState.Error("No autenticado")
                return
            }

            // Llamar a la API
            val response = ApiClient.qrApi.generateQr(
                mapOf("student_code" to studentCode)
            )

            if (response.isSuccessful && response.body() != null) {
                val qrResponse = response.body()!!

                // Guardar en cache
                qrPrefs.saveQR(
                    qrBase64 = qrResponse.qr,
                    jwt = qrResponse.jwt,
                    expiresIn = qrResponse.expiresIn,
                    studentCode = studentCode
                )

                // Actualizar estado
                _qrState.value = QRState.Success(
                    qrBase64 = qrResponse.qr,
                    jwt = qrResponse.jwt,
                    expiresIn = qrResponse.expiresIn,
                    isNew = qrResponse.isNew
                )
                _timeRemaining.value = qrResponse.expiresIn.toLong()

            } else {
                if (!silent) {
                    _qrState.value = QRState.Error(
                        "Error al generar QR: ${response.message()}"
                    )
                }
            }

        } catch (e: Exception) {
            if (!silent) {
                _qrState.value = QRState.Error(
                    "Error de conexión: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualiza el contador de tiempo restante
     */
    fun updateTimeRemaining() {
        _timeRemaining.value = qrPrefs.getTimeRemaining()
    }

    /**
     * Fuerza la renovación del QR
     */
    fun forceRefresh(studentCode: String) {
        loadQR(studentCode, forceRefresh = true)
    }

    /**
     * Limpia el QR del cache
     */
    fun clearQR() {
        qrPrefs.clearQR()
        _qrState.value = QRState.Initial
        _timeRemaining.value = 0
    }
}