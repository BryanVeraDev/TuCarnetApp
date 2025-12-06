package com.example.tucarnetapp.data.repository

import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.UpdateBiometricRequest
import com.example.tucarnetapp.data.remote.dto.UpdateBiometricResponse

class StudentRepository {

    private val api = ApiClient.studentApi
    /**
     * Actualiza el perfil biométrico del estudiante
     */
    suspend fun updateBiometricProfile(
        studentId: String,
        cardPhotoKey: String,
        similarity: Double
    ): UpdateBiometricResponse {

        val request = UpdateBiometricRequest(
            student_id = studentId,
            card_photo_key = cardPhotoKey,
            similarity = similarity
        )

        return api.updateBiometricProfile(request)
    }
}
