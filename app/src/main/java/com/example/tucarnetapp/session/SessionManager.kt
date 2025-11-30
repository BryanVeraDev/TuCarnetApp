package com.example.tucarnetapp.session

import android.content.Context
import android.content.SharedPreferences
import com.example.tucarnetapp.data.remote.dto.StudentResponse
import com.google.gson.Gson

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Guarda el estudiante en disco
     */
    fun saveStudent(student: StudentResponse) {
        val json = gson.toJson(student)
        prefs.edit().apply {
            putString("student_data", json)
            putBoolean("is_logged_in", true)
            putLong("last_login", System.currentTimeMillis())
            apply()
        }
        UserSession.setUser(student)
    }

    /**
     * Recupera el estudiante del disco
     */
    fun getStudent(): StudentResponse? {
        val json = prefs.getString("student_data", null) ?: return null
        return try {
            gson.fromJson(json, StudentResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifica si hay sesión activa
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    /**
     * Verifica si la sesión expiró (30 días)
     */
    fun isSessionExpired(): Boolean {
        val lastLogin = prefs.getLong("last_login", 0)
        if (lastLogin == 0L) return true

        val thirtyDays = 30L * 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - lastLogin) > thirtyDays
    }

    /**
     * Actualiza el timestamp de último acceso
     */
    fun updateLastAccess() {
        prefs.edit().putLong("last_login", System.currentTimeMillis()).apply()
    }

    /**
     * Verifica si los datos están completos
     */
    fun hasCompleteData(): Boolean {
        val student = getStudent()
        return student != null && !student.student_code.isNullOrEmpty()
    }

    /**
     * Limpia toda la sesión
     */
    fun clearSession() {
        prefs.edit().clear().apply()
        UserSession.clear()
    }
}