package com.example.tucarnetapp.session

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Gestiona la persistencia del QR usando SharedPreferences
 * El QR se mantiene incluso después de cerrar la app
 */
class QRPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "qr_preferences"
        private const val KEY_QR_BASE64 = "qr_base64"
        private const val KEY_JWT = "qr_jwt"
        private const val KEY_EXPIRES_AT = "qr_expires_at"
        private const val KEY_STUDENT_CODE = "qr_student_code"

        @Volatile
        private var INSTANCE: QRPreferences? = null

        fun getInstance(context: Context): QRPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QRPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Guarda el QR y sus datos
     */
    fun saveQR(
        qrBase64: String,
        jwt: String,
        expiresIn: Int,
        studentCode: String
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

        prefs.edit().apply {
            putString(KEY_QR_BASE64, qrBase64)
            putString(KEY_JWT, jwt)
            putLong(KEY_EXPIRES_AT, expiresAt)
            putString(KEY_STUDENT_CODE, studentCode)
            apply()
        }
    }

    /**
     * Obtiene el QR en base64
     */
    fun getQRBase64(): String? = prefs.getString(KEY_QR_BASE64, null)

    /**
     * Obtiene el JWT del QR
     */
    fun getJWT(): String? = prefs.getString(KEY_JWT, null)

    /**
     * Obtiene el código del estudiante
     */
    fun getStudentCode(): String? = prefs.getString(KEY_STUDENT_CODE, null)

    /**
     * Verifica si el QR existe y es válido
     */
    fun isQRValid(): Boolean {
        val qr = getQRBase64()
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)

        if (qr == null || expiresAt == 0L) {
            return false
        }

        return System.currentTimeMillis() < expiresAt
    }

    /**
     * Verifica si el QR necesita renovarse (menos de 5 minutos)
     */
    fun needsRenewal(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        if (expiresAt == 0L) return true

        val fiveMinutes = 5 * 60 * 1000L
        val timeRemaining = expiresAt - System.currentTimeMillis()

        return timeRemaining < fiveMinutes
    }

    /**
     * Obtiene el tiempo restante en segundos
     */
    fun getTimeRemaining(): Long {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        if (expiresAt == 0L) return 0

        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining else 0
    }

    /**
     * Convierte el QR base64 a Bitmap
     */
    fun getQRBitmap(): Bitmap? {
        val base64 = getQRBase64() ?: return null
        return base64ToBitmap(base64)
    }

    /**
     * Limpia todos los datos del QR
     */
    fun clearQR() {
        prefs.edit().apply {
            remove(KEY_QR_BASE64)
            remove(KEY_JWT)
            remove(KEY_EXPIRES_AT)
            remove(KEY_STUDENT_CODE)
            apply()
        }
    }

    /**
     * Verifica si hay un QR guardado
     */
    fun hasQR(): Boolean = getQRBase64() != null

    // Helper para convertir base64 a Bitmap
    private fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val pureBase64 = if (base64.contains("base64,")) {
                base64.substringAfter("base64,")
            } else {
                base64
            }

            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}